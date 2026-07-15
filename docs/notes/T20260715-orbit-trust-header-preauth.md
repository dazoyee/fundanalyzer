# タスクノート: orbit トラストヘッダーによる Pre-Authentication への移行

関連: orbit リポジトリ `docs/notes/T024-fundanalyzerへの署名付きトラストヘッダー伝搬.md`（付与側の設計・ヘッダー仕様の正本）

## Gate 進捗 早見表

| Gate | 状態 | 承認者 | 回答日 |
|---|---|---|---|
| Gate 1: 設計の承認 | 合格 | iori-oiso | 2026-07-15 |
| Gate 2: 最終確認   | 合格 | iori-oiso | 2026-07-15 |

## 検証結果

- テスト: `./mvnw test -DexcludedGroups=playwright` 全件 PASS（trust-header 構成の統合テスト・フィルター単体テストを新規追加、既存テスト無変更）
- レビュー: コード品質・セキュリティ（security-reviewer）・機能完全性/ドキュメント整合の3系統で実施。
  CRITICAL/HIGH なし。指摘反映: テストアサーションの JUnit5 統一・検証失敗ログの CRLF サニタイズ・
  制御文字を含むユーザー名/ロールの明示拒否（区切り文字インジェクション対策）
- ローカル実機 E2E: 直アクセス（ヘッダーなし/偽署名）401・orbit ログインのみで `/fundanalyzer/v3/index` が
  200 表示（二重ログイン解消）を確認
- 本番反映（nssm への `ORBIT_TRUST_HEADER_SECRET` 投入・再起動・実機疎通）はリリース作業として別途実施

## 本番反映（v2.3.15）

- シークレット投入: orbit の nssm でサーバ上生成 → `sync-trust-secret.ps1` で fundanalyzer の env へ同期
  （手順・スクリプトの正本は iorid リポジトリ `docs/orbit/set-orbit-env.ps1` / `docs/fundanalyzer/sync-trust-secret.ps1`）
- orbit のデプロイ成功後、GitHub Actions Pipeline（version=2.3.15）でリリース。health check 成功・
  develop へ 2.3.16-SNAPSHOT バックマージ済み（タグ `v2.3.15`）
- 反映後検証: LAN からの 8890 直アクセス不達（ループバックバインド有効）、サーバ内 loopback からの
  ヘッダーなし・偽署名リクエストとも 401（Pre-Authentication 有効）

## 本番反映後の障害と修正

1. **ページ遷移がダウンロードになる**: Tailscale Serve に orbit 導入前の `/fundanalyzer` 直結ルートが
   残置され orbit を素通り→ヘッダーなし 401（空ボディ＋nosniff）をブラウザがダウンロード扱い。
   Serve から直結ルートを削除して orbit 経由に一本化（記録は iorid `ssh-log.md`）
2. **コンテキストルートが白画面（v2.3.16 で修正）**: `/` にマッピングがなく 404 → ERROR ディスパッチで
   エラーページ描画時、Pre-Authentication は認証をセッション保存せず `OncePerRequestFilter` も
   再実行されないため `/error` が未認証 401（空ボディ）となり白画面化。旧フォームログインは
   セッション保存があるため露呈しなかった。対処:
   - `WebMvcConfig` に `/` → `/v3/index` のリダイレクトを追加（プロキシのリンク先が `/fundanalyzer` のため）
   - `TrustHeaderSecurityConfig` で `DispatcherType.ERROR` を permitAll（エラーページ描画を認可対象から除外。
     直接の `/error` リクエストは REQUEST ディスパッチのため従来どおり認証必須）
   - 実起動検証: 署名付きで `/` → 302 `/v3/index`・存在しないパス → 404 エラーページ HTML（401 でない）・
     `/v3/index` → 200

v2.3.16 反映後、実端末（パスキー）で orbit ログイン → fundanalyzer が二重ログインなしで
表示されることを人間レビュアが確認し、完了条件をすべて達成した。

## 課題

本番アクセスは orbit（WebAuthn 認証付きリバースプロキシ）経由に一本化されているが、
fundanalyzer 自身もフォームログイン（メモリ内単一ユーザー）を要求するため二重ログインになる。

## 方針

orbit がプロキシ転送時に付与する署名付きトラストヘッダー（`X-Orbit-Auth-*` 4 ヘッダー、
HMAC-SHA256、仕様の正本は orbit 側 T024）を prod プロファイルで検証・信頼する
Pre-Authentication へ移行し、prod のフォームログイン・httpBasic・メモリ内ユーザーを撤廃する。
dev プロファイルは orbit なしで単独起動・Playwright テストができるよう既存フォームログインを維持する。

## 設計

### 認証フィルタ（新規）

- `config/TrustHeaderAuthenticationFilter`（`OncePerRequestFilter`）
  - 検証: 4 ヘッダーすべて存在・各ヘッダー単一値・ユーザー名/ロールに制御文字を含まない
    （改行区切り連結の署名対象への区切り文字インジェクション防止）・HMAC-SHA256 署名一致
    （定数時間比較）・タイムスタンプ鮮度 ±300 秒
  - 成立時: `PreAuthenticatedAuthenticationToken(username, "N/A", ROLE_USER)` を
    SecurityContext に設定（orbit ロールは fundanalyzer 内の認可には使わない。
    アクセス可否は orbit 側の `app_visibility` が決定済み）
  - 不成立時: 未認証のまま後続へ（`anyRequest().authenticated()` により 401）
  - シークレット: `app.security.trust-secret`（環境変数 `ORBIT_TRUST_HEADER_SECRET`。
    orbit と同一値を nssm で注入）

### SecurityConfig の分割

切り替えはプロファイル直接分岐ではなく、設定プロパティ `app.security.mode` による
`@ConditionalOnProperty` 分岐とした（テストがプロファイル一式〈本番 DB 等〉を持ち込まずに
trust-header 構成を検証できるようにするため。挙動は設計どおり prod=trust-header / dev=form-login）。

| クラス | 有効条件 | 内容 |
|---|---|---|
| `SecurityConfig`（既存を変更） | `app.security.mode=form-login`（既定） | 現行のフォームログイン構成を維持（dev・テスト用）。httpBasic は未使用のため撤去 |
| `TrustHeaderSecurityConfig`（新規） | `app.security.mode=trust-header`（application-prod.yml で指定） | formLogin / httpBasic / InMemoryUser なし。`TrustHeaderAuthenticationFilter` + 401 エントリポイント。CSRF・セキュリティヘッダー・Actuator health permitAll は現行と同一。既定ユーザー自動生成を抑止する空 `UserDetailsService` を定義 |

セキュリティヘッダー（CSP / HSTS / frameOptions / Referrer-Policy）の定義は
`SecurityHeadersCustomizer` に共通化し、両チェーンから使用する。

### バインド制限（多層防御）

`application-prod.yml` に以下を追加し、orbit（同一ホスト・ループバック経由）以外からの
直アクセスをネットワーク層でも遮断する:

- `server.address: 127.0.0.1`
- `management.server.address: 127.0.0.1`

### 設定・環境変数の変更

| 対象 | 変更 |
|---|---|
| `application-prod.yml` | `app.security.user` / `password`（`SECURITY_USER` / `SECURITY_PASSWORD`）を削除し、`app.security.trust-secret: ${ORBIT_TRUST_HEADER_SECRET}` を追加（未設定なら起動失敗） |
| `application.yml` | `app.security.user` / `password` の既定値は dev 用として存置。コメントを実態に合わせ更新 |
| `release/env.example` | `SECURITY_USER` / `SECURITY_PASSWORD` を削除し `ORBIT_TRUST_HEADER_SECRET` を追加 |
| `SECURITY.md` | 認証方式の節を「prod: orbit トラストヘッダー / dev: フォームログイン」へ更新。バインド制限を追記 |

### 変更しないもの

- `login.html` と `/login` エンドポイント（dev 用に存置。prod チェーンには組み込まれないため到達不能）
- CSRF（有効のまま。セッション・Thymeleaf フォームの挙動は不変）
- Actuator の別ポート構成（8990）と health のみ permitAll
- Playwright 系テスト（dev プロファイルのフォームログインで動作継続）

## テスト設計

1. フィルタ単体: 正しい署名で認証成立／署名不正・タイムスタンプ期限切れ・ヘッダー欠落・
   ヘッダー重複（複数値）で未認証
2. prod チェーン統合（MockMvc、trust-secret をテストプロパティで注入）:
   正しいヘッダー付きで `/v3/index` が 200／ヘッダーなしで 401／`/login` フォームが存在しない／
   Actuator health は未認証で 200／セキュリティヘッダー（CSP 等）が現行と同一
3. 既存 `SecurityConfigIntegrationTest`: dev 側チェーンの検証として無変更で PASS を維持
   （httpBasic 検証があれば当該ケースのみ削除を検討 — 既存テスト書き換えは最小限に）

## 完了条件

- prod: orbit 経由アクセスのみでログインなしに全画面へ到達できる（実機確認は orbit 側 T024 Gate 2 と合同）
- prod: 直アクセス（非ループバック）はネットワーク層で不達、署名なしリクエストは 401
- dev: `./mvnw spring-boot:run` 単独起動＋フォームログインが従来どおり機能
- `./mvnw test -DexcludedGroups=playwright` 全件 PASS
- SECURITY.md / env.example の整合

## スコープ外

- OIDC クライアント化（orbit フェーズ3 で本機構ごと置き換え）
- orbit ログアウトと fundanalyzer セッション破棄の連動
- Windows サーバーの nssm への環境変数投入作業（リリース作業として別途実施）
