# 0001: 画面層を Thymeleaf+htmx+Tailwind+Alpine.js に刷新（AdminLTE 排除）

- **日付**: 2026-04-29
- **ステータス**: 承認済（2026-04-29、iori-oiso 承認）
- **決定者**: iori-oiso（プロジェクトオーナー）

---

## コンテキスト

`templates/*.html` と `static/dist`・`static/plugins/` が AdminLTE 由来のクライアント主導アーキテクチャに強く依存しており、以下の課題を抱えている。

- **保守性**: jQuery / Bootstrap 4 / DataTables / daterangepicker / moment / inputmask / pdfmake / jszip / Font Awesome の多数の旧世代ライブラリに分散依存
- **見た目**: AdminLTE 固有のクラスが 103 件、Font Awesome アイコンが 149 件と多く、近代的な UI への切替コストが大きい
- **配信**: Chart.js が外部 CDN（cdnjs）から読み込まれており、サプライチェーンと閉域環境耐性に懸念
- **モバイル対応**: 現状はデスクトップ向けレイアウトのみで、運用者のモバイル参照に応えられない
- **拡張性**: DataTables のクライアント側全件保持はデータ規模拡大時のスケール上限がある

新規スタックの選定にあたり、以下の制約を満たす必要がある:

1. **本番ランタイムへの新規依存ゼロ**（Spring Boot 単一プロセス・Windows サービス起動を維持）
2. **Java/Maven 中心のビルドフロー** との統合（Jenkins CI が `./mvnw package` を実行）
3. **Thymeleaf SSR の継続**（既存 Presenter / view DTO の構造を活かす）
4. **段階移行可能**（旧 layout と新 layout の並走）

---

## 検討した選択肢

### 選択肢 A: Thymeleaf + htmx + Tailwind CSS + Alpine.js（採用）

- **構成**: Thymeleaf SSR を維持し、部分更新は htmx の `hx-*` 属性で表現。CSS は Tailwind utility-first、クライアント状態は Alpine.js、アイコンは Lucide、期間選択は Litepicker、グラフは Chart.js（CDN→ローカル化）
- **メリット**:
  - SSR を維持するため既存 Controller / Presenter / view DTO の責務をほぼ変えずに済む
  - フロントエンド SPA を別プロセスに分離する必要がなく、Spring Boot 単一プロセス構成を維持できる
  - htmx の学習コストが低く、jQuery 排除と段階移行が両立できる
  - Tailwind のダークモード・レスポンシブが標準で備わっており、社内管理画面に必要な要素が揃う
  - utility-first により AdminLTE のような巨大テーマ依存を排除できる
- **デメリット**:
  - htmx 導入により Spring 側の Presenter にフラグメント返却用エンドポイントの追加が必要（11 テーブル分）
  - Tailwind のクラス名が HTML に直接書かれるため、テンプレートが冗長になる
  - 新しいビルドツールチェーン（Node + frontend-maven-plugin）の導入コストがある

### 選択肢 B: SPA 分離（React / Vue + REST API）

- **構成**: Spring Boot を REST API 化し、React または Vue で SPA を構築。Thymeleaf を撤去
- **メリット**:
  - リッチな UI が作りやすく、コンポーネント再利用も豊富
  - フロント・バック分離で責務が明確
- **デメルット**:
  - Vite/Node 等のビルドサーバが本番にも必要、もしくはアセット配信構成が複雑化する
  - 既存 Presenter / view DTO / 5 画面のテンプレートを **すべて捨てる** 必要がある
  - CORS / 認証 / ルーティング / 状態管理 / API 設計など実装コストが膨らむ
  - 運用者向けの社内管理画面に対してオーバーキル

### 選択肢 C: AdminLTE 維持・段階アップデート

- **構成**: AdminLTE を新版（v3 / v4）にアップデートし、Bootstrap 4 → 5 への追従、jQuery プラグイン群を維持
- **メリット**:
  - 既存テンプレート資産を最大限活かせる
  - 学習コストが最も低い
