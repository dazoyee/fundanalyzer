# タスクノート: Slack通知機能の廃止とダッシュボードへの一本化

起点: 「Slack通知機能を廃止してダッシュボードに一本化したい」という依頼。
方針決定（AskUserQuestion による認識合わせ、オーナー承認）: 全経路を廃止対象とし、情報種類ごとに移行方式を個別判断。スケジューラ異常終了エラー・財務諸表バリデーション警告のみダッシュボードに新規受け皿（新規DBテーブル＋IndexPresenter新セクション）を作り、それ以外（分析完了・画面更新完了・株価取得/削除件数・株価停滞警告）は単純削除。develop から1本のfeatureブランチで一括実施。

## Gate 進捗 早見表

| Gate | 状態 | 備考 |
|---|---|---|
| 方針選択（全廃止＋情報種類ごとの移行方式） | 承認済み | AskUserQuestion による選択（廃止スコープ／移行方式／受け皿対象／実装場所／永続化方式／進め方の6論点） |
| Gate 1（設計承認） | 承認済み | オーナー承認（AskUserQuestion経由）。保持期間管理スコープ外／リアルタイム性不要の2点も推奨案どおり承認 |
| Gate 2（最終確認） | 承認済み | オーナー承認（AskUserQuestion経由）。発見した既存バグ（dev H2コンソール403）は別タスクで対応 |

## 検証結果（ステップ5・1回目、差し戻し）

- 実装: Codex rescue に委譲。1回目実行が90分停止したためオーナー承認を得て再委任し、テストコード対応込みで完了（`./mvnw test -DexcludedGroups=playwright` 910件・失敗0・エラー0・BUILD SUCCESS）
- **プロセス逸脱の是正**: Codexが`develop`に直接コミット前の変更を加えていたことを検出。未コミットの段階だったため`feature/T20260720-slack-notice-dashboard-consolidation`ブランチを作成し移動して是正
- 検証（3並列サブエージェント、観点1+5／観点2+3／観点4）
  - 観点4（セキュリティ）: 問題なし。MINOR2件（例外情報の画面露出範囲拡大、メッセージ長未制御）は設計許容範囲内
  - 観点2+3（テスト構造品質+機能完全性）: MAJOR1件（`StockSourceStalenessSpecification`本体+専用テスト+設定キーの削除漏れ、デッドコード化）、MINOR4件
  - 観点1+5（コード品質+ドキュメント整合性）: MAJOR5件、MINOR4件
    1. ER図（`develop/document/Entity-Relationship-Diagram.drawio`）未更新
    2. `CLAUDE.md:57`のclient/パッケージ説明にSlack記載残存
    3. `README.md:13`の外部連携表にSlack Webhook記載残存
    4. `StockSourceStalenessSpecification`+`Process.SLACK`enum定数の削除漏れ（観点2+3の指摘と重複）
    5. `V0.4.8__create_system_event.sql`がプロジェクトのDDL規約（バッククォート識別子／`IF NOT EXISTS`／日本語`COMMENT`／`BIGINT UNSIGNED AUTO_INCREMENT`／`DATETIME DEFAULT CURRENT_TIME()`）から逸脱
- 判定: 不合格（MAJOR5件、重複除く）。実装エージェント（Codex）に差し戻し、MINOR項目も含めて一括修正を依頼

## 検証結果（ステップ5・2回目、差し戻し修正後）

- Codexが差し戻し10件（MAJOR5件・MINOR5件）すべてに対応（`feature/T20260720-slack-notice-dashboard-consolidation`ブランチ上）
- 独自再確認（Claude側、検証エージェントとは別に直接実施）:
  - `grep`で`SlackClient`/`NoticeUseCase`/`NoticeInteractor`/`StockSourceStalenessSpecification`/Webhookトークンの残存が`src/main`に0件であることを確認
  - `README.md`/`CLAUDE.md`/`SECURITY.md`のSlack記載が削除済みであることを確認
  - ER図（drawio、Base64圧縮形式のためデコードして確認）に`system_event`テーブルが追加されていることを確認
  - `V0.4.8__create_system_event.sql`が既存マイグレーション規約（バッククォート識別子／`IF NOT EXISTS`／日本語`COMMENT`／`BIGINT UNSIGNED AUTO_INCREMENT`／`DATETIME DEFAULT CURRENT_TIME()`）に準拠していることを確認
  - `SystemEventInteractor.buildMessage()`の1000文字トリム実装を確認
  - 全体テストを自分の環境で再実行: **912件・失敗0・エラー0・BUILD SUCCESS**（Codexが報告した52件のエラーはCodex実行環境のネットワークサンドボックス制約によるもので無関係と確認）
