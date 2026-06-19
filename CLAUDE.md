# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## プロジェクト概要

EDINETから取得した有価証券報告書（XBRL）と各種株価サイトのスクレイピング結果を突き合わせ、企業価値（理論株価・割引率・バリュエーション指標）を算出してWeb画面で可視化するSpring Bootアプリケーション。`pom.xml` の description は "Analyzing stocks from financial statements"。本番運用はWindowsサービス想定（`release/start.bat`, `release/tool/service-install.bat`）。

- **Java 17 / Spring Boot 3.1.0**
- **永続化**: Doma 2（DAO + SQLファイル）+ Flyway（`src/main/resources/db/migration/`）+ MySQL（本番） / H2（dev・test）
- **View**: Thymeleaf + thymeleaf-layout-dialect、AdminLTE系の静的アセット同梱。`src/main/resources/static/plugins/` はテンプレートが参照する 12 ディレクトリ（`bootstrap` / `datatables*` / `daterangepicker` / `fontawesome-free` / `inputmask` / `jquery` / `jszip` / `moment` / `pdfmake`）のみ保持。新規プラグイン追加時は AdminLTE 公式配布物から該当ディレクトリのみ取り込む
- **外部I/O**: EDINET API、日経/kabuoji3/minkabu/Yahoo Finance のスクレイピング、Slack Webhook通知、Selenium連携
- **可観測性**: log4j2 + ECS logging、Micrometer + OpenTelemetry/Zipkin、Resilience4j（CircuitBreaker / RateLimiter）

## ビルド・テストコマンド

Maven Wrapper を使うこと（`mvn` ではなく `./mvnw`）。本番Jenkins も `./mvnw` 前提。

```bash
# クリーンビルド（テスト込み・パッケージ生成）
./mvnw clean package

# テストのみ
./mvnw test

# 単一テストクラス / 単一メソッド
./mvnw test -Dtest=AnalyzeInteractorTest
./mvnw test -Dtest=AnalyzeInteractorTest#analyze_正常系

# Spring Bootアプリの起動（dev プロファイル既定。spring.profiles.active=dev|prod）
./mvnw spring-boot:run

# 起動時のプロファイル切替例
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod

# CIで実行している静的解析・カバレッジ一式（pipeline/Jenkinsfile-ci-prod.groovy 参照）
./mvnw test surefire-report:report pmd:pmd pmd:cpd jacoco:report spotbugs:spotbugs
./mvnw checkstyle:check          # sun_checks.xml に基づくチェック
```

起動後の入口: dev は `http://localhost:8889/fundanalyzer/`、prod は `http://localhost:8890/fundanalyzer/`（context-path は `application.yml` で `/fundanalyzer` 固定）。Actuator は dev では別ポート `http://localhost:8989/actuator/*` で全公開（`management.server.port` 設定）、prod は `http://localhost:8990/actuator/*`。

### フロントエンドビルド（画面刷新タスク Phase 1 以降）

`./mvnw package`（および `process-resources` 以降を含む phase）を実行すると、frontend-maven-plugin が `generate-resources` フェーズで以下を自動実行する:

1. `target/node/` に Node.js 20 LTS を初回のみ自動取得（`https://nodejs.org/dist/` 既定経路）
2. `src/main/frontend/` で `npm ci`（`package-lock.json` から再現可能インストール）
3. `npm run build` で Tailwind CSS と esbuild を順に起動し、`target/classes/static/css/app.css` と `target/classes/static/js/app.js` を生成

初回ビルドは Node 取得＋依存解決で +1〜2 分。後続は frontend-maven-plugin がキャッシュを活用し +5〜15 秒程度。本番 Windows サービス環境には Node を一切配置しない（ビルド時のみ取得・jar には CSS/JS 成果物のみ同梱）。

```bash
# フロントエンドのみ Maven 経由で再ビルド
./mvnw frontend:install-node-and-npm frontend:npm@npm-ci frontend:npm@npm-run-build

# ローカルに Node 20 がある場合（手元で素早く回したいとき）
cd src/main/frontend && npm ci && npm run build

# 開発時のウォッチモード（CSS / JS を別ターミナルで並行ウォッチ）
cd src/main/frontend
npm run watch:css   # 別ターミナル
npm run watch:js    # 別ターミナル
```

