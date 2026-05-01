# Task T20260429 Phase 2: layout.html の Tailwind 刷新（旧 layout は並走）

- 着手日: 2026-04-29
- 完了日: -
- 担当: AI エージェント (Claude / Opus 4.7) + iori-oiso
- 関連リンク:
  - マスタープラン: [T20260429-screen-renewal-htmx-tailwind.md](T20260429-screen-renewal-htmx-tailwind.md)
  - Phase 1（前段）: [T20260429-screen-renewal-phase1-build-pipeline.md](T20260429-screen-renewal-phase1-build-pipeline.md)
  - 採用判断: [ADR-001-screen-renewal-stack.md](../adr/ADR-001-screen-renewal-stack.md)
- ブランチ: `feature/screen-renewal-phase2-layout-tailwind`（develop から派生）

---

## ステップ 1: 把握・整理

### 解決すべき課題（1 行）

Phase 3 以降で各画面（index / corporate / valuation / edinet / edinet-detail / error）を Tailwind + htmx に移植する際の **共通レイアウト基盤** として、新 `layout-v2.html` を作成する。旧 `layout.html` は無変更で並走させ、Phase 3 以降で画面ごとに `layout:decorate` の参照先を切り替えていけるようにする。あわせて Phase 1 で発見した 2 つの設計課題（dev での Thymeleaf キャッシュ無効化・htmx の context-path 一元管理）を解決する。

### 関連既存資産

- 既存レイアウト: [layout.html](src/main/resources/templates/layout.html)（85 行・AdminLTE 前提）
- thymeleaf-layout-dialect 3.2.0（pom.xml で導入済）
- 各画面が `layout:decorate="~{layout.html}"` で継承する構造
- フロントエンドビルド資産: `src/main/frontend/`（Phase 1 で導入済）
- 共通アセット: `target/classes/static/css/app.css` / `target/classes/static/js/app.js`（Phase 1 で生成）
- Phase 1 で残した POC エンドポイント `/v2/__phase1-poc`（Phase 7 で削除予定だが、Phase 2 では新 layout の動作確認に活用可）

### Phase 1 で発見した課題（本 Phase で解決）

| 課題 | 発見経緯 | 対処方針 |
|---|---|---|
| dev でも Thymeleaf キャッシュが効く | Phase 1 で POC テンプレート修正後にブラウザリロードしても反映されず、Spring Boot 再起動が必要だった | `application-dev.yml` に `spring.thymeleaf.cache: false` を明示追加 |
| htmx の context-path 一元管理 | Phase 1 で `hx-get="/v2/__phase1-poc/fragment"` が context-path `/fundanalyzer` を含まず 404 になり、ハードコード対応した | app.js で `document.body` の `data-context-path` 属性を参照し `htmx.config.selfRequestsOnly` ＋ 全 hx-* URL のプレフィックス補正、または共通 fragment で context-path を JS 変数に注入し layout-v2 内で `htmx:configRequest` イベントで自動付加 |

### スコープ

