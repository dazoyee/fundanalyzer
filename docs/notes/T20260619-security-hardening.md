# Task T20260619: セキュリティ強化（外部公開対応）

- 着手日: 2026-06-19
- 完了日: -
- 担当: iori-oiso / Claude Code

---

## ステップ 1: 把握・整理

### 解決すべき課題

**認証・認可ゼロの状態で外部公開すると、誰でも全画面・全操作が可能になるため、最低限の認証（Basic認証）とセキュリティヘッダーを整備して外部公開に耐えられる状態にする。**

### スコープ確定

| 区分 | 内容 |
|---|---|
| **コア** | ① Spring Security Basic 認証（全エンドポイント保護）<br>② セキュリティヘッダー追加（CSP / HSTS / X-Frame-Options / X-Content-Type-Options / Referrer-Policy）<br>③ Slack Webhook トークン環境変数化（application-dev.yml / application-prod.yml の平文 → `${ENV_VAR}`）<br>④ Cookie セキュリティ（HttpOnly / Secure / SameSite=Strict）<br>⑤ 影響範囲全テスト（MockMvc Security統合テスト + 既存Playwright対応） |
| **後回し** | 受信 HTTP レート制限（Actuator は別ポートで外部非公開のため緊急度低） |
| **対象外** | HTTPS 終端（nginx 側の作業）、ユーザー管理機能（1人専用のため不要）、Actuator 認証保護（別ポート8990は外部非公開前提） |

### 関連既存資産

| 種別 | 内容 |
|---|---|
| 設定 | `application.yml`, `application-dev.yml`, `application-prod.yml`, `release/config/application-prod.yml` |
| コンフィグ | `config/WebMvcConfig.java`（FilterRegistrationBean 登録のみ） |
| フィルター | `web/filter/AccessLogFilter.java` |
| クライアント | `client/slack/SlackClient.java`（`@Value("${app.config.slack.parameter.t/b/x}")`）|
| コントローラー | `web/controller/AnalysisController.java`, `EdinetController.java`（DevelopControllerは`@Profile("!prod")`で対象外） |
| プレゼンター | `web/presenter/` 配下 6 クラス |
| 既存テスト | `web/controller/*ControllerTest.java`（3件、MockMvcなし・Mockitoのみ）<br>`web/presenter/*PresenterTest.java`（5件、MockMvcなし）<br>`web/filter/AccessLogFilterTest.java`（1件）<br>`Phase8ScreenSnapshotTest.java`, `MobileScreenshotRegressionTest.java`（`@SpringBootTest(RANDOM_PORT)` + Playwright） |

---

## ステップ 2: プロトタイピング

**該当なし。** 本タスクは内部インフラ（認証フィルター・ヘッダー設定）の追加であり、画面上の変更・API 追加はない。プロトタイプ不要。

---

## ステップ 3: 影響設計

### 変更属性チェック

| 変更属性 | 該当 | 詳細 |
|---|---|---|
| シンボル参照の変更/追加 | **該当あり** | Slack パラメータの YML キー値（文字列リテラル）が環境変数プレースホルダーに変わる |
| 状態遷移・ライフサイクルの変更 | 該当なし | ドメイン状態遷移を変更しない |
| データ形の変更（スキーマ等） | 該当なし | DB スキーマ・既存データへの変更なし |

### 参照層分析

#### Slack トークン参照箇所（`app.config.slack.parameter.*`）

| ファイル | 行 | 参照方式 | 影響 |
|---|---|---|---|
| `client/slack/SlackClient.java` | ~35-40 | `@Value("${app.config.slack.parameter.t/b/x}")` | 影響あり：YML の値を `${SLACK_WEBHOOK_T}` 等に変更するだけ。Java 側は変更不要 |
| `application-dev.yml` | ~49-52 | 直書き `TKN2V6NQ4` | 影響あり：環境変数参照に置換 |
| `application-prod.yml` | ~54-57 | 直書き `TKN2V6NQ4` | 影響あり：環境変数参照に置換 |
| `release/config/application-prod.yml` | 要確認 | 設定上書きの可能性あり | 要確認 |

#### Spring Security 新規追加による影響

