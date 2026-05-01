# Task T20260429 Phase 4: valuation.html 移植（5 テーブル並列）

- 着手日: 2026-05-01
- 完了日: -
- 担当: AI エージェント (Claude / Opus 4.7) + iori-oiso
- 関連リンク:
  - マスタープラン: [T20260429-screen-renewal-htmx-tailwind.md](T20260429-screen-renewal-htmx-tailwind.md)
  - Phase 3（テーブル汎用パターン確立）: [T20260429-screen-renewal-phase3-index-htmx.md](T20260429-screen-renewal-phase3-index-htmx.md)
- ブランチ: `feature/screen-renewal-phase4-valuation-htmx`（develop から派生）

---

## ステップ 1: 把握・整理

### 解決すべき課題（1 行）

株価評価画面（valuation.html）を Tailwind + htmx + layout-v2 に移植する。Phase 3 で確立したテーブル汎用パターンを **5 つのテーブル並列** に拡張適用し、view 切替（stock / submit / graham-index / dividend-yield / industry）+ target 切替（main / all / favorite / industry）の組合せに対応する。

### 関連既存資産

- 既存 [ValuationPresenter](src/main/java/github/com/ioridazo/fundanalyzer/web/presenter/ValuationPresenter.java) 57 行: `/v2/valuation` で `?target=null|all|favorite|industry` に応じて異なる valuations を返す
- 既存 [valuation.html](src/main/resources/templates/valuation.html) 577 行: layout.html 継承・5 テーブル + DataTables Buttons + daterangepicker
- ViewService の `getValuationView()` / `getAllValuationView()` / `getFavoriteValuationView()` / `getIndustryValuationView()`
- `CompanyValuationViewModel`（株価評価データ）/ `IndustryValuationViewModel`（業種別評価データ）
- Phase 3 で確立済の **テーブル汎用パターン**

### 5 テーブルの構造把握

| view | 表示する target | データソース | 主な列 |
|---|---|---|---|
| stock | main / all / favorite | CompanyValuationViewModel | code / name / targetDate / stockPrice / differenceFromSubmitDate / submitDateRatio |
| submit | main / all / favorite | CompanyValuationViewModel | code / name / submitDate / stockPriceOfSubmitDate / grahamIndexOfSubmitDate / corporateValue |
| graham-index | main / all / favorite | CompanyValuationViewModel | code / name / grahamIndex / grahamIndexOfSubmitDate ほか |
| dividend-yield | main / all / favorite | CompanyValuationViewModel | code / name / dividendYield ほか |
| industry | industry | IndustryValuationViewModel | industry / averageGrahamIndex 等の業種集計値 |

stock / submit / graham-index / dividend-yield は **同じデータの違うビュー**（DTO は CompanyValuationViewModel 1 種類）。industry のみ別 DTO。

### スコープ

| 区分 | 内容 |
|---|---|
| **コア** | (a) ViewService に `findCompanyValuationTable(CompanyValuationTableQuery)` と `findIndustryValuationTable(IndustryValuationTableQuery)` を追加（Phase 3 のテーブル汎用パターン踏襲・既存メソッド無変更） (b) record 4 個新設: `CompanyValuationTableQuery` / `CompanyValuationTablePage` / `IndustryValuationTableQuery` / `IndustryValuationTablePage` (c) ValuationPresenter に `/v3/valuation` と `/v3/valuation/table` 2 エンドポイント追加（旧 `/v2/valuation` は無変更で並走） (d) `/v3/valuation/table?view=stock|submit|graham-index|dividend-yield|industry` で view ごとに異なる fragment を返却（5 fragment） (e) 新 `templates/valuation-v2.html` を layout-v2 継承で作成（target タブ + view タブ + 検索 + テーブル） (f) `templates/fragments/valuation-table.html` に 5 つの fragment を定義（`th:fragment="stock-table"` / `submit-table"` / `graham-index-table"` / `dividend-yield-table"` / `industry-table"`） (g) htmx で view 切替時にも部分更新（hx-get + hx-target="#valuation-table" + hx-push-url） (h) view ごとに **異なるソート可能列** をホワイトリストで定義 (i) 検索は code / name partial match（industry view のみ industry name partial match） (j) ページング・ソートヘッダー・空件数は Phase 3 と同じパターン (k) `templates/layout-v2.html` のサイドバーナビ「株価評価」リンク先を `/v3/valuation` に切替 (l) ValuationPresenterTest と ViewServiceTest に新規メソッドのテストを追加 |
| **後回し** | (1) DataTables 由来の CSV/Excel/PDF/Print/ColVis 機能は **完全廃止**（マスタープラン承認済） (2) 件数選択 UI は Phase 3 同様 25 件固定（Phase 7 で UI 化） (3) Litepicker による期間絞り込みは Phase 4 では未対応（既存も target=industry 等の絞り込みのみ）。期間絞り込み UI は Phase 7 で再評価 (4) Playwright スナップショット（Phase 8） |
| **対象外** | (A) 旧 [valuation.html](src/main/resources/templates/valuation.html) / [ValuationPresenter `/v2/valuation`](src/main/java/github/com/ioridazo/fundanalyzer/web/presenter/ValuationPresenter.java) / `templates/layout.html` の編集 (B) DAO / SQL / DB スキーマ・ViewValuationUseCase / Specification の挙動変更 (C) 各列の表示書式変更（差分の +/- 色分け等は新画面でも維持・既存の意味的な変更は対象外） (D) 認証認可機能の新規導入 (E) Jenkinsfile のあらゆる変更 (F) `static/dist`・`static/plugins/` の削除（Phase 7） (G) 他 4 画面（index 既完了 / corporate / edinet / edinet-detail / error）の移植 |