- **デメリット**:
  - jQuery 依存 / DataTables のクライアント側全件保持 / pdfmake / Font Awesome の依存は残る
  - 新版 AdminLTE は v3 (Bootstrap 5) 系統だがメンテナンス頻度が低下しており、長期投資先として弱い
  - 「保守性向上」「ダークモード対応」「サプライチェーン縮小」などの主要課題を解決できない

### 選択肢 D: AdminLTE → 既製 Bootstrap 5 系管理画面テーマ（Tabler / CoreUI / Hyper）

- **構成**: AdminLTE の代わりに Tabler 等の Bootstrap 5 ベース管理画面テーマを採用。jQuery を排除し、テーマ既製コンポーネントを使う
- **メリット**:
  - ダークモード対応・モダンな見た目が標準で備わる
  - Bootstrap の知見をある程度流用できる
- **デメリット**:
  - 既製テーマの巨大 CSS / JS バンドル依存は AdminLTE と同質の問題（テーマ独自クラスへのロックイン）
  - utility-first の自由度に欠け、刷新後の見た目改善コストが残る
  - DataTables 排除・htmx 採用と組み合わせる場合、既製テーマの JS との衝突を解消する必要がある

---

## 決定

**選択肢 A（Thymeleaf + htmx + Tailwind CSS + Alpine.js）を採用する。**

採用するライブラリ群と現行安定版の目安:

| ライブラリ | 採用バージョン目安 | ライセンス | 役割 |
|---|---|---|---|
| Tailwind CSS | 3.4.x（最新の 3.x 系） | MIT | utility-first CSS。ダークモード・レスポンシブ標準対応 |
| htmx | 2.0.x | BSD 2-Clause | SSR 維持の部分更新 |
| Alpine.js | 3.x | MIT | クライアント状態（ドロップダウン・タブ等の小さな UI 状態） |
| Lucide | 0.x（最新） | ISC | SVG アイコン（Font Awesome 代替） |
| Litepicker | 2.0.x | MIT | 期間選択（daterangepicker 代替） |
| Chart.js | 4.4.x（既存 3.8.0 から更新） | MIT | グラフ描画。CDN→ローカルバンドル化 |
| Playwright Java | 1.x（最新） | Apache 2.0 | スナップショット・実機操作テスト |
| frontend-maven-plugin | 1.15.x | Apache 2.0 | Maven build 時に Node.js を取得して npm/Tailwind/esbuild を実行 |
| Node.js | 20.x LTS | MIT | ビルド時のみ取得（本番ランタイム不要） |
| esbuild | 0.20.x | MIT | JS バンドラー（htmx + Alpine + Lucide + Litepicker + Chart.js を 1 ファイル `app.js` に統合） |

## 理由

- **本番ランタイムへの新規依存ゼロ**: Node.js は frontend-maven-plugin 経由でビルド時のみ取得。生成された `app.css` / `app.js` のみが jar に同梱される
- **段階移行が可能**: 旧 layout.html を残しながら新 layout を並走させ、画面ごとに移植できる
- **既存資産の最大活用**: Presenter / view DTO / Doma DAO / Service / Interactor / Specification はすべて無変更で済み、フロントエンド層だけを刷新できる
- **Chart.js は継続**: 14 個の canvas が既に稼働しており、CDN→ローカル化のみで移行コスト最小
- **utility-first の保守性**: クラス名がそのまま意味を持つため、テーマライブラリへのロックインが発生しない
- **jQuery 排除と htmx 採用**: 古い属性駆動の宣言的 DOM 操作（jQuery）→ 新しい属性駆動の HTTP 駆動（htmx）への置換で、テンプレートの可読性を維持しつつ近代化できる

## 影響

### ポジティブ

