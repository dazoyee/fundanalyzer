# タスクノート: 認証機構の全廃とネットワーク境界へのアクセス制御移行

## 背景

本番アクセスは orbit（WebAuthn 認証付きリバースプロキシ）経由に一本化し、fundanalyzer 自身は
orbit が付与する署名付きトラストヘッダーを検証する Pre-Authentication（`trust-header` モード）を
採用していた。この構成では、orbit を経由せず内部 LAN から fundanalyzer のポートへ直接アクセスした
場合に、フォームログイン画面（dev 用に残置されていた実装）が露出し、ブラウザのパスワード
マネージャー機能がパスキー候補を提示するなど、想定外の認証体験が発生していた。

内部 LAN からの直接アクセスを日常的な利用経路として位置づけ、認証の責務をアプリケーション層から
ネットワーク境界（ファイアウォール・到達性の制御）へ一本化する方針とした。

## 方針

- アプリケーション層の認証を全廃する（`trust-header` / `form-login` いずれの方式も撤去）。
- アクセス制御はネットワーク境界（ファイアウォール等、LAN 内アクセスのみを許可する運用）に委ねる。
- セキュリティヘッダー（CSP/HSTS 等）と CSRF 保護はアプリケーション層で引き続き維持する。
- Actuator の管理系エンドポイント（`env` / `configprops` / `heapdump` 等）の公開範囲は
  `management.endpoints.web.exposure.include` の設定（環境ごとの絞り込み）でのみ制御する。

## 設計変更

### 削除したファイル

- `config/TrustHeaderSecurityConfig`（trust-header 認証チェーン）
- `config/TrustHeaderAuthenticationFilter`（トラストヘッダー署名検証フィルター）
- `templates/login.html`（フォームログイン画面）
- `TrustHeaderSecurityConfigIntegrationTest` / `TrustHeaderAuthenticationFilterTest`
- `MobileLoginE2ETest`（ログインフローの E2E テスト。ログイン画面自体が撤廃されたため）

### 変更したファイル

- `config/SecurityConfig`: `formLogin` / `InMemoryUserDetailsManager` を撤去し、
  全リクエスト `permitAll` + CSRF 保護 + `SecurityHeadersCustomizer` のみの単一設定に簡略化。
  プロファイルによる分岐は行わない（dev/prod 共通）。
- `application.yml` / `application-prod.yml`: `app.security.mode` / `user` / `password` /
  `trust-secret` を削除。`app.security.csp`（セキュリティヘッダー用途）のみ残置。
- `release/env.example`: `ORBIT_TRUST_HEADER_SECRET` を削除。
- Playwright 系のスクリーンショット・スナップショットテスト
  （`MobileScreenshotRegressionTest` / `Phase8ScreenSnapshotTest` / `ManualMobileScreenshotTest`）:
  ログイン処理の呼び出しを除去し、認証なしで直接画面へ遷移するよう変更。

### 変更しないもの

- `config/DevSecurityConfig`（H2 コンソール専用のフレーム許可設定。認証とは無関係）
- `config/SecurityHeadersCustomizer`（CSP/HSTS 等の設定。認証とは独立した実装）
- CSRF 保護（有効のまま維持）
- Actuator の別ポート構成と `management.endpoints.web.exposure.include` による公開範囲制御

## スコープ外

- orbit（リバースプロキシ）自体の廃止・設定変更は本タスクのスコープ外（orbit リポジトリ側で
  別途対応する）。
- 各運用環境（本番機の `release/config/application-prod.yml` 等）のバインドアドレス・
  ファイアウォール設定は、環境ごとに個別に見直す。
