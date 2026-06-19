# Task T20260601-2: 業種相対評価（グレアム指数の業種内zスコア差し替え）（拡張④）

- 着手日: 2026-06-01
- 完了日: —
- 担当: 計画/実装/検証エージェント（Claude） + 人間レビュア（iori-oiso）
- 関連リンク: 分析モデル高度化ロードマップ（①→④→②）の第2弾。①[T20260601-analysis-coefficient-externalize.md](T20260601-analysis-coefficient-externalize.md) 完了後に着手。

## 解決すべき課題（1 行）

グレアム指数は業種で適正水準が異なり単純比較では業種バイアスが出るため、**既存のグレアム指数列の表示値を「業種内zスコア（同業他社比の標準化得点）」に差し替えるトグル**を設け、業種補正後の割安度で銘柄を見られるようにする。

## ステップ1: 把握・整理

### 現状（一次情報）

- 業種マスタ: `industry`（id/name）。`Company` レコードが `industryId`/`industryName` を保持。`CompanySpecification.findAllTargetCompanies()` で全対象企業（業種付き）取得可、`IndustrySpecification`（キャッシュ）あり。
- 個社評価の永続ビュー: `valuation_view`（PRIMARY KEY=code, 1社最新1行）。**グレアム指数 `graham_index` を既に保持**。`ViewValuationInteractor.viewAllValuation()` 等がメモリ読込。
- 画面 `/v3/valuation`: `target`(null/all/favorite/industry) × `view`(stock/submit/**graham-index**/dividend-yield/industry)。graham-index view はグレアム指数（および提出日グレアム指数）を列表示。view 別 `ALLOWED_SORT_BY_VIEW` で `grahamIndex` ソート可（[ValuationPresenter.java:46-47](../../src/main/java/github/com/ioridazo/fundanalyzer/web/presenter/ValuationPresenter.java)）。
- `CompanyValuationViewModel` は `valuation_view` 由来・永続パス（`generateCompanyValuationView`→`upsert`）と共用。**フィールド追加は避ける**。

### 設計方針（ユーザー合意）

既存列を増やさず、**graham-index view の「グレアム指数」列が表示する値を、相対モード時に業種内zスコアへ差し替える**（その場計算・永続化なし）。生値の絶対基準（PER×PBR≤22.5 が割安）を失わないよう **実数/業種内相対のトグルで切替**（常時相対は採らない）。対象指標は **グレアム指数のみ**（合成指標で PER/PBR を内包）。

### スコープ確定

| 区分 | 内容 |
|---|---|
| **コア** | graham-index view に「実数 / 業種内相対」トグルを追加。相対ON時、グレアム指数列の表示値を**業種内zスコア**に差し替える（request 時に全対象企業を業種グルーピングして平均/σ/z 算出、メモリ内・非永続）。小規模業種（n<3／σ≒0）は「-」表示。 |
| **後回し** | 割安度/PER/PBR の相対化、専用スクリーニングタブ、提出日グレアム指数の相対化、zスコアの永続化・バッチ事前計算。 |
| **対象外** | `valuation_view`／`CompanyValuationViewModel` のスキーマ・フィールド追加、DB マイグレーション、新規 DAO クエリ、他 view（stock/submit/dividend/industry）の変更。 |

### ドキュメントとコードの乖離

なし。

## ステップ2: プロトタイピング（外から見える形）

graham-index view（`target` は null/all/favorite で利用）に、テーブル上部へトグルを置く:

```
[ 実数 | 業種内相対 ]   ← htmx で table fragment を部分更新（mode パラメータ）
```

| モード | グレアム指数列の表示 | 列ヘッダ | 割安方向 |
|---|---|---|---|
| 実数（既定） | 21.6（生値） | グレアム指数 | 低いほど割安（絶対基準 22.5） |
| 業種内相対 | -0.85（zスコア） | グレアム指数（業種内z） | **z 低いほど同業比で割安** |

- 相対モードでもソートは既存 `grahamIndex` ソートをそのまま使う（差し替え後の値で並ぶ）。
- URL は `?view=graham-index&mode=relative` のように `mode` を追加し `hx-push-url` で同期。
- zスコア母集団は**全対象企業**（target 絞り込み前）を業種グルーピング。