- 静的アセットサイズが大幅縮小（AdminLTE 約 50MB → Tailwind ビルド成果物の数百 KB レベル）
- 外部 CDN 依存ゼロ化によりサプライチェーンリスクと閉域環境耐性が改善
- jQuery / DataTables / pdfmake / jszip / inputmask / moment 廃止により XSS / プロトタイプ汚染 / クライアントバイナリ生成の脆弱性経路が縮小
- ダークモード・モバイルレスポンシブが標準対応となる
- htmx + サーバページングにより、データ規模拡大時のスケール上限を解消

### ネガティブ

- ~~Jenkins ビルドノードに Node.js 配布元（既定 `https://nodejs.org/dist/`）への到達性が必要。社内 Nexus 経由の場合は frontend-maven-plugin の `nodeDownloadRoot` プロパティで指定する追加設定が必要~~
  → **本タスクではスコープアウト**（マスタープラン Gate 1 再実施 2026-04-29 で確定）。CI 側のフロントエンドビルド対応は別タスクで実施する。本タスクはローカル開発環境からの到達性のみで進める
- 初回ビルドで Node.js + node_modules を取得するため、`./mvnw package` 初回時間が +1〜2 分の見込み（後続ビルドはキャッシュで短縮）
- 移行期間中は旧 layout と新 layout が一時的に共存（Phase 2〜7 の期間中）
- Tailwind クラス名が HTML に直接書かれるため、テンプレートの一行が長くなる（ただし `@apply` で抽出可能）

### 既知のリスク

- htmx を多用するとクライアント・サーバ間の往復が増え、ページング/ソート/検索ごとに 100〜500ms 程度の遅延が体感される可能性。Phase 3 で実測し、目標 500ms 以下を満たさない場合はキャッシュ戦略を見直す
- Litepicker のメンテナンス頻度がやや低下しているため、将来的に Flatpickr 等への再移行が必要になる可能性。Phase 3 で導入時に直近 1 年のリリース状況を再確認
- Tailwind 4.x が安定版となった場合、3.x からの移行コストが発生する可能性。3.x のサポート期間と移行ガイドの公開状況を半年に 1 回確認

## 関連

- 関連判断記録: なし（本リポ初の ADR）
- 関連ドキュメント: [マスタープラン](../notes/T20260429-screen-renewal-htmx-tailwind.md)・[Phase 1 サブタスク](../notes/T20260429-screen-renewal-phase1-build-pipeline.md)（後続作成）

## 更新履歴

| 日付 | 版 | 変更者 | 内容 |
|---|---|---|---|
| 2026-04-29 | 1.0 | iori-oiso + AI（Claude Opus 4.7） | 初版作成・承認 |
| 2026-04-29 | 1.1 | iori-oiso + AI（Claude Opus 4.7） | §影響 §ネガティブ で Jenkins ノード到達性に関する記述を取消し線に変更し、本タスクではスコープアウト・別タスクで対応する旨を注記（マスタープラン Gate 1 再実施同日記録） |
| 2026-05-03 | 1.2 | iori-oiso + AI（Claude Opus 4.7） | スマホ UI 刷新タスク（[T20260502-mobile-ui-renewal.md](../notes/T20260502-mobile-ui-renewal.md)）にて **PNG ビジュアルリグレッション採用** に方針変更。初版で「フルカラー比較は不採用」としていたが、モバイル特化の視覚回帰検出が必要と判断し、Playwright Java の `assertThat(page).hasScreenshot()` ベースの baseline 比較（`src/test/resources/playwright-baselines/<screen>-<viewport>.png` 配置・差分許容閾値 `setMaxDiffPixelRatio(0.02)`）を併用する。DOM 構造アサーション（既存 `Phase8ScreenSnapshotTest`）は継続。baseline 更新運用は本 ADR ではなく [T20260502-mobile-ui-renewal.md](../notes/T20260502-mobile-ui-renewal.md) §3.3 を一次情報源とする |
