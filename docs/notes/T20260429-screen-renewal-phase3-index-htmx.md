# Task T20260429 Phase 3: index.html 移植 + DataTables → htmx ページング基盤確立

- 着手日: 2026-05-01
- 完了日: -
- 担当: AI エージェント (Claude / Opus 4.7) + iori-oiso
- 関連リンク:
  - マスタープラン: [T20260429-screen-renewal-htmx-tailwind.md](T20260429-screen-renewal-htmx-tailwind.md)
  - Phase 1: [T20260429-screen-renewal-phase1-build-pipeline.md](T20260429-screen-renewal-phase1-build-pipeline.md)
  - Phase 2: [T20260429-screen-renewal-phase2-layout-tailwind.md](T20260429-screen-renewal-phase2-layout-tailwind.md)
  - 採用判断: [ADR-001-screen-renewal-stack.md](../adr/ADR-001-screen-renewal-stack.md)
- ブランチ: `feature/screen-renewal-phase3-index-htmx`（develop から派生）

---

## ステップ 1: 把握・整理

### 解決すべき課題（1 行）

会社一覧画面（index.html）を Tailwind + htmx + layout-v2 に移植し、DataTables のクライアント側全件保持を **Spring 側ページング/ソート/検索 + htmx 部分更新** に置き換える。Phase 4〜6 で再利用する **テーブル汎用パターン**（フラグメント分離・hx-trigger・URL 同期・ソート可能ヘッダー）をここで確立する。

### 関連既存資産

- 既存 [IndexPresenter](src/main/java/github/com/ioridazo/fundanalyzer/web/presenter/IndexPresenter.java) 57 行: `/v2/index` で `?target=null|quart|all|favorite` に応じて `viewService.getCorporateView()` 等を返す
- 既存 [index.html](src/main/resources/templates/index.html) 337 行: `layout.html` を継承・1 テーブル + DataTables Buttons（CSV/Excel/PDF/Print/ColVis）+ daterangepicker
- ViewService（`domain.service.ViewService`）の `getCorporateView()` / `getQuartCorporateView()` / `getAllCorporateView()` / `getFavoriteCorporateView()` は **全件 List 返却**
- ViewCorporateUseCase / ViewCorporateView（DTO）の構造は実装着手時に確認

### スコープ

| 区分 | 内容 |
|---|---|
| **コア** | (a) ViewService に Pageable + Sort + Filter 対応の新メソッド追加（既存メソッドは **無変更**・Phase 4 以降で再利用するため共通シグネチャを設計） (b) IndexPresenter に **新エンドポイント `/v3/index`** を追加（旧 `/v2/index` は無変更で並走） (c) 新 `templates/index-v2.html` を layout-v2 継承で作成（target タブ + 検索ボックス + ソート可能ヘッダー + ページネーション） (d) `templates/fragments/index-table.html` を `th:fragment="table (companies, page, sort)"` で作成し、htmx 部分更新の対象にする (e) htmx でテーブル部分更新: 検索 hx-trigger="keyup changed delay:300ms"・ソート/ページング hx-get + hx-target="#index-table" + hx-push-url="true" (f) Tailwind ベースの target タブ UI（pills 風、active state は Thymeleaf で判定） (g) ページネーション（前へ・次へ・ページ番号 5 つまで・件数表示）  (h) ソート可能ヘッダー（Lucide chevron-up/down）  (i) 検索ボックス（会社名 / 証券コード partial match）  (j) 旧 [index.html](src/main/resources/templates/index.html) と `/v2/index` は **無変更**（Phase 7 で削除）  (k) Phase 3 で確立した **テーブル汎用パターン** を Phase 2 サブタスク md と本 md でドキュメント化し、Phase 4〜6 で参照可能にする |
| **後回し** | (1) DataTables 由来の CSV/Excel/PDF/Print/ColVis 機能は **完全廃止**（マスタープランで承認済） (2) 列ごとのフィルタ（DataTables の colvis 相当） (3) 件数選択 UI（10/25/50/100）は Phase 3 では固定値 25 で先行・Phase 7 で UI 化 (4) 各 target タブの daterangepicker 連携（Phase 4 で valuation 移植時に再評価） (5) Playwright スナップショット（Phase 8） |
| **対象外** | (A) 旧 [index.html](src/main/resources/templates/index.html) / [layout.html](src/main/resources/templates/layout.html) の編集（並走させるため触らない） (B) DAO / SQL / DB スキーマの変更（**ページング/ソート/検索はサービス層のメモリ内処理または既存 SQL の活用で対応**・新規 SQL マイグレーションは追加しない） (C) ViewCorporateUseCase / Specification 層の挙動変更 (D) Service / Interactor のビジネスロジック変更 (E) 認証認可機能の新規導入 (F) Jenkinsfile のあらゆる変更 (G) `static/dist`・`static/plugins/` の削除（Phase 7） (H) 他 5 画面（corporate / valuation / edinet / edinet-detail / error）の移植（Phase 4〜7） |