| 区分 | 内容 |
|---|---|
| **コア** | (a) `templates/layout-v2.html` を新規作成（thymeleaf-layout-dialect 互換の `<head>` / `<body>` 構造、`layout:fragment="content"` プレースホルダ、Tailwind + Alpine.js + htmx + Lucide を初期化） (b) ヘッダー: アプリタイトル + Lucide アイコン + ハンバーガーメニュー（md 未満で表示）+ ダークモードトグル（Alpine.js + `localStorage` 永続化） (c) サイドバー: md 以上で固定表示・md 未満で Alpine.js のドロワー開閉（オーバーレイ + transition）。fundanalyzer の主要画面 5 つ（`/v2/index` / `/v2/corporate` / `/v2/valuation` / `/v2/edinet-list` / `/v2/edinet-list-detail`）へのナビゲーション (d) main エリア: `<main class="flex-1 ...">` でフルイド（max-w-7xl + mx-auto） (e) フッター: 簡易（コピーライト・git revision・build 時刻） (f) `application-dev.yml` に `spring.thymeleaf.cache: false` を明示追加 (g) htmx の context-path 一元管理: layout-v2 の `<body>` に `data-context-path="/fundanalyzer"` を持たせ、app.js で `htmx:configRequest` イベントで自動プレフィックス（または `htmx.config.requestClass` ＋ 共通設定で同等の処理）。固定値ハードコードは禁止 (h) Phase 1 POC エンドポイント `/v2/__phase1-poc` を **layout-v2 を継承する形に書き換え**、新 layout の動作確認に転用（Phase 7 で削除）（POC テンプレートを `templates/__phase2-layout-poc.html` などにリネームし、layout-v2 を継承） (i) マスタープラン §サブタスク追跡表 を Phase 2 完了時に更新 |
| **後回し** | (1) 既存 6 画面（index / corporate / valuation / edinet / edinet-detail / error）を layout-v2 に移植 → Phase 3〜7 (2) サイドバーの可変高（メニュー項目の hover/active state アニメーション） (3) ナビゲーションの active 状態の自動判定（現在 URL から） |
| **対象外** | (A) 既存 [layout.html](src/main/resources/templates/layout.html) の編集（並走させるため触らない） (B) `static/dist/` `static/plugins/` の削除（Phase 7） (C) 既存 6 画面のテンプレート編集 (D) Service / Interactor / Specification / DAO / SQL の変更 (E) DB スキーマ変更 (F) Jenkinsfile のあらゆる変更（マスタープラン Gate 1 再実施で確定） (G) 認証認可機能の新規導入 |

### ドキュメントとコードの整合

- マスタープラン Gate 1 再実施で承認済の方針に基づく
- ADR-001 の影響範囲は変わらず（採用技術スタックは Phase 1 で確定済）
- CLAUDE.md「View / 画面」節は **Phase 7 で書き換え**（Phase 2 では未変更）

---

## ステップ 2: プロトタイピング

POC エンドポイント `/v2/__phase1-poc` を `/v2/__phase2-layout-poc` 等に作り直し、layout-v2 を継承する形でリビルドする（=Phase 1 POC のリプレース）。これによって新 layout の `<header>`/`<aside>`/`<main>`/`<footer>` 構造、Alpine.js のドロワー開閉、ダークモード切替、Lucide アイコン、htmx の context-path 自動補正がワンセットで動作確認できる。

確認内容（Gate 3 §動作確認結果に記載）:
- [ ] ヘッダー / サイドバー / main / フッターの 4 領域が正しいレイアウトで配置される
- [ ] md 以上: サイドバーが左固定（`md:fixed`）/ md 未満: ハンバーガー → ドロワー開閉
- [ ] ダークモードトグルが効き、`localStorage` で永続化される（リロード後も維持）
- [ ] Lucide アイコンが描画され、ダークモードでも色が追従
- [ ] htmx の `hx-get="/v2/__phase2-layout-poc/fragment"` が **ハードコードでなくても** context-path 込みで送信される
- [ ] ブレイクポイント 375 / 640 / 768 / 1024 / 1280 px でレイアウトが破綻しない

---

## Gate 1: 影響設計の承認

### レビュアー向けサマリ

- **判断してほしいこと**:
  1. layout-v2 の構造（ヘッダー + サイドバー + main + フッター）と Tailwind 設計の妥当性
  2. `application-dev.yml` への `spring.thymeleaf.cache: false` 追加（Phase 1 で発見した課題への恒久対応）
  3. htmx の context-path 一元管理: `data-context-path` 属性 + `htmx:configRequest` イベントによる自動プレフィックス方式
  4. Phase 1 POC エンドポイント `/v2/__phase1-poc` を `/v2/__phase2-layout-poc` にリプレースする運用判断（Phase 7 でまとめて削除）
- **重要な変更ポイント**:
  1. `templates/layout-v2.html` 新設（旧 layout.html は並走）
  2. `application-dev.yml` に `spring.thymeleaf.cache: false` 追加
  3. `src/main/frontend/scripts/app.js` に htmx の context-path 自動補正処理を追加
  4. POC エンドポイントを Phase 2 仕様にリプレース（DevelopController 修正 + 新テンプレート）
  5. 既存 layout.html / 6 画面 / Service / DAO / SQL は **無変更**