| 影響箇所 | 内容 |
|---|---|
| `@SpringBootTest(RANDOM_PORT)` テスト（Phase8 / MobileScreenshot） | Security 有効化後は 401 が返り HTML アサーションが失敗する。テストプロファイルで Security を無効化（`spring.security.enabled=false`）するか、`@WithMockUser` を追加する必要がある |
| 既存 Controller/Presenter テスト | MockMvc を使っていないため、Security 導入の直接影響なし |
| `AccessLogFilter` | Filter 実行順序が Security フィルターチェーンより後になる可能性があるが、アクセスログ記録自体の動作に影響なし |

### 状態層分析

**該当なし。** 認証状態はリクエストスコープで処理され、アプリのドメイン状態遷移には一切影響しない。

### データ層分析

**該当なし。** DB スキーマ変更なし。Basic 認証はステートレスのためセッション永続化不要。

### インフラ影響チェック

| 項目 | 判定 | 内容 |
|---|---|---|
| 新規ライブラリ追加 | **あり** | `spring-boot-starter-security` を pom.xml に追加（Spring BOM 管理下のため依存衝突リスク低） |
| タイムアウト | なし | Security フィルターはインメモリ処理のみ |
| スキーマ変更 | なし | - |
| バッチ・非同期処理 | なし | Scheduler（`@Profile("prod")`）は Security フィルターチェーン外で動作 |
| 環境変数追加 | **あり** | `SLACK_WEBHOOK_T`, `SLACK_WEBHOOK_B`, `SLACK_WEBHOOK_X`, `SECURITY_USER`, `SECURITY_PASSWORD` を `release/env` に追加（Git 管理外） |

### 品質設計の三本柱

#### テスト戦略

| テスト種別 | 採用 | 対象 | 理由 |
|---|---|---|---|
| Controller 統合テスト（MockMvc） | **採用** | SecurityConfig + 全エンドポイント認証 | `@WebMvcTest` + `@WithMockUser` で認証状態を制御。機能適合性・セキュリティ特性を担保 |
| セキュリティヘッダー検証テスト | **採用** | SecurityConfig | レスポンスヘッダーの値を MockMvc でアサーション |
| 既存 Playwright テストの対応 | **採用** | Phase8, MobileScreenshot | テストプロファイル（`@ActiveProfiles("test")`）で Security を無効化 or `@WithMockUser` 追加 |
| 負荷試験 | 不採用 | - | 1人専用のため過剰 |
| ペネトレーションテスト | 不採用 | - | 初期段階ではスコープ外（後続タスク候補） |

**カバレッジ目標:** SecurityConfig クラスは設定クラスのためテスト不要。統合テスト（MockMvc）で全エンドポイントの認証チェックを網羅する。

#### セキュリティ方針

| 観点 | 採用方針 | 不採用・後回し | 理由 |
|---|---|---|---|
| 認証 | Spring Security Basic 認証（ID/PW を環境変数で設定） | OAuth2 / OIDC / MFA | 1人専用のため最小構成で十分 |
| 認可 | 認証後は全エンドポイント許可（単一ロール） | RBAC | 1人専用のため不要 |
| セキュリティヘッダー | CSP / HSTS / X-Frame-Options / X-Content-Type-Options / Referrer-Policy | 独自ヘッダー | Spring Security の標準機能で対応 |
| 機密情報管理 | Slack トークンを環境変数化 | Vault / AWS Secrets Manager | 初期段階では環境変数で十分 |
| Cookie | HttpOnly / Secure / SameSite=Strict | - | 基本的なセキュリティ必須要件 |
| 暗号化 | nginx 側で TLS 終端（本タスク対象外） | Spring Boot 内 TLS | 認識合わせ済み |
| 受信レート制限 | 後回し | - | Actuator 別ポートで外部非公開のため緊急度低 |

#### ドキュメント計画

| ドキュメント | タイミング | 内容 |
|---|---|---|
| 本タスク md（本ファイル） | 各 Gate 完了時 | Gate 1/2/3 通過記録 |
| `release/env.example` 新規作成 | 実装中 | 必要な環境変数一覧（値なし）。`.gitignore` 済みの `release/env` の雛形として |
| `CLAUDE.md` または `README.md` | 実装完了後 | 起動前に設定が必要な環境変数の説明を追記 |

---

## Gate 1: 影響設計の承認

### レビュアー向けサマリ