### 設計方針（Phase 3 パターン拡張）

#### 1. ViewService 拡張

```java
record CompanyValuationTableQuery(String target, String keyword, String view, Pageable pageable) {}
record CompanyValuationTablePage(List<CompanyValuationViewModel> rows, int totalPages, long totalElements, int pageNumber, int pageSize, Sort sort, String view) {}

record IndustryValuationTableQuery(String keyword, Pageable pageable) {}
record IndustryValuationTablePage(List<IndustryValuationViewModel> rows, int totalPages, long totalElements, int pageNumber, int pageSize, Sort sort) {}

CompanyValuationTablePage findCompanyValuationTable(CompanyValuationTableQuery query);
IndustryValuationTablePage findIndustryValuationTable(IndustryValuationTableQuery query);
```

`findCompanyValuationTable` は target に応じた既存メソッドから List を取得し、view ごとに異なる sort field ホワイトリスト + filter ロジックを適用。

#### 2. ソート可能列のホワイトリスト（view ごと）

| view | 許可ソート列 |
|---|---|
| stock | code / name / targetDate / stockPrice / differenceFromSubmitDate / submitDateRatio |
| submit | code / name / submitDate / stockPriceOfSubmitDate / grahamIndexOfSubmitDate / corporateValue |
| graham-index | code / name / grahamIndex / grahamIndexOfSubmitDate |
| dividend-yield | code / name / dividendYield |
| industry | industry / averageGrahamIndex 等（実装時に決定） |

#### 3. 2 エンドポイント方式（Phase 3 と同じ）

- `/v3/valuation`: HTML 全体（valuation-v2.html）
- `/v3/valuation/table`: fragment 部分更新（view ごとに異なる fragment 名）

#### 4. URL 同期

`hx-push-url="true"` で target / view / q / page / sort をすべて URL に同期。リロード時に同じ状態が復元される。

---

## ステップ 2: プロトタイピング

実機ブラウザで以下を確認する（Gate 3 §動作確認結果に記録）:

- [ ] `/v3/valuation` が 200 OK で 5 テーブルすべての view 切替動作
- [ ] target タブ（main / all / favorite / industry）切替で stock / submit / graham-index / dividend-yield と industry の表示分岐
- [ ] view タブ（stock / submit / graham-index / dividend-yield）切替が htmx で部分更新
- [ ] 検索ボックスで keyup changed delay:300ms で部分更新
- [ ] 各 view で許可ソート列をクリックすると ASC/DESC 切替
- [ ] 前へ/次へでページング
- [ ] ブラウザリロード後 URL から状態復元
- [ ] ダークモード / レスポンシブ（375 / 768 / desktop）

---

## Gate 1: 影響設計の承認

### レビュアー向けサマリ

- **判断してほしいこと**:
  1. Phase 3 のテーブル汎用パターンを 5 テーブル並列に拡張する設計（record 4 個・ViewService 2 メソッド・5 fragment）の妥当性
  2. view パラメータ追加方式（`/v3/valuation/table?view=stock|...`）と URL に view を載せる運用判断
  3. ソート可能列を view ごとにホワイトリスト化する運用
  4. industry view のみ別 DTO（IndustryValuationViewModel）扱いで record / メソッドを分離する判断
  5. 期間絞り込み UI を Phase 4 ではスコープ外（Phase 7 で再評価）とする判断