## Gate 1: 影響設計の承認

### レビュアー向けサマリ

- **判断してほしいこと**: 「graham-index view のトグルで、相対ON時にグレアム指数列の値を業種内zスコアへ差し替える（新列・DB変更・新クエリなし）」設計が妥当か。
- **重要な変更ポイント**:
  1. `ValuationPresenter`: graham-index view に `mode`（raw 既定 / relative）パラメータを追加。許可値を列挙で検証。
  2. `ViewService`: 相対モード時、全対象企業を業種グルーピングし、グレアム指数の業種内 平均/σ/z を算出。表示用に `CompanyValuationViewModel` の `grahamIndex` 値を**メモリ内で z へ差し替えた**リストを生成（構造体は不変・永続非影響）。業種は `CompanySpecification`（キャッシュ）で code→industryId 引き当て。
  3. `fragments/valuation-table.html` の graham-index fragment（desktop+mobile）にトグルと列ヘッダ切替を追加。
  4. **新規データソース・新規クエリ・DB マイグレーションは無し**（グレアム指数は `valuation_view`、業種は company マスタに既存）。
- **確認してほしい観点**:
  1. zスコア母集団を「全対象企業（target 絞り込み前）」とする点。
  2. 相対モード時にグレアム指数列の値を差し替える（同列・別意味）UI が直感的か。ヘッダ表記「（業種内z）」で十分か。
  3. 小規模業種（n<3／σ≒0）の「-」ガード。

### 重点観点

#### 影響範囲分析（参照層 / 状態層 / データ層）

- **参照層**:
  - `CompanyValuationViewModel` は**フィールド追加せず**、相対モードのときに `grahamIndex` 値を差し替えた新インスタンスを生成（`withXxx` 相当の再生成）。→ 既存の全構築箇所・永続パス（`upsert`）への波及なし。
  - 変更は `ValuationPresenter`（mode 分岐追加）・`ViewService`（相対計算メソッド追加）・graham-index fragment（トグル）に限定。いずれも**加算的**で既存 view を壊さない。
  - `CompanySpecification.findAllTargetCompanies()`（既存・キャッシュ）を参照するのみ。
- **状態層**: ステートマシンなし。mode は raw/relative の2値で graham-index view 内に閉じる（他 view・target と直交）。
- **データ層**: **DB スキーマ変更なし・マイグレーションなし・新規クエリなし**。zスコアは request 時メモリ算出で永続化しない。既存データへの影響なし。

#### インフラ影響チェック

- 大量データ処理/タイムアウト: 対象は数百社規模、業種グルーピング・統計はメモリ内 O(n)。**相対モードを開いた時のみ**実行。→ 影響軽微。
- 新規外部サービス連携 / スキーマ変更・移行 / バッチ追加 / 依存追加（J.1）: **すべて無し**。ADR 不要。

#### 品質設計の三本柱

| 柱 | 方針 |
|---|---|
| **テスト戦略** | ユニット中心（JUnit5標準アサーションのみ）。業種内 平均/σ/z の算出（正/負・σ=0・n<3・null除外）、業種グルーピング、相対リスト生成（grahamIndex 差し替え・他フィールド不変）。`ViewService` の相対モード filter/sort/page。`ValuationPresenter` の mode 分岐を MockMvc。既存テストは無変更（加算のみ）。カバレッジ80%以上。 |
| **セキュリティ方針** | 参照系のみ。`mode` は列挙値（raw/relative）でホワイトリスト検証（既存の view/sort と同様）。新規シークレット・新規入力経路なし。段階: 影響軽微。 |
| **ドキュメント計画** | 本 md（Gate記録）。CLAUDE.md「View/画面」節の valuation 記述に「graham-index view の実数/業種内相対トグル」を追記。 |

#### スコープ確定（再掲）

コア=graham-index view の実数/業種内相対トグル＋値差し替え（その場計算）／後回し=割安度・PER・PBR の相対化、専用タブ、永続化／対象外=既存永続モデル変更・DBマイグレーション・新クエリ・他view変更。

### レビュアー記入欄

