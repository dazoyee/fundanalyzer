# タスクノート: dev環境のH2コンソール403 Forbidden修正

起点: 別タスク（Slack通知廃止・ダッシュボード一本化）のGate 2実機動作確認中に発見した、本タスクと無関係な既存バグ。オーナー承認により本タスクとして即時調査・修正した。

## 影響範囲タイプ 早見

- 参照層: 該当（`DevSecurityConfig`の`securityMatcher`実装変更のみ、他クラスからの参照なし）
- 状態層: 該当なし（状態遷移を伴わない）
- データ層: 該当なし（スキーマ・データ変更なし）
- インフラ影響: 該当なし（dev専用設定、本番影響なし）

## 課題

dev環境のH2コンソール（`/h2-console`）へのPOSTアクセスが403 Forbiddenになる。`DevSecurityConfig`で`/h2-console/**`に対しCSRF無効化・`permitAll`を設定しているにもかかわらず、実際には主`SecurityConfig`のチェーン（CSRF有効）が適用されていた。

## 原因

`HttpSecurity.securityMatcher(String... patterns)`はSpring MVCがクラスパスに存在する場合、デフォルトで`MvcRequestMatcher`（`HandlerMappingIntrospector`依存）を使う。H2コンソールはSpring MVCのDispatcherServletを経由しない専用Servlet（`org.h2.server.web.WebServlet`）で処理されるため、`MvcRequestMatcher`によるパス一致判定が機能せず、`h2ConsoleSecurityFilterChain`（`@Order(1)`）が一致しないまま素通りし、`@Order`未指定の主`SecurityConfig`のチェーン（CSRF有効）にフォールバックしていた。

独自の`ApplicationContext`検査テストで実証済み: 修正前は `RequestMatcher=Or [Mvc [pattern='/h2-console/**']]`、修正後は `RequestMatcher=Ant [pattern='/h2-console/**']`。

## 修正

`DevSecurityConfig.h2ConsoleSecurityFilterChain`の`securityMatcher("/h2-console/**")`を、明示的な`AntPathRequestMatcher`に変更した。

```java
.securityMatcher(new AntPathRequestMatcher("/h2-console/**"))
```

## テスト

- 新規`DevSecurityConfigTest`（`@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@ActiveProfiles("dev")`）:
  - CSRFトークンなしでH2コンソールにPOST→403にならないこと
  - H2コンソールのレスポンスに`X-Frame-Options: SAMEORIGIN`が付与されること
- MockMvcでは実サーブレット（H2コンソール）を経由できず常に404になるため、`RANDOM_PORT`の実サーバー環境で検証する方式を採用した
- 赤（修正前・クリーンビルドで確認）→緑（修正後）のサイクルを実施済み
- 実機確認: dev起動 → ブラウザ（Playwright）でH2コンソールへの実際のJDBC接続・テーブル一覧表示（`SYSTEM_EVENT`含む）まで成功することを確認
- `./mvnw clean test -DexcludedGroups=playwright`: 914件・失敗0・エラー0・BUILD SUCCESS

## スコープ外

- 本番環境（`application-prod.yml`）は元々H2コンソール自体を無効化しており対象外
- `SecurityConfig`（主チェーン）自体の変更は不要（dev専用の`DevSecurityConfig`のみで完結）
