# Security Policy

## セキュリティ設定の概要

### 認証

アプリケーション層では認証を行わない。全リクエストを `permitAll` とし、アクセス制御はネットワーク境界
（ファイアウォール・バインドアドレス等、LAN 内アクセスのみを許可する運用）に委ねる。
セキュリティヘッダー（CSP/HSTS 等）と CSRF 保護のみアプリケーション層で維持する。

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
| `SLACK_WEBHOOK_T` | Slack Webhook トークン（t 部） |
| `SLACK_WEBHOOK_B` | Slack Webhook トークン（b 部） |
| `SLACK_WEBHOOK_X` | Slack Webhook トークン（x 部） |
| `edinet.api-key` | EDINET API キー |

設定テンプレートは [`release/env.example`](release/env.example) を参照してください。

`.gitignore` により `release/env` はリポジトリに含まれません。

---

## Actuator エンドポイント

Actuator は別ポート（dev: 8989 / prod: 8990）で公開し、外部に露出させない運用を前提としています。
アプリケーション層では認証を行わないため、公開エンドポイントの範囲（`management.endpoints.web.exposure.include`）で
機密情報を含むエンドポイント（`env` / `configprops` / `heapdump` 等）の露出を制御する。
dev の既定公開エンドポイントは `health,info` のみ、prod は `health,info,prometheus,circuitbreakers,ratelimiters` のみ。

---

## 脆弱性の報告

このリポジトリは個人プロジェクトです。脆弱性を発見した場合は、GitHub の Issues または
コミット履歴の連絡先までご連絡ください。