- **重要な変更ポイント**:
  1. record 4 個新設（CompanyValuationTableQuery / CompanyValuationTablePage / IndustryValuationTableQuery / IndustryValuationTablePage）
  2. ValuationPresenter に `/v3/valuation` と `/v3/valuation/table` 追加
  3. ViewService に findCompanyValuationTable / findIndustryValuationTable 追加
  4. templates/valuation-v2.html 新設（layout-v2 継承）
  5. templates/fragments/valuation-table.html に 5 fragment（th:fragment）
  6. layout-v2 サイドバー「株価評価」リンク先を /v3/valuation に切替
  7. テスト追加（ValuationPresenterTest / ViewServiceTest）
  8. 旧 /v2/valuation / valuation.html / layout.html / 5 画面のうち他 4 画面は **無変更**
- **確認してほしい観点**:
  1. view × target 組合せの URL 設計が将来 corporate / edinet 詳細にも応用可能か
  2. industry 用 record のみ target 持たない設計（industry は target=industry のときのみ表示）の妥当性
  3. 各 view の表示列が DataTables 廃止後も既存と等価か（差分表示の +/- 色分け含む）

### 重点観点

#### 影響範囲分析

変更属性チェック:

- **参照層: 該当**（ValuationPresenter / ViewService 拡張・新規 record 4 個・新 template 2 ファイル・layout-v2 ナビ）
- **状態層: 該当なし**（業務状態遷移なし）
- **データ層: 該当なし**（DB スキーマ・既存データ・Doma DAO 無変更）

##### 参照層分析結果

| 対象 | 参照箇所 | 影響 |
|---|---|---|
| ValuationPresenter.java | `/v2/valuation` 単独 GET | 中：`/v3/valuation` と `/v3/valuation/table` 追加（既存メソッド無変更） |
| ViewService | 各 Presenter から呼出 | 中：findCompanyValuationTable / findIndustryValuationTable 追加（既存メソッド無変更） |
| record 4 個（新設） | Presenter から呼出 | 大：本 Phase の中核 |
| templates/valuation-v2.html（新設） | `/v3/valuation` で参照 | 大：本 Phase の中核 |
| templates/fragments/valuation-table.html（新設） | valuation-v2.html から replace + `/v3/valuation/table` で fragment 返却 | 大：5 fragment |
| layout-v2.html サイドバーナビ | 「株価評価」リンク先 | 小：1 行修正 |
| 旧 valuation.html / layout.html / 4 画面 | — | **無変更** |

##### 状態層・データ層分析結果

該当なし。

#### インフラ影響チェック

| カテゴリ | 判定 | 内容 |
|---|---|---|
| **A. 処理時間** | 該当 | (1) Phase 3 と同じくメモリ内 stream で処理 (2) 5 view それぞれの sort 処理は 1 リクエストあたり 1 view 分なので Phase 3 と同程度 |
| **B〜F** | 該当なし | Phase 3 と同様 |
| **G. セキュリティ** | 該当 | view パラメータをホワイトリスト（stock / submit / graham-index / dividend-yield / industry）で検証・任意値は stock にフォールバック・他は Phase 3 と同様 |
| **H〜J** | 該当なし | Phase 3 と同様 |

#### 依存追加判断

該当なし。

#### 三本柱

##### テスト戦略

| 種別 | 採用 |
|---|---|
| 既存 49+ 件未変更 | ✅ |
| ValuationPresenterTest 追加（v3 メソッド・view 切替・パラメータ検証） | ✅ |
| ViewServiceTest 追加（findCompanyValuationTable / findIndustryValuationTable・view ごとのソート列ホワイトリスト・filter・page） | ✅ |
| Playwright スナップショット | ⭕ Phase 8 |

カバレッジ目標: 新規追加コードで 80% 以上。

##### セキュリティ方針

| 観点 | 採用 |
|---|---|
| view パラメータホワイトリスト | ✅（stock / submit / graham-index / dividend-yield / industry） |
| sort フィールドホワイトリスト（view ごと） | ✅ |
| 入力検証（page / size / sort） | ✅（Phase 3 と同じパターン） |
| Thymeleaf 標準エスケープ | ✅ |
| 認証認可 | ❌（対象外） |

##### ドキュメント計画

| ドキュメント | 対応 |
|---|---|
| 本 Phase 4 サブタスク md | 一次情報源 |
| マスタープラン §サブタスク追跡表 | Phase 4 完了時に更新 |
| ADR-001 / CLAUDE.md | 無変更 |

#### スコープ確定

§ステップ 1 のスコープ表に従う。

### レビュアー記入欄

