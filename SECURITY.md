# Security Policy

## セキュリティ設定の概要

### 認証

認証方式は `app.security.mode` で切り替える（Spring Security）。

- **prod: `trust-header`（orbit への認証委譲）**
  - リバースプロキシ [orbit](../orbit) が WebAuthn 認証後に付与する署名付きトラストヘッダー
    （`X-Orbit-Auth-User` / `X-Orbit-Auth-Role` / `X-Orbit-Auth-Timestamp` / `X-Orbit-Auth-Signature`）を
    `TrustHeaderAuthenticationFilter` が検証し、Pre-Authentication として受け入れる
  - 検証規約: 4 ヘッダーすべて存在・各ヘッダー単一値・ユーザー名/ロールに制御文字を含まない・
    HMAC-SHA256 署名一致（定数時間比較）・タイムスタンプ鮮度 ±300 秒。
    不成立のリクエストは 401（ヘッダー仕様の正本は orbit リポジトリ `docs/auth-sso.md`）
  - 共有シークレットは環境変数 `ORBIT_TRUST_HEADER_SECRET` で注入（orbit 側と同一値。未設定の場合は起動失敗）
  - 多層防御としてバインドをループバックに限定（`server.address` / `management.server.address`）。
    orbit を経由しない直アクセスはネットワーク層で不達
  - フォームログイン・パスワード認証の経路は持たない
- **dev（既定）: `form-login`（フォームログイン）**
  - メモリ内単一ユーザー（`app.security.user` / `app.security.password`）。既定値（`admin` / `fundanalyzer-local-dev`）で
    orbit なしの単独起動が可能。環境変数 `SECURITY_USER` / `SECURITY_PASSWORD` で上書きできる

### セキュリティヘッダー

| ヘッダー | 設定値 |
|---|---|
| `X-Frame-Options` | `DENY` |
| `X-Content-Type-Options` | `nosniff` |
| `Referrer-Policy` | `same-origin` |
| `Strict-Transport-Security` | 有効（HTTPS 前提） |
| `Content-Security-Policy` | `app.security.csp` で設定（外部オリジン遮断） |

### CSRF

有効のまま維持。Thymeleaf の `th:action` フォームに Spring Security がトークンを自動注入します。
htmx によるテーブル fragment は全て GET リクエストのため対象外。

### Cookie

| 属性 | 設定 |
|---|---|
| `HttpOnly` | 有効 |
| `Secure` | 有効 |
| `SameSite` | `Strict` |

### HTTPS

アプリケーション内では TLS 終端しません。リバースプロキシ（orbit の前段の Tailscale Serve 等）での TLS 終端を前提としています。

---

## 機密情報の管理

シークレットは環境変数で管理し、コードや設定ファイルにハードコードしません。

| 環境変数 | 用途 |
|---|---|
| `ORBIT_TRUST_HEADER_SECRET` | orbit と共有するトラストヘッダー署名シークレット（prod 必須） |
| `SECURITY_USER` | フォームログイン用ユーザー名（dev のみ） |
| `SECURITY_PASSWORD` | フォームログイン用パスワード（dev のみ） |
| `SLACK_WEBHOOK_T` | Slack Webhook トークン（t 部） |
| `SLACK_WEBHOOK_B` | Slack Webhook トークン（b 部） |
| `SLACK_WEBHOOK_X` | Slack Webhook トークン（x 部） |
| `edinet.api-key` | EDINET API キー |

設定テンプレートは [`release/env.example`](release/env.example) を参照してください。

`.gitignore` により `release/env` はリポジトリに含まれません。

---

## Actuator エンドポイント

Actuator は別ポート（dev: 8989 / prod: 8990）で公開し、外部に露出させない運用を前提としています。
`/actuator/health` は未認証でも疎通確認可能ですが、詳細情報は認証時のみ表示されます。
公開エンドポイントは `health` と `info` のみ（`env` / `configprops` は無効）。

---

## 脆弱性の報告

このリポジトリは個人プロジェクトです。脆弱性を発見した場合は、GitHub の Issues または
コミット履歴の連絡先までご連絡ください。