- 承認者: iori-oiso（人間レビュア）
- レビュー依頼日: 2026-06-01
- 回答日: 2026-06-01
- 結論: **合格**
- コメント: 「graham-index view のトグルで相対ON時にグレアム指数列の値を業種内zスコアへ差し替え（新列・DB変更・新クエリなし）」設計を承認。母集団=全対象企業を業種グルーピング、各業種でグレアム指数を持つ社のみで平均/σ算出、各社は自業種内で標準化。小規模業種(n<3／σ≒0)は「-」。

## ステップ4: テスト設計

### テストケース（自然言語）

**新規: 業種内zスコア算出（ロジックの中核。配置先は ViewService もしくは新規 ヘルパ）**

1. 同一業種3社のグレアム指数 {21.6, 18.0, 30.0} に対し、各社の z=(値−平均)/σ が正しく算出される。
2. 業種が異なる2社は、それぞれ自業種の平均・σで標準化される（業種を跨いで混ざらない）。
3. グレアム指数が null の社は母集団から除外され、自身の z も null（「-」）。
4. 業種内の社数が n<3 のとき z は null（「-」）。
5. 業種内のσが 0（全社同値）のとき z は null（「-」、ゼロ除算回避）。
6. 相対リスト生成: grahamIndex のみ z へ差し替わり、他フィールド（code/name/discountRate 等）は不変。

**新規: ViewService 相対モード**

7. mode=relative で graham-index テーブルを引くと、各行の grahamIndex が業種内zに差し替わり、keyword/sort/page が従来どおり機能する（z 値でソートされる）。
8. mode=raw（既定）では従来どおり生のグレアム指数が出る（挙動不変）。

**新規: ValuationPresenter（MockMvc）**

9. `/v3/valuation/table?view=graham-index&mode=relative` で graham-index fragment が返る。
10. 不正な mode 値はホワイトリスト検証で既定（raw）にフォールバックする。

### 既存テストとの重複・補完

- 既存の graham-index view（mode 未指定＝raw）テストは**無変更で温存**。新規ケース8が「raw＝既存挙動」を担保。
- 状態遷移なし → マトリクス該当なし。

## Gate 2: 完了条件の確認

### 運用ルート

**インライン**（複数ファイルにまたがるため省略不可）。

### 完了条件（機能）

- [ ] 業種内zスコア算出ロジック（全対象企業を業種グルーピング → 業種ごとに graham の平均/σ → 各社 z。null/n<3/σ=0 は null）
- [ ] `ViewService` に graham-index 相対モード処理を追加（`grahamIndex` 値を z に差し替えた `CompanyValuationViewModel` リストを生成。母集団は全対象企業、業種は `CompanySpecification` で引き当て）
- [ ] `ValuationPresenter` に `mode`(raw/relative) パラメータを追加（ホワイトリスト検証、graham-index view のみ有効）
- [ ] graham-index fragment（desktop+mobile）にトグルと列ヘッダ切替（「グレアム指数」⇔「グレアム指数（業種内z）」）、`hx-get`+`mode`+`hx-push-url`

### 完了条件（テスト）

- [ ] 単体テスト: 上記テストケース 1〜10（JUnit5標準アサーションのみ）
- [ ] 既存 graham-index view（raw）テストを変更していない
- [ ] `./mvnw test -DexcludedGroups=playwright` 全緑・カバレッジ80%以上維持
- [ ] dev 実機で graham-index view のトグル動作・z表示・小規模業種「-」を確認（Gate3）

### 完了条件（ドキュメント）

- [ ] 本 md の Gate 2 / Gate 3 を記入
- [ ] CLAUDE.md「View/画面」節の valuation 記述に「graham-index view の実数/業種内相対トグル」を追記

### スコープ外（やらないこと）

- 割安度/PER/PBR の相対化、専用スクリーニングタブ、提出日グレアム指数の相対化
- zスコアの永続化・バッチ事前計算、`valuation_view`/`CompanyValuationViewModel` のスキーマ・フィールド追加
- DB マイグレーション・新規 DAO クエリ・他 view（stock/submit/dividend/industry）の変更

### レビュアー記入欄

- 承認者: iori-oiso（人間レビュア）
- レビュー依頼日: 2026-06-01
- 回答日: 2026-06-01
- 結論: **合格**（インライン承認）
- コメント: 機能・テスト・ドキュメント・スコープ外の完了条件で承認。TDD 実装着手可。