- 承認者: iori-oiso（プロジェクトオーナー）
- レビュー依頼日: 2026-05-01
- 回答日: 2026-05-01
- 結論: 合格
- コメント: 5 テーブル並列拡張・view パラメータ方式・ソート列ホワイトリスト・industry 分離・期間絞り込み Phase 7 後回しすべて承認。Phase 4 実装着手して可。

---

## Gate 2: 完了条件の確認

### 運用ルート

**インライン**（マスタープラン Gate 1 で全体方針承認済・Phase 3 のパターンを踏襲）。

### 完了条件

#### 機能

- [ ] ValuationPresenter に `/v3/valuation` と `/v3/valuation/table` の 2 エンドポイント追加
- [ ] ViewService に findCompanyValuationTable と findIndustryValuationTable を追加（既存メソッド無変更）
- [ ] record 4 個（CompanyValuationTableQuery / CompanyValuationTablePage / IndustryValuationTableQuery / IndustryValuationTablePage）新設
- [ ] templates/valuation-v2.html が layout-v2 継承で target タブ + view タブ + 検索 + テーブル
- [ ] templates/fragments/valuation-table.html に 5 fragment（stock-table / submit-table / graham-index-table / dividend-yield-table / industry-table）
- [ ] layout-v2 サイドバー「株価評価」リンク先を /v3/valuation に切替
- [ ] 旧 /v2/valuation / valuation.html / layout.html / 4 画面は無変更

#### テスト

- [ ] `./mvnw clean test` 既存 + Phase 3 + Phase 4 追加テストすべて緑
- [ ] ValuationPresenterTest に v3 系テスト追加
- [ ] ViewServiceTest に findCompanyValuationTable / findIndustryValuationTable のテスト追加
- [ ] dev 起動 + Claude Preview で実機動作確認

#### ドキュメント

- [ ] 本 Phase 4 サブタスク md に Gate 1 / Gate 2 / Gate 3 通過記録
- [ ] マスタープラン §サブタスク追跡表 Phase 4 行更新

#### スコープ外

- 旧 valuation.html / ValuationPresenter `/v2/valuation` / layout.html / 4 画面の編集
- DAO / SQL / DB スキーマ / ViewValuationUseCase の挙動変更
- DataTables Buttons / 件数選択 UI / 期間絞り込み UI（Phase 7）
- Jenkinsfile のあらゆる変更
- 認証認可機能

### レビュアー記入欄

- 承認者: <氏名・役割>
- レビュー依頼日: -
- 回答日: -
- 結論: -
- コメント: -

---

## ステップ 5: 実行サイクル

### コミット履歴

| # | コミット | 概要 |
|---|---|---|
| 0 | `1ddcfa75` | docs: Phase 4 サブタスク md を起票する |
| 1 | `7bca65a2` | feat: ViewService に findCompanyValuationTable / findIndustryValuationTable と record 4 個を追加する |
| 2 | `5898a335` | feat: ValuationPresenter に /v3/valuation と /v3/valuation/table を追加する |
| 3 | `e2836110` | feat: valuation-v2.html と fragments/valuation-table.html を作成する |
| 4 | `4cfc282b` | feat: layout-v2 サイドバーナビの株価評価リンク先を /v3/valuation に切り替える |
| 5 | `b2b7cae1` | test: ValuationPresenter v3 / ViewService valuation のテストを追加する |

ブランチ: `feature/screen-renewal-phase4-valuation-htmx`（develop から派生）

最終的な Squash Merge 時の 1 コミット要約: `feat: 画面刷新 Phase 4 で株価評価 (/v3/valuation) を Tailwind+htmx に移植し 5 テーブル並列を実現する`

---

## ステップ 6: 多軸検証

| 観点 | 結果 | 担当 | 確認内容 |
|---|---|---|---|
| **観点 1: コード品質** | OK（AI 実施） | AI | record 4 個 immutable + Javadoc / Presenter 責務集中 / Map.of で view ホワイトリスト静的定義 / var 不使用・final 明示 |
| **観点 2: テスト構造品質** | OK（AI 実施） | AI | 既存 Phase 1〜3 テスト未変更。新規 +20 件（ValuationPresenterTest 11 / ViewServiceTest 9）。`./mvnw test` 76 件全パス |
| **観点 3: 機能完全性** | OK（AI 実施） | AI | Gate 2 §完了条件 §機能 すべて達成。スコープ外（旧 /v2/valuation / 4 画面 / DAO・SQL・DB / Specification 変更 / Jenkinsfile / 認証認可）に手をつけず |
| **観点 4: セキュリティ** | OK（AI 実施） | AI | view パラメータ 5 値ホワイトリスト / sort field view ごとホワイトリスト / page・size クランプ / Thymeleaf 標準エスケープ |
| **観点 5: ドキュメント整合性** | OK（AI 実施） | AI | 本 md 一次情報源 / マスタープラン追跡表 最終コミット更新 / ADR-001・CLAUDE.md 無変更 |

