# Changelog

このプロジェクトのすべての注目すべき変更を記録します。
フォーマットは [Keep a Changelog](https://keepachangelog.com/ja/1.0.0/) に準拠し、
バージョン管理は [Semantic Versioning](https://semver.org/lang/ja/) に従います。

---

## [Unreleased]

## [2.3.2] - 2026-06-20

### Added
- 外部公開向け Basic 認証・セキュリティ強化（Spring Security / CSP / HttpOnly Cookie / HSTS）
- デスクトップサイドバーの折りたたみ機能（`localStorage` で永続化）
- フッターへのアプリバージョン表示

### Changed
- Basic 認証から Form Login に変更（モバイル対応）

### Added（v2.3.1 から取り込み）
- 残余利益モデル（RIM）の理論株価と2モデル合意度
- 企業価値算出係数の業種別重みづけ対応
- グレアム指数の業種内 z スコア表示
- 企業価値算出係数を `app.config.analysis` に外部化

## [2.3.1] - 2026-06-19

### Added
- 銘柄詳細への専門用語ツールチップ
- 銘柄詳細の提出日順前後ナビ（前の銘柄 / 次の銘柄ボタン）
- `/v3/index` デフォルトソートを `submitDate desc → code desc` に変更
- スマホ向け UI 刷新（3 タブボトムバー / テーブルカード化 / 並び替え select）
- PNG ビジュアルリグレッションテスト（5 画面 × 2 viewport = 10 ケース）

### Fixed
- `edinet-list-detail` で null 要素が `EL1007E` になる Thymeleaf バグ

## [2.3.0] - 2026-05-13

### Added
- 全画面を AdminLTE / jQuery / Bootstrap 4 から Tailwind CSS 3.4 / htmx 2.0 / Alpine.js 3 へ刷新
- ダークモード対応（`prefers-color-scheme` + localStorage トグル）
- Chart.js 4 による銘柄詳細グラフ（14 個）
- htmx 部分更新による高速テーブル検索・ソート・ページネーション
- Playwright スナップショット回帰検証

## [2.2.13] - 2024-03-10

### Changed
- EDINET API v2 対応

## [2.2.0] - 2022-12-10

### Added
- Graham 指数によるバリュエーション評価
- 株価評価画面（`/v3/valuation`）

## [2.1.0] - 2022-07-10

### Added
- 残余利益モデル（RIM）初期実装
- Resilience4j CircuitBreaker / RateLimiter によるスクレイピング保護

## [2.0.0] - 2022-04-01

### Changed
- クリーンアーキテクチャへのリアーキテクチャ（UseCase / Interactor 分離）
- Doma 2 + Flyway による永続化層の刷新

## [1.0.0] - 2020-07-18

### Added
- 初回リリース
- EDINET XBRL スクレイピング・企業価値算出・Web 画面表示の基本機能

[Unreleased]: https://github.com/ioridazo/fundanalyzer/compare/v2.3.2...HEAD
[2.3.2]: https://github.com/ioridazo/fundanalyzer/compare/v2.3.1...v2.3.2
[2.3.1]: https://github.com/ioridazo/fundanalyzer/compare/v2.3.0...v2.3.1
[2.3.0]: https://github.com/ioridazo/fundanalyzer/compare/v2.2.13...v2.3.0
[2.2.13]: https://github.com/ioridazo/fundanalyzer/compare/v2.2.0...v2.2.13
[2.2.0]: https://github.com/ioridazo/fundanalyzer/compare/v2.1.0...v2.2.0
[2.1.0]: https://github.com/ioridazo/fundanalyzer/compare/v2.0.0...v2.1.0
[2.0.0]: https://github.com/ioridazo/fundanalyzer/compare/v1.0.0...v2.0.0
[1.0.0]: https://github.com/ioridazo/fundanalyzer/releases/tag/v1.0.0
