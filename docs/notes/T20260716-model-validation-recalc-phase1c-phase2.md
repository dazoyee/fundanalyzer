# タスクノート: モデル検証意味論への統一（係数一括再計算・フェーズ1c・フェーズ2）

関連調査: [T20260713-always-compute-redesign-investigation.md](T20260713-always-compute-redesign-investigation.md) /
[T20260715-always-compute-phase1a.md](T20260715-always-compute-phase1a.md)

## 背景と方針決定

V0.4.0 の業種係数変更が過去の `analysis_result` 行へ遡及しないため、算出値に新旧係数のばらつきが存在する。
調査ノートは「バックテスト再現性のため凍結維持＋係数バージョニング」を推奨していたが、認識合わせの結果、
前提を見直して次の通り決定した（AskUserQuestion で確認済み）。

- **バックテストの意味論は「(B) モデル検証」に統一する**。「今の係数のモデルは過去に対して有効だったか」を
  答えさせる。判定履歴の生データ（凍結株価・日付）は `valuation` に残るため、失うのは
  「当時表示していた理論株価」の監査性のみと整理した
- 根拠: 判定入力の割引率は `valuation` 側に凍結済みで、`analysis_result` の凍結が効くのは収束度計算のみ。
  個人用途で係数チューニングを継続する運用では、係数変更のたびに過去分を現行係数で再計算して
  全期間を単一係数で一貫させる方が目的に合う
- これに伴い**係数バージョニング（スナップショット列/バージョンID）のタスクは不要となる**
- 実現方式は**係数変更時の一括再計算バッチ**（per-request 都度計算は性能上不利と調査済みのため不採用）
- `valuation` の係数依存列（`discount_value`・`discount_rate`）も再計算対象に含める
  （含めないと再計算後 corporate_value × 旧係数の割引率が混在し、収束度が非一貫になるため）
- 併せてフェーズ1c（指標4列 DROP COLUMN）・フェーズ2（investment_indicator 読取切替＋書込停止）も
  worktree 分離で並行実装する（実行主体は Claude worktree サブエージェント。Codex 委譲ルールの明示切替）
- 本番 DB の過去分再計算は本番リリース後に管理エンドポイントから実行する（バックアップ取得を手順化）

## ストリームA: 係数一括再計算バッチ

### 設計

- ユースケース追加の鉄則に従い `AnalyzeUseCase` に `recalculate()` を追加し `AnalyzeInteractor` に実装
  （企業価値算出のドメインなので新規ユースケース対ではなく既存対に追加する）
- エンドポイントは撤去済み P3 のパターン（`git show 25e6e238` の削除差分）を踏襲:
  - `GET /v1/admin/analysis/recalculate/preview` — 対象件数（analysis_result 全件・valuation 全件）の事前確認
  - `POST /v1/admin/analysis/recalculate` — 実行。既存管理系と同じく同期実行・`/v3/index` へのリダイレクト＋件数メッセージ
- 処理フロー:
  1. `analysis_result` 全行を走査し、行ごとに `documentId` → `Document` → `FinancialStatementSpecification.getFinanceValue(document)`、
     `companySpecification.findCompanyByEdinetCode(...)` → `industrySpecification.resolveCoefficient(industryId)` で入力を再解決し、
     `new AnalysisResult(financeValue, document, coefficient)` で corporate_value / rim_value を再計算 → 値が変わる行のみ UPDATE
  2. `valuation` は集合演算で一括更新（Java ループ不要）:
     `discount_value = ar.corporate_value - v.stock_price`、`discount_rate = ROUND(ar.corporate_value / v.stock_price, 2)`
     を `analysis_result_id` JOIN の UPDATE 1文で適用（現行 `ValuationSpecification.evaluate()` の式と同一。
     `stock_price` は評価時点の凍結値のため再取得不要。丸めは HALF_UP と同挙動になることをテストで担保）
  3. キャッシュ整合: 完了時に `backtest` キャッシュを明示 evict（現状 `@CacheEvict` はコードベースに存在しないため新設）。
     `industryList` キャッシュは実行冒頭に `findIndustryList()`（`@CachePut`）で更新してから係数解決する
  4. `corporate_view` / `valuation_view` の再生成（既存 `ViewService.updateCorporateView()` / `updateValuationView()`）まで
     連鎖させ、1操作で画面まで一貫させる
- 入力欠損（財務諸表値なし・企業マスタなし等）の行は warn ログ＋スキップして継続（`parallelUpdateView` の例外方針を踏襲）。
  結果は success / skipped / failed 件数をログ出力（旧 `IndicatorBackfillResult` パターン）
- トランザクション境界は既存の流儀（1 DAO 呼び出し単位コミット）を踏襲。バッチは冪等
  （再実行しても同じ係数なら同じ値に収束する）ため途中失敗時は再実行で回復する

### 変更ファイル（新設含む）

| 対象 | 変更 |
|---|---|
| `AnalysisResultDao` | `selectAll`（または走査用 select）と `updateCorporateValueAndRimValue(id, corporateValue, rimValue)` を新設（param ベースにしてエンティティ形状への依存を避け、ストリームBとの競合を最小化） |
| `ValuationDao` | 集合 UPDATE `updateDerivedValuesFromAnalysisResult()` を新設（Doma の `@Update(sqlFile = true)`） |
| `AnalysisResultSpecification` / `ValuationSpecification` | 再計算の仲介メソッド追加 |
| `AnalyzeUseCase` / `AnalyzeInteractor` | `recalculate()` 追加 |
| `AnalysisController` | preview / 実行エンドポイント追加 |
| `BacktestInteractor` または `CacheConfig` 近傍 | `backtest` キャッシュの evict 手段新設 |

## ストリームB: フェーズ1c（analysis_result の指標4列 DROP COLUMN）

### 設計

- 新規マイグレーション `V0.4.2__drop_analysis_result_indicator_columns.sql`（既存ファイルは編集しない）。
  H2（dev/test）互換のため DROP COLUMN は1文ずつ4文に分ける
- `AnalysisResultEntity`: `bps`/`eps`/`roe`/`roa` フィールド・getter を削除し 10 引数構成へ。
  旧 13/14 引数コンストラクタの呼び出し元（本体はフィールド定義以外に実質参照ゼロ、
  テストは 13 ファイル・約 45 箇所）を一括で新シグネチャに置換する。
  RIM 時の「後方互換コンストラクタ」パターンは採らない（残すと null 渡し箇所が恒久化するため）
- `AnalysisResultSpecification.insert` の null 固定 4 引数と `AnalysisResultEntity.of(...)` の該当引数を除去
  （Doma の INSERT はエンティティから自動生成のため SQL ファイル修正は不要）
- 追随修正（dev 環境の起動失敗に直結するため必須）:
  - `src/main/resources/schema.sql` の `analysis_result` 定義から4列を削除（Flyway と別管理のため手動追随）
  - dev シード `V1.0.1__dev_seed_screen_test.sql` / `V1.0.3__dev_seed_split_scenario.sql` / `V1.0.4__dev_seed_backtest.sql` の
    `analysis_result` INSERT 列リストから4列を削除（`corporate_view` 側の同名列は別テーブルのため対象外）

### 1b 本番フルバッチサイクル一巡の確認手順（DROP COLUMN リリース前の必須ゲート）

1a/1b は v2.3.15 で本番反映済み（反映記録あり）。以下で「反映後にスケジューラが問題なく一巡した」ことを確認する。

1. 本番ログ（ECS → Elastic）で `Category.SCHEDULER` の BEGINNING/END ペアを反映日以降の日付で確認する:
   `analysisScheduler`（`app.scheduler.hour.analysis` 時台）・`recoverDocumentPeriodScheduler`・`updateViewScheduler` の3本が
   すべて正常 END していること
2. Slack の `notice.error` テンプレート通知（スケジューラ異常時のみ送信）が反映日以降届いていないこと
3. 画面確認: `/v3/index`・企業詳細で指標（BPS/EPS/ROE/ROA・PER/PBR）が表示されていること
   （1a の都度計算経路が本番データで機能している傍証）
4. DB 確認: 反映日以降に INSERT された `analysis_result` 行の4列が NULL であること（1b の書き込み停止の実証）

**本番への 1c リリース（DROP COLUMN 実行）はこの確認完了後に Gate 2 で判断する。**
DROP COLUMN は不可逆のため、実装・マージと本番リリース判断を分離する。

## ストリームC: フェーズ2（investment_indicator 読取切替＋書込停止）

### 設計

読み取りを「`stock_price`（実測・履歴） × 都度計算 bps/eps」に切り替え、`investment_indicator` への書き込みを停止する。
約300万件の既存行は残置（列削除 2c は本番確認後の後続タスク）。

- **共通計算ロジック**: 「対象日 `targetDate` に対し `submitDate <= targetDate` で最新の `AnalysisResultEntity` を選び、
  その document の `FinanceValue` から都度計算した bps/eps と調整後株価で `IndicatorValue` を構築する」突合を
  ドメインサービスとして新設し、以下 3 経路で共用する
- **詳細画面チャート**（`ViewCorporateInteractor.viewCorporateDetailRaw`）:
  `investmentIndicatorSpecification.findIndicatorValueList(code)` を廃し、
  取得済みの `stockSpecification.findEntityList(code)`（既存呼び出しあり）× `displayTargetList`（既存）×
  書類ごとの `getFinanceValue`（対象書類数分の小ループ。日次件数比で十分小さい）でメモリ内計算に置換。
  株価は `corporateActionSpecification.adjustToBasis` で書込時と同じ調整を適用する
- **corporate_view 生成**（`ViewSpecification.generateCorporateView`）:
  per/pbr/grahamIndex 引数を「最新株価 × 都度計算値」由来に差し替え（bps/eps/roe/roa は 1a で切替済み、同じ形に揃える）
- **valuation 評価**（`ValuationSpecification.evaluate`）:
  `investment_indicator` からの `graham_index` コピーを廃し、調整後株価＋都度計算 bps/eps から直接算出。
  `investment_indicator_id` は書込停止に伴い null を保存（列は残置。エンティティは既に null 許容）
- **書込停止**（`AnalyzeInteractor.indicate`）:
  提出日〜最新株価日を1日ずつ埋める挿入ループ（約300万件の発生源）と「直近算出済み日」判定を除去。
  `InvestmentIndicatorSpecification.insert` と `InvestmentIndicatorDao.insert` は到達不能になるため撤去
- **応答時間の実測**: dev（シードデータ）＋可能なら本番相当件数で企業詳細ページの表示時間を切替前後で計測し、
  ノートに記録する。悪化が顕著な場合は企業コード単位の Caffeine キャッシュ（TTL 短め）を緩和策として検討
- valuation の既存 `graham_index` 列・`valuation_view.graham_index`・zスコア/分布計算（下流）は
  値の供給元が変わるだけでスキーマ・参照コードは無変更

## worktree 分割と統合順序

| ストリーム | ブランチ | 主な競合面 |
|---|---|---|
| A 再計算バッチ | `feature/coefficient-recalculation-batch` | `AnalyzeInteractor`・`AnalysisController`・DAO |
| B フェーズ1c | `feature/phase1c-drop-indicator-columns` | `AnalysisResultEntity`・テスト群・schema/シード |
| C フェーズ2 | `feature/phase2-indicator-always-compute` | `AnalyzeInteractor.indicate`・`ValuationSpecification`・View 系 |

- 3 worktree を develop から分岐して並列実装。統合は **B → A → C** の順に develop へ順次マージ
  （B は機械的・最小、A は additive、C が最大のため後段でリベース解消）
- A の UPDATE DAO を param ベースにすることで B のエンティティ形状変更と原理的に競合しない。
  A と C は `AnalyzeInteractor` で競合するが領域が異なる（A: 新規メソッド追加 / C: indicate 内ループ除去）ため
  リベースで機械的に解消可能

## テスト戦略・完了条件

- 各ストリームで単体テストを追加・改修（A: 再計算の値検証・スキップ継続・valuation 集合更新の丸め挙動、
  B: エンティティ新シグネチャ・insert 引数、C: 突合ロジックの境界（提出日前の株価・書類なし期間・調整適用）と
  evaluate の都度計算切替）
- 統合後に全テストスイート（playwright 除外）グリーン・checkstyle 既存ベースライン維持
- code-reviewer エージェントで検証し CRITICAL/HIGH 指摘ゼロ
- dev 起動確認（シード適用・画面表示・詳細チャート）
- 完了後、決定した意味論（係数変更時は一括再計算で全期間を現行係数に統一する）を CLAUDE.md の係数管理の記述に追記

## Gate 1（設計の承認）

- 影響範囲: ドメイン層（Interactor/Specification/DAO）・Web 層（管理エンドポイント）・DDL（DROP COLUMN）・dev 資材
- リスク: 最重: B の DROP COLUMN（不可逆・ただし本番リリース判断は分離）/ 中: C の詳細画面応答時間（実測で担保）/
  低: A（additive・冪等バッチ）
- ロールバック: A=エンドポイント無効化のみ / B=本番リリース前なら revert で完結 / C=読取切替の呼び出し元を戻す
- 通過記録: 人間レビュアが AskUserQuestion 経由で設計を承認（2026-07-16）

## 実装・統合の記録

- 3ストリームを worktree 分離の並列サブエージェントで実装し、`feature/model-validation-recalc` に B → A → C の順で統合
- 競合解消: `AnalyzeUseCase` / `AnalyzeInteractor` / `AnalyzeInteractorTest`（A の recalculate 追加と C の indicate 撤去の重なり。
  recalculate 群を残し indicate 群を除去）、`InvestmentIndicatorSpecificationTest`（C の削除を採用）。
  A・C が旧エンティティシグネチャ（B 適用前）で書いた新規テスト4箇所を 10 引数構成へ追随修正
- 統合後テスト: 861 件グリーン（playwright 除外）
- 主な実装上の決定:
  - `AnalyzeInteractor` の再計算は入力欠損行を warn ログ＋failed カウントで継続（skipped=値変化なし、failed=入力欠損）
  - valuation の集合 UPDATE は相関サブクエリ形式で MySQL/H2 両対応。実 H2 での統合テストで ROUND ≒ HALF_UP を検証
  - C は `InvestmentIndicatorSpecification` を全撤去（全 find 系が呼び出し元を失ったため）。
    `InvestmentIndicatorDao` の読取と Entity は約300万件の残置データと整合させるため維持（列・テーブル撤去は後続 2c）

## dev 環境での E2E 検証（統合後）

- `/v3/index`・`/v3/corporate?code=9001`（詳細）・`/v3/analysis` すべて 200。詳細画面はウォームで約 60〜70ms、
  PER/PBR/グレアム指数が都度計算経路で描画されることを確認（シードデータ規模）
- `GET /v1/admin/analysis/recalculate/preview` → 「対象件数: analysis_result=15件, valuation=5件」を確認
- `POST /v1/admin/analysis/recalculate` → target=15, failed=15, valuationUpdated=5。
  failed の原因は dev シードに financial_statement 行が存在しないため（環境要因）。
  本番は全 analysis_result 行が財務諸表値由来（フェーズ1a 検証で 31,776 行の再計算一致を確認済み）のため入力は揃う
- 本番相当件数での詳細画面応答時間はローカルで実測不能のため、リリース後の運用確認項目とする

## 検証（code-reviewer レビューと対応）

- code-reviewer エージェントによる統合差分レビュー: CRITICAL 0 / HIGH 3 / MEDIUM 3 / LOW 3
- HIGH 対応（すべて修正済み・全テストグリーンで再確認）:
  1. recalculate の例外継続漏れ → 想定外の実行時例外も該当行のみ失敗として継続（検証テスト追加）
  2. InvestmentIndicatorReconciliationService のテスト欠如 → 境界条件の単体テスト9件を新設
  3. parallelUpdateView の FinanceValue 二重計算 → 事前計算済み AnalysisResult を渡す reconcilePrecomputed で解消
     （検証テストで findDocument / getFinanceValue が1回のみであることを確認）
- MEDIUM 対応: SQL コメントの精度主張是正・WHERE 句の防御意図明記（2件）。
  「同期HTTP実行のタイムアウトリスク」は本番実行時の運用確認項目とする（下記）
- マージ統合の整合性・新設 SQL の正しさ・冪等性・N+1 回避はレビュー側でも確認済み（消し残し・デッドコードなし）

## Gate 2（検証完了の承認）

- 全テストグリーン・code-reviewer CRITICAL/HIGH ゼロ（HIGH 3件は修正済み）・dev E2E 確認済みの状態で、
  人間レビュアが AskUserQuestion 経由で develop へのマージを承認（2026-07-16）
- 本番リリース（バージョンタグ付与）と本番再計算の実行は別途判断。
  V0.4.2（DROP COLUMN）を含むリリースの前に「1b 本番フルバッチ一巡確認」を実施すること

## 1b 本番フルバッチ一巡確認（V0.4.2 リリース前提条件）

- v2.3.15（1a/1b 含む）反映後のスケジューラ一巡について、人間レビュアが本番を確認し問題なしと判断（2026-07-16、AskUserQuestion 経由）。
  これをもって V0.4.2（DROP COLUMN）を含むリリースの前提条件を充足とする

## 本番反映（v2.4.0）

- GitHub Actions Pipeline（version=2.4.0、run 29499152564）でリリースし、release ジョブ成功（2026-07-16）
- 起動時の Flyway で V0.4.2（指標4列 DROP COLUMN）が適用される構成。全期間再計算バッチの実行は下記手順で別途実施

## 本番運用手順（リリース後の再計算実行）

1. 対象テーブルのバックアップ取得: `analysis_result`・`valuation`（mysqldump 等）
2. `GET /v1/admin/analysis/recalculate/preview` で対象件数を確認
3. `POST /v1/admin/analysis/recalculate` を実行（リバースプロキシのタイムアウト設定に注意。
   数万件規模の同期実行のため、必要ならプロキシを経由せずループバックから直接実行する）
4. ログの完了メッセージ（対象/更新/スキップ/失敗/valuation更新件数)と、失敗行の warn ログを確認
5. 画面（一覧・詳細・バックテスト）で値が一貫していることを確認。`updateCorporateView` / `updateValuationView` は実行後に自動連鎖する
6. `updateCorporateView` バッチの実行時間を切替前後で比較する（reconcilePrecomputed による軽減済みだが実測で確認）
