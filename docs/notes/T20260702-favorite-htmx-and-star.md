# T20260702 — 銘柄詳細のお気に入り htmx トグル統一と星マーク（注目）機能の追加

## タスク要約（1 行）
銘柄詳細（/v3/corporate）のお気に入りを会社一覧（/v3/index）と同じ htmx トグルに統一し、お気に入りとは別軸の星マーク（star＝注目/ウォッチ、ON/OFF トグル）を新規追加する。

## Gate 進捗 早見表
| Gate | 状態 | 承認者 | 回答日 |
|---|---|---|---|
| Gate 1: 設計の承認 | 合格 | iori-oiso（人間レビュア） | 2026-07-02 |
| Gate 2: 最終確認   | 合格（条件付き） | iori-oiso（人間レビュア） | 2026-07-02 |

## 影響範囲タイプ 早見
- 参照層: 該当（`Company` record に `star` boolean、`CompanyEntity` に `star` を追加 → 全生成箇所・テストに波及）
- 状態層: 該当（`star` の '0'↔'1' トグル。favorite と同型の 2 状態）
- データ層: 該当（`company.star CHAR(1)` を追加。migration `V0.3.9__add_company_star.sql`）
- インフラ影響: すべて該当なし（新規テーブル・外部接続・設定・スケジューラ追加なし。既存 View 2 エンドポイント方式に準拠）

