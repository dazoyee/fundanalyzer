# fundanalyzer

EDINET から取得した有価証券報告書（XBRL）と各種株価サイトのスクレイピング結果を突き合わせ、企業価値（理論株価・割引率・バリュエーション指標）を算出して Web 画面で可視化する Spring Boot アプリケーション。

## 技術スタック

| カテゴリ | 採用技術 |
|---|---|
| バックエンド | Java 17 / Spring Boot 3.1.0 / Doma 2 / Flyway |
| フロントエンド | Tailwind CSS 3.4 / htmx 2.0 / Alpine.js 3 / Chart.js 4 |
| データベース | MySQL（本番） / H2（dev・test） |
| 可観測性 | log4j2 / Micrometer / OpenTelemetry / Zipkin / Resilience4j |
| 外部連携 | EDINET API / 日経・kabuoji3・minkabu・Yahoo Finance |

## エンドポイント一覧

| 画面 | URL |
|---|---|
| 銘柄一覧 | `/v3/index` |
| 銘柄詳細 | `/v3/corporate?code={code}` |
| 株価評価 | `/v3/valuation` |
| EDINET 一覧 | `/v3/edinet-list` |
| Actuator（dev） | `http://localhost:8989/actuator/*` |
| Actuator（prod） | `http://localhost:8990/actuator/*` |

## プロファイル

| プロファイル | ポート | DB | スケジューラ |
|---|---|---|---|
| `dev`（既定） | 8889 | H2 | 無効 |
| `prod` | 8890 | MySQL | 有効 |

## ドキュメント

| ドキュメント | 場所 |
|---|---|
| 開発ガイドライン | [`docs/guideline/`](docs/guideline/README.md) |
| タスク記録・設計メモ | [`docs/notes/`](docs/notes/) |
| ADR（アーキテクチャ決定記録） | [`docs/adr/`](docs/adr/) |
| ER 図・構成図 | [`develop/document/`](develop/document/) |

## 関連ドキュメント

- [CHANGELOG.md](CHANGELOG.md) — バージョン別の変更履歴
- [CONTRIBUTING.md](CONTRIBUTING.md) — 開発・ビルド・コミット規約
- [SECURITY.md](SECURITY.md) — セキュリティ方針・脆弱性報告
