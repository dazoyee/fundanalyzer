# T20260704B — 評価分析ダッシュボード Phase B（評価精度のバックテスト）

## タスク要約（1 行）
アプリが「割安」と判定した銘柄のその後の実株価パフォーマンスを集計し、評価ロジックの妥当性を検証する「バックテスト」タブを `/v3/analysis` に実装する。

## Gate 進捗 早見表
| Gate | 状態 | 承認者 | 回答日 |
|---|---|---|---|
| Gate 1: 設計の承認 | 合格 | iori-oiso | 2026-07-04 |
| Gate 2: 最終確認   | 合格 | iori-oiso | 2026-07-04 |

## 影響範囲タイプ 早見
- 参照層: **該当**（新規 `BacktestUseCase`/`BacktestInteractor`、集計純粋クラス、`web/view/model/analysis/*` の record、`fragments/analysis-backtest.html`、`AnalysisPresenter` にバックテスト用エンドポイント追加）
- 状態層: **該当（限定的）**（新規永続化なし。バックテスト集計結果の事前計算キャッシュ＝メモリ。evict 契機を評価/株価更新に紐付け）
- データ層: **該当なし**（DB スキーマ変更なし。既存 `valuation`/`stock_price`/`analysis_result` の読み取りのみ。新規 SQL の要否は設計で確認）
- インフラ影響: **要検討**（全社×株価の集計は大量データ処理。同期実行禁止・事前計算/非同期＋キャッシュ。新規依存なし）

## 関連
- 事前計画: `docs/plans/valuation-app-analysis-dashboard.md`（§Phase B に確定仕様）
- 前提: Phase A（`/v3/analysis` 3タブの器・個別推移）完了済み（`T20260704-analysis-phaseA.md`）
- プロトタイプ: `T20260704-analysis-phaseB-attachments/prototype-backtest.html`（Artifact: https://claude.ai/code/artifact/dc199813-7b88-48f9-b341-f5527f53dce1 ）

---

## Gate 1: 設計の承認

### レビュアー向けサマリ
- **判断してほしいこと**: バックテストの集計設計（統計対策込み）・実行方式（非同期＋キャッシュ）・UI・完了条件を承認してよいか。
- **重要な変更ポイント**:
  - **リターン定義（計画確定）**: ①単純株価騰落率 と ②企業価値収束度 を対等併記。期間 T+3M/6M/12M。集計軸 = 割安度バケット別＋割安度×リターン相関＋業種別内訳。
  - **統計対策（必須）**: (1) エピソード単位集計（(企業×有報submit_date)を1件、月次12行の水増し排除、独立エピソード数を horizon 別に表示）、(2) 生存バイアス明示（上場廃止の欠損を分離・上方バイアス注記）、(3) 株価調整の基準統一（判定時・T+n を同一 basisDate で再調整）。
  - **実行方式**: htmx 同期で全件集計しない。事前計算＋Caffeine キャッシュ（既存 `CacheConfig`）、evict は評価/株価更新後。
  - **アーキ**: `BacktestUseCase`/`BacktestInteractor` の対、集計は純粋クラスに切り出し、DAO 直呼び禁止（specification 経由）、per-company ストリーミング集計。
  - 既存 `/v3/valuation`・Phase A・DB は不変更。
- **確認してほしい観点**:
  - **データ量の裏取り（着手前必須）**: T+12M まで株価が取れる独立エピソードが実データにどれだけあるか。薄ければ T+12M は参考表示に留める。※本番データは 192.168.1.49（直接クエリ不可）のため、確認方法を要相談。
  - リターン定義②（企業価値収束度）の算式の妥当性。
  - プロトタイプの UI（バケット表・相関・業種内訳・免責表示）が意図に合うか。

### 重点観点
- 影響範囲分析（参照/状態/データ）の網羅
- 三本柱（テスト戦略 / セキュリティ方針 / ドキュメント計画）
- スコープ確定（コア=集計＋バケット表 / 後回し=高度な統計補正 / 対象外=投資UX）
- 完了条件（機能 / テスト / ドキュメント / スコープ外）

### レビュアー記入欄
| 項目 | 内容 |
|---|---|
| 承認者 | iori-oiso |
| レビュー依頼日 | 2026-07-04 |
| 回答日 | 2026-07-04 |
| **結論** | **合格** |
| **コメント** | プロトタイプ合意後の集計設計・実行方式・統計対策を承認。実装（Codex委譲）へ。 |

---