## 関連
- 着手日: 2026-07-02 / 完了日: 2026-07-02
- 担当: 計画・検証＝Claude（オーケストレーション）／実装＝Codex（rescue 委譲）／人間レビュア＝iori-oiso
- 事前計画ドキュメント: なし（本記録に集約。認識合わせは AskUserQuestion で実施）
- PR: [#283](https://github.com/dazoyee/fundanalyzer/pull/283)（draft, base: develop）
- ブランチ: `worktree-favorite-htmx-and-star`（origin/develop 派生）

---

## Gate 1: 設計の承認

### レビュアー向けサマリ
- **判断してほしいこと**: 星マークの用途（注目/ウォッチ）・形式（ON/OFF トグル）・表示範囲（index/corporate 両画面＋index に絞り込みタブ）と、お気に入りの htmx トグル統一方針を承認してほしい。
- **重要な変更ポイント**:
  - corporate のお気に入りを旧フォーム POST＋リダイレクト（`POST /v2/favorite/company`）→ index 式 htmx トグル（アイコンのみ・画面遷移なし）に統一。旧エンドポイントは削除。
  - 星マーク `star` を favorite 一式の複製で新規追加（`company.star` 列、両画面＋index 絞り込みタブ）。
  - `star` は favorite（ハート）とは別軸の ON/OFF トグル（注目/ウォッチの目印）。
- **確認してほしい観点**: favorite と star の対称性／コード桁数変換（4↔5 桁）の一貫性／星の用途がお気に入りと明確に別軸になっているか。

### 重点観点
- 影響範囲分析: 参照層（`Company` record 拡張）・状態層（トグル）・データ層（列追加）。詳細は AI 作業ログ §3.2。
- 三本柱: テスト戦略＝favorite テストに倣い star テストを追加。セキュリティ方針＝認証必須・CSRF・パラメータ化 SQL（既存基盤踏襲、新規リスクなし）。ドキュメント計画＝本タスク記録に集約。
- スコープ確定: コア＝お気に入り htmx 統一（corporate）＋星トグル（両画面＋index タブ）。対象外＝valuation 画面の星ボタン・星タブ、5 段階評価。
- 依存追加判断: なし。
- 完了条件: 機能（両画面で星／お気に入りが即時トグル動作）＋テスト（star 関連テスト緑・回帰なし）＋ドキュメント（本記録）。

### レビュアー記入欄
| 項目 | 内容 |
|---|---|
| 承認者 | iori-oiso（人間レビュア） |
| レビュー依頼日 | 2026-07-02 |
| 回答日 | 2026-07-02 |
| **結論** | **合格** |
| **コメント** | 認識合わせ（AskUserQuestion）で用途・形式・表示範囲・お気に入り統一方針の 4 点を合意し設計承認。favorite 一式の複製方針で妥当。 |

---

## Gate 2: 最終確認

### レビュアー向けサマリ
- **判断してほしいこと**: 実装が利用者視点で正しく動くか／副次影響がないか。
- **重要な変更ポイント**:
  - 銘柄詳細でお気に入りがアイコンのみの htmx トグル（画面遷移なし）で切替。
  - index/corporate 両画面に星ボタン、index に「注目」絞り込みタブを追加。
  - migration `V0.3.9` で `company.star` を追加。
- **確認してほしい観点**: 星ボタンのレイアウト（corporate デスクトップ 4 カラム）崩れ／ダークモード表示／トグルの体感。

### 重点観点
- 差分レビュー: PR [#283](https://github.com/dazoyee/fundanalyzer/pull/283)。code-reviewer による観点別レビュー実施済み（CRITICAL/HIGH のセキュリティ問題なし。指摘のテスト対称性・Javadoc・substring 安全化を反映）。
- 動作確認結果（実機・実環境）: dev（H2）で起動成功（Tomcat 8889、`Started FundanalyzerApplication`、ログイン画面 HTTP 200）。画面確認は人間レビュア実施済み（2026-07-02）。**Playwright ビジュアルベースラインの再取得はマージ前タスクとして残**（下記 T20260702-9 相当）。
- 副次影響: 旧 `POST /v2/favorite/company` 削除（参照 0 件確認）。既存 favorite 機能・テストに回帰なし（star 中核テスト 168 件緑）。
- ドキュメント整合性: migration / schema.sql / 本タスク記録が整合。

### レビュアー記入欄
| 項目 | 内容 |
|---|---|
| 承認者 | iori-oiso（人間レビュア） |
| レビュー依頼日 | 2026-07-02 |
| 回答日 | 2026-07-02 |
| **結論** | **合格（条件付き）** |
| **コメント** | 画面確認済みで承認。**条件**: マージ前に Playwright ビジュアルリグレッションのベースラインを再取得すること（UI 変更＝corporate 3→4 カラム・index 星アイコン追加のため既存スナップショットと差分が発生する）。 |

---

<details>
<summary>📁 AI 作業ログ（ステップ 1〜5 の詳細・通常時は折りたたみ）</summary>

### ステップ 1: 把握・整理
- **認識合わせ結果（4 点）**:
  1. 星の用途＝注目・ウォッチ（命名 `star`、お気に入りと別軸）
  2. 星の形式＝ON/OFF トグル（`CHAR(1)`、favorite 複製）
  3. 表示範囲＝index/corporate 両画面＋index に絞り込みタブ
  4. 作業 A＝index 式アイコンのみ htmx トグル（テキストラベル廃止）
- **現状（一次情報）**:
  - index 式 htmx トグル: [`fragments/index-table.html`](../../src/main/resources/templates/fragments/index-table.html) の `favorite-button`、[`IndexPresenter.toggleFavorite`](../../src/main/java/github/com/ioridazo/fundanalyzer/web/presenter/IndexPresenter.java)
  - corporate 旧式: [`AnalysisController`](../../src/main/java/github/com/ioridazo/fundanalyzer/web/controller/AnalysisController.java) `POST /v2/favorite/company`（フォーム＋リダイレクト）
  - favorite データ: `company.favorite CHAR(1)`、[`CompanyEntity.ofUpdateFavorite`](../../src/main/java/github/com/ioridazo/fundanalyzer/domain/domain/entity/master/CompanyEntity.java)（反転トグル）
  - 星マーク相当: 既存になし（grep で 0 件）。lucide `star` アイコン利用可
- **スコープ確定**:
  - コア: お気に入り htmx 統一（corporate）、星トグル（両画面＋index タブ）
  - 対象外: valuation 画面の星ボタン・星タブ、5 段階評価

### ステップ 2: プロトタイピング
- 該当なし（星は既存 favorite 一式の複製で実現可能と確認済み。新規プロトタイプ不要）。

### ステップ 3: 設計（影響設計＋テスト設計）
#### 3.2 影響範囲分析
- **参照層**: `Company`（record）に `star` boolean を favorite の直後に追加 → `Company.of(...)` および全 `new Company(...)` 呼び出し（テスト多数）に波及。`CompanyEntity` に `star`（String）を removed の直後に追加 → 生成・ファクトリに波及。→ テストのコンストラクタ追従修正で対応（挙動不変・Failures 0）。
- **状態層**: `star` の '0'↔'1' 反転トグル（`ofUpdateStar`）。favorite と同型の単純 2 状態。矛盾遷移・到達不能なし。
- **データ層**: `company` テーブルに `star CHAR(1) NOT NULL DEFAULT '0' CHECK (star IN ('0','1'))` を `removed` の後に追加。migration `V0.3.9__add_company_star.sql`。移行戦略＝インプレース更新（DEFAULT '0' 付きのため既存行は自動的に '0'＝未設定）。切り戻し＝列 DROP。参照整合性・大量データ移行の懸念なし。
#### 3.3 インフラ影響チェック
- A〜J すべて該当なし（新規テーブルなし=列追加のみ／外部接続・API 追加なし／設定キー追加なし／スケジューラ変更なし／権限・ネットワーク変更なし）。
#### 3.4 品質設計の三本柱（確認・更新）
- テスト戦略: favorite のユニット／Presenter テストに倣い star テストを追加。既存テストのコンストラクタ追従。
- セキュリティ方針: 全リクエスト認証必須（SecurityConfig）、CSRF 有効（htmx meta 経由で非 GET に自動付与）、Doma パラメータ化 SQL。新エンドポイント（`POST /v3/index/star`・`/v3/corporate/star`・`/v3/corporate/favorite`）も同基盤適用。新規リスクなし。
- ドキュメント計画: 本タスク記録に集約。View 2 エンドポイント方式（CLAUDE.md）に準拠。
#### 3.5 設計ドキュメント更新
- 不要（本タスク記録以外に更新すべき一次ドキュメントなし。ER 図 drawio は列 1 つの追加のため今回は非更新、必要時に別タスク）。
#### 3.6 テスト設計
- 自然言語テストケース:
  - `updateStarCompany`: 未設定→注目登録／注目→解除／存在しないコードは `FundanalyzerNotExistException`
  - `findStarCodes`: star='1' の企業コードを 5→4 桁変換した集合を返す
  - `findStarCompanies`: star='1' の企業のみ抽出
  - `ofUpdateStar`: 現在値を '0'↔'1' 反転
  - `toggleStar`（IndexPresenter/CorporatePresenter）: トグル後の状態で star fragment を返す
- 状態遷移マトリクス: star は {未設定(0), 注目(1)} の 2 状態、トグルで相互遷移（favorite と同一）。
- 完了条件: 両画面で星／お気に入りが即時トグル動作、star 関連テスト緑、既存回帰なし、本タスク記録の整備。

### ステップ 4: 実行サイクル
- 実装順序（密結合のため直列）: 作業 A（お気に入り htmx 化）→ B-1（star ドメイン層）→ B-2a（star サーバ View 層）→ B-2b（star テンプレート）→ テスト追従・追加 → レビュー指摘反映。
- 実装エージェント: Codex（rescue 委譲）。各バッチ後に `./mvnw compile -DskipTests -Dmaven.gitcommitid.skip=true` で確認。
- 既存テスト変更: `Company`/`CompanyEntity` コンストラクタへの `star` 引数追従（位置ずれなし＝既存アサーション Failures 0 で担保）。挙動を変える変更ではない。

### ステップ 5: 多軸検証（検証 5 観点）
- **観点 1 コード品質**: code-reviewer レビュー。favorite との対称性が正確、ワイルドカード import／var なし、DTO は Record で一貫。checkstyle（sun_checks.xml）は既存負債 4200 件超で CI gate 外（gate は test＋spotbugs）、本変更で新規違反追加なし。
- **観点 2 テスト構造品質**: JUnit5 標準アサーション・`@Nested`・`@DisplayName`（日本語）遵守。star 中核テスト 168 件緑。カバレッジは favorite と同水準。
- **観点 3 機能完全性**: 完了条件達成。スコープ外（valuation 星）未実装を確認。起動性＝dev 起動成功・ログイン画面 HTTP 200。
- **観点 4 セキュリティ**: CRITICAL/HIGH なし（認証必須・CSRF・パラメータ化 SQL・秘密情報ハードコードなし）。
- **観点 5 ドキュメント整合性**: migration／schema.sql／本タスク記録が整合。
- **code-reviewer 指摘の反映**: `AnalysisService`/`ViewService`/`ViewCorporateInteractor` に star テスト追加、`viewStar`/`viewFavorite` の `substring(0,4)` 安全化、`updateStarCompany` の Javadoc `@return` 追加、`CompanyInteractorTest` の FQN → import 整理。

### コミット・検証履歴
- コミット: `0e513075` `feat(view): 銘柄詳細のお気に入りをhtmxトグル化し星マーク(注目)を追加`（origin/develop 派生に rebase 済み）
- テスト: star 中核 168 件緑（`./mvnw test -Dtest='CompanyEntityTest,CompanySpecificationTest,CompanyInteractorTest,IndexPresenterTest,CorporatePresenterTest,AnalysisServiceTest,ViewServiceTest,ViewCorporateInteractorTest' -Dmaven.gitcommitid.skip=true`）。※全体テストはサンドボックスのネットワーク制限で外部接続クライアント系（Edinet/Jsoup/Selenium/Slack）58 件がエラー化するが本変更と無関係。
- 検証環境: dev（H2）、Tomcat 8889。
- PR: [#283](https://github.com/dazoyee/fundanalyzer/pull/283)（draft, base: develop）。

</details>

---

## 添付ファイル参照（該当時のみ）
- Playwright ベースライン更新時に `T20260702-attachments/gate2-*.png`（index 一覧・corporate 詳細）を配置し、Gate 2 セクションから相対リンク予定。

## 更新履歴
| 日付 | 概要 |
|---|---|
| 2026-07-02 | 起票。認識合わせ→設計（Gate 1 合格）→実装（Codex 委譲）→検証（code-reviewer・テスト 168 件緑）→PR #283 作成→Gate 2 条件付き合格を記録。残: Playwright ベースライン再取得。 |