- **判断してほしいこと**: 影響範囲分析（参照層）と実装方針（Basic認証・環境変数化・Playwrightテスト対応）が妥当か確認してください。
- **重要な変更ポイント**:
  1. `spring-boot-starter-security` 追加と `SecurityConfig.java` 新規作成
  2. Slack トークン 3 値（t/b/x）を環境変数化（`application-dev/prod.yml` 変更）
  3. セキュリティヘッダー 5 種（CSP / HSTS / X-Frame-Options / X-Content-Type-Options / Referrer-Policy）を SecurityConfig で設定
  4. Cookie の HttpOnly / Secure / SameSite=Strict 設定
  5. `Phase8ScreenSnapshotTest` / `MobileScreenshotRegressionTest` のテストプロファイル対応（Security 無効化）
- **確認してほしい観点**:
  1. Actuator 別ポート（8990）を認証なしのままにすることへの合意
  2. `release/config/application-prod.yml` にも Slack トークンの直書きがあるか（手元で確認後、同様に環境変数化が必要）

### 重点観点

- **参照層**: Slack トークン YML キーの変更のみ。Java 側（`SlackClient.java`）は変更不要。
- **状態層**: 該当なし（ドメイン状態遷移変更なし）
- **データ層**: 該当なし（スキーマ変更なし）
- **三本柱**: テスト戦略・セキュリティ方針・ドキュメント計画を上記に記載済み
- **依存追加**: `spring-boot-starter-security`（Spring BOM 管理下、ADR 起票不要レベルの標準ライブラリ）

### レビュアー記入欄

- 承認者: iori-oiso
- レビュー依頼日: 2026-06-19
- 回答日: 2026-06-19
- 結論: 合格
- コメント: コア①〜⑤ / スコープ外宣言ともに承認。Actuator 別ポート（8990）は認証なしのまま、HTTPS 終端・受信レート制限・ユーザー管理は対象外で進める。

---

## ステップ 4: テスト設計

### 設計上の重要な前提（実装方針）

| 項目 | 方針 | 根拠 |
|---|---|---|
| 保護範囲 | `anyRequest().authenticated()` で全リクエスト保護（静的リソース含む） | 1人専用 + Basic認証のため、認証後はブラウザが全リクエストに認証ヘッダを自動付与。最もシンプルかつ安全 |
| CSRF | **有効のまま維持** | POST フォームは `th:action` 使用 → Spring Security の `CsrfRequestDataValueProcessor` が Thymeleaf にトークン自動注入。htmx table fragment は全て GET で非対象。フォームは壊れない |
| 認証方式 | HTTP Basic（`httpBasic`）+ メモリ内ユーザー（環境変数から ID/PW） | 1人専用 |
| Actuator（別ポート8990） | 保護対象外 | Gate 1 承認済み |

### エンドポイント一覧（保護対象）

| 種別 | エンドポイント | メソッド |
|---|---|---|
| 画面（Presenter） | `/v3/index`, `/v3/index/table`, `/v3/corporate`, `/v3/valuation`, `/v3/valuation/table`, `/v3/edinet-list`, `/v3/edinet-list/table`, `/v3/edinet-list-detail` | GET |
| 業務（AnalysisController） | `/v1/document/analysis`, `/v1/scrape/id` 他 | POST |
| 業務（EdinetController） | `/v1/company`, `/v1/update/edinet/view` | GET |
| 業務（EdinetController） | `/v2/edinet-list`, `/v1/update/edinet-list`, `/v1/fix-fundamental-value`, `/v1/update/all-done/status`, `/v1/analyze/date`, `/v2/scrape/id`, `/v1/remove/document` | POST |
| 静的リソース | `/css/**`, `/js/**`, `/plugins/**`, `/favicon.ico` | GET |

### テストケース（自然言語）

#### 認証（Authentication）

| # | テストケース | 期待結果 |
|---|---|---|
| A-1 | 未認証で GET `/v3/index` にアクセスする | 401 Unauthorized |
| A-2 | 正しい認証情報（Basic）で GET `/v3/index` にアクセスする | 200 OK |
| A-3 | 誤った認証情報で GET `/v3/index` にアクセスする | 401 Unauthorized |
| A-4 | 未認証で POST `/v1/document/analysis` にアクセスする | 401 Unauthorized |
| A-5 | 未認証で GET 静的リソース `/css/app.css` にアクセスする | 401 Unauthorized |
| A-6 | 未認証で GET `/v3/valuation` にアクセスする | 401 Unauthorized（代表エンドポイントの網羅確認） |

