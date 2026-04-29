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

起動後の入口: `http://localhost:8080/fundanalyzer/`（context-path は `application.yml` で `/fundanalyzer` 固定）。Actuator は `/fundanalyzer/actuator/*` で全公開（dev既定）。

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

### View / 画面

`web/presenter/*Presenter.java` が Thymeleaf テンプレ（`src/main/resources/templates/*.html`）にバインド。`layout.html` を thymeleaf-layout-dialect で継承。`static/dist` および `static/plugins` の資産は AdminLTE テーマ由来。**個別ファイルの内容は改変しない**（バージョンアップ時は配布物ごと差し替え）。一方、テンプレートから未参照のディレクトリ・ファイルは保守対象を絞るために削除する方針（[T20260429-frontend-asset-cleanup](docs/notes/T20260429-frontend-asset-cleanup.md) で初回整理済み）。

## 設定ファイル

- `src/main/resources/application.yml` — 共通設定（dev既定）
- `src/main/resources/application-dev.yml` / `application-prod.yml` — プロファイル個別
- `release/config/application-prod.yml` — 本番Windowsサービス起動時に読み込む差分設定（`release/start.bat`）
- `app.config.edinet.api-key` は環境変数 `edinet.api-key`（`release/env` 経由）に対応。**コミットしない**

dev起動でファイル出力先（`app.settings.file.path.*`）が `C:/fundanalyzer/...` 固定の点に注意。Mac/Linuxで動かすときは個人プロファイル等で上書きが必要。

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