採用ライブラリ: Tailwind CSS 3.4 / htmx 2.0 / Alpine.js 3 / Lucide / Litepicker 2 / Chart.js 4 / esbuild 0.20。詳細は [docs/adr/ADR-001-screen-renewal-stack.md](docs/adr/ADR-001-screen-renewal-stack.md)。

#### Playwright スナップショット回帰検証

Phase 8 で `Phase8ScreenSnapshotTest` を導入。`@SpringBootTest` で Spring Boot を起動し Chromium で主要 3 画面（/v3/index / /v3/valuation / /v3/edinet-list）× 2 ビューポート（desktop 1280x800 / mobile 375x812）= 6 ケースのスナップショットを取得して回帰検証する。

```bash
# Playwright スナップショット込みで全テスト実行（初回 Chromium 取得で +200MB / +1 分）
./mvnw test

# Playwright を除外して既存テストのみ実行（高速）
./mvnw test -DexcludedGroups=playwright
```

スナップショットは `target/playwright-snapshots/<screen>-<viewport>.png` に書き出される。フルカラー比較は ADR-001 の方針で不採用、HTML 構造（aside / header / main / title）の存在のみアサーション。

> 画面刷新タスク（Phase 1〜7）完了済み。AdminLTE / jQuery / Bootstrap 4 / DataTables 等の旧スタックは全廃済み。詳細は [docs/notes/T20260429-screen-renewal-htmx-tailwind.md](docs/notes/T20260429-screen-renewal-htmx-tailwind.md) のマスタープラン参照。

### Mac で dev 起動する場合

`JAVA_HOME` を Homebrew 版 openjdk@17 に向けて起動する。

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home

# dev 起動（devtools の自動再起動が classloader 起因の起動失敗を起こすため、画面確認だけなら無効化推奨）
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments='-Dspring.devtools.restart.enabled=false'
```

ファイル出力先（`app.settings.file.path.*`）は dev プロファイルで `${user.home}/.fundanalyzer/...`（Mac: `/Users/<user>/.fundanalyzer/...`、Windows: `C:\Users\<user>\.fundanalyzer\...`）に上書き済み。`application.yml` のデフォルト `C:/fundanalyzer/...` は本番 Windows サービス向けで、`release/config/application-prod.yml` で再定義される。

スケジューラ群は全て `@Profile({"prod"})` のため dev では Bean 登録されず自動起動しない（[StockScheduler.java](src/main/java/github/com/ioridazo/fundanalyzer/web/scheduler/StockScheduler.java) 等参照）。画面表示確認のみであれば EDINET API キーや Selenium の準備は不要。

## アーキテクチャ（重要）

ルートパッケージ: `github.com.ioridazo.fundanalyzer`。**クリーンアーキテクチャ風の分離**を採用しており、これを崩さないこと。

```
client/      … 外部I/O（EDINET API, jsoup スクレイパ, Selenium, Slack, CSV, ログ）
config/      … Spring設定・Bean定義
domain/
  usecase/      … ユースケースのインタフェース（XxxUseCase）
  interactor/   … ユースケースの実装（XxxInteractor implements XxxUseCase）
  service/      … ドメインサービス
  domain/
    entity/         … Doma エンティティ（master / transaction / view）
    dao/            … Doma DAO（@Dao）
    specification/  … ドメインルールの集約点
  value/        … 値オブジェクト
  util/         … 汎用ユーティリティ
web/
  controller/   … 業務エンドポイント（AnalysisController, EdinetController, DevelopController）
  presenter/    … 画面表示用 Thymeleaf エンドポイント（IndexPresenter 等）
  scheduler/    … 定時バッチ（AnalysisScheduler / CompanyScheduler / StockScheduler）
  view/model/   … 画面DTO
  filter/       … サーブレットフィルタ