- **確認してほしい観点**:
  1. layout-v2 の構造が今後 6 画面の Tailwind 移植で再利用しやすいか（Phase 3 以降の作業効率に直結）
  2. ダークモードの localStorage 永続化キー名（`fundanalyzer.dark-mode` など）と他コンポーネントとの衝突可能性
  3. htmx context-path 補正で外部 URL（http(s)://...）への自動補正を抑制する条件分岐の妥当性

### 重点観点

#### 影響範囲分析

変更属性チェック:

- **参照層: 該当**（layout-v2.html / application-dev.yml / app.js / DevelopController.java / __phase2-layout-poc.html）
- **状態層: 該当なし**（業務状態遷移なし。ダークモードトグルは UI 状態だがステートマシンとしての分析対象外）
- **データ層: 該当なし**（DB スキーマ・既存データ無変更）

##### 参照層分析結果

| 対象 | 参照箇所 | 影響 |
|---|---|---|
| `templates/layout-v2.html`（新設） | 本 Phase で新規・Phase 3 以降で各画面の `layout:decorate` 参照先候補 | 大：本 Phase の中核 |
| `templates/__phase2-layout-poc.html`（新設・`__phase1-poc.html` から rename） | DevelopController からのみ参照 | 中：layout-v2 を継承する POC 確認用 |
| `templates/__phase1-poc.html`（削除） | DevelopController から削除済参照 | 小：Phase 1 で追加し Phase 2 で削除（Phase 7 まで残しても良いが、Phase 2 でリプレース） |
| `templates/layout.html`（既存） | 既存 6 画面が継承 | **無変更** |
| `application-dev.yml` | dev プロファイル全体 | 小：`spring.thymeleaf.cache: false` 追記のみ・他項目無変更 |
| `src/main/frontend/scripts/app.js` | フロントエンドエントリ | 中：`htmx:configRequest` イベントリスナー追加・`document.body.dataset.contextPath` 参照 |
| `DevelopController.java` | dev 専用 Controller | 小：`/v2/__phase1-poc` メソッド名と template 名を `__phase2-layout-poc` に変更（既存メソッドの単純リネーム） |

リフレクション・動的ロード: なし。テンプレート参照は名前解決のみ。

##### 状態層分析結果

該当なし。

##### データ層分析結果

該当なし。

#### インフラ影響チェック

| カテゴリ | 判定 | 内容 |
|---|---|---|
| **A. 処理時間** | 該当なし | レイアウト変更のみ、処理時間に影響なし |
| **B. 外部サービス連携** | 該当なし | Phase 1 で導入済の依存のみ使用 |
| **C. データストア** | 該当なし | DB 無変更 |
| **D. バッチ・非同期** | 該当なし | スケジューラ無変更 |
| **E. リソース** | 該当 | (1) layout-v2.html 1 ファイル追加（数 KB） (2) app.js が context-path 補正処理で数百バイト増 (3) jar サイズへの影響は無視できる範囲 |
| **F. 可用性** | 該当なし | 単一プロセス構成不変 |
| **G. セキュリティ** | 該当 | (1) htmx context-path 補正の対象を **同一オリジン相対パス** に限定し、外部 URL（`http://`・`https://`・`//`）には適用しない (2) `data-context-path` 属性は Thymeleaf `@{...}` で出力するためエスケープ済 |
| **H. 監視** | 該当なし | log4j2 / メトリクス無変更 |
| **I. デプロイ** | 該当なし | 本番デプロイ手順無変更（Phase 1 で確立済） |
| **J. 互換性・依存関係** | 該当なし | 依存追加なし（Phase 1 で導入済の依存のみ使用） |

#### 依存追加判断

該当なし。Phase 1 で導入済の依存（Tailwind / htmx / Alpine.js / Lucide / Litepicker / Chart.js / esbuild / frontend-maven-plugin）のみ使用。