- 動作確認（実機・dev環境）:
  - `./mvnw spring-boot:run`でdev起動、Flyway `V0.4.8`の適用を確認
  - `/v3/index`にアクセスし、データ0件時のプレースホルダー表示を確認（スクリーンショット取得）
  - 一時的な検証用結合テスト（`@SpringBootTest(webEnvironment = RANDOM_PORT)` + Playwright、検証後に削除）でERROR/WARNING各1件を投入し、新セクションに種別バッジ（ERROR=赤、WARNING=黄）・発生元・メッセージ・発生日時が正しく表示されることをスクリーンショットで確認。既存の会社一覧テーブル等、他機能への見た目上の悪影響なし
- 判定: 合格。ただし調査中に**本タスクと無関係な既存バグ**を発見（下記「課題（スコープ外・別課題）」参照）

## Gate 2

- 承認日: 2026-07-20（オーナー承認、AskUserQuestion経由）
- 承認内容: 差し戻し修正10件対応・独自再確認（残存参照0件・DDL規約準拠・ER図更新・テスト912件全緑）・実機動作確認（空/populated両状態のスクリーンショット確認）の全体を承認。発見した既存バグ（dev H2コンソール403）は本タスクのスコープに含めず、別タスクで対応する方針も承認。

## 課題（スコープ外・別課題）

- dev環境のH2コンソール（`/h2-console`）へのPOSTアクセスが403 Forbiddenになる。`DevSecurityConfig`で`/h2-console/**`に対しCSRF無効化・`permitAll`を設定しているにもかかわらず、実際には主`SecurityConfig`のチェーンが適用されている挙動が確認された（`git diff`上、本タスクではSecurityConfig関連ファイルを変更していないため、本タスク起因ではない既存の潜在バグ）。別タスクで調査・修正する（オーナー承認済み、2026-07-20）。

## Gate 1

- 承認日: 2026-07-20（オーナー承認、AskUserQuestion経由）
- 承認内容: 本ノートの設計・テスト設計・完了条件の全体。個別確認: `system_event`テーブルの保持期間管理はスコープ外（無制限保持）、新セクションにリアルタイム性（htmx自動ポーリング等）は不要、の2点も推奨案どおり承認。

## 影響範囲タイプ 早見

- 参照層: 該当（SlackClient / NoticeUseCase 呼び出し8箇所＋関連設定・テストの削除、新規UseCase/DAOの追加）
- 状態層: 該当なし（状態遷移を伴わない。イベントは都度追記のみ）
- データ層: 該当（新規テーブル `system_event` を追加。既存データの移行は発生しない）
- インフラ影響: 該当カテゴリ = C（データストア・スキーマ移行）／G（セキュリティ、外部送信の削減）／I（デプロイ・設定変更）。他は該当なし

## 課題

Slack通知が8経路（NoticeInteractor／CompanyScheduler／AnalysisScheduler×3／StockScheduler×6／FinancialStatementSpecification／ViewEdinetInteractor／ViewCorporateInteractor／ViewValuationInteractor）に分散しており、運用の監視動線がSlackとダッシュボード（v3画面）に分かれている。ダッシュボードに一本化することで監視動線を統一する。

## 方針

- Slack通知の全経路とSlackClient本体を廃止する。
- 情報種類ごとに移行方式を分ける。
  - スケジューラ異常終了エラー（CompanyScheduler／AnalysisScheduler×3／StockScheduler×2 の計6箇所）と財務諸表バリデーション警告（FinancialStatementSpecification）→ **新規受け皿が必要**（現状ダッシュボードに同等表示がないため）
  - 分析完了通知（NoticeInteractor）→ 単純削除（EdinetPresenterの件数表示と重複）
  - 画面更新完了通知（ViewEdinetInteractor／ViewCorporateInteractor／ViewValuationInteractor、計3箇所）→ 単純削除（情報価値の低い完了ping）
  - 株価取得/削除件数通知（StockScheduler、計2箇所）→ 単純削除
  - 株価ソース停滞警告（StockScheduler、1箇所）→ 単純削除（AnalysisPresenterに既存表示あり）