exception/   … 業務例外
```

**依存方向**: `web` → `domain.usecase`（interface）→ `domain.interactor`（impl）→ `domain.service` / `domain.domain.specification` → `domain.domain.dao` / `client`。Controller / Scheduler / Presenter から DAO や client を直接呼ばない。

**ユースケースの追加・変更時の鉄則**: 新しい処理を足すときは必ず `XxxUseCase`（interface）と `XxxInteractor`（impl）の **対** で追加する。既存のペア（`AnalyzeUseCase` / `AnalyzeInteractor` 等）を踏襲。

### 主要ユースケース（業務フローの骨格）

| ユースケース | 役割 |
|---|---|
| `CompanyUseCase` | EDINET の企業一覧（CSV）取り込み・マスタ更新 |
| `DocumentUseCase` | 提出書類一覧の取得・ダウンロード・解凍・対象判定 |
| `ScrapingUseCase` | XBRLからBS/PL/CFの数値抽出（jsoup + キーワード照合） |
| `AnalyzeUseCase` | 抽出済み財務数値から企業価値・理論株価を算出 |
| `StockUseCase` | 各種株価サイト（日経/kabuoji3/minkabu/Yahoo）からの株価取得 |
| `ValuationUseCase` | 株価とバリュエーション指標の評価（Graham等） |
| `ViewCorporateUseCase` / `ViewEdinetUseCase` / `ViewValuationUseCase` | 画面表示用ビューの集計 |
| `NoticeUseCase` | Slack通知 |

スケジューラ（`web/scheduler/`）は `app.scheduler.hour.*`（`application.yml`）で時刻トリガを設定。`@EnableAsync @EnableScheduling @EnableRetry` は `FundanalyzerApplication` で有効化済み。

### 外部呼び出しのレジリエンス

`client/edinet`, `client/jsoup` の各クライアントは Resilience4j の **CircuitBreaker + RateLimiter** が `application.yml` の `resilience4j.circuitbreaker.instances.*` / `resilience4j.ratelimiter.instances.*` で個別チューニングされている。新規スクレイピング先を追加するときは、必ず両方の設定を追加し、`recordFailurePredicate` を実装側に用意する（`EdinetClient.RecordFailurePredicate` / `JsoupClient.RecordFailurePredicate` を参照）。`base-uri` などの接続情報は `app.config.rest-client.<name>.*` に集約されている。

### 永続化（Doma + Flyway）

- マイグレーションは `src/main/resources/db/migration/V*.sql`。**既存ファイルは編集せず、新規バージョンを追加**（Flyway 規約）。
- DAO は `domain/domain/dao/{master|transaction|view}/` に配置。SQLは Doma 規約で `src/main/resources/META-INF/github/...` に対応する SQL ファイルを置く（`META-INF/github/com/...` を `find` で確認）。
- 設定で対象外とする企業/業種/書類タイプ等は `app.config.scraping.*`, `app.config.remove-document.*`, `app.config.view.*` に集約されており、コード内ハードコードしない。
- 企業価値算出の係数: **営業利益倍率・流動負債調整係数・資本コスト(RIM用 r) は `industry` マスタの列**（`operating_profit_weight`/`current_liabilities_ratio`/`cost_of_equity`・既定値も列 DEFAULT 10/1.2/0.08 で保持）、**年換算重み(4) はソース定数** `AnalysisResult.ANNUAL_WEIGHT`（不変）。`AnalyzeInteractor` が会社の `industryId` から `IndustrySpecification.resolveCoefficient(industryId)` で業種別の実効係数（`config/AnalysisCoefficient`＝op/curr/r の値オブジェクト）を解決し `new AnalysisResult(financeValue, document, coefficient)` で算出。業種別係数の変更は**新規分析分から反映**（既存 `analysis_result` は再分析の一意制約スキップにより据え置き）。
- **残余利益モデル(RIM)**: `AnalysisResult` が `BPS×(ROE/100)÷r` で2つ目の理論株価 `rimValue` を算出し `analysis_result.rim_value` に保存（前向きのみ・既存行はNULL）。`/v3/corporate` の表示情報に「RIM理論株価」＋「2モデル合意度（企業価値モデルとRIMのうち株価より高い＝割安と判定した数 / 評価できた数）」を表示（`CorporatePresenter.setRimAndAgreement`）。

### View / 画面

`web/presenter/*Presenter.java` が Thymeleaf テンプレート（`src/main/resources/templates/*-v2.html` および `templates/fragments/*.html`）にバインド。`layout-v2.html` を thymeleaf-layout-dialect で継承する構造（[T20260429-screen-renewal-htmx-tailwind.md](docs/notes/T20260429-screen-renewal-htmx-tailwind.md) で全画面刷新済）。

#### 採用スタック

- **CSS**: Tailwind CSS 3.4（utility-first・ダークモード `dark:` variant）
- **クライアント JS**: htmx 2.0（部分更新）+ Alpine.js 3（ローカル UI 状態）
- **アイコン**: Lucide（SVG）
- **期間選択**: Litepicker 2
- **グラフ**: Chart.js 4
- **すべて npm 経由・esbuild バンドル** で `target/classes/static/css/app.css` および `target/classes/static/js/app.js` に出力。**外部 CDN への依存ゼロ**

#### エンドポイントとテンプレート

| URL | テンプレート | Presenter |
|---|---|---|
| `/v3/index` / `/v3/index/table` | `index-v2.html` + `fragments/index-table.html` | IndexPresenter |
| `/v3/corporate` | `corporate-v2.html`（Chart.js 14 個） | CorporatePresenter |
| `/v3/valuation` / `/v3/valuation/table` | `valuation-v2.html` + `fragments/valuation-table.html`（5 view fragment） | ValuationPresenter |
| `/v3/edinet-list` / `/v3/edinet-list/table` | `edinet-list-v2.html` + `fragments/edinet-list-table.html` | EdinetPresenter |
| `/v3/edinet-list-detail` | `edinet-list-detail-v2.html` | EdinetDetailPresenter |
| エラー画面 | `error.html`（layout-v2 継承） | Spring Boot 標準 |

#### デフォルトソート

`/v3/index` のデフォルトソートは **`submitDate desc → code desc`** の複合ソート。`submitDate` 明示ソート時のみ `code desc` を tie-break として加える（他カラム明示ソート時は tie-break なし）。`viewMain()` 等の Interactor 内部順を v2 時代と同等に画面に反映する目的（[T20260509](docs/notes/T20260509-corporate-detail-prev-next-nav.md)）。

#### 銘柄詳細の前後ナビ

`/v3/corporate?code=XXXX` のヘッダー H1 横に **前の銘柄 / 次の銘柄** ボタンを配置。`backwardCode` / `forwardCode` は `target=main|quart|all` 指定時はその view ベース、`target` 未指定時は `viewAll()` ベースで算出する。**「次=より新しい提出日」「前=より古い提出日」** で統一。端ではボタン非表示（`th:if`）。スマホ (sm 未満) はタイトル下の独立行に表示（[T20260509](docs/notes/T20260509-corporate-detail-prev-next-nav.md)）。

#### 株価評価の業種内相対表示（グレアム指数）

`/v3/valuation` の graham-index view に **「実数 / 業種内相対」トグル** を配置。相対 ON（`mode=relative`）時、グレアム指数列の表示値を**業種内zスコア**（全対象企業を業種グルーピングし `(値−業種平均)/業種σ`）に差し替える（その場計算・非永続・DB変更なし）。z 算出は `ViewValuationUseCase#findGrahamIndustryZScore` / `ViewValuationInteractor.computeGrahamIndustryZScore`（業種内 n<3・σ=0・graham null は対象外）。`ViewService` が `CompanyValuationViewModel#withGrahamIndex` で値を差し替え。`mode` は `ValuationPresenter` がホワイトリスト検証（graham-index view のみ relative 有効、他は raw）。sort/search/paginator は `mode=relative` 時のみ URL に mode を伝播（[T20260601-2](docs/notes/T20260601-industry-relative-zscore.md)）。

#### 銘柄詳細の用語ツールチップ

`/v3/corporate` の `dt` / `th` / 見出しテキスト直後に `<th:block th:replace="~{fragments/tooltip :: hint('<key>')}"></th:block>` を埋め込み、hover / focus / tap で用語解説ポップオーバーを表示する。用語キー → ラベル + 解説本文の辞書は [fragments/tooltip.html](src/main/resources/templates/fragments/tooltip.html) に Thymeleaf inline Map で集約。クライアント側は Alpine.js コンポーネント `tooltip` ([app.js](src/main/frontend/scripts/app.js)) で開閉、ESC / 外側クリックで閉じる。`x-cloak` の display:none は [main.css](src/main/frontend/styles/main.css) の `@layer base` で定義（[T20260512](docs/notes/T20260512-corporate-glossary-tooltip.md)）。

#### テーブル汎用パターン

各画面は **2 エンドポイント方式**（HTML 全体 + fragment 部分更新）+ **record（XxxTableQuery / XxxTablePage）** + **ViewService.findXxxTable**（メモリ内 stream で filter/sort/page）+ **htmx 属性**（`hx-get` + `hx-target` + `hx-trigger="keyup changed delay:300ms"` + `hx-push-url="true"` で URL 同期）で統一。詳細は Phase 3〜5 のサブタスク md 参照。

#### ダークモード

`layout-v2.html` の Alpine.js x-data でダークモード状態を管理し、`localStorage('fundanalyzer.dark-mode')` を優先・未設定時は `prefers-color-scheme: dark` で初期反映。ヘッダー右上の sun/moon トグルで切替・永続化。

#### サイドバー折りたたみ（デスクトップ / T20260619-2）

デスクトップ (md+) でサイドバーをアイコンのみ (w-16) に折りたたむ機能。`layout-v2.html` の Alpine.js x-data に `sidebarCollapsed` boolean を保持し `localStorage('fundanalyzer.sidebar-collapsed')` で永続化。ヘッダー左上のトグルボタン（旧 `md:hidden` を外して全幅で常時表示）をクリックすると、デスクトップでは幅をトグル・モバイルでは従来どおりオーバーレイサイドバーを開閉する（`window.matchMedia('(min-width: 768px)')` で分岐）。折りたたみ時はナビテキストを非表示・アイコンを中央揃え・ロゴ文字を非表示にし、各ナビリンクに `title` 属性でホバーツールチップを表示する。aside は静的クラスで `w-64` を保持し折りたたみ時は `:class="{ 'md:w-16': sidebarCollapsed }"` で上書き、コンテンツ wrapper は `:class="sidebarCollapsed ? 'md:ml-16' : 'md:ml-64'"` で連動する（[T20260619-2](docs/notes/T20260619-2-sidebar-collapsible.md)）。

#### スマホ対応（sm 未満 / Phase 9 = T20260502）

[T20260502-mobile-ui-renewal.md](docs/notes/T20260502-mobile-ui-renewal.md) で対応。Tailwind `sm` (640px) 未満で:

- **ボトムバー** 3 タブ（会社 / 株価 / EDINET）が `<nav aria-label="モバイルナビ">` で `fixed bottom-0 sm:hidden`。layout-v2 の wrapper は `min-w-0 overflow-x-hidden` で flex item shrink + 横オーバーフロー抑止
- **テーブル → カード化**: 全 fragment（index-table / valuation-table 5 view / edinet-list-table / edinet-document-card / edinet-list-detail サマリ / corporate-v2 5 サブテーブル）に desktop `<table class="hidden sm:block">` + mobile `<ul class="block sm:hidden" data-mobile-card>` の 2 系統
- **並び替え select**: 各画面 (sm 未満で表示) に view 別オプションを持たせ `hx-get` で fragment 部分更新
- **canvas オーバーフロー対策**: `main.css` の `@layer base` で `canvas { max-width:100%; min-width:0; width:100% !important }` を設定（Chart.js inline `style="width: 420px"` 抑止）
- **手動スクショテスト**: [ManualMobileScreenshotTest.java](src/test/java/github/com/ioridazo/fundanalyzer/web/ManualMobileScreenshotTest.java) を `@Tag("manual-screenshot")` で外付け Spring Boot 起動時に CDP 経由で撮影。`./mvnw test -Dtest=ManualMobileScreenshotTest -Dgroups=manual-screenshot -DfailIfNoTests=false` で実行可能
- **PNG ビジュアルリグレッション baseline**: `src/test/resources/playwright-baselines/<screen>-<viewport>.png` 配置（ADR-001 v1.2 で方針追記）
- **PNG ビジュアルリグレッションテスト**: [MobileScreenshotRegressionTest.java](src/test/java/github/com/ioridazo/fundanalyzer/web/MobileScreenshotRegressionTest.java) で 5 画面 × 2 viewport = 10 ケースを `@SpringBootTest(RANDOM_PORT)` + Playwright で baseline 比較。BufferedImage の ARGB 単位でピクセル差分を計算し、差分比率 2% を超えたら失敗。差分発生時は `target/playwright-snapshots/diff-<screen>-<viewport>{,-baseline,-current}.png` に baseline / current / diff 強調画像を出力。

  ```bash
  # baseline と現在の UI が一致するか検証
  ./mvnw test -Dtest=MobileScreenshotRegressionTest -Dgroups=playwright

  # 通常ビルドから除外
  ./mvnw test -DexcludedGroups=playwright
  ```

- **baseline 更新手順**（意図した UI 変更を反映する場合のみ実施）:
  1. ローカルで dev サーバーを起動: `./mvnw spring-boot:run -Dspring-boot.run.jvmArguments='-Dspring.devtools.restart.enabled=false'`
  2. 別ターミナルで baseline 上書きを実行:

     ```bash
     ./mvnw test -Dtest=ManualMobileScreenshotTest -Dgroups=manual-screenshot -DfailIfNoTests=false -DupdateBaselines=true
     ```

  3. `git diff src/test/resources/playwright-baselines/` で差分画像をコミット前に確認し、PR 説明に変更理由と差分の見え方を記載
  4. PR レビューで baseline 画像を **目視承認** してからマージ（CI に baseline 正当性の判断は委ねない）

## 設定ファイル

- `src/main/resources/application.yml` — 共通設定（dev既定）
- `src/main/resources/application-dev.yml` / `application-prod.yml` — プロファイル個別
- `release/config/application-prod.yml` — 本番Windowsサービス起動時に読み込む差分設定（`release/start.bat`）
- `app.config.edinet.api-key` は環境変数 `edinet.api-key`（`release/env` 経由）に対応。**コミットしない**

### セキュリティ（外部公開対応 / T20260619）

- **Basic 認証**: [SecurityConfig.java](src/main/java/github/com/ioridazo/fundanalyzer/config/SecurityConfig.java) で Spring Security を構成。全リクエスト（静的リソース含む）を認証必須とし、利用者は `app.security.user` / `app.security.password` から構成する単一のメモリ内ユーザーのみ。
- **資格情報の環境変数**: `SECURITY_USER` / `SECURITY_PASSWORD`。dev は `application.yml` の既定値（`admin` / `fundanalyzer-local-dev`）で起動可能。**prod は [release/config/application-prod.yml](release/config/application-prod.yml) でフォールバックなし `${SECURITY_USER}` / `${SECURITY_PASSWORD}` を必須化**しており、`release/env` に未設定だと起動失敗で検知する。
- **Slack トークン**: `app.config.slack.parameter.t/b/x` は `SLACK_WEBHOOK_T` / `SLACK_WEBHOOK_B` / `SLACK_WEBHOOK_X` に対応（直書き廃止）。`release/env` 経由で設定。テンプレートは [release/env.example](release/env.example)。
- **CSRF**: 有効のまま維持。Thymeleaf の `th:action` フォームは Spring Security の `CsrfRequestDataValueProcessor` がトークンを自動注入する。htmx の table fragment は全て GET のため非対象。
- **セキュリティヘッダー**: SecurityConfig で CSP / X-Frame-Options(DENY) / X-Content-Type-Options(nosniff) / Referrer-Policy(same-origin) / HSTS を付与。CSP はバンドル済み自前 JS（Alpine.js が `unsafe-eval`、`corporate-v2.html` のインライン Chart.js データが `unsafe-inline` を要する）を許可しつつ外部オリジンを遮断する方針。
- **Cookie**: `server.servlet.session.cookie` で HttpOnly / Secure / SameSite=Strict を設定。
- **HTTPS**: アプリ内では終端しない。リバースプロキシ（nginx 等）で TLS 終端する前提（Secure Cookie / HSTS はこれに依存）。
- **Actuator 別ポート（dev 8989 / prod 8990）は認証保護対象外**（外部に露出させない運用前提）。
- **Playwright テスト**: Security 有効化により実 HTTP アクセスする `Phase8ScreenSnapshotTest` / `MobileScreenshotRegressionTest` は `@SpringBootTest(properties=app.security.user/password=playwright)` + Playwright の `setHttpCredentials` で Basic 認証を付与する。`ManualMobileScreenshotTest` は dev 既定資格情報（`-DmanualScreenshotUser/Password` で上書き可）。

ファイル出力先（`app.settings.file.path.*`）は dev で `${user.home}/.fundanalyzer/...`（OS 非依存）、prod で `C:/fundanalyzer/...`（[release/config/application-prod.yml](release/config/application-prod.yml) で再定義、Windows サービス前提）に切り替わる。`application.yml` のデフォルト値は prod 互換のため Windows パスを保持しているが、dev ではこの値は使われない。

## CI / リリース

- **CI**: Jenkins（`pipeline/Jenkinsfile-ci-prod.groovy`, `Jenkinsfile-dev.groovy`）— openjdk17 + `./mvnw test surefire-report:report pmd:pmd pmd:cpd jacoco:report spotbugs:spotbugs`
- **CD**: `pipeline/Jenkinsfile-cd-prod.groovy` で本番Windows機へデプロイ
- **Maven distribution**: `pom.xml` で社内Nexus（`http://localhost:8081/repository/...`）。手元から deploy する用途は通常無し。

## コーディング上の留意点（このリポ固有）

- **Lombok 使用中**: 既存コードでは `@Value` 等を使っている箇所がある。グローバル規約で「Lombok禁止」を採るプロジェクトと混同しないこと（このリポではLombokを外す指示が無い限り維持）。
- **Java 17 / Doma 2**: アノテーションプロセッサ（`doma-processor`）が `provided` で入っている。IDE で注釈処理を有効化していないと DAO 実装クラスが生成されずビルドが通らない。
- **Checkstyle は `sun_checks.xml`** をプロジェクト直下に置いて使用。新規クラス追加時はこの規約に通すこと。
- **`develop/document/*.drawio`** にER図・フローチャート・アプリ構成図がある。アーキテクチャ変更時は更新する。
- **`docs/plans/` は `.gitignore` 済み**（個人作業用）。
- **`release/env` も `.gitignore` 済み**（APIキー等の本番環境変数）。

## デバッグ・トラブル時の入口

- ログは `log4j2-spring.xml` / `log4j2-spring-dev.xml`、本番は ECS フォーマット → Filebeat（`elastic/filebeat/filebeat.yml`）→ Logstash（`elastic/logstash/`）→ Elastic。
- 外部APIで止まったら、まず Actuator `/fundanalyzer/actuator/health` の `circuitBreakers` と `rateLimiters` を確認。
- スケジューラの実行時刻は `app.scheduler.hour.*` を確認（dev では時刻によっては動かない）。

## 開発プロセス（一次情報源: `docs/guideline/`）

本リポジトリの開発プロセスは [`docs/guideline/`](docs/guideline/) に集約されている。タスク着手前に必ず参照し、これを **一次情報源** として運用する（CLAUDE.md にルールを再掲しない／重複させない）。

| ファイル | 目的 |
|---|---|
| [README.md](docs/guideline/README.md) | 索引・3+N 体制・タスクサイクル・コア原則 |
| [workflow.md](docs/guideline/workflow.md) | 6 ステップのタスクサイクル本体 |
| [roles.md](docs/guideline/roles.md) | 計画 / 実装 / 検証エージェントの役割と禁止事項 |
| [human-checkpoints.md](docs/guideline/human-checkpoints.md) | Gate 1 / 2 / 3 の運用と通過記録（タスク 1 md 統合方式） |
| [impact-analysis.md](docs/guideline/impact-analysis.md) | 影響範囲分析（参照層 / 状態層 / データ層） |
| [infra-impact-checklist.md](docs/guideline/infra-impact-checklist.md) | インフラ影響チェック |
| [test-strategy.md](docs/guideline/test-strategy.md) | テスト戦略テンプレート |
| [security-policy.md](docs/guideline/security-policy.md) | セキュリティ方針テンプレート |
| [document-plan.md](docs/guideline/document-plan.md) | ドキュメント計画テンプレート |
| [git-strategy.md](docs/guideline/git-strategy.md) | Git 戦略（ブランチ・コミット・マージ規約・禁止操作） |

> Claude Code として作業する際は、タスク開始時に最低限 `README.md` → `workflow.md` → `roles.md` → `human-checkpoints.md` を参照する。Gate 1 / Gate 3 は **人間レビュアの承認が必須・スキップ不可** であり、Claude が独断で通過させない。
