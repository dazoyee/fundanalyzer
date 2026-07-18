# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## プロジェクト概要

EDINETから取得した有価証券報告書（XBRL）と各種株価サイトのスクレイピング結果を突き合わせ、企業価値（理論株価・割引率・バリュエーション指標）を算出してWeb画面で可視化するSpring Bootアプリケーション。本番運用はWindowsサービス想定（`release/start.bat`）。

- **Java 17 / Spring Boot 3.1.0**
- **永続化**: Doma 2 + Flyway + MySQL（本番） / H2（dev・test）
- **View**: Thymeleaf + Tailwind CSS 3.4 / htmx 2.0 / Alpine.js 3 / Chart.js 4
- **外部I/O**: EDINET API、日経/kabuoji3/minkabu/Yahoo Finance スクレイピング、Slack Webhook
- **可観測性**: log4j2、Micrometer + OpenTelemetry/Zipkin、Resilience4j（CircuitBreaker / RateLimiter）

## ビルド・テストコマンド

Maven Wrapper を使うこと（`mvn` ではなく `./mvnw`）。

```bash
# クリーンビルド（テスト込み）
./mvnw clean package

# テストのみ（Playwright 除外で高速）
./mvnw test -DexcludedGroups=playwright

# 単一テストクラス / 単一メソッド
./mvnw test -Dtest=AnalyzeInteractorTest
./mvnw test -Dtest=AnalyzeInteractorTest#analyze_正常系

# 静的解析・カバレッジ一式（CI と同等）
./mvnw test surefire-report:report pmd:pmd pmd:cpd jacoco:report spotbugs:spotbugs
./mvnw checkstyle:check

# フロントエンド開発時のウォッチモード（別ターミナルで並行起動）
cd src/main/frontend
npm run watch:css
npm run watch:js
```

### Mac で dev 起動する場合

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments='-Dspring.devtools.restart.enabled=false'
```

起動後: `http://localhost:8889/fundanalyzer/`（dev）/ `http://localhost:8890/fundanalyzer/`（prod）
Actuator: `http://localhost:8989/actuator/*`（dev）/ `http://localhost:8990/actuator/*`（prod）

スケジューラは `@Profile({"prod"})` のため dev では起動しない。画面確認のみなら EDINET API キーや Selenium 不要。

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
web/
  controller/   … 業務エンドポイント
  presenter/    … 画面表示用 Thymeleaf エンドポイント
  scheduler/    … 定時バッチ（prod のみ起動）
  view/model/   … 画面DTO