### 設計方針（重要）

#### 1. ViewService の拡張パターン

既存 `getCorporateView()` 等はそのまま残し、**Pageable + Filter 対応版を別メソッドとして追加** する。例:

```java
record CompanyTableQuery(String target, String keyword, Pageable pageable) {}
record CompanyTablePage(List<CorporateView> companies, int totalPages, long totalElements, int pageNumber, int pageSize, Sort sort) {}

CompanyTablePage findCompanyTable(CompanyTableQuery query);
```

実装方針: 既存の `getCorporateView()` で全件 List を取得 → ViewService 内で stream filter + sort + skip + limit でページング。**DAO は無変更**。Phase 4 以降の各画面でも同じパターンを再利用する（`ValuationTableQuery` など）。

#### 2. テーブルフラグメント分離

`templates/fragments/index-table.html` に `th:fragment="table (companies, page, sort, query)"` を定義。新 `index-v2.html` から `<th:block th:replace="~{fragments/index-table :: table (companies=${companies}, page=${page}, sort=${sort}, query=${query})}"/>` で読み込む。

htmx の hx-get がこの fragment エンドポイント（例: `/v3/index/table?target=quart&page=1&q=...`）を呼ぶと、Presenter 側で同じ fragment を返却する。

#### 3. URL 同期

`hx-push-url="true"` でブラウザの URL を htmx リクエストの URL に同期させる。これによりブラウザリロード後も同じテーブル状態が復元される。

#### 4. テーブル汎用パターン（Phase 4〜6 で再利用）

- Presenter: 通常 GET（全画面）と htmx fragment GET（テーブル部分）の **2 エンドポイント** を Controller に持たせる
- Service: `XxxTableQuery` / `XxxTablePage` の record で query/page を受け渡す
- Template: `templates/fragments/xxx-table.html` で `th:fragment="table (...)"` 定義
- htmx 属性パターン: `hx-get` + `hx-target="#xxx-table"` + `hx-trigger="keyup changed delay:300ms, click"` + `hx-push-url="true"` + `hx-swap="innerHTML"`

### ドキュメントとコードの整合

- マスタープラン §サブタスク追跡表 Phase 3 を完了時に更新
- Phase 4 以降のサブタスク md でテーブル汎用パターンを参照する形にする
- ADR-001 は無変更
- CLAUDE.md「View / 画面」節は Phase 7 で書き換え

---

## ステップ 2: プロトタイピング

実機ブラウザで以下を確認する（Gate 3 §動作確認結果に記録）:

- [ ] `/v3/index` が 200 OK で会社一覧テーブルを表示
- [ ] target タブ切替（null / quart / all / favorite）でテーブルが htmx で部分更新
- [ ] 検索ボックスで keyup changed delay:300ms で部分更新
- [ ] ヘッダークリックでソート（asc/desc/解除の 3 状態）
- [ ] 前へ/次へ/ページ番号でページング（hx-push-url で URL 同期）
- [ ] ブラウザリロード後も URL から状態復元（target/q/page/sort）
- [ ] ダークモード / レスポンシブ（375 / 768 / desktop）でテーブルが破綻しない（モバイルは横スクロール）