- 新規受け皿は `IndexPresenter`（`/v3/index`）に新セクションとして追加し、発生イベントは新規DBテーブルに永続化する。
- develop から1本のfeatureブランチを切り、受け皿の新規実装 → Slack削除の順に一括実施し、1PRでマージする。

## 設計

### 1. 新規ドメイン: システムイベント記録

クリーンアーキテクチャの鉄則（`XxxUseCase`/`XxxInteractor`の対追加）に従い、新規ユースケースを追加する。

- `domain/usecase/SystemEventUseCase`（interface）: `record(SystemEventType type, String source, String message)` / `findRecent(int limit)`
- `domain/interactor/SystemEventInteractor implements SystemEventUseCase`
- `domain/domain/entity/transaction/SystemEventEntity`: `id` / `eventType`（`ERROR` / `WARNING`）/ `source`（発生元クラス名相当）/ `message` / `occurredAt`
- `domain/domain/dao/transaction/SystemEventDao`（Doma `@Dao`）: `insert(SystemEventEntity)` / `selectRecent(int limit)`（`occurredAt` 降順）
- `web/view/model/index/SystemEventViewModel`: 画面表示用（`occurredAt` の表示整形・種別に応じたバッジ色分け等）

### 2. Flyway マイグレーション

`src/main/resources/db/migration/V0.4.8__create_system_event.sql`（直近が `V0.4.7`）で新規作成。

```sql
CREATE TABLE system_event (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type  VARCHAR(10) NOT NULL CHECK (event_type IN ('ERROR', 'WARNING')),
    source      VARCHAR(100) NOT NULL,
    message     VARCHAR(1000) NOT NULL,
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_system_event_occurred_at ON system_event (occurred_at DESC);
```

既存データは存在しない（新規テーブルのため移行不要）。

### 3. 呼び出し元の置き換え（新規受け皿対象）

以下6箇所の `slackClient.sendMessage(...)` 呼び出しを `systemEventUseCase.record(SystemEventType.ERROR, "<スケジューラ名>", <例外メッセージ>)` に置き換える。

- `CompanyScheduler.java:60`
- `AnalysisScheduler.java:81,108,137`（3箇所）
- `StockScheduler.java:97,154`（`stockScheduler()` と `evaluateScheduler()` のエラー通知2箇所。件数通知・停滞警告は単純削除対象のため対象外）

`FinancialStatementSpecification.java:312-320` の警告通知1箇所を `systemEventUseCase.record(SystemEventType.WARNING, "FinancialStatementSpecification", <乖離内容>)` に置き換える。

### 4. 単純削除対象

- `NoticeUseCase` / `NoticeInteractor`（クラスごと削除）
- `AnalysisService.java:91` の `noticeSlack` 呼び出しを削除
- `StockScheduler.java:133,185,207-210,223`（件数通知・停滞警告、計3箇所）の `slackClient.sendMessage` 呼び出しを削除
- `ViewEdinetInteractor.java:123` / `ViewCorporateInteractor.java:458` / `ViewValuationInteractor.java:163` の完了通知呼び出しを削除
- `SlackClient` 本体（`client/slack/SlackClient.java`）を削除
- `AppConfig.java` の `restSlack`（L51-52）/ `retrySlack`（L92-93）Bean を削除
- `application.yml` の `app.config.client.rest.slack.*`（L171-176）/ `app.slack.*`（L283-289）を削除
- `application-dev.yml` の `app.config.slack.parameter.*`（L53-59）を削除
- `application-prod.yml` の `app.config.client.rest.slack.*`（L118-123）/ `app.config.slack.parameter.*`（L143-147）/ `app.slack.*`（L201-206）を削除
- `release/env.example` の `SLACK_WEBHOOK_T/B/X` 記述を削除
- `messages_ja.properties` の L1-9（表示更新完了・スケジューラエラー・分析完了・株価件数・株価停滞の各テンプレート）を削除。スケジューラエラーテンプレート（L4）は新規受け皿側で使わず、`SystemEventInteractor` 内でメッセージを組み立てる（`MessageFormat` 依存を外す）

### 5. IndexPresenter への新セクション追加