## ステップ5・6: 実装と多軸検証

### 実装サマリ

| ファイル | 変更 |
|---|---|
| `usecase/ViewValuationUseCase` | `findGrahamIndustryZScore()` 追加 |
| `interactor/ViewValuationInteractor` | 実装＋純粋関数 `computeGrahamIndustryZScore`（業種グルーピング・平均/母集団σ/z・n<3/σ=0/null ガード） |
| `service/ViewService` | `findCompanyValuationTable` に relative 分岐（`replaceGrahamWithIndustryZScore`）。raw 時は z 計算を呼ばない |
| `web/.../CompanyValuationViewModel` | `withGrahamIndex`（不変コピー・新フィールドなし） |
| `web/.../CompanyValuationTableQuery` | `mode` フィールド追加 |
| `web/presenter/ValuationPresenter` | `mode` パラメータ＋`resolveMode`（ホワイトリスト raw/relative・graham-index のみ relative 有効）＋model 属性 |
| `templates/fragments/valuation-table.html` | graham-index fragment にトグル・ヘッダ切替・null→「-」、sortable-th/paginator に mode 伝播 |
| `templates/valuation-v2.html` | 検索/並び替えの hx-get に mode 伝播 |
| `CLAUDE.md` | View/画面 節に本機能を追記 |

### テスト結果

- `./mvnw test -DexcludedGroups=playwright` → **738 件全緑（+11 新規）・回帰なし**。
  - `computeGrahamIndustryZScore` 5 ケース（正常 z=-1.22/0.00/1.22・業種分離・null除外・n<3・σ=0）
  - `findGrahamIndustryZScore` 結線 1・`ViewService` relative/raw 2・`ValuationPresenter` mode 3

### 多軸検証（観点別）

| 観点 | 結果 |
|---|---|
| コード品質（code-reviewer） | CRITICAL/HIGH なし。z算出（母集団σ・スケール・境界）正確、業種引き当て（getCode4↔code 4桁）妥当、raw不変性 verify 済み、clean arch 整合と確認。MEDIUM 指摘のうち toMap マージ意図コメント・double精度の意図 Javadoc を反映。`getCode4` NPE は呼び出し側 filter でガード済（共有メソッドのため変更見送り）。 |
| テスト構造・機能完全性 | 完了条件テスト1〜10 実装。既存 raw テスト無変更。 |
| セキュリティ（security-reviewer） | 本変更起因の重大リスクなし。`mode` はホワイトリスト2値に正規化、テンプレートは生値を `th:text`/`th:utext` せず三項リテラル分岐＋URLエンコードのみ＝XSSなし。動的 fragment 名 `__${view}__` も view ホワイトリストで安全と確認。 |
| ドキュメント整合性 | CLAUDE.md・本 md を更新。 |

## Gate 3: 最終確認

### レビュアー向けサマリ

- **判断してほしいこと**: 利用者視点でトグル動作・相対表示が妥当か。z が全行「-」になる現 dev seed の挙動（後述）が許容できるか。
- **重要な変更ポイント**:
  1. graham-index view にトグル「実数/業種内相対」を追加。相対 ON で列ヘッダが「グレアム指数（業種内z）」に変わり値を差し替え。
  2. z は全対象企業を業種グルーピングして算出（n<3/σ=0/null は「-」）。DB変更・新クエリなし。
  3. 既存 raw 表示・他 view は不変（738件緑で回帰なし）。
- **確認してほしい観点**:
  1. 現 dev seed は **各社が異なる業種に1社ずつ**割り当て（9001=情報通信業, 9002=食料品, 9003=電気機器…）のため、どの業種も n<3 で z は全行「-」になる（= n<3 ガードの正しい挙動）。値ありの実機デモには ≥3社/業種の seed が必要。

### 重点観点

#### 差分レビュー
本番コード7ファイル＋テンプレート2＋テスト3（新規ケース11）＋CLAUDE.md。