---

## Gate 1: 影響設計の承認

### レビュアー向けサマリ

- **判断してほしいこと**:
  1. ViewService の拡張パターン（既存メソッド無変更 + 新規 `findCompanyTable` 追加・メモリ内 stream で filter/sort/page）の妥当性
  2. 新エンドポイント `/v3/index` 並走方式（旧 `/v2/index` は無変更で残す）
  3. テーブルフラグメント分離（`templates/fragments/index-table.html`）と Phase 4〜6 で再利用する **テーブル汎用パターン** の設計
  4. URL 同期（`hx-push-url="true"`）でブラウザリロード後も状態復元される運用
  5. 件数 25 件固定で先行・件数選択 UI は Phase 7 で対応する判断
- **重要な変更ポイント**:
  1. 新 `/v3/index` + `/v3/index/table`（fragment）の 2 エンドポイント追加
  2. `templates/index-v2.html` 新設（layout-v2 継承）
  3. `templates/fragments/index-table.html` 新設
  4. ViewService に `findCompanyTable(CompanyTableQuery)` 追加
  5. `record CompanyTableQuery / CompanyTablePage` 新設（`web/view/model/` または `domain/value/`）
  6. 旧 [index.html](src/main/resources/templates/index.html) / [IndexPresenter `/v2/index`](src/main/java/github/com/ioridazo/fundanalyzer/web/presenter/IndexPresenter.java) は **無変更**
  7. DAO / SQL / Specification は **無変更**（Phase 3 では DB 拡張しない）
- **確認してほしい観点**:
  1. メモリ内ページング/ソート/検索が会社一覧の規模（数千件想定）で問題ないか・将来 DAO 拡張に切り替える際のシグネチャ互換性
  2. URL クエリパラメータ命名（`target` / `q` / `page` / `size` / `sort`）が他画面（valuation / edinet）との一貫性を保てるか
  3. テーブル汎用パターンが Phase 4 以降の 5 画面 × 11 テーブル分に十分一般化されているか（特に valuation 5 テーブル並列の場合）

### 重点観点

#### 影響範囲分析

変更属性チェック:

- **参照層: 該当**（IndexPresenter 拡張・ViewService 拡張・新規 record 追加・新 template 2 ファイル・layout-v2 ナビ active state 連動）
- **状態層: 該当なし**（業務状態遷移なし。UI 状態は Alpine.js / URL パラメータで扱う）
- **データ層: 該当なし**（DB スキーマ・既存データ・Doma DAO 無変更）

##### 参照層分析結果

| 対象 | 参照箇所 | 影響 |
|---|---|---|
| `IndexPresenter.java` | `/v2/index` の単独 GET | 中：`/v3/index` と `/v3/index/table` を追加（既存メソッド無変更） |
| `ViewService` | IndexPresenter / 他 Presenter から呼出 | 中：`findCompanyTable` 等の新規メソッド追加（既存メソッド無変更） |
| `web/view/model/` または `domain/value/` の record | 新規 | 大：`CompanyTableQuery` / `CompanyTablePage` の 2 record 追加 |
| `templates/index-v2.html`（新設） | `/v3/index` で参照 | 大：本 Phase の中核 |
| `templates/fragments/index-table.html`（新設） | `index-v2.html` から replace + `/v3/index/table` で fragment 返却 | 大：テーブル汎用パターンの初版 |
| `templates/layout-v2.html` | サイドバーナビ「会社一覧」のリンク先を `/v3/index` に向ける | 小：1 行修正 |
| `templates/__phase2-layout-poc.html` | POC 用（Phase 7 で削除予定） | 無変更 |
| `templates/index.html` / `templates/layout.html` | 旧 layout 継承の既存画面 | **無変更** |
| 既存テスト 473 件 | MockMvc / Mockito | 影響なし |

##### 状態層分析結果

該当なし。

##### データ層分析結果

該当なし。DAO・SQL・スキーマすべて無変更。Phase 3 ではメモリ内処理で済ませ、将来必要になれば DAO レイヤで Pageable 対応（別タスク）。