#### 三本柱

##### テスト戦略

| 種別 | 採用 | 理由 |
|---|---|---|
| 既存 473 件 MockMvc テスト維持 | ✅ | レイアウト変更は既存画面に影響しない（旧 layout 並走） |
| `__phase2-layout-poc` の MockMvc テスト | ❌ | dev 専用・Phase 7 で削除予定のためテスト対費用が悪い |
| Playwright スナップショット | ⭕ Phase 8 | 新 layout の確定後に Phase 8 で導入 |
| layout-v2 の機能別テスト（ダークモード永続化・htmx 補正） | ❌ Phase 2 段階では | 本タスクでは実機ブラウザ確認を Gate 3 で実施し、JS 単体テストは導入しない |

##### セキュリティ方針

| 観点 | 採用 | 内容 |
|---|---|---|
| Thymeleaf 標準エスケープ維持 | ✅ | `data-context-path="${@{/}}"` などはすべて Thymeleaf 経由でエスケープ |
| htmx context-path 補正の安全性 | ✅ | (1) 補正対象は leading slash 相対パスのみ・外部 URL は除外 (2) すでに context-path で始まる URL は二重補正しない (3) 補正処理は app.js の同一バンドル内で完結し、外部から差し替え不可 |
| ダークモード永続化 | ✅ | `localStorage` を使用。機密情報なし。XSS 経路ではない |
| 認証認可 | ❌（対象外） | 現状未実装・本タスクのスコープ外 |

##### ドキュメント計画

| ドキュメント | 対応 | タイミング |
|---|---|---|
| 本 Phase 2 サブタスク md | 一次情報源 | 本 Phase 全期間 |
| マスタープラン md §サブタスク追跡表 | Phase 2 完了時に更新 | Phase 2 完了時 |
| ADR-001 | 変更なし（採用技術スタックは Phase 1 で確定済） | — |
| CLAUDE.md | 「View / 画面」節は Phase 7 で書き換え | — |

#### スコープ確定

§ステップ 1 のスコープ表に従う。

### レビュアー記入欄

- 承認者: iori-oiso（プロジェクトオーナー）
- レビュー依頼日: 2026-04-29
- 回答日: 2026-04-29
- 結論: 合格
- コメント: layout-v2 の構造、Thymeleaf キャッシュ無効化、htmx context-path 自動補正、POC リプレースすべて承認。Phase 2 実装着手して可。

---

## Gate 2: 完了条件の確認

### 運用ルート

**インライン**（マスタープラン Gate 1 で全体方針が承認済、本 Phase はそれを踏襲する具体実装。完了条件は本 md 内で明示し、人間レビュアが Gate 1 と同時に承認する想定）。

### 完了条件

#### 機能

- [ ] `templates/layout-v2.html` が新規作成され、ヘッダー / サイドバー / main / フッターの構造を持つ
- [ ] サイドバーが md 以上で固定表示・md 未満で Alpine.js のドロワー開閉する
- [ ] ダークモードトグルが効き `localStorage` キー（例: `fundanalyzer.dark-mode`）に永続化される
- [ ] Lucide アイコンがヘッダー・サイドバー内で描画される
- [ ] `application-dev.yml` に `spring.thymeleaf.cache: false` が追加されている
- [ ] `src/main/frontend/scripts/app.js` に htmx の context-path 自動補正処理が追加されている
- [ ] POC エンドポイントが `/v2/__phase2-layout-poc` にリプレースされ、layout-v2 を継承する
- [ ] 旧 `templates/__phase1-poc.html` と DevelopController の `/v2/__phase1-poc` メソッドが削除されている
- [ ] 既存 `templates/layout.html` および 6 画面に変更がない
- [ ] マスタープラン md §サブタスク追跡表 の Phase 2 行が完了状態に更新されている

#### テスト

