# Security Policy

## セキュリティ設定の概要

### 認証

- **方式**: フォームログイン（Spring Security）
- **ユーザー管理**: メモリ内単一ユーザー（`app.security.user` / `app.security.password`）
- **資格情報の設定**: 環境変数 `SECURITY_USER` / `SECURITY_PASSWORD` で注入
  - dev: `application.yml` のデフォルト値（`admin` / `fundanalyzer-local-dev`）で起動可能
  - prod: `release/env` に必須設定（未設定の場合は起動失敗）

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

アプリケーション内では TLS 終端しません。リバースプロキシ（nginx 等）での TLS 終端を前提としています。

---

## 機密情報の管理

シークレットは環境変数で管理し、コードや設定ファイルにハードコードしません。

| 環境変数 | 用途 |
|---|---|
| `SECURITY_USER` | ログインユーザー名 |
| `SECURITY_PASSWORD` | ログインパスワード |
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