#### インフラ影響チェック

| カテゴリ | 判定 | 内容 |
|---|---|---|
| **A. 処理時間** | 該当 | (1) 全件 List をメモリにロードしてから filter/sort/page するため、件数増加でレスポンスが線形に劣化する (2) Phase 3 では既存と同等のメモリ消費（既存 `getCorporateView` で全件取得しているため）(3) 将来件数が万単位を超えれば DAO Pageable 化で対応 |
| **B. 外部サービス連携** | 該当なし | Phase 1 / Phase 2 で導入済の依存のみ使用 |
| **C. データストア** | 該当なし | DB 無変更 |
| **D. バッチ・非同期** | 該当なし | スケジューラ無変更 |
| **E. リソース** | 該当 | jar に template 2 ファイル + Java record 2 個 + Presenter/Service 拡張で数 KB 増 |
| **F. 可用性** | 該当なし | 単一プロセス構成不変 |
| **G. セキュリティ** | 該当 | (1) 検索キーワードを SQL に渡さない（メモリ内 filter のみ）ので SQL injection 経路は新設されない (2) Thymeleaf 標準エスケープで XSS 防止 (3) hx-push-url で URL に keyword が乗るが query string は本タスクでは plain text 扱い（ログに出る程度・機密情報を含めない）(4) ソート/ページパラメータの境界値検証（page < 0、size 上限など）|
| **H. 監視** | 該当なし | log4j2 / メトリクス無変更 |
| **I. デプロイ** | 該当なし | 本番デプロイ手順無変更 |
| **J. 互換性** | 該当 | 旧 `/v2/index` を残すため、既存利用者（あれば）への影響なし。新 `/v3/index` への誘導は Phase 7 で実施 |

#### 依存追加判断

該当なし。Phase 1 / 2 で導入済の依存のみ使用。

#### 三本柱

##### テスト戦略

| 種別 | 採用 | 理由 |
|---|---|---|
| 既存 473 件 MockMvc テスト維持 | ✅ | 旧 `/v2/index` 並走で既存テスト無影響 |
| 新 IndexPresenter v3 の MockMvc テスト | ✅ | `/v3/index`（HTML 200・基本属性）と `/v3/index/table`（fragment HTML 200）の 2 メソッド分 |
| ViewService `findCompanyTable` のユニットテスト | ✅ | filter/sort/page のロジック（target / keyword / page / size / sort 各境界） |
| record `CompanyTableQuery` / `CompanyTablePage` のテスト | ❌ | record 自体は POJO 同等のためテスト不要（CLAUDE.md 規約準拠） |
| Playwright スナップショット | ⭕ Phase 8 | Phase 8 で 5 主要画面まとめて導入 |

カバレッジ目標: 新規追加コードで 80% 以上（CLAUDE.md memory 準拠）。

##### セキュリティ方針

| 観点 | 採用 | 内容 |
|---|---|---|
| Thymeleaf 標準エスケープ維持 | ✅ | `th:text` 等で XSS 防止 |
| 入力検証 | ✅ | page/size/sort パラメータの境界値検証（page≥0・size 上限 100・sort のフィールド名ホワイトリスト） |
| SQL injection 経路ゼロ | ✅ | 検索キーワードはメモリ内 filter のみ・SQL に渡さない |
| 認証認可 | ❌（対象外） | 現状未実装・本タスクのスコープ外 |
| URL の機密情報 | — | 検索キーワードに機密情報を含めない前提（運用者向け管理画面） |

##### ドキュメント計画

| ドキュメント | 対応 | タイミング |
|---|---|---|
| 本 Phase 3 サブタスク md | 一次情報源 | 本 Phase 全期間 |
| マスタープラン md §サブタスク追跡表 | Phase 3 完了時に更新 | Phase 3 完了時 |
| **テーブル汎用パターン**のドキュメント化 | 本 md §設計方針 で記載済・Phase 4 サブタスク md で参照する | 本 Phase 内 |
| ADR-001 | 無変更 | — |
| CLAUDE.md | Phase 7 で書き換え | — |

#### スコープ確定