#### CSRF

| # | テストケース | 期待結果 |
|---|---|---|
| C-1 | 認証済み + 有効な CSRF トークン付きで POST `/v1/document/analysis` | 処理実行（302 リダイレクト or 200） |
| C-2 | 認証済みだが CSRF トークンなしで POST `/v1/document/analysis` | 403 Forbidden |

#### セキュリティヘッダー（認証済みアクセス時）

| # | テストケース | 期待結果 |
|---|---|---|
| H-1 | レスポンスに `X-Content-Type-Options: nosniff` が含まれる | ヘッダ存在・値一致 |
| H-2 | レスポンスに `X-Frame-Options: DENY` が含まれる | ヘッダ存在・値一致 |
| H-3 | レスポンスに `Content-Security-Policy` が設定される | ヘッダ存在 |
| H-4 | レスポンスに `Referrer-Policy` が設定される | ヘッダ存在 |
| H-5 | HTTPS（`secure`）リクエストで `Strict-Transport-Security` が含まれる | ヘッダ存在（HSTS は HTTPS 時のみ付与される Spring Security 仕様） |

#### Cookie セキュリティ

| # | テストケース | 期待結果 |
|---|---|---|
| K-1 | セッション生成時の Cookie に `HttpOnly` 属性が付く | application.yml の `server.servlet.session.cookie.*` 設定で担保。設定値の存在をテスト or 起動時検証 |
| K-2 | Cookie に `SameSite=Strict` が付く | 同上 |

> Basic 認証はステートレスのため通常セッション Cookie を生成しないが、将来の拡張・Spring の `JSESSIONID` 生成に備えて `server.servlet.session.cookie` で HttpOnly/Secure/SameSite を設定する。検証は設定値ベース（統合テストでの Set-Cookie 検証は環境依存のため補助的）。

#### Slack トークン環境変数化

| # | テストケース | 期待結果 |
|---|---|---|
| S-1 | アプリ起動時に `${SLACK_WEBHOOK_T}` 等が解決される（環境変数 or デフォルト） | コンテキスト起動成功（既存 `@SpringBootTest` で担保） |

> トークン値の中身は機密のため、テストでは「プレースホルダーが解決されコンテキストが起動すること」を確認するに留める。

#### 既存 Playwright テストの対応（回帰防止）

| # | テストケース | 期待結果 |
|---|---|---|
| P-1 | `Phase8ScreenSnapshotTest` が Security 有効化後も主要 3 画面を取得できる | test プロファイルで Security 無効化 or 認証付きアクセスで 200 |
| P-2 | `MobileScreenshotRegressionTest` が Security 有効化後も baseline 比較できる | 同上 |

### 既存テストとの重複・補完

- 既存 `*ControllerTest.java` は Mockito 単体テストで、Security フィルターを通らない → **変更不要**（補完関係）。新規に `SecurityConfigIntegrationTest`（MockMvc）を追加する。
- 既存 Playwright テストは `@SpringBootTest(RANDOM_PORT)` で実際の Security フィルターを通るため → **テストプロファイル対応が必要**（既存テストの「変更」ではなく、test プロファイル設定の追加で対応）。

### テスト戦略の網羅性

- 認証（A-1〜A-6）: 保護の有効性
- CSRF（C-1〜C-2）: 状態変更操作の保護とフォーム互換性
- ヘッダー（H-1〜H-5）: ブラウザ側防御の有効化
- Cookie（K-1〜K-2）: セッション保護
- 回帰（P-1〜P-2）: 既存機能の非破壊

---

## Gate 2: 完了条件の確認

### 運用ルート

**インライン**（完了条件が明確で、ユーザーと同期承認可能なため）

### レビュアー向けサマリ

- **判断してほしいこと**: 完了条件が「機能 + テスト + ドキュメント」の3点セットで揃っているか、CSRF を有効維持する方針で問題ないか。
- **重要な変更ポイント**:
  1. 全リクエストを `authenticated()` で保護（静的リソース含む）
  2. CSRF は有効維持（`th:action` フォームは自動でトークン注入されるため壊れない）
  3. 既存 Playwright テストは test プロファイルで Security 無効化して回帰防止