## Gate 2: 最終確認

### レビュアー向けサマリ
- **判断してほしいこと**: 集計が統計的に妥当か / 実データで意味ある結果が出るか / 性能問題がないか
- **重要な変更ポイント**: BacktestUseCase/Interactor＋純粋集計、/v3/analysis バックテストタブ、dev シード(V1.0.4・dev限定)、ViewService 単一コンストラクタ化
- **確認してほしい観点**: 全テスト830 PASS、実データで単調性・散布図・業種内訳が描画、本番非影響（seed は dev のみ）

### 重点観点
- 差分レビュー / 動作確認結果（実データ） / 統計妥当性 / 性能 / ドキュメント整合性

### レビュアー記入欄
| 項目 | 内容 |
|---|---|
| 承認者 | iori-oiso |
| レビュー依頼日 | 2026-07-04 |
| 回答日 | 2026-07-04 |
| **結論** | **合格** |
| **コメント** | 全テスト830 PASS・実データでバックテスト描画（単調性/相関0.98）確認。コミット承認。 |

---

<details>
<summary>📁 AI 作業ログ（ステップ 1〜5）</summary>

### ステップ 1: 把握・整理
- 計画 §Phase B に確定仕様（リターン定義・統計対策・実行方式）あり。
- 流用: `ValuationSpecification.findAllValuationEntities`/`findValuationOfSubmitDate`、`StockSpecification.findEntityList`、`AnalysisResultSpecification.findAnalysisResult`、`CorporateActionSpecification.adjustToBasisWithActions`、`companySpecification.inquiryAllTargetCompanies`。
- スコープ: コア=バケット集計＋独立エピソード数/生存バイアス表示 / 後回し=相関の有意性補正の高度化 / 対象外=投資UX。

### ステップ 2: プロトタイピング（合意済み 2026-07-04）
- バックテストタブのモックを Artifact で提示・合意。
- **合意結果**: UI=4要素（母数/免責バー・バケット表・相関散布図・業種内訳）。期間=セグメント切替（3M/6M/12M）。データ裏取り=**dev シードでロジック担保、実データ検証は本番デプロイ後**。

### ステップ 3: 設計
#### 3.2 影響範囲分析
- 参照層=該当（`BacktestUseCase`/`BacktestInteractor`、集計純粋クラス、`web/view/model/analysis/*` record、`fragments/analysis-backtest.html`、`AnalysisPresenter` にバックテスト用 fragment エンドポイント、`ViewService` 委譲メソッド）。
- 状態層=該当（限定的）＝集計結果の Caffeine キャッシュ（メモリ）。evict は評価スケジューラ後 or 手動再計算。
- データ層=該当なし（既存3テーブル read のみ、スキーマ・新規SQL不要。全社取得は per-company `selectByCode` ループ）。

#### 3.3 集計アルゴリズム（per-company ストリーミング）
1. `companySpecification.inquiryAllTargetCompanies()` → 企業ごとに:
   - **エピソード化**: `findAllValuationEntities(code)` を submit_date（有報単位）でグループ化し、各エピソードの代表判定点を1つ抽出（`findValuationOfSubmitDate` 相当＝最小 daySinceSubmitDate）。→ 月次12行の水増しを排除。
   - `StockSpecification.findEntityList(code)` で株価時系列を一括ロード（N+1回避）。
   - 各 horizon h∈{3M,6M,12M}: 判定 target_date+h の**最近接営業日終値**（+h 以降・±X営業日以内、X は `app.config.analysis` 化）。無ければ除外し理由を分類（`Company.lived=false`→上場廃止 / それ以外→データ欠損）。
   - **株価調整**: 判定株価と T+h 株価を**同一 basisDate（エピソード submit_date）へ `adjustToBasisWithActions` で再調整**してから騰落率を算出（分割で壊れない）。
   - **リターン①**（騰落率）= adjusted(T+h) / adjusted(判定) − 1。
   - **収束度②** = (|判定時乖離| − |T+h 時点乖離|) / |判定時乖離|（企業価値は `analysisResultId → findAnalysisResult`、ID単位キャッシュ）。
2. 全エピソード集計（**純粋クラス**）: 割安度バケット別（境界=`app.config.analysis`）に 独立件数・平均/中央値リターン・的中率(>0割合)・平均収束度。加えて 割安度×リターンの Pearson 相関、業種別内訳、horizon 別の独立エピソード数と除外内訳。

#### 3.4 実行方式
- **htmx 同期で全件集計しない**。`BacktestUseCase.recompute()` で事前計算 → Caffeine キャッシュ（`spring.cache.caffeine`、キャッシュ名は application.yml）。evict/更新契機は評価スケジューラ(evaluate)後 or 手動。
- 画面 `/v3/analysis/backtest` fragment はキャッシュ済み結果を遅延ロード。未計算時は「未計算/集計中」を表示。

#### 3.5 三本柱
- テスト: 集計純粋関数（バケット/平均・中央値/的中率/相関/収束度/最近接マッチング/除外分類）を `@ParameterizedTest`。`BacktestInteractor` は specification モックで per-company ループを検証。カバレッジ80%。
- セキュリティ: 認証必須で自動保護、新規入力なし（horizon は enum 化）。新規リスクなし。
- ドキュメント: `develop/document/アプリ構成図.drawio` に `BacktestUseCase` 追加、タスクmd Gate2。

#### 3.6 dev シードデータ
- test/dev(H2) に valuation/stock_price/analysis_result を **複数銘柄×複数 submit_date×T+12M 分**投入するシード（`@Sql` or Flyway dev 用 or テストフィクスチャ）。既存テストシード拡張可否を確認。

### ステップ 4: 実行サイクル
- 実装順: ①集計純粋クラス＋ユニットテスト（データ経路に依存せず先行可）→ ②`BacktestUseCase`/`Interactor`（per-company ループ・調整・除外）→ ③キャッシュ/事前計算 → ④`AnalysisPresenter` fragment＋`analysis-backtest.html`＋app.js（散布図/バー）→ ⑤dev シード＋結合確認。

### ステップ 5: 多軸検証
- **実装バッチ**: ①純粋集計（Horizon/EpisodeOutcome/BacktestResult/BacktestCalculator）②UseCase/Interactor＋ViewService＋設定 ③表示層（Presenter/fragment/タブ配線/散布図JS）④dev シード（V1.0.4）。各バッチを Codex 委譲→Claude 検証。
- **テスト（Java 17）**: BacktestCalculatorTest 18／BacktestInteractorTest 5／ViewServiceTest 含む 55／AnalysisPresenterTest 3／Phase8ScreenSnapshotTest 10（app 起動健全性）／backtestTabRenders 1（実レンダリング）すべて PASS。
- **実データ描画確認**: dev シード投入後、バックテストタブで割安度バケットの**単調性**（割安度↑→平均リターン・的中率↑、相関0.98）・散布図・業種内訳が正しく描画（スクショ取得済 `target/playwright-snapshots/analysis-backtest.png`）。
- **検証で発見・修正した不具合（Codex出力レビュー）**:
  1. `ViewService` が引数違いの2コンストラクタ（backtestUseCase=null 委譲）→ Spring DI 曖昧＋null注入NPE。単一コンストラクタに修正、`ViewServiceTest` を6引数＋mock に更新。
  2. `Phase8ScreenSnapshotTest` に追加した backtest 検証メソッドで `org.junit.jupiter.api.Test` import 欠落→追加。
- **セキュリティ**: 認証必須で自動保護、horizon は enum＋ラベル解決（不正値は M6 フォールバック）、SQLはDoma。新規リスクなし。
- **ドキュメント整合性**: アプリ構成図.drawio への BacktestUseCase 追記は Gate 2 前に対応。

### コミット・検証履歴（追記）
- 環境: `JAVA_HOME=openjdk@17` 必須（Java 25 は Lombok 失敗）。
- 全テスト（Playwright除く）回帰は Gate 2 直前に実行。

### 完了条件
- 機能: バックテストタブ（4要素）、期間セグメント、事前計算＋キャッシュ、母数/除外表示。
- テスト: 集計純粋関数ユニット・`BacktestInteractor`・カバレッジ80%、dev シードで集計が描画。
- ドキュメント: タスクmd Gate2、アプリ構成図.drawio 更新。
- スコープ外（宣言）: 相関の高度な有意性補正、投資UX、**実データでの妥当性検証（本番デプロイ後）**。

### コミット・検証履歴
- **環境**: Maven は `JAVA_HOME=openjdk@17` 必須（Phase A で判明。Java 25 は Lombok 失敗）。

</details>

---

## 更新履歴
| 日付 | 概要 |
|---|---|
| 2026-07-04 | 起票。Gate 1 準備（影響範囲3層・確定仕様・データ量裏取りの必要性を記載）。 |