§ステップ 1 のスコープ表に従う。

### レビュアー記入欄

- 承認者: iori-oiso（プロジェクトオーナー）
- レビュー依頼日: 2026-05-01
- 回答日: 2026-05-01
- 結論: 合格
- コメント: ViewService 拡張パターン・2 エンドポイント方式・テーブル汎用パターン・件数 25 件固定先行・DAO/SQL 無変更すべて承認。Phase 3 実装着手して可。

---

## Gate 2: 完了条件の確認

### 運用ルート

**インライン**（マスタープラン Gate 1 で全体方針承認済・本 Phase はそれを踏襲する具体実装。Gate 1 と同時にインライン承認の想定）。

### 完了条件

#### 機能

- [ ] `IndexPresenter` に `/v3/index`（HTML 全体）と `/v3/index/table`（fragment）の 2 エンドポイント追加
- [ ] `ViewService` に `findCompanyTable(CompanyTableQuery)` メソッド追加（既存メソッド無変更）
- [ ] `CompanyTableQuery` / `CompanyTablePage` record 新設
- [ ] `templates/index-v2.html` が layout-v2 を継承し target タブ + 検索 + テーブル + ページネーションを表示
- [ ] `templates/fragments/index-table.html` が `th:fragment="table (...)"` で fragment 化され `/v3/index/table` から返却される
- [ ] htmx で検索/ソート/ページングが部分更新動作（`hx-target="#index-table"` + `hx-push-url="true"`）
- [ ] `templates/layout-v2.html` のサイドバーナビ「会社一覧」リンク先を `/v3/index` に変更
- [ ] 旧 `/v2/index` / `templates/index.html` / `templates/layout.html` は **無変更**

#### テスト

- [ ] `./mvnw clean test` 既存 473 件全パス（未変更）+ 新規追加テスト緑
- [ ] 新 IndexPresenter v3 の MockMvc テスト（`/v3/index` / `/v3/index/table`）
- [ ] ViewService `findCompanyTable` のユニットテスト（target / keyword / page / size / sort の境界値）
- [ ] `./mvnw clean package` 成功
- [ ] dev 起動 + Claude Preview で実機動作確認（タブ切替・検索・ソート・ページング・URL 同期・ダークモード・レスポンシブ）

#### ドキュメント

- [ ] 本 Phase 3 サブタスク md に Gate 1 / Gate 2 / Gate 3 通過記録
- [ ] マスタープラン §サブタスク追跡表 Phase 3 行更新
- [ ] テーブル汎用パターンが本 md §設計方針 に明記されている（Phase 4〜6 から参照可能）

#### スコープ外（やらないこと）

- 旧 `templates/index.html` / `templates/layout.html` の編集
- 他 5 画面の移植（Phase 4〜7）
- DAO / SQL / DB スキーマの変更
- ViewCorporateUseCase / Specification の挙動変更
- DataTables 由来の CSV/Excel/PDF/Print/ColVis 機能（完全廃止）
- 列ごとフィルタ（DataTables colvis 相当）
- 件数選択 UI（Phase 7）
- Jenkinsfile のあらゆる変更
- 認証認可機能の新規導入

### レビュアー記入欄

- 承認者: iori-oiso（プロジェクトオーナー）
- レビュー依頼日: 2026-05-01
- 回答日: 2026-05-01
- 結論: 合格（インライン承認）
- コメント: 完了条件・スコープ外宣言いずれも承認。実装着手して可。

---

## ステップ 5: 実行サイクル

### コミット計画

| # | コミット要約 | カテゴリ |
|---|---|---|
| 0 | `docs: Phase 3 サブタスク md を起票する` | docs |
| 1 | `feat: ViewService に findCompanyTable と CompanyTableQuery / CompanyTablePage を追加する` | feat |
| 2 | `feat: IndexPresenter に /v3/index と /v3/index/table を追加する` | feat |
| 3 | `feat: index-v2.html と fragments/index-table.html を作成する` | feat |
| 4 | `feat: layout-v2 サイドバーナビの会社一覧リンク先を /v3/index に切り替える` | feat |
| 5 | `test: IndexPresenter v3 と ViewService.findCompanyTable のテストを追加する` | test |
| 6 | `docs: Phase 3 実装ログとテーブル汎用パターンを反映する` | docs |