- **確認してほしい観点**:
  1. 静的リソース（CSS/JS）も認証必須にしてよいか（Basic 認証では実用上問題なし）
  2. CSRF 有効維持の方針

### 重点観点

#### 機能要件（完了条件）

- [ ] `spring-boot-starter-security` を pom.xml に追加
- [ ] `SecurityConfig.java` で Basic 認証 + 全エンドポイント保護
- [ ] メモリ内ユーザーを環境変数（`SECURITY_USER` / `SECURITY_PASSWORD`）から構成
- [ ] セキュリティヘッダー 5 種を設定（CSP / X-Frame-Options / X-Content-Type-Options / Referrer-Policy / HSTS）
- [ ] Cookie の HttpOnly / Secure / SameSite=Strict を application.yml で設定
- [ ] Slack トークン（t/b/x）を 3 ファイルで環境変数化 + `release/env.example` 作成
- [ ] 既存 Playwright テストの test プロファイル対応

#### テスト要件

- [ ] 認証テスト A-1〜A-6（MockMvc）
- [ ] CSRF テスト C-1〜C-2
- [ ] セキュリティヘッダーテスト H-1〜H-5
- [ ] Cookie 設定 K-1〜K-2（設定値ベース）
- [ ] 既存 Playwright テストが緑のまま

#### ドキュメント要件

- [ ] 本タスク md（Gate 1/2/3 記録）
- [ ] `release/env.example` 作成
- [ ] `CLAUDE.md` に起動前必須環境変数の説明追記

#### スコープ外宣言

- HTTPS 終端（nginx 側）
- 受信 HTTP レート制限
- ユーザー管理機能（複数ユーザー・RBAC）
- Actuator 別ポート（8990）の認証保護

### レビュアー記入欄

- 承認者: iori-oiso
- レビュー依頼日: 2026-06-19
- 回答日: 2026-06-19
- 結論: 合格（インライン承認）
- コメント: 静的リソースも認証必須 / CSRF 有効維持の方針で承認。完了条件（機能・テスト・ドキュメント・スコープ外）に合意。

---

## ステップ 5: 実行サイクル（TDD 実装結果）

### 実装ファイル

| ファイル | 変更 |
|---|---|
| `pom.xml` | `spring-boot-starter-security` / `spring-security-test`(test) を追加 |
| `config/SecurityConfig.java`（新規） | Basic 認証・全リクエスト認証必須・CSRF有効・セキュリティヘッダー5種。CSP は `app.security.csp`(YML) から注入、`httpBasic` は `Customizer.withDefaults()` |
| `application.yml` | `app.security.user/password/csp`、Cookie `http-only`/`same-site=strict`（`secure` はプロファイル別） |
| `application-dev.yml` | Cookie `secure: false`（HTTP）、Slack トークン環境変数化 |
| `application-prod.yml` | Cookie `secure: true`、認証情報 env 必須化（フォールバックなし）、Actuator 公開最小化（`info.env.enabled:false`・exposure 限定・`show-details: when-authorized`）、Slack トークン環境変数化 |
| `release/config/application-prod.yml` | 認証情報 env 必須化、Slack トークン環境変数化 |
| `release/env.example`（新規） | 必須環境変数のテンプレート |
| `SecurityConfigIntegrationTest.java`（新規） | MockMvc 統合テスト13件（認証6・CSRF2・ヘッダー5、ヘッダーは値検証） |
| `Phase8/MobileScreenshot/ManualMobileScreenshotTest.java` | Playwright に Basic 認証情報を付与 |
| `SlackClientTest.java` | テスト内の実トークン断片をダミー値に置換 |

### テスト結果（TDD: RED → GREEN）

- `SecurityConfigIntegrationTest`: **13/13 GREEN**（初回 RED 4件 → SecurityConfig 実装で GREEN）
- 全非 Playwright テスト: **770/770 GREEN**（回帰なし）
- `Phase8ScreenSnapshotTest`(playwright・dev実HTTP): **8/8 GREEN**（Basic認証通過）
- `SlackClientTest`: 6/6 GREEN（環境変数化の影響なし）

---

## ステップ 6: 多軸検証

security-reviewer / code-reviewer エージェントを並列実行し、指摘を事実確認の上で切り分け。

### 検証で修正した項目（私の実装に起因する欠陥・スコープ内）