- `IndexPresenter` に `SystemEventUseCase` を注入し、`GET /v3/index` で `systemEventUseCase.findRecent(20)` を呼び `recentSystemEvents` としてモデルに追加
- `index-v2.html` に新セクション（直近のシステムイベント一覧。種別バッジ＋発生元＋メッセージ＋発生日時）を追加。既存の `errorMessage`/`successMessage` 表示パターンとは独立した恒常表示のセクションとする
- リアルタイム性は求めない（画面訪問時に最新状態を確認する運用。Slackのプッシュ通知から「見に行く」運用への変更は方針決定の折り込み済み）

### 6. 変更しないもの

- `EdinetPresenter` / `AnalysisPresenter` の既存表示（対象/処理件数、株価停滞企業）はそのまま維持
- CircuitBreaker / RateLimiter 設定（Slack以外の外部連携）
- `DevSecurityConfig` 等、Slackと無関係な設定

## テスト設計

- `SystemEventInteractorTest`（新規）: `record` で種別・発生元・メッセージを渡すと1件保存される／`findRecent(N)` で直近N件が新しい順に取得できる
- `SystemEventDaoTest`（新規、Dao統合テスト）: INSERT/SELECTがH2で動作する
- `CompanySchedulerTest` / `AnalysisSchedulerTest` / `StockSchedulerTest` / `FinancialStatementSpecificationTest`: 既存のSlack送信検証アサーションを `SystemEventUseCase.record` 呼び出し検証に置き換え（対象6+1箇所のみ。件数通知・完了通知・停滞警告の検証は削除）
- `IndexPresenterTest`: `recentSystemEvents` がモデルに含まれ、画面に表示されることを確認するテストケースを追加
- `NoticeInteractorTest` / `SlackClientTest`: クラスごと削除
- `ViewEdinetInteractorTest` / `ViewCorporateInteractorTest` / `ViewValuationInteractorTest`: Slack完了通知の検証アサーションを削除（他のアサーションは維持）
- 起動テスト: `AppConfig` の Bean 削除後、コンテキストが正常起動すること（既存の起動テストで担保）
- スモークE2E: 実装エージェントが `mvn spring-boot:run` で `/v3/index` にアクセスし、新セクションが表示されることを確認

### 状態遷移マトリクス

該当なし（システムイベントは都度追記のみで状態遷移を持たない）。

## 完了条件

### 機能
- [ ] `SystemEventUseCase`/`SystemEventInteractor`/`SystemEventEntity`/`SystemEventDao` を新規作成
- [ ] `V0.4.8__create_system_event.sql` で `system_event` テーブルを作成
- [ ] スケジューラ異常終了エラー（6箇所）と財務諸表バリデーション警告（1箇所）を `SystemEventUseCase.record` 呼び出しに置き換え
- [ ] `IndexPresenter` に直近システムイベント表示セクションを追加
- [ ] 分析完了・画面更新完了（3箇所）・株価取得/削除件数（2箇所）・株価停滞警告（1箇所）のSlack通知呼び出しを削除
- [ ] `NoticeUseCase`/`NoticeInteractor`/`SlackClient` を削除
- [ ] `AppConfig` の `restSlack`/`retrySlack` Bean を削除
- [ ] `application.yml`/`application-dev.yml`/`application-prod.yml` のSlack関連設定を削除
- [ ] `release/env.example` の `SLACK_WEBHOOK_*` 記述を削除
- [ ] `messages_ja.properties` の不要な通知メッセージ（L1-9）を削除

### テスト
- 単体テスト: `SystemEventInteractorTest`（新規）、既存7テストクラスのSlack関連アサーション置き換え・削除
- Dao統合テスト: `SystemEventDaoTest`（新規）
- 結合テスト: `IndexPresenterTest` に新セクション表示確認を追加
- 起動テスト: Bean削除後のコンテキスト起動確認
- スモークE2E: `/v3/index` の新セクション表示確認

### ドキュメント
- [ ] `CLAUDE.md`: Slack通知・`NoticeUseCase`の記載を削除し、`SystemEventUseCase`と新セクションの記載を追加
- [ ] `SECURITY.md`: `SLACK_WEBHOOK_*` の記載を削除
- [ ] `develop/document/Entity-Relationship-Diagram.drawio`: `system_event` テーブルを追加

## スコープ外

- Slack以外の新規通知手段（メール等）の追加
- `system_event` テーブルの自動パージ・保持期間管理（エラー・警告は低頻度のため当面無制限保持とし、将来の課題とする）
- 過去のSlack通知ログの移行・保存
- リアルタイム性のある画面更新（htmxによる自動ポーリング等）の実装