exception/   … 業務例外
```

**依存方向**: `web` → `domain.usecase`（interface）→ `domain.interactor`（impl）→ `domain.service` / `specification` → `dao` / `client`。Controller / Scheduler / Presenter から DAO や client を直接呼ばない。

**ユースケース追加時の鉄則**: `XxxUseCase`（interface）と `XxxInteractor`（impl）の **対** で追加する。

### 主要ユースケース

| ユースケース | 役割 |
|---|---|
| `CompanyUseCase` | EDINET 企業一覧（CSV）取り込み・マスタ更新 |
| `DocumentUseCase` | 提出書類一覧の取得・ダウンロード・解凍・対象判定 |
| `ScrapingUseCase` | XBRLからBS/PL/CFの数値抽出（jsoup + キーワード照合） |
| `AnalyzeUseCase` | 財務数値から企業価値・理論株価を算出 |
| `StockUseCase` | 各種株価サイトからの株価取得 |
| `ValuationUseCase` | 株価とバリュエーション指標の評価（Graham等） |
| `ViewCorporateUseCase` / `ViewEdinetUseCase` / `ViewValuationUseCase` | 画面表示用ビューの集計 |
| `NoticeUseCase` | Slack通知 |

### 外部呼び出しのレジリエンス

`client/edinet`, `client/jsoup` は Resilience4j の **CircuitBreaker + RateLimiter** で保護。新規スクレイピング先を追加するときは `application.yml` の `resilience4j.circuitbreaker.instances.*` / `resilience4j.ratelimiter.instances.*` に設定を追加し、`recordFailurePredicate` を実装する（`EdinetClient.RecordFailurePredicate` を参照）。

### 永続化（Doma + Flyway）

- マイグレーションは `src/main/resources/db/migration/V*.sql`。**既存ファイルは編集せず、新規バージョンを追加**。
- DAO は `domain/domain/dao/{master|transaction|view}/` に配置。SQLは Doma 規約で `src/main/resources/META-INF/github/...` に対応する SQL ファイルを置く。
- 設定で対象外とする企業/業種/書類タイプ等は `app.config.scraping.*` 等に集約。コード内ハードコードしない。
- 企業価値算出の係数（営業利益倍率・流動負債比率・資本コスト）は `industry` マスタの列で管理。`IndustrySpecification.resolveCoefficient()` で解決する。
- **係数を変更したら必ず一括再計算を実行する**: 分析結果・評価は「モデル検証」の意味論（全期間を常に現行係数で一貫させる）を採用しており、係数マイグレーション適用後に `POST /v1/admin/analysis/recalculate` で過去分の `analysis_result`（企業価値・RIM）と `valuation`（割引値・割引率）を再計算する。新旧係数の混在を許容しない（設計判断の経緯は `docs/notes/` の対応タスク md を参照）。
- BPS/EPS/ROE/ROA・PER/PBR/グレアム指数などの係数非依存指標は永続化せず、財務諸表値・株価から表示時に都度計算する。

### View / 画面

各画面は **2 エンドポイント方式**（HTML 全体 + htmx fragment 部分更新）で統一。`web/presenter/*Presenter.java` が `src/main/resources/templates/*-v2.html` にバインド。

| URL | Presenter |
|---|---|
| `/v3/index` | IndexPresenter |
| `/v3/corporate` | CorporatePresenter |
| `/v3/edinet-list` | EdinetPresenter |
| `/v3/edinet-list-detail` | EdinetDetailPresenter |
| `/v3/analysis` | AnalysisPresenter |

各画面の実装詳細（ソート・前後ナビ・ツールチップ・グレアムzスコア・ダークモード・サイドバー・スマホ対応等）は [`docs/notes/`](docs/notes/) の対応タスク md を参照。

## 設定ファイル

- `src/main/resources/application.yml` — 共通設定（dev 既定）
- `src/main/resources/application-dev.yml` / `application-prod.yml` — プロファイル個別
- `release/config/application-prod.yml` — 本番 Windows サービス起動時に読み込む差分設定

### セキュリティ

アプリケーション層では認証を行わない（全リクエスト `permitAll`）。アクセス制御はネットワーク境界
（ファイアウォール・バインドアドレス等）に委ねる。セキュリティヘッダー（CSP/HSTS 等）と CSRF 保護のみ
アプリケーション層で維持する。

- Slack トークン: `SLACK_WEBHOOK_T` / `SLACK_WEBHOOK_B` / `SLACK_WEBHOOK_X`（`release/env` 経由）

詳細は [SECURITY.md](SECURITY.md) を参照。

## CI / リリース

- **CI**: Jenkins（`pipeline/Jenkinsfile-ci-prod.groovy`）— `./mvnw test ... spotbugs:spotbugs`
- **CD**: `pipeline/Jenkinsfile-cd-prod.groovy` で本番 Windows 機へデプロイ
- **ブランチ**: Git Flow 採用。**作業は `develop` から派生させ `develop` にマージする**（`main` への直接 push 禁止）
- **バージョン更新**: `./mvnw versions:set -DnewVersion=X.Y.Z && ./mvnw versions:commit`

リリース手順の詳細は [CONTRIBUTING.md](CONTRIBUTING.md) を参照。

## コーディング上の留意点（このリポ固有）

- **Lombok 使用中**: 既存コードでは `@Value` 等を使っている。グローバル規約の「Lombok 禁止」と混同しないこと。
- **Doma 2**: アノテーションプロセッサ（`doma-processor`）が `provided`。IDE で注釈処理を有効化していないと DAO 実装クラスが生成されずビルドが通らない。
- **Checkstyle**: `sun_checks.xml` をプロジェクト直下に置いて使用。新規クラス追加時は通すこと。
- **`develop/document/*.drawio`**: ER 図・フローチャート・アプリ構成図。アーキテクチャ変更時は更新する。
- **`docs/plans/`** は `.gitignore` 済み（個人作業用）。**`release/env`** も `.gitignore` 済み（APIキー等）。

## デバッグ・トラブル時の入口

- ログ: `log4j2-spring.xml` / `log4j2-spring-dev.xml`（本番は ECS フォーマット → Filebeat → Elastic）
- 外部 API で止まったら Actuator の `circuitBreakers` と `rateLimiters` を確認
- スケジューラの実行時刻は `app.scheduler.hour.*` を確認（dev では時刻によっては動かない）

## 開発プロセス（一次情報源: `docs/guideline/`）

| ファイル | 目的 |
|---|---|
| [README.md](docs/guideline/README.md) | 索引・3+N 体制・タスクサイクル・コア原則 |
| [workflow.md](docs/guideline/workflow.md) | 5 ステップのタスクサイクル本体 |
| [roles.md](docs/guideline/roles.md) | 計画 / 実装 / 検証エージェントの役割と禁止事項 |
| [human-checkpoints.md](docs/guideline/human-checkpoints.md) | Gate 1 / 2 の運用と通過記録 |
| [impact-analysis.md](docs/guideline/impact-analysis.md) | 影響範囲分析 |
| [git-strategy.md](docs/guideline/git-strategy.md) | Git 戦略（ブランチ・コミット・マージ規約） |

> Gate 1 / Gate 2 は **人間レビュアの承認が必須・スキップ不可**。