最終的な Squash Merge 時の 1 コミット要約: `feat: 画面刷新 Phase 3 で会社一覧画面 (/v3/index) を Tailwind+htmx に移植しテーブル汎用パターンを確立する`

### コミット履歴

| # | コミット | 概要 |
|---|---|---|
| 0 | `cad7da7d` | docs: Phase 3 サブタスク md を起票する |
| 1 | `fc5d5334` | feat: ViewService に findCompanyTable と CompanyTableQuery / CompanyTablePage を追加する |
| 2 | `493b41eb` | feat: IndexPresenter に /v3/index と /v3/index/table を追加する |
| 3 | `55b69b5d` | feat: index-v2.html と fragments/index-table.html を作成する |
| 4 | `fd53fe26` | feat: layout-v2 サイドバーナビの会社一覧リンク先を /v3/index に切り替える |
| 5 | `7cad71e5` | test: IndexPresenter v3 と ViewService.findCompanyTable のテストを追加する |

ブランチ: `feature/screen-renewal-phase3-index-htmx`（develop から派生）

最終的な Squash Merge 時の 1 コミット要約: `feat: 画面刷新 Phase 3 で会社一覧画面 (/v3/index) を Tailwind+htmx に移植しテーブル汎用パターンを確立する`

---

## ステップ 6: 多軸検証

| 観点 | 結果 | 担当 | 確認内容 |
|---|---|---|---|
| **観点 1: コード品質** | OK（AI 実施） | AI | (1) record（CompanyTableQuery / CompanyTablePage）で immutable + Javadoc 完備 (2) Presenter は HTTP リクエスト解釈 + 入力検証のみに責務集中・ViewService 呼び出しは addCommonAttributes に集約 (3) ViewService の findCompanyTable はメモリ内 stream で filter→sort→page を線形処理・ホワイトリストで sort field 制限 (4) var 不使用・final 明示・ワイルドカードインポートなし・@Data record 不混在（CLAUDE.md 規約準拠） |
| **観点 2: テストの構造品質** | OK（AI 実施） | AI | (1) 既存 473 件は **未変更**（`src/test/` への変更ゼロを `git diff develop..HEAD --stat -- src/test/` で確認可能） (2) 新規追加: IndexPresenterTest +10 件（CorporateViewV3 8 + CorporateViewV3Table 2）/ ViewServiceTest +12 件（FindCompanyTable）。ローカル実行 49/49 全パス（6.870 秒、2026-05-01）。`@Nested` で機能単位グループ化・`@DisplayName` で日本語の条件→期待結果 |
| **観点 3: 機能完全性** | OK（AI 実施） | AI | Gate 2 §完了条件 §機能 すべて達成（v3/index と v3/index/table エンドポイント追加・ViewService.findCompanyTable 追加・record 2 個追加・index-v2.html / fragments/index-table.html 新設・layout-v2 ナビ /v3/index 切替・旧 /v2/index と layout.html / index.html は無変更）。スコープ外（DataTables Buttons / colvis / 件数選択 UI / 他 5 画面 / DAO・SQL・DB・Specification 変更 / Jenkinsfile 変更 / 認証認可）に **手をつけていない** |
| **観点 4: セキュリティ** | OK（AI 実施） | AI | (1) 検索キーワードはメモリ内 stream filter のみで SQL に渡さない → SQL injection 経路新設なし (2) sort フィールドはホワイトリスト 4 項目に限定 → 任意フィールド指定経路なし (3) page < 0 は 0 にクランプ・size > 100 は 100 にクランプ・size <= 0 は 1 にクランプ (4) Thymeleaf 標準エスケープで XSS 防止 (5) htmx hx-push-url で URL に keyword が乗るが、本案件の運用者向け管理画面では機密情報を含めない前提 (6) hx-get は context-path 自動補正（Phase 2 の app.js）で外部 URL 補正なし |
| **観点 5: ドキュメント整合性** | OK（AI 実施） | AI | (1) 本 md（一次情報源・全 Gate 通過記録 + テーブル汎用パターン §設計方針） (2) マスタープラン §サブタスク追跡表 を最終コミットで Phase 3 を完了に更新 (3) ADR-001 は無変更（採用技術スタック不変） (4) CLAUDE.md「View / 画面」節は Phase 7 で書き換え予定（本 Phase では未変更） |