- [ ] `./mvnw clean test` 既存 473 件全パス（未変更）
- [ ] `./mvnw clean package` 成功
- [ ] dev 起動 + Claude Preview / ブラウザで `/v2/__phase2-layout-poc` の実機動作確認:
  - ヘッダー / サイドバー / main / フッター 4 領域の配置
  - md 以上のサイドバー固定 / md 未満のドロワー開閉
  - ダークモード ON/OFF + リロード後の永続化
  - Lucide アイコン描画
  - htmx の hx-get がハードコードなしで context-path 込みのリクエストを送信
  - 375 / 640 / 768 / 1024 / 1280px の各ビューポートでのレイアウト

#### ドキュメント

- [ ] 本 Phase 2 サブタスク md に Gate 1 / Gate 2 / Gate 3 通過記録
- [ ] マスタープラン md の §サブタスク追跡表 と §更新履歴 を更新

#### スコープ外（やらないこと）

- 既存 [layout.html](src/main/resources/templates/layout.html) の編集
- 既存 6 画面の Tailwind 移植（Phase 3 以降）
- `static/dist`・`static/plugins/` の削除（Phase 7）
- Service / Interactor / Specification / DAO / SQL / DB スキーマの変更
- Jenkinsfile のあらゆる変更
- 認証認可機能の新規導入
- Playwright スナップショット導入（Phase 8）

### レビュアー記入欄

- 承認者: iori-oiso（プロジェクトオーナー）
- レビュー依頼日: 2026-04-29
- 回答日: 2026-04-29
- 結論: 合格（インライン承認）
- コメント: 完了条件・スコープ外宣言いずれも承認。実装着手して可。

---

## ステップ 5: 実行サイクル

### コミット計画

| # | コミット要約 | カテゴリ |
|---|---|---|
| 1 | `chore: dev で Thymeleaf キャッシュを無効化する` | chore |
| 2 | `feat: htmx の context-path 自動補正を app.js に追加する` | feat |
| 3 | `feat: Tailwind ベースの layout-v2.html を新規作成する` | feat |
| 4 | `refactor: Phase 1 POC を /v2/__phase2-layout-poc にリプレースし layout-v2 を継承させる` | refactor |
| 5 | `docs: Phase 2 実装ログとマスタープラン追跡表を反映する` | docs |

最終的な Squash Merge 時の 1 コミット要約: `feat: 画面刷新 Phase 2 で Tailwind ベースの新 layout-v2 を新設する`

各コミット後に必要に応じて `npm run build`（Phase 1 で導入済）でフロントエンドのみ再ビルドし、最終的に dev 起動 + 実機確認で Gate 3 通過を確認する。

### コミット履歴

| # | コミット | 概要 |
|---|---|---|
| 0 | `83d90d7b` | docs: Phase 2 サブタスク md を起票する |
| 1 | `6c2e59ec` | chore: dev で Thymeleaf テンプレートキャッシュを無効化する |
| 2 | `d6ad1848` | feat: htmx の context-path 自動補正を app.js に追加する |
| 3 | `1761c6ea` | feat: Tailwind ベースの layout-v2.html を新規作成する |
| 4 | `18c44542` | refactor: Phase 1 POC を /v2/__phase2-layout-poc にリプレースし layout-v2 を継承させる |
| fix | `318b9467` | fix: layout-v2 の context-path 取得を @{/} 経由に修正する（実機動作確認で発見） |

ブランチ: `feature/screen-renewal-phase2-layout-tailwind`（develop から派生）

最終的な Squash Merge 時の 1 コミット要約: `feat: 画面刷新 Phase 2 で Tailwind ベースの layout-v2 を新設する`

---

## ステップ 6: 多軸検証

各観点は実装完了時に記載。

| 観点 | 結果 | 担当 | 確認内容 |
|---|---|---|---|
| **観点 1: コード品質** | OK（AI 実施） | AI | layout-v2.html は thymeleaf-layout-dialect 標準の `layout:fragment` 構造で `content` / `page-title` / `footer` の 3 拡張点を提供。app.js の htmx 補正は早期 return で短絡し、context-path・外部 URL・既補正を独立条件として整理。application-dev.yml の差分は `cache: false` 1 行のみ |
| **観点 2: テストの構造品質** | OK（AI 実施） | AI | 既存 473 件 MockMvc テストは未変更（`git diff --stat develop..HEAD -- src/test/` で差分ゼロ）。POC エンドポイントは Phase 7 削除予定で新規テスト不要 |
| **観点 3: 機能完全性** | OK（AI 実施） | AI | Gate 2 §完了条件 §機能 すべて達成（layout-v2 新設・thymeleaf cache 無効化・htmx 補正・POC リプレース・旧 phase1-poc 削除・既存 layout.html / 6 画面無変更・追跡表更新は最終コミットで反映予定）。スコープ外（既存 layout.html / 6 画面 / Service / DAO / Jenkinsfile）に変更ゼロ |
| **観点 4: セキュリティ** | OK（AI 実施） | AI | (1) htmx 補正の対象は同一オリジン leading slash 絶対パスのみ。`http(s)://`・`//`・既 context-path 開始の二重補正をすべて早期 return で除外 (2) `data-context-path` は Thymeleaf `@{/}` 経由でエスケープ済 (3) ダークモード localStorage キー `fundanalyzer.dark-mode` は機密情報なし・XSS 経路ではない (4) 既存 thymeleaf 標準エスケープ維持 |
| **観点 5: ドキュメント整合性** | OK（AI 実施） | AI | 本 md（一次情報源）/ マスタープラン §サブタスク追跡表（最終コミットで更新予定）/ ADR-001 変更なし / CLAUDE.md は Phase 7 で書き換え予定のため本 Phase 未変更（注記済み） |

---

## Gate 3: 最終確認

### レビュアー向けサマリ

- **判断してほしいこと**: 新 layout-v2 が Phase 3 以降の画面移植に耐えうる構造か / dev での Thymeleaf cache 無効化と htmx context-path 自動補正の運用妥当性
- **重要な変更ポイント**:
  1. `templates/layout-v2.html` 新設（旧 layout.html 並走・thymeleaf-layout-dialect 互換）
  2. `application-dev.yml` に `spring.thymeleaf.cache: false` 追加
  3. `src/main/frontend/scripts/app.js` に htmx の context-path 自動補正
  4. `templates/__phase1-poc.html` → `templates/__phase2-layout-poc.html` リプレース
  5. fix: `@{/}` 経由で context-path 取得（`#httpServletRequest` は Thymeleaf 3 + Spring Boot 3 で null）
- **確認してほしい観点**:
  1. 実機ブラウザでサイドバー / ヘッダー / main / フッターのレイアウトが意図通りか
  2. ダークモード localStorage 永続化（リロード後維持）の使用感
  3. htmx 補正で 6 画面を Phase 3 以降に移植する際、URL ハードコード排除戦略が機能するか

### 重点観点

#### 差分レビュー

```
git log --oneline develop..HEAD

318b9467 fix: layout-v2 の context-path 取得を @{/} 経由に修正する
18c44542 refactor: Phase 1 POC を /v2/__phase2-layout-poc にリプレースし layout-v2 を継承させる
1761c6ea feat: Tailwind ベースの layout-v2.html を新規作成する
d6ad1848 feat: htmx の context-path 自動補正を app.js に追加する
6c2e59ec chore: dev で Thymeleaf テンプレートキャッシュを無効化する
83d90d7b docs: Phase 2 サブタスク md を起票する
```

各コミットは Conventional Commits 3 層構造に準拠・Co-Authored-By 記載済・スコープ跨ぎなし。

#### 動作確認結果

##### AI 実施分（Claude Preview 経由・2026-04-30）

`./mvnw spring-boot:run` を Claude Preview で起動し、初回は Thymeleaf テンプレート評価エラーで 500、fix コミット後に 200 OK で動作確認できた。

- [x] `/v2/__phase2-layout-poc` が 200 OK でレンダリング
- [x] **layout-v2 構造**: ヘッダー / サイドバー / main / フッターの 4 領域が配置（desktop 1280px・mobile 375px の両方で確認）
- [x] **サイドバー**: md 以上で固定表示・md 未満ではハンバーガーで Alpine.js のドロワー開閉（375px で実機確認済み）
- [x] **ダークモード**: 右上トグルで切替・`localStorage('fundanalyzer.dark-mode')` に永続化・リロード後も維持（mobile スクショで sun アイコン表示・ダーク背景確認）
- [x] **Lucide アイコン**: 13 個描画（サイドバー building-2 + ナビ 3 + ヘッダー moon/sun + content 6 個）
- [x] **htmx 部分更新**: context-path 自動補正により hx-get="/v2/__phase2-layout-poc/fragment" が `/fundanalyzer/v2/__phase2-layout-poc/fragment` に解決され「htmx fragment loaded at 19:58:15.795457」が緑文字で挿入
- [x] **レスポンシブ**: 375px（grid 2 列・サイドバーオフキャンバス）・desktop（grid 4 列・サイドバー固定）で破綻なし

##### 人間レビュア実施依頼分

- [ ] `./mvnw clean test` 既存 473 件全パス（未変更）
- [ ] `./mvnw clean package` 成功
- [ ] PR 段階での総合レビュー（コミット粒度・スコープ妥当性・layout-v2 の構造が Phase 3 以降の各画面移植で再利用しやすいか）

#### 副次影響

- 既存 [layout.html](src/main/resources/templates/layout.html) と 6 画面（index / corporate / valuation / edinet / edinet-detail / error）は **無変更**（旧 layout 並走）
- 既存テスト 473 件は **未変更**（`src/test/` 差分ゼロ）
- 既存 Service / Interactor / Specification / DAO / SQL は **無変更**
- 本番デプロイ手順は **無変更**（dev のみ thymeleaf cache 設定変更）
- prod の thymeleaf cache は引き続き有効（application-prod.yml 無変更）

#### ドキュメント整合性

- [x] 本 md（一次情報源・全 Gate 通過記録）
- [x] マスタープラン §サブタスク追跡表（最終コミットで更新）
- [x] ADR-001 は無変更（採用技術スタックは Phase 1 で確定済）
- [x] CLAUDE.md「View / 画面」節は Phase 7 で書き換え予定（本 Phase では未変更）

### レビュアー記入欄

- 承認者: iori-oiso（プロジェクトオーナー）
- レビュー依頼日: 2026-04-30
- 回答日: 2026-05-01
- 結論: 合格
- コメント: 2026-05-01 の Claude Preview 再起動で 9 観点をすべて再確認。タブレット 768px でサイドバー左固定 + 右上ダークモードトグル (sun/moon)、ダーク背景での htmx fragment 挿入、モバイル 375px でハンバーガー → ドロワー開（オーバーレイ dim 含む）すべて正常動作。Phase 2 完了として Phase 3（index.html 移植 + DataTables → htmx ページング基盤確立）へ進めて可。

---

## 添付ファイル

`docs/notes/T20260429-screen-renewal-phase2-attachments/` 配下に配置予定（実機画面スクリーンショット等）。

---

## 更新履歴

- 2026-04-29: 初版作成（ステップ 1〜2・Gate 1・Gate 2 セクション記載・コミット計画策定）
- 2026-04-29: Gate 1 / Gate 2（インライン）承認記録（iori-oiso・合格）
- 2026-04-30: 5 コミット（83d90d7b / 6c2e59ec / d6ad1848 / 1761c6ea / 18c44542）を `feature/screen-renewal-phase2-layout-tailwind` ブランチで実装
- 2026-04-30: Claude Preview で実機ブラウザ表示。Thymeleaf 3 / Spring Boot 3 で `#httpServletRequest` が null になる問題を発見し fix コミット 318b9467 で `@{/}` 経由 + app.js 側 trailing slash 除去に修正。全 9 観点の動作確認 OK
- 2026-05-01: Claude Preview を再起動し 9 観点を再確認（タブレット 768px サイドバー固定・ダークモード切替・htmx context-path 自動補正・モバイル 375px ハンバーガードロワー開閉）。Phase 2 Gate 3 承認記録（iori-oiso・合格）