| 指摘 | 重大度 | 対応 |
|---|---|---|
| Cookie `secure:true` を共通設定に置き dev(HTTP) で CSRF 用セッション Cookie が飛ばない恐れ | CRITICAL | プロファイル別に分離（dev=false / prod=true） |
| dev 既定資格情報が jar 内 `application-prod.yml` 経由で prod に漏れ込む経路 | MEDIUM | jar 内 `application-prod.yml` にも env 必須化を追加 |
| CSP 文字列のコードハードコード（YML集約規約違反） | HIGH | `app.security.csp` として YML に移し `@Value` で注入 |
| 追加した `SECURITY_PASSWORD` が prod `/actuator/env` で露出可能 | HIGH | prod の Actuator 公開を最小化（`info.env.enabled:false`・exposure 限定） |
| テストコードに実 Slack トークン断片が残存 | MEDIUM | ダミー値に置換 |
| `httpBasic` 空ラムダ / ヘッダーテストが存在チェックのみ | MEDIUM | `Customizer.withDefaults()` 化・CSP/Referrer-Policy を値検証に強化 |

### 残存リスク（本タスクのスコープ外・別対応が必要）

| リスク | 重大度 | 推奨対応（担当: 人間/別タスク） |
|---|---|---|
| **Slack Webhook トークンが公開 git 履歴に残存**（`github.com/dazoyee/fundanalyzer` は public） | CRITICAL | **当該 Webhook を即時無効化・再発行**（コード修正では消えない。GitHub Secret Scanning 確認） |
| DB パスワード平文（`application-prod.yml` の `fundanalyzer/fundanalyzer`） | HIGH | 環境変数 `${DB_PASSWORD}` 化（別タスク） |
| EDINET API キーがリテラル文字列 `"edinet.api-key"` のまま | HIGH | `${EDINET_API_KEY:}` 化＋ env キー名整合（別タスク・要動作確認） |
| Basic 認証のブルートフォース保護なし | LOW | nginx の `limit_req` でレート制限（HTTPS 終端と同時に対応） |
| CSP の `unsafe-eval`/`unsafe-inline`（Alpine.js が要求） | MEDIUM | 中長期で Alpine CSP ビルド / nonce 化を検討 |

> 検証ノート: `MobileScreenshotRegressionTest` は baseline（6/19 14:01）がフッター版数表示（16:37）・サイドバー折りたたみ（17:25）コミット**後**に更新されておらず、本作業前から失敗する既存状態。差分画像で CSS/JS は CSP 下で正常描画を確認済（セキュリティ変更が原因ではない）。baseline 更新は当該 UI タスクの責務として別途実施。

---

## Gate 3: 最終確認

### レビュアー向けサマリ

- **判断してほしいこと**: 外部公開に向けたセキュリティ強化が利用者視点で正しく動作するか（実機で Basic 認証ログイン・画面表示・POST フォーム動作）、および残存リスク（特に CRITICAL のトークン再発行）の対応方針。
- **重要な変更ポイント**:
  1. 全エンドポイント Basic 認証必須（dev 既定 `admin` / `fundanalyzer-local-dev`、prod は env 必須）
  2. セキュリティヘッダー5種 + Cookie 保護（prod のみ Secure）
  3. Slack トークン・prod 認証情報を環境変数化、prod Actuator 公開を最小化
  4. 全テスト GREEN（新規13 + 既存770 + Phase8 8）・回帰なし
- **確認してほしい観点**:
  1. **CRITICAL: 公開 git 履歴の Slack トークン再発行**（コードでは消せない・要手動対応）
  2. dev 起動で実際に Basic 認証・画面・POST（分析実行）が動くか実機確認
  3. 残存リスク（DB パスワード・EDINET キー）を別タスク化してよいか

### 重点観点

- **差分レビュー**: 上記実装ファイル一覧
- **動作確認（実機）**: dev 起動 → `http://localhost:8889/fundanalyzer/v3/index` で Basic 認証ダイアログ → ログイン後に各画面・POST フォームが動作するか【人間確認】
- **副次影響**: 全テスト回帰なし。Playwright は認証付与で対応済
- **ドキュメント整合性**: CLAUDE.md「セキュリティ」節・本 md・`release/env.example` 更新済

### レビュアー記入欄

- 承認者:
- レビュー依頼日: 2026-06-19
- 回答日:
- 結論: 合格 / 差し戻し
- コメント:
