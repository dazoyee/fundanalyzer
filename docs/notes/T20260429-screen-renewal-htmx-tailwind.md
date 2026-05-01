# Task T20260429: 画面層を Thymeleaf+htmx+Tailwind に刷新（AdminLTE 依存排除）

- 着手日: 2026-04-29
- 完了日: -
- 担当: AI エージェント (Claude / Opus 4.7) + iori-oiso
- 関連リンク: [T20260429-frontend-asset-cleanup.md](T20260429-frontend-asset-cleanup.md)（先行タスク・未参照資産の削除）

---

## ステップ 1: 把握・整理

### 解決すべき課題（1 行）

`templates/*.html` と `static/dist`・`static/plugins/` が AdminLTE / jQuery / Bootstrap 4 / DataTables / daterangepicker / pdfmake などの古いクライアント主導アーキテクチャに強く依存しており、見た目・操作性・保守性のいずれも刷新したい。サーバ主導 (SSR + htmx) への転換と Tailwind CSS への置換で、運用者向け管理画面として近代化する。

### 関連既存資産

- テンプレート: [layout.html](src/main/resources/templates/layout.html) を継承する 6 画面（[index.html](src/main/resources/templates/index.html) / [corporate.html](src/main/resources/templates/corporate.html) / [valuation.html](src/main/resources/templates/valuation.html) / [edinet.html](src/main/resources/templates/edinet.html) / [edinet-detail.html](src/main/resources/templates/edinet-detail.html) / [error.html](src/main/resources/templates/error.html)）
- 静的アセット: `src/main/resources/static/dist/`（AdminLTE 本体）・`src/main/resources/static/plugins/` 12 ディレクトリ
- 画面用 Presenter: [IndexPresenter](src/main/java/github/com/ioridazo/fundanalyzer/web/presenter/IndexPresenter.java) / [CorporatePresenter](src/main/java/github/com/ioridazo/fundanalyzer/web/presenter/CorporatePresenter.java) / [ValuationPresenter](src/main/java/github/com/ioridazo/fundanalyzer/web/presenter/ValuationPresenter.java) / [EdinetPresenter](src/main/java/github/com/ioridazo/fundanalyzer/web/presenter/EdinetPresenter.java) / [EdinetDetailPresenter](src/main/java/github/com/ioridazo/fundanalyzer/web/presenter/EdinetDetailPresenter.java)
- ビュー DTO: `web/view/model/` 配下（先行 PR で record 化済み）
- ビルド: [pom.xml](pom.xml)（frontend-maven-plugin 未導入）/ [Jenkinsfile-ci-prod.groovy](pipeline/Jenkinsfile-ci-prod.groovy) / [Jenkinsfile-dev.groovy](pipeline/Jenkinsfile-dev.groovy) / [Jenkinsfile-cd-prod.groovy](pipeline/Jenkinsfile-cd-prod.groovy)
- CLAUDE.md の記述: 「`static/dist`, `static/plugins` の資産は AdminLTE テーマ由来。**個別ファイルの内容は改変しない**（バージョンアップ時は配布物ごと差し替え）」

### 影響範囲調査結果（参照層）

調査日: 2026-04-29（Explore エージェント実施）

| 観点 | 結果 |
|---|---|
| layout.html 継承画面数 | 7 ファイル（layout 含む） |
| jQuery 初期化箇所 | layout.html:85（読み込み）+ index/valuation/edinet で `$()` 使用 計 5 箇所 |
| DataTables 初期化テーブル数 | 11（index 1 / valuation 5 / edinet 1 + 各種関連） |
| daterangepicker 利用画面 | 2（index, valuation） |
| moment / inputmask 利用画面 | 2（index, valuation） |
| pdfmake / jszip 利用画面 | 3（index, valuation, edinet）— **DataTables Buttons 経由のクライアント側生成のみ。サーバ側に PDF/Excel 出力 Controller は存在しない** |
| Font Awesome `fa-*` クラス | 計 149 件 |
| AdminLTE 専用クラス・属性 | 計 103 件（`hold-transition` `sidebar-mini` `data-widget` `card-widget` 等） |
| Chart.js | corporate.html で 14 個の canvas（CDN: cdnjs Chart.js 3.8.0）。既に利用中 |
| pom.xml の frontend-maven-plugin / Node | 未導入 |
| Jenkinsfile の Node 利用 | 未利用 |

詳細インベントリは Gate 1 §影響範囲分析 に転記する。

### スコープ

| 区分 | 内容 |
|---|---|
| **コア** | (a) frontend-maven-plugin + Node.js + Tailwind CLI のビルドパイプ整備 (b) htmx / Alpine.js / Lucide / Litepicker のローカルバンドル化 (c) layout.html を Tailwind ベースに刷新 (d) 既存 6 画面を Tailwind + htmx に移植（DataTables → サーバページング/ソート/検索、daterangepicker → Litepicker、Font Awesome → Lucide、Bootstrap 4 クラス → Tailwind） (e) jQuery 完全排除 (f) Chart.js を CDN からローカルバンドルへ (g) PDF/Excel/CSV/Print/ColVis ボタン廃止 (h) ダークモード対応（トグル方式） (i) Playwright スナップショットテスト導入（主要 5 画面 × デスクトップ + モバイルの 2 ビューポート） (j) 不要な `static/dist` と `static/plugins/*` の物理削除 (k) CLAUDE.md「View / 画面」「ビルド・テストコマンド」節の更新 (l) **レスポンシブ対応（375px 以上 / sm・md・lg・xl の Tailwind 標準ブレイクポイントで全画面動作）**：サイドバーは md 未満でハンバーガーメニュー（Alpine.js でドロワー開閉）、main エリアはフルイド、テーブルは `overflow-x-auto` で横スクロール許容、Chart.js は親幅追従でリサイズ |
| **後回し** | (1) `prefers-color-scheme` 自動切替（まずトグル先行） (2) Playwright 完全網羅（初版は主要画面 × 2 ビューポート） (3) i18n 対応 (4) CSP ヘッダー導入 (5) モバイル時のテーブル表示最適化（カード型レイアウト・重要列のみ表示等。本タスクでは横スクロール許容で済ませる） |
| **対象外** | (A) Service / Interactor / Specification / DAO / SQL の挙動変更 (B) DB スキーマ変更 (C) 認証認可機能の新規導入（現状未実装） (D) 新規業務エンドポイント追加（既存 Presenter のフラグメント返却対応のみ） (E) Selenium / EDINET API / スクレイピング処理の変更 (F) Resilience4j 設定の変更 (G) スケジューラの変更 (H) **Jenkinsfile のあらゆる変更**（Node 到達性確認・コメント追記・`nodeDownloadRoot` 設定を含めて全 Phase で対象外。CI 側のフロントエンドビルド対応は別タスクで実施。Gate 1 再実施 2026-04-29 で確定） |

### サブタスク分割の方針（Gate 1 で承認を仰ぐ論点）

git-strategy.md の「1 タスク = 1 コミット = Squash Merge」原則に従うと、本刷新は規模的にサブタスク分割が筋。以下の **マスタープラン（本 md） + フェーズ別サブタスク** 構成を提案する。

| Phase | タイトル | サブタスク md（予定） |
|---|---|---|
| 1 | ビルドパイプ整備（frontend-maven-plugin / Tailwind / htmx / Alpine / Lucide / Litepicker のローカル取得） | [T20260429-screen-renewal-phase1-build-pipeline.md](T20260429-screen-renewal-phase1-build-pipeline.md) |
| 2 | layout.html の Tailwind 刷新（旧 layout は並走） | T<連番>-layout-tailwind.md |
| 3 | index.html 移植 + DataTables → htmx ページング基盤確立 | T<連番>-screen-index-htmx.md |
| 4 | valuation.html 移植（5 テーブル） | T<連番>-screen-valuation-htmx.md |
| 5 | edinet.html / edinet-detail.html 移植 | T<連番>-screen-edinet-htmx.md |
| 6 | corporate.html 移植（Chart.js 14 個 + ローカルバンドル化） | T<連番>-screen-corporate-htmx.md |
| 7 | error.html 移植・旧 layout 廃止・`static/dist` と未使用 plugins 削除・ダークモード仕上げ | T<連番>-cleanup-and-darkmode.md |
| 8 | Playwright スナップショット導入 | T<連番>-playwright-snapshots.md |

各 Phase は独立して develop へ Squash Merge 可能（`feature/<phase-内容>` ブランチ）とし、各 Phase で Gate 1 / Gate 3 を回す。本 md は全 Phase の方針を束ねる **マスタープラン** として一次情報源化する。

### ドキュメントとコードの整合

- CLAUDE.md「View / 画面」節は AdminLTE 前提の記述 → 本タスク完了時に Tailwind/htmx/Alpine 前提へ書き換え
- CLAUDE.md「ビルド・テストコマンド」節に frontend-maven-plugin 経由の Node ダウンロードと Tailwind ビルドの説明を追加
- 既存 [T20260429-frontend-asset-cleanup.md](T20260429-frontend-asset-cleanup.md) は静的資産の **未参照分削除** で本タスクと住み分け（本タスクは **使用中の AdminLTE 資産** を含めた撤去）

---

## ステップ 2: プロトタイピング

**Phase 1 で実施予定**: frontend-maven-plugin + Tailwind CLI 起動の最小構成を `target/generated-resources/` に CSS を 1 ファイル吐かせるところまで通し、`./mvnw package` が成功することを検証してから本格着手する。Phase 1 のサブタスクで詳細記録する。

本マスタープラン段階ではプロトタイピング不要（採用技術はすべて成熟しており、リスクの高い判断は無い）。

---

## Gate 1: 影響設計の承認

### レビュアー向けサマリ

- **判断してほしいこと**:
  1. 採用技術スタック（Thymeleaf SSR + htmx + Tailwind + Alpine.js + Lucide + Litepicker + Chart.js + Playwright）の妥当性
  2. **マスタープラン + フェーズ別サブタスク分割方式** で進める運用判断
  3. PDF/Excel/CSV 出力機能の **完全廃止** で問題ないか（DataTables Buttons の自動生成のみで、業務上の必須要件として明示されていないが、運用者が常用していないかの最終確認が必要）
- **重要な変更ポイント**:
  1. AdminLTE / jQuery / Bootstrap 4 / DataTables / daterangepicker / moment / inputmask / pdfmake / jszip / Font Awesome を **全廃**
  2. Tailwind CSS / htmx / Alpine.js / Lucide / Litepicker を新規採用、Chart.js は継続するも CDN→ローカル化
  3. `pom.xml` に **frontend-maven-plugin** を追加し、`./mvnw package` のビルドフローに Node + Tailwind ビルドを組み込む（本番ランタイムでは Node 不要）
  4. Jenkins ビルドノードに Node 取得経路の追加が必要（frontend-maven-plugin が自動 DL するため Jenkins 側へのインストールは不要見込みだが、ネットワーク制約の事前確認が必要）
  5. テーブル機能を **DataTables 廃止 → Spring 側で Pageable + sort + filter を実装し htmx でフラグメント返却** に変更（11 テーブル分の Presenter 拡張）
  6. 段階移行のため、刷新中は旧 layout と新 layout が一時的に共存（Phase 2 のサブタスクで詳細）
  7. **レスポンシブ対応（375px 以上）**：md 未満ではサイドバーがハンバーガードロワー化、テーブルは横スクロール許容、Chart.js は `responsive: true` で親幅追従。Playwright スナップショットはデスクトップ（1280px）とモバイル（375px）の 2 ビューポートで取得
- **確認してほしい観点**:
  1. PDF/Excel/CSV 出力廃止の業務影響（運用者ヒアリングの必要性）
  2. Jenkins ノードに Node ダウンロード用ネットワーク経路があるか（社内 Nexus 経由が必須なら frontend-maven-plugin の `nodeDownloadRoot` / `npmRegistryURL` に社内 Nexus を指定する設定が必要）
  3. サブタスク分割粒度（Phase 1〜8 の切り方）の妥当性
  4. レスポンシブ対応の下限を 375px（スマホ）に置く判断と、テーブルは横スクロール許容で済ませる方針の妥当性（後段でカード型レイアウトに高度化する余地は「後回し」に明記済み）

### 重点観点

#### 影響範囲分析

変更属性チェック:

- **参照層: 該当**（テンプレート全 7 ファイル / static/dist + static/plugins / pom.xml / Jenkinsfile / Presenter 全 5 ファイル / 関連 view DTO / FundanalyzerApplication 起動構成）
- **状態層: 該当なし**（業務的な状態遷移・ライフサイクルは変更しないため。画面の「ロード中 → 表示」状態は htmx の `hx-indicator` で扱うが、ステートマシンとしての分析対象外）
- **データ層: 該当なし**（DB スキーマ・既存データ・Doma Entity は無変更。サーバ側ページング/ソート/検索のためのクエリ拡張は **Specification 層に追加するロジック** で済み、スキーマ変更は伴わない）

##### 参照層分析結果

| 対象 | 参照箇所 | 影響 |
|---|---|---|
| `templates/layout.html` | 全 6 画面が継承 | 影響大：全画面に波及するため Phase 2 で旧 layout を残しつつ並走 |
| `templates/index.html` ほか 5 画面 | 各 Presenter からの直接参照のみ | 影響大：Phase 3〜7 で個別移植 |
| `static/plugins/*`（jquery / datatables-* / daterangepicker / fontawesome-free / inputmask / jszip / moment / pdfmake / bootstrap） | テンプレートからの `th:src` / `th:href` 参照 | 影響大：Phase 7 で全削除 |
| `static/dist/*`（adminlte.min.css / adminlte.min.js） | layout.html のみ | 影響大：Phase 2/7 で削除 |
| Presenter 5 クラスの `@GetMapping` メソッド | 既存テンプレート参照のみ | 中：htmx フラグメント返却用に新規メソッド追加（既存メソッドは影響受けない、Phase 3 以降で順次） |
| view DTO（`web/view/model/`） | Presenter からの直接参照 | 小：必要に応じてページング情報を持つ DTO を追加（既存 record は変更しない） |
| `pom.xml` | Maven ビルドフロー全体 | 影響大：frontend-maven-plugin と関連 execution を追加（Phase 1） |
| `Jenkinsfile-ci-prod.groovy` / `Jenkinsfile-dev.groovy` / `Jenkinsfile-cd-prod.groovy` | CI/CD 全体 | 中：Node 取得経路の確認のみ（ビルドコマンド自体は `./mvnw package` のままで OK） |
| `application.yml` の `spring.web.resources` | 静的アセット配信 | 小：生成 CSS の配置先が `target/classes/static/` で標準解決可能、設定変更は基本不要 |
| 既存テスト（`src/test/`） | Presenter の MockMvc テスト | 小：既存テストは API 動作の検証で、HTML 構造の検証はしていないため影響軽微（要再確認）|

リフレクション・動的ロード: **該当なし**（Thymeleaf テンプレートはファイル名解決、Spring Boot の静的リソース解決もパス指定。動的呼び出しなし）。

設定ファイル参照: `application.yml` で静的リソースパスの直接参照なし（標準パス利用）。

テストコード参照: 既存 MockMvc テスト 473 件は HTML 構造を検証していない。Playwright 導入により HTML 構造のスナップショット検証を新規追加。

##### 状態層分析結果

該当なし（業務状態遷移を変更しないため）。

##### データ層分析結果

該当なし（DB スキーマ・既存データを変更しないため）。サーバ側ページング/ソート/検索のための SQL 拡張は Specification 層に追加するロジックで済み、新規 SQL ファイルは Phase 3〜6 で各画面のサブタスク内で扱う（既存 SQL ファイルは編集せず、新規 Doma SQL ファイル追加で対応）。

#### インフラ影響チェック

| カテゴリ | 判定 | 内容 |
|---|---|---|
| **A. 処理時間・タイムアウト** | 該当 | DataTables 撤廃で初回 HTML サイズが縮小し体感は改善見込み。一方で **htmx の部分更新で都度 HTTP 往復が発生** するため、ページング/ソート時のレスポンスタイムを各 Phase で計測（目標: 500ms 以下）。モバイル回線（4G 想定）でも初回ロード 3 秒以内を目安にバンドルサイズを抑制（Tailwind は purge 必須、Chart.js / htmx / Alpine / Litepicker / Lucide のバンドル合計 200KB 以下を Phase 1 で見積もり） |
| **B. 外部サービス連携** | 該当（CDN 排除） | Chart.js を `cdnjs.cloudflare.com` から `static/js/` へローカル化。**外部 CDN への依存をゼロに** する（攻撃面縮小・閉域環境耐性向上） |
| **C. データストア・スキーマ** | 該当なし | DB スキーマ無変更 |
| **D. バッチ・非同期処理** | 該当なし | スケジューラ・バッチ無変更 |
| **E. リソース** | 該当 | jar サイズ縮小（AdminLTE 約 50MB → Tailwind ビルド成果物の数百 KB レベル）。**Node 自体はビルド時のみ取得** し、本番 Windows サービス環境には Node を配置しない（frontend-maven-plugin の標準挙動）|
| **F. 可用性・障害許容** | 該当なし | 単一プロセス構成は不変 |
| **G. セキュリティ・認証認可** | 該当 | (1) 外部 CDN 依存ゼロ化でサプライチェーン縮小 (2) jQuery 廃止で XSS 経路縮小 (3) pdfmake/jszip 廃止でクライアント側バイナリ生成経路の脆弱性縮小 (4) Thymeleaf エスケープは標準維持 (5) htmx の `hx-*` 属性は Thymeleaf テンプレ内に書くため XSS 入口にならない |
| **H. 監視・ロギング・アラート** | 該当なし | log4j2 + ECS フォーマット + Filebeat の構成は不変 |
| **I. デプロイ・リリース** | 該当 | (1) Jenkins ビルドフロー: `./mvnw package` 中に frontend-maven-plugin が Node を一時 DL → Tailwind ビルド → 既存 jar 生成へ続く (2) Jenkins ビルドノードのネットワーク経路で **Node 配布元（既定 `https://nodejs.org/dist/`）への到達性が必要**。社内 Nexus 経由の場合は `nodeDownloadRoot` プロパティで指定 (3) 本番デプロイ手順（[release/start.bat](release/start.bat) 等）は無変更 (4) 段階移行のため Phase 2〜7 では旧 layout と新 layout が共存し、各 Phase ごとにリリース可能 |
| **J. 互換性・依存関係** | 該当（依存追加多数） | §依存追加判断 を参照 |

#### 依存追加判断

新規導入予定の全ライブラリを以下に列挙する。**Phase 1 のサブタスク Gate 1 で各々の確定承認** を仰ぎ、本マスタープラン Gate 1 では「方向性」を承認する位置付け。ADR は `docs/adr/` を新設し `ADR-001-screen-renewal-stack.md` を起票（Phase 1 のサブタスク内）。

| ライブラリ | バージョン目安 | ライセンス | 採用理由 | 代替検討 |
|---|---|---|---|---|
| frontend-maven-plugin | 1.15.x | Apache 2.0 | Maven 中心の本リポにフロントビルドを統合する事実上の標準。Node を Maven build 時のみ自動取得。本番ランタイム不要 | ① Tailwind CLI standalone 直叩き → Maven exec 連携が煩雑で却下 ② 手作業で事前ビルド → 再現性低く却下 |
| Node.js | 20.x LTS | MIT | frontend-maven-plugin が DL する。本番不要 | — |
| Tailwind CSS | 3.x | MIT | utility-first・ダークモード・レスポンシブ標準対応・コミュニティ最大 | ① Bootstrap 5 → 既製 markup に近いが utility が貧弱 ② Pico.css → 軽量だが utility なし。クラス命名で対応となり保守性に劣る |
| htmx | 1.9.x（近く 2.x） | BSD 2-Clause | 単一 JS ファイル・SSR 維持・部分更新を `hx-*` 属性で表現可能。社内管理画面に最適 | ① Turbo (Hotwire) → Rails 寄りで Java エコの実例少 ② 自作 fetch + DOM 差し替え → htmx 相当を再実装することになる |
| Alpine.js | 3.x | MIT | 軽量・宣言的・htmx と相性◎・Tailwind 公式エコシステムでも併用例多数 | ① Vanilla JS のみ → ドロップダウン等の小さな状態管理を毎回書くのは保守性が低い ② Stimulus → 重い |
| Lucide | 0.x（最新） | ISC | SVG・MIT 同等の緩いライセンス・モダン管理画面に標準的・Tailwind とよく合う | ① Heroicons → Tailwind Labs 製で軽量だがアイコン数で劣る ② Tabler Icons → 多すぎるため絞り込みコスト |
| Litepicker | 2.0.x | MIT | jQuery 不要・期間選択特化・軽量（30KB 程度）・daterangepicker の正統な代替 | Flatpickr → 多機能だが期間選択は別アドオン必要 |
| Chart.js | 4.x（or 既存 3.8.0 維持） | MIT | **既に利用中**。CDN 経由をローカル化するだけでよい。一部 deprecated API のチェックは Phase 6 で実施 | ① ApexCharts → 見た目モダンだがバンドル大きい ② D3 → 過剰 |
| Playwright Java | 1.x | Apache 2.0 | JUnit 5 / Java エコシステム公式バインディングあり、HTML スナップショット・実機操作両方対応 | ① Selenium → 既に Selenium 連携を別用途で使用中だが画面テスト用にはオーバースペック ② Cypress → JS のみで Java テストランナーから扱えない |

#### 三本柱

##### テスト戦略

| 種別 | 採用 | 不採用理由（採用しない場合） |
|---|---|---|
| 既存 MockMvc テスト維持 | ✅ | — |
| Presenter のフラグメント返却 MockMvc テスト追加 | ✅ | htmx エンドポイントは HTML フラグメントを返すため、HTTP 200 と Content-Type、`th:fragment` の存在確認を最低限テスト |
| Playwright スナップショット（主要 5 画面 × デスクトップ 1280px + モバイル 375px の 2 ビューポート） | ✅ | 画面 HTML 構造の意図せぬ変更を検出するため。フルカラー比較は不採用（ノイズ多）、HTML 構造のテキスト比較に留める。モバイルビューポートではサイドバーがハンバーガー化されること、テーブルが横スクロール領域に収まることを構造的に確認 |
| Playwright での実機操作テスト（クリック・入力・遷移） | ⭕ Phase 8 のサブタスクで判断 | スナップショットで 8 割は防げる。クリック・入力テストは保守コスト見合いで Phase 8 サブタスクで判断 |
| JS 単体テスト（Vitest 等） | ❌ | SPA でなく Alpine.js のスクリプトは画面に紐づく短いものに限られる。テスト対費用が悪い |
| ビジュアルリグレッション（フルスクショ比較） | ❌ | 微小レンダリング差異でノイズ多。ROI 低 |
| 負荷テスト | ⭕ Phase 3 で htmx ページング初導入時に 1 度だけ実測 | 本格 LB 不要、k6 等の小規模実測で十分 |

##### セキュリティ方針

| 観点 | 採用 | 内容 |
|---|---|---|
| 外部 CDN 排除 | ✅ | Chart.js 含む全アセットを self-host。サプライチェーンリスク削減 |
| jQuery 廃止 | ✅ | XSS / プロトタイプ汚染の経路を縮小 |
| pdfmake / jszip 廃止 | ✅ | クライアント側バイナリ生成の脆弱性経路ゼロ化 |
| Thymeleaf 標準エスケープ維持 | ✅ | htmx フラグメント返却時も `th:text` 経由で必ずエスケープ |
| htmx `hx-*` の安全性 | ✅ | Thymeleaf テンプレ内のリテラルとして書くため XSS 入口にはならない。`hx-vals` で動的値を渡す場合は必ず `th:attr` でエスケープ |
| CSP ヘッダー導入 | ❌（後回し） | 適切な CSP 設計には全画面の inline script / style の整理が必要。本タスクの後段で別タスク化 |
| 認証認可 | ❌（対象外） | 現状未実装。本タスクのスコープ外 |
| シークレット | — | 本タスクで新規シークレット追加なし |

##### ドキュメント計画

| ドキュメント | 対応 | タイミング |
|---|---|---|
| 本マスタープラン md | 一次情報源として維持 | 全 Phase で参照・必要時更新 |
| Phase 別サブタスク md（`docs/notes/T<連番>-*.md`） | 各 Phase 着手時に新規作成 | 各 Phase Gate 1 直前 |
| `docs/adr/ADR-001-screen-renewal-stack.md` | 新規起票（Phase 1 サブタスク内） | Phase 1 着手前 |
| CLAUDE.md「View / 画面」節 | 書き換え | Phase 7 完了時 |
| CLAUDE.md「ビルド・テストコマンド」節 | frontend-maven-plugin の挙動を追記 | Phase 1 完了時 |
| `develop/document/*.drawio` の構成図 | 必要時に画面側構成図を更新 | Phase 7 完了時に判断 |
| `pipeline/Jenkinsfile-*` への変更コメント | コミット本文に「なぜ Node が必要か」を明記 | Phase 1 |