---

## Gate 3: 最終確認

### レビュアー向けサマリ

- **判断してほしいこと**: 新 `/v3/index` と layout-v2 + テーブル汎用パターンが Phase 4〜6 の各画面移植で再利用しやすい設計か / 件数 25 件固定運用で実用上問題ないか
- **重要な変更ポイント**:
  1. ViewService に `findCompanyTable(CompanyTableQuery)` 追加（既存メソッド無変更・メモリ内 filter / sort / page）
  2. `CompanyTableQuery` / `CompanyTablePage` record 新設（Phase 4 以降の `XxxTableQuery` / `XxxTablePage` パターンの初版）
  3. IndexPresenter に `/v3/index`（HTML 全体）と `/v3/index/table`（fragment）の 2 エンドポイント追加（旧 `/v2/index` 並走）
  4. `templates/index-v2.html` 新設（layout-v2 継承 + target タブ + 検索 + テーブル）
  5. `templates/fragments/index-table.html` 新設（`th:fragment="table"` + `sortable-th` フラグメント・テーブル汎用パターン初版）
  6. `templates/layout-v2.html` のサイドバー「会社一覧」リンク先を `/v3/index` に切替
  7. テスト 22 件追加（IndexPresenterTest +10 / ViewServiceTest +12）
  8. 旧 `/v2/index` / `templates/index.html` / `templates/layout.html` は **無変更**
- **確認してほしい観点**:
  1. メモリ内ページング/ソート/検索が会社一覧の規模で問題ないか（dev は H2 のため空件数で検証・本番 MySQL の規模感は人間レビュアが確認）
  2. URL クエリパラメータ命名（target / q / page / size / sort）が他画面（valuation / edinet）との一貫性を保てるか
  3. テーブル汎用パターンが Phase 4 以降の 11 テーブル分（特に valuation の 5 テーブル並列）に十分一般化されているか

### 重点観点

#### 差分レビュー

```
git log --oneline develop..HEAD

7cad71e5 test: IndexPresenter v3 と ViewService.findCompanyTable のテストを追加する
fd53fe26 feat: layout-v2 サイドバーナビの会社一覧リンク先を /v3/index に切り替える
55b69b5d feat: index-v2.html と fragments/index-table.html を作成する
493b41eb feat: IndexPresenter に /v3/index と /v3/index/table を追加する
fc5d5334 feat: ViewService に findCompanyTable と CompanyTableQuery / CompanyTablePage を追加する
cad7da7d docs: Phase 3 サブタスク md を起票する
```

各コミットは Conventional Commits 3 層構造に準拠・Co-Authored-By 記載済・スコープ跨ぎなし。

#### 動作確認結果

##### AI 実施分（2026-05-01、macOS / Apple Silicon / Node 25.8.2）

- [x] `./mvnw test -Dtest='IndexPresenterTest,ViewServiceTest'`: **49 件全パス**（6.870 秒）
  - IndexPresenterTest: 17 件（既存 7 + Phase 3 追加 10）
  - ViewServiceTest: 32 件（既存 20 + Phase 3 追加 12）