#### 動作確認結果（dev 実機起動）
- アプリ起動成功（`Started FundanalyzerApplication`）。
- graham-index view raw/relative とも **HTTP 200**、Thymeleaf 例外なし。`${mode}` のフラグメント参照も実機で正常描画。
- トグル「実数/業種内相対」表示・切替・列ヘッダ「グレアム指数（業種内z）」への変化を確認（[相対スクショ](T20260601-2-attachments/gate3-graham-relative.png) / [実数スクショ](T20260601-2-attachments/gate3-graham-raw.png)）。
- z 列は現 dev seed では全行「-」（各社が別業種＝n<3 の正しい挙動）。z 数値の正しさはユニットテストが厳密値で担保。
- **実値デモ**: 一時 seed（`V1.0.3__demo_same_industry.sql`・コミットせず撮影後削除）で 9001/9002/9003 を同一業種(12)に寄せ n=3 を満たした結果、z=**-1.06 / -0.28 / 1.34**（{17.66, 22.5, 32.62} の業種内標準化）を表示。同業 3 社未満の 9004/9005 は「-」のまま＝仕様どおりの挙動を実機で確認（[実値スクショ](T20260601-2-attachments/gate3-graham-relative-values.png)）。一時 seed は削除済み（git 痕跡なし）。

#### 動作確認結果（スコープ拡張: 銘柄詳細の併記）
- `/v3/corporate?code=9001` 見出しカードに **グレアム指数 17.660 ＋「業種内z: -1.06」併記** を確認（業種=情報・通信業、`/v3/valuation` の z と同一値＝一貫性）（[銘柄詳細スクショ](T20260601-2-attachments/gate3-corporate-graham-zscore.png)）。
- 全テスト 750 件緑（拡張分 +12）。`getGrahamIndustryZScore` ユニット・`CorporatePresenter` モデル属性 MockMvc を追加。
- 銘柄詳細の他のグレアム指数（投資指標履歴・評価履歴テーブル）は実数のまま（スコープ外）。

#### 副次影響
なし（DB/スキーマ/外部連携/依存追加なし、他 view 不変）。

#### ドキュメント整合性
CLAUDE.md・本 md 更新済み。

### レビュアー記入欄

- 承認者: iori-oiso（人間レビュア）
- レビュー依頼日: 2026-06-01
- 回答日: 2026-06-02
- 結論: **合格**（タスク④全体＝/v3/valuation トグル＋銘柄詳細併記 を含む）
- コメント: 両画面で実機動作・同一z値を確認。既存の保存値・表示値に影響なし（z は非永続・raw 既定・併記は加算）を確認のうえ承認。実値デモ用一時 seed はコミット対象外として削除済み。

## Gate 1 再実施（スコープ拡張: 銘柄詳細の見出しグレアム指数に業種内z併記）

> 既存 Gate 1（/v3/valuation 限定）は承認・実装・検証済み。ユーザー要望により対象画面を拡張するため、本セクションを新規追記（既存 Gate 記録は履歴として保持）。

### 認識合わせ結果（2026-06-02）

- **対象**: `/v3/corporate`（銘柄詳細）の**見出しの最新グレアム指数のみ**（表示情報カード desktop [corporate-v2.html:192-193](../../src/main/resources/templates/corporate-v2.html) / mobile :238）。投資指標履歴・評価履歴テーブル（②③）は**実数のまま＝後回し**（各過去日の業種母集団再構築が必要で重いため）。
- **見せ方**: **実数＋「業種内z: X.XX」併記**（トグルや差し替えではない）。絶対基準(22.5)を保持しつつ同業比を同時表示。算出不能（n<3／σ=0／null）は「業種内z: -」。
- **算出**: `/v3/valuation` と同じ `ViewValuationUseCase#findGrahamIndustryZScore` を再利用 → 両画面で同一値・同一ロジック。

### レビュアー向けサマリ

- **判断してほしいこと**: 銘柄詳細の見出しグレアム指数への業種内z併記を、既存ビューモデル非変更・`findGrahamIndustryZScore` 再利用の加算実装で行う設計が妥当か。
- **重要な変更ポイント**:
  1. `ViewService` に `getGrahamIndustryZScore(CodeInputData)` 追加（`viewValuationUseCase.findGrahamIndustryZScore()` の結果から当該コードの z を返す。再利用）。
  2. `CorporatePresenter.populateModel` で z をモデル属性 `grahamIndustryZScore` として追加（既存 `CorporateDetailViewModel` は無変更）。
  3. `corporate-v2.html` 見出しカード（desktop+mobile）に「業種内z」併記行を追加（null は「-」）。