#### スコープ確定

§ステップ 1 のスコープ表（コア / 後回し / 対象外）に従う。

#### サブタスク化判断

§ステップ 1「サブタスク分割の方針」の Phase 1〜8 体制を採用する前提で進める。**本マスタープラン Gate 1 が承認された時点で各 Phase のサブタスク化を確定** とし、Phase 1 から着手する。

各 Phase は独立したフィーチャーブランチで Squash Merge を行う。本 md の更新履歴に Phase 完了日を時系列で追記する。

### レビュアー記入欄

- 承認者: iori-oiso（プロジェクトオーナー）
- レビュー依頼日: 2026-04-29
- 回答日: 2026-04-29
- 結論: 合格
- コメント: 採用技術スタック（Thymeleaf SSR + htmx + Tailwind + Alpine.js + Lucide + Litepicker + Chart.js 継続 + Playwright）、マスタープラン + フェーズ別サブタスク分割方式、PDF/Excel/CSV 出力の完全廃止、375px 下限のレスポンシブ対応すべて承認。Phase 1（ビルドパイプ整備）から着手して可。

---

## Gate 1 再実施（スコープ縮小: Jenkins 関連を全 Phase で対象外化）

> 既存の `## Gate 1` セクションは履歴として残し、本セクションでスコープ縮小の変更点と再承認を記録する（[human-checkpoints.md §Gate の差し戻し](../guideline/human-checkpoints.md#gate-の差し戻し) 準拠）。

### レビュアー向けサマリ

- **判断してほしいこと**: Jenkinsfile への変更（Node 到達性確認・コメント追記・`nodeDownloadRoot` 設定を含む）を **本タスクの全 Phase で対象外** とする運用変更の承認
- **重要な変更ポイント**:
  1. §ステップ 1 §スコープ §コア の (j) **Jenkinsfile への Node 導入** を削除（コア項目は (a)〜(l) にリラベル）
  2. §ステップ 1 §スコープ §対象外 に (H) **Jenkinsfile のあらゆる変更** を追加
  3. §サブタスク分割の方針 Phase 1 のタイトルから「+ Jenkinsfile」を削除
  4. §Gate 2 §完了条件 §テスト の **「Jenkins CI が緑」を削除**（CI 検証の責務はマスタープランから外し、別タスクで実施する形に変更）
  5. Phase 1 サブタスク md（[T20260429-screen-renewal-phase1-build-pipeline.md](T20260429-screen-renewal-phase1-build-pipeline.md)）も同期して修正（Jenkinsfile 関連を対象外へ移動・コミット計画から Jenkinsfile 関連コミットを削除）
  6. ADR-001（[ADR-001-screen-renewal-stack.md](../adr/ADR-001-screen-renewal-stack.md)）の §影響 §ネガティブ に記載した「Jenkins ビルドノードに Node 配布元への到達性が必要」を更新履歴付きで「（本タスクではスコープアウト・別タスクで対応）」と注記
- **確認してほしい観点**:
  1. Jenkins 側のフロントエンドビルド対応を別タスク化することによるリスク（マージ後に CI ビルドが Node 取得経路で失敗する可能性）。受容するか、Phase 1 完了時点で別タスクを起票するか
  2. 既存 Gate 1 §インフラ影響チェック §I. デプロイ・リリース で「Jenkins ビルドフローで Node を一時 DL」と記述している部分は **歴史的経緯として残す**（編集禁止ルール準拠）が、最新の方針はスコープ表と本再実施セクションが優先することへの合意

### 重点観点

#### 影響範囲分析

- スコープ **縮小** のみで、新たに変更する範囲はない（Jenkinsfile の参照を「該当」→「対象外」へ移すのみ）
- 参照層・状態層・データ層の判定は変更なし

#### インフラ影響チェック

- スコープ縮小のためインフラ影響評価は変更なし（リスク減方向）
- I. デプロイ・リリース: Jenkins への影響評価は **対象外** へ。本番デプロイ手順は無変更（変わらず）
- B. 外部サービス連携: Node 配布元・npm registry への到達性確認は **ローカル開発環境のみで担保**（Jenkins は別タスク）

#### 三本柱

- テスト戦略: 変更なし（Jenkins CI が緑であることをマクロ完了条件から外すのみ）
- セキュリティ方針: 変更なし
- ドキュメント計画: 変更なし

#### スコープ確定

§ステップ 1 のスコープ表（コア / 後回し / 対象外）の更新済バージョンに従う。

#### 依存追加判断

ADR-001 の決定内容は変更なし。影響セクションに「Jenkins 関連は別タスク化」を更新履歴付きで注記。

### レビュアー記入欄

- 承認者: iori-oiso（プロジェクトオーナー）
- レビュー依頼日: 2026-04-29
- 回答日: 2026-04-29
- 結論: 合格
- コメント: Jenkins 関連は本タスクのスコープアウトでよい。別タスクで対応する。

---

## Gate 2: 完了条件の確認

### 運用ルート

**正式**（マスタープランは複数 Phase に渡る非小タスクのため、省略不可）。

ただし、本 md は **マスタープラン（方針集約）** であり、具体的な完了条件・テスト要件・ドキュメント要件は **各 Phase のサブタスク md** で個別に定義する。本マスタープランの Gate 2 は「全 Phase 完了 = 全画面が新スタックで稼働 + 旧アセット削除済 + Playwright スナップショット稼働」をマクロな完了条件として宣言する位置付け。

### 完了条件（マスタープラン全体）

#### 機能

- [ ] Phase 1〜8 のすべてが完了し各 Gate 3 を通過
- [ ] 旧 `static/dist/` および AdminLTE / jQuery 関連の `static/plugins/*` が物理削除されている
- [ ] 全 6 画面（index / corporate / valuation / edinet / edinet-detail / error）が Tailwind + htmx で稼働
- [ ] PDF/Excel/CSV/Print/ColVis の DataTables Buttons 機能が削除されている（業務影響無しの前提・Gate 1 での確認結果に従う）
- [ ] Chart.js が CDN ではなくローカルバンドルから読み込まれる
- [ ] ダークモードのトグル切替が動作する
- [ ] **375px（iPhone SE 等）/ 768px（タブレット）/ 1280px（デスクトップ）の各ビューポートで 6 画面すべてが破綻なく表示される**（サイドバーのハンバーガー化、テーブル横スクロール、Chart.js リサイズ含む）

#### テスト

- [ ] 既存 473 件の MockMvc テスト全パス（変更なし）
- [ ] htmx フラグメント返却用の追加 MockMvc テストが緑
- [ ] Playwright スナップショットが主要 5 画面で固定化されている
- [ ] `./mvnw clean package` が成功（frontend-maven-plugin 経由の Tailwind ビルド込み）
- ~~Jenkins CI が緑~~（**Gate 1 再実施 2026-04-29 で対象外化**。CI 側のフロントエンドビルド検証は別タスクで実施）

#### ドキュメント

- [ ] CLAUDE.md「View / 画面」節と「ビルド・テストコマンド」節が新スタックを反映
- [ ] `docs/adr/ADR-001-screen-renewal-stack.md` が起票済み
- [ ] 本マスタープラン md の更新履歴が全 Phase 完了まで埋まっている
- [ ] 各 Phase のサブタスク md が `docs/notes/` に揃っている

#### スコープ外（やらないこと）

- Service / Interactor / Specification / DAO / SQL の挙動変更
- DB スキーマ変更
- 認証認可機能の新規導入
- 新規業務エンドポイント追加（既存 Presenter のフラグメント返却対応のみ）
- Selenium / EDINET API / スクレイピング処理の変更
- Resilience4j 設定の変更
- スケジューラの変更
- i18n 対応・モバイル最適化・CSP ヘッダー導入

### レビュアー記入欄

- 承認者: <氏名・役割>
- レビュー依頼日: -
- 回答日: -
- 結論: -
- コメント: -

---

## ステップ 5: 実行サイクル

各 Phase のサブタスク md に詳細を記録する。本 md は完了したサブタスクの **追跡表** のみ保持する。

### サブタスク追跡表

| Phase | タイトル | サブタスク md | ブランチ | コミット | 状態 |
|---|---|---|---|---|---|
| 1 | ビルドパイプ整備 | [Phase 1](T20260429-screen-renewal-phase1-build-pipeline.md) | `feature/screen-renewal-phase1-build-pipeline` | d0ff0b03→1bf4062d（8 コミット） | **完了**（Gate 3 承認・2026-04-29） |
| 2 | layout.html 刷新 | [Phase 2](T20260429-screen-renewal-phase2-layout-tailwind.md) | `feature/screen-renewal-phase2-layout-tailwind` | 83d90d7b→be898087（7 コミット） | **完了**（Gate 3 承認・2026-05-01） |
| 3 | index.html 移植 + htmx ページング基盤 | [Phase 3](T20260429-screen-renewal-phase3-index-htmx.md) | `feature/screen-renewal-phase3-index-htmx` | cad7da7d→82faf5cd（7 コミット） | **完了**（Gate 3 承認・2026-05-01） |
| 4 | valuation.html 移植 | [Phase 4](T20260429-screen-renewal-phase4-valuation-htmx.md) | `feature/screen-renewal-phase4-valuation-htmx` | 1ddcfa75→d6fb7bcb（7 コミット） | **完了**（Gate 3 承認・2026-05-01） |
| 5 | edinet.html / edinet-detail.html 移植 | [Phase 5](T20260429-screen-renewal-phase5-edinet-htmx.md) | `feature/screen-renewal-phase5-edinet-htmx` | cafd0040→368f801a（7 コミット） | **完了**（Gate 3 承認・2026-05-01・実機確認は人間レビュアに委ねる） |
| 6 | corporate.html 移植 | [Phase 6](T20260429-screen-renewal-phase6-corporate-htmx.md) | `feature/screen-renewal-phase6-corporate-htmx` | 2c704b73→10f9c63a（6 コミット） | **完了**（Gate 3 承認・2026-05-01・実機確認は人間レビュアに委ねる） |
| 7 | error.html 移植・旧資産削除・ダークモード仕上げ | - | - | - | 未着手 |
| 8 | Playwright スナップショット導入 | - | - | - | 未着手 |

---

## ステップ 6: 多軸検証

各 Phase のサブタスク md で個別に実施。本マスタープラン側の最終検証は Phase 8 完了時に Gate 3 として実施する。

---

## Gate 3: 最終確認

### レビュアー向けサマリ

（Phase 8 完了時に記載）

### 重点観点

#### 差分レビュー

（記載予定）

#### 動作確認結果

（記載予定）

#### 副次影響

（記載予定）

#### ドキュメント整合性

（記載予定）

### レビュアー記入欄

- 承認者: <氏名・役割>
- レビュー依頼日: -
- 回答日: -
- 結論: -
- コメント: -

---

## 添付ファイル参照

`docs/notes/T20260429-screen-renewal-attachments/` 配下に配置予定（影響範囲調査の生 grep 結果・Playwright スナップショット例・Tailwind ビルド成果物サイズ計測結果 等）。

---

## 更新履歴

- 2026-04-29: 初版作成（ステップ 1〜2・Gate 1 セクション記載・Phase 1〜8 のサブタスク分割案を提示）
- 2026-04-29: レスポンシブ対応（375px 以上）をコアスコープに追加。Playwright をデスクトップ + モバイルの 2 ビューポートに拡張、テーブルは横スクロール許容方針を確定、モバイル時のテーブルカード化は「後回し」スコープへ移動
- 2026-04-29: Gate 1 承認記録（iori-oiso・合格）。Phase 1 着手開始
- 2026-04-29: Phase 1 サブタスク md と ADR-001 を起票（[T20260429-screen-renewal-phase1-build-pipeline.md](T20260429-screen-renewal-phase1-build-pipeline.md) / [ADR-001-screen-renewal-stack.md](../adr/ADR-001-screen-renewal-stack.md)）。サブタスク追跡表の Phase 1 行にリンクを設定
- 2026-04-29: Gate 1 再実施（スコープ縮小: Jenkins 関連を全 Phase で対象外化）。スコープ表 §コア (j) を削除し §対象外 (H) に追加、Phase 1 タイトルから「+ Jenkinsfile」を削除、Gate 2 完了条件の「Jenkins CI が緑」を取消し線に変更。iori-oiso 承認
- 2026-04-29: Phase 1 完了（8 コミット d0ff0b03→1bf4062d）。Claude Preview で実機ブラウザ表示確認・全 6 観点 + ダークモード + レスポンシブ動作 OK。Phase 1 Gate 3 承認（iori-oiso）。次は Phase 2（layout.html 刷新）へ
- 2026-05-01: Phase 2 完了（7 コミット 83d90d7b→be898087）。Claude Preview で再確認・タブレット 768px サイドバー固定・ダークモード+ htmx 補正・モバイル 375px ハンバーガードロワー動作 OK。Phase 2 Gate 3 承認（iori-oiso）。次は Phase 3（index.html 移植 + DataTables → htmx ページング基盤確立）へ
- 2026-05-01: Phase 3 完了（7 コミット cad7da7d→82faf5cd）。テスト 49 件全パス・テーブル汎用パターン（2 エンドポイント方式 / record / ViewService 拡張 / 入力検証 / fragments / htmx 属性）を確立。Phase 3 Gate 3 承認（iori-oiso）。次は Phase 4（valuation.html 移植・5 テーブル並列）へ
- 2026-05-01: Phase 4 完了（7 コミット 1ddcfa75→d6fb7bcb）。テスト 76 件全パス・5 view fragment（stock / submit / graham-index / dividend-yield / industry）+ Map.of view ホワイトリストでテーブル汎用パターンを 5 テーブル並列に拡張。dev H2 で 31 件の業種マスタ実データ表示を確認。Phase 4 Gate 3 承認（iori-oiso）。次は Phase 5（edinet.html / edinet-detail.html 移植）へ
- 2026-05-01: Phase 5 完了（7 コミット cafd0040→368f801a）。テスト 59 件全パス（テーブル汎用パターンを edinet-list に適用 + edinet-detail は単純 layout-v2 継承）。Claude Preview 実機確認は省略・人間レビュアに委ねる。Phase 5 Gate 3 承認（iori-oiso）。次は Phase 6（corporate.html 移植・Chart.js 14 個 + ローカルバンドル化）へ
- 2026-05-01: Phase 6 完了（6 コミット 2c704b73→10f9c63a）。CorporatePresenterTest 12 件全パス。CorporatePresenter は private populateModel 軽量リファクタで v2/v3 共通化、corporate-v2.html は機能等価最低限版（約 200 行・14 canvas + 主要セクション）、Chart.js は Phase 1 ローカルバンドル window.Chart 利用で CDN 撤去、index/valuation の code リンクを /v3/corporate に書き換え。Phase 6 Gate 3 承認（iori-oiso）。次は Phase 7（error.html 移植 + 旧資産削除 + ダークモード仕上げ）へ