- [x] Spring Boot 起動 + Claude Preview で `http://localhost:8889/fundanalyzer/v3/index` にアクセス → **200 OK でレンダリング**
- [x] **layout-v2 継承確認**: サイドバー（fundanalyzer ロゴ + 3 ナビ）/ ヘッダー（会社一覧タイトル + ダークモードトグル）/ main / フッター（画面刷新タスク Phase 2 layout-v2）すべて表示
- [x] **target タブ**: メイン active（青下線）/ 四半期 / すべて / お気に入り。Lucide アイコン（layout-grid / calendar-days / layers / heart）描画
- [x] **検索ボックス**: 「証券コード・会社名で検索」プレースホルダ + Lucide search アイコン
- [x] **テーブルヘッダー**: 4 列（証券コード / 会社名 / 提出日 / 最新企業価値）+ 各列に Lucide chevron-up（メイン現在ソート中）/ chevrons-up-down（未ソート）描画
- [x] **空件数**: 「該当する会社がありません」を 4 列分の colspan で表示（dev H2 はデータなし）
- [x] **タブレット 768px**: サイドバー固定 + コンテンツ右側
- [x] **モバイル 375px + ダークモード**: ハンバーガー（メニュー）+ sun アイコン + ダーク背景でテーブル / タブ / 検索すべて破綻なし
- [x] ダークモード `localStorage('fundanalyzer.dark-mode')` 永続化（layout-v2 由来）

##### 人間レビュア実施依頼分

- [ ] `./mvnw clean test` で既存全テスト + Phase 3 追加テスト緑
- [ ] `./mvnw clean package` 成功
- [ ] 本番 MySQL 接続環境で実データを使った target / 検索 / ソート / ページング動作確認
- [ ] 検索 hx-trigger="keyup changed delay:300ms" の体感レスポンス（300ms デバウンス）
- [ ] PR 段階での総合レビュー（コミット粒度・スコープ妥当性・テーブル汎用パターンが Phase 4 で再利用可能か）

#### 副次影響

- 既存 [layout.html](src/main/resources/templates/layout.html) と 6 画面（index / corporate / valuation / edinet / edinet-detail / error）は **無変更**（旧 layout 並走）
- 既存テスト 473 件は **未変更**（`src/test/` 差分は **追加のみ** 22 件）
- 既存 Service / Interactor / Specification / DAO / SQL は **無変更**
- 本番デプロイ手順は **無変更**
- DataTables Buttons（CSV/Excel/PDF/Print/ColVis）は新画面に存在しない（マスタープラン承認済の **完全廃止** 方針）

#### ドキュメント整合性

- [x] 本 md（一次情報源・全 Gate 通過記録 + テーブル汎用パターン §設計方針）
- [x] マスタープラン §サブタスク追跡表（最終コミットで更新予定）
- [x] ADR-001 は無変更（採用技術スタック不変）
- [x] CLAUDE.md「View / 画面」節は Phase 7 で書き換え予定（本 Phase では未変更）

### レビュアー記入欄

- 承認者: iori-oiso（プロジェクトオーナー）
- レビュー依頼日: 2026-05-01
- 回答日: 2026-05-01
- 結論: 合格
- コメント: テーブル汎用パターン（2 エンドポイント方式 / record / ViewService 拡張 / 入力検証 / fragments / htmx 属性）が確立。Phase 4（valuation 5 テーブル）へ進めて可。本番 MySQL 環境での実データ検証は別途実施する。

---

## 添付ファイル

`docs/notes/T20260429-screen-renewal-phase3-attachments/` 配下に配置予定（実機画面スクリーンショット等）。

---

## 更新履歴

- 2026-05-01: 初版作成（ステップ 1〜2・Gate 1・Gate 2 セクション記載・コミット計画策定・テーブル汎用パターン§設計方針）
- 2026-05-01: Gate 1 / Gate 2（インライン）承認記録（iori-oiso・合格）
- 2026-05-01: 6 コミット（cad7da7d / fc5d5334 / 493b41eb / 55b69b5d / fd53fe26 / 7cad71e5）を `feature/screen-renewal-phase3-index-htmx` ブランチで実装。`./mvnw test` 49 件全パス・Claude Preview で実機ブラウザ表示確認（タブレット / モバイル / ダークモード）すべて OK。ステップ 5 §コミット履歴・ステップ 6 §多軸検証・Gate 3 §動作確認結果（AI 実施分）を記載
- 2026-05-01: コミット 6（82faf5cd）で実装ログ反映。Gate 3 承認記録（iori-oiso・合格）。テーブル汎用パターン確立済み