- **確認してほしい観点**:
  1. 既存ビューモデル（`CorporateDetailViewModel` / `corporateView`）を変えず、モデル属性で z を渡す方針でよいか。
  2. 併記の文言「業種内z」と、算出不能時「-」表記でよいか。

### 重点観点

#### 影響範囲分析（参照層 / 状態層 / データ層）
- **参照層**: 加算のみ。`ViewService` 新メソッド・`CorporatePresenter` 1属性追加・テンプレート併記行。既存メソッド・既存ビューモデル・他画面は不変。`findGrahamIndustryZScore` は既存（task ④本体で追加・テスト済）を再利用。
- **状態層**: なし。
- **データ層**: **DB変更・新クエリなし**（z は request 時メモリ算出・非永続。グレアム指数・業種は既存データ）。

#### インフラ影響チェック
- 銘柄詳細表示時に `findGrahamIndustryZScore`（全対象企業の業種集計）を1回呼ぶ（数百社・メモリ O(n)）。詳細画面1回/表示のため影響軽微。新規依存・スキーマ・バッチなし。ADR不要。

#### 品質設計の三本柱
- **テスト**: `ViewService.getGrahamIndustryZScore` のユニット（該当コードの z 返却・該当なしで null）。`CorporatePresenter` の MockMvc で `grahamIndustryZScore` モデル属性付与。既存テスト無変更・加算のみ。カバレッジ維持。
- **セキュリティ**: 参照系のみ・新規入力経路なし（code は既存パラメータ）。テンプレートは z 値を `th:text` で数値表示（XSS非該当）。影響軽微。
- **ドキュメント**: CLAUDE.md の本機能記述に「銘柄詳細の見出しにも業種内z併記」を追記。本 md 更新。

#### スコープ（拡張分）
コア=銘柄詳細見出しの業種内z併記／後回し=履歴テーブルの相対化／対象外=ビューモデル変更・DB変更・他指標。

### レビュアー記入欄

- 承認者: iori-oiso（人間レビュア）
- レビュー依頼日: 2026-06-02
- 回答日: 2026-06-02
- 結論: **合格**
- コメント: 既存ビューモデル非変更・モデル属性で z 伝達・`findGrahamIndustryZScore` 再利用の加算実装を承認。併記文言「業種内z」・算出不能時「-」を承認。

## Gate 2 再実施（スコープ拡張分の完了条件・インライン）

### 完了条件（機能）
- [ ] `ViewService.getGrahamIndustryZScore(CodeInputData)`: 当該コードの業種内z（無ければ null）を `viewValuationUseCase.findGrahamIndustryZScore()` から返す
- [ ] `CorporatePresenter.populateModel`: `grahamIndustryZScore` をモデル属性に追加（既存ビューモデル無変更）
- [ ] `corporate-v2.html` 見出しカード（desktop:192-193 / mobile:238）に「業種内z」併記行追加（null→「-」）

### 完了条件（テスト）
- [ ] `ViewService.getGrahamIndustryZScore` ユニット（該当コード z 返却 / 該当なし null）
- [ ] `CorporatePresenter` MockMvc で `grahamIndustryZScore` モデル属性付与
- [ ] 既存テスト無変更・全テスト緑・カバレッジ維持
- [ ] dev 実機で銘柄詳細の併記表示（同一業種3社の銘柄で z 値、それ以外「-」）を確認（Gate3）

### 完了条件（ドキュメント）
- [ ] CLAUDE.md・本 md 更新

### スコープ外
- 履歴テーブル（投資指標履歴・評価履歴）の相対化、ビューモデル変更、DB変更、他指標

### レビュアー記入欄
- 承認者: iori-oiso（人間レビュア）
- レビュー依頼日: 2026-06-02
- 回答日: 2026-06-02
- 結論: **合格**（インライン承認）

## 更新履歴

- 2026-06-01: 初版（新タブ＋4指標案）。
- 2026-06-01: 設計見直し。ユーザー合意により「既存グレアム指数列の値を業種内zスコアへ差し替えるトグル」方式へ全面改訂（新列・DB変更・新クエリなし／対象はグレアム指数のみ／割安度・PER・PBR は後回し）。Gate1 承認待ち。