---

## Gate 3: 最終確認

### レビュアー向けサマリ

- **判断してほしいこと**: 5 テーブル並列でテーブル汎用パターンが正しく機能しているか / Phase 5（edinet）以降への適用準備として view パラメータ運用が妥当か
- **重要な変更ポイント**:
  1. record 4 個（CompanyValuationTableQuery / CompanyValuationTablePage / IndustryValuationTableQuery / IndustryValuationTablePage）
  2. ViewService 2 メソッド追加 / ValuationPresenter v3 2 エンドポイント追加
  3. valuation-v2.html + 5 fragment（stock-table / submit-table / graham-index-table / dividend-yield-table / industry-table）+ 共通 fragment（sortable-th / paginator / header / empty）
  4. layout-v2 サイドバー /v3/valuation 切替
  5. テスト 20 件追加
  6. 旧 /v2/valuation / valuation.html / layout.html / 4 画面は無変更

### 重点観点

#### 差分レビュー

```
git log --oneline develop..HEAD

b2b7cae1 test: ValuationPresenter v3 / ViewService valuation のテストを追加する
4cfc282b feat: layout-v2 サイドバーナビの株価評価リンク先を /v3/valuation に切り替える
e2836110 feat: valuation-v2.html と fragments/valuation-table.html を作成する
5898a335 feat: ValuationPresenter に /v3/valuation と /v3/valuation/table を追加する
7bca65a2 feat: ViewService に findCompanyValuationTable / findIndustryValuationTable と record 4 個を追加する
1ddcfa75 docs: Phase 4 サブタスク md を起票する
```

#### 動作確認結果（AI 実施・2026-05-01）

- [x] `./mvnw test`: **76 件全パス**（13.3 秒）
- [x] `/v3/valuation` 200 OK で stock view レンダリング（target タブ + view タブ + 検索 + ソートヘッダー + 空件数）
- [x] `/v3/valuation?target=industry` で industry view 描画（view タブ非表示・「業種名で検索」placeholder・5 列の業種テーブル・dev H2 で **31 件のデータ表示**）
- [x] layout-v2 継承（サイドバー / ヘッダー / main / フッター）
- [x] dev H2 の業種マスタ実データ（その他製品 / その他金融業 / ガラス・土石製品 等）でページング動作確認済

#### 副次影響

- 旧 /v2/valuation / valuation.html / layout.html / 他 4 画面は無変更
- 既存テスト + Phase 1〜3 テストは未変更
- DAO / SQL / DB スキーマ無変更
- DataTables Buttons / 期間絞り込み UI は新画面に存在しない

#### ドキュメント整合性

- [x] 本 md（一次情報源）
- [x] マスタープラン §サブタスク追跡表（最終コミットで更新）
- [x] ADR-001 / CLAUDE.md 無変更

### レビュアー記入欄

- 承認者: iori-oiso（プロジェクトオーナー）
- レビュー依頼日: 2026-05-01
- 回答日: 2026-05-01
- 結論: 合格
- コメント: 5 テーブル並列でテーブル汎用パターンが正しく機能。dev H2 で 31 件の業種マスタ実データを確認できた。Phase 5（edinet 2 画面）へ進めて可。

---

## 添付ファイル

`docs/notes/T20260429-screen-renewal-phase4-attachments/` 配下に配置予定。

---

## 更新履歴

- 2026-05-01: 初版作成（ステップ 1〜2・Gate 1・Gate 2 セクション記載・コミット計画策定・5 テーブル × 4 view + 1 industry の構造分析）
- 2026-05-01: Gate 1 / Gate 2（インライン）承認記録（iori-oiso・合格）
- 2026-05-01: 6 コミット（1ddcfa75 / 7bca65a2 / 5898a335 / e2836110 / 4cfc282b / b2b7cae1）を `feature/screen-renewal-phase4-valuation-htmx` ブランチで実装。`./mvnw test` 76 件全パス・Claude Preview で /v3/valuation stock view + /v3/valuation?target=industry の実機動作確認 OK（dev H2 の業種マスタ 31 件データで実データ表示）。ステップ 5 §コミット履歴・ステップ 6 §多軸検証・Gate 3 §動作確認結果（AI 実施分）を記載
- 2026-05-01: コミット 6（d6fb7bcb）で実装ログ反映。Gate 3 承認記録（iori-oiso・合格）
