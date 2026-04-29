# Task T20260429: フロントエンド未参照資産の削除

- 着手日: 2026-04-29
- 完了日: -
- 担当: AI エージェント (Claude / Opus 4.7)
- 関連リンク: なし（事前計画ドキュメントなし。本 md が一次情報源）

---

## ステップ 1: 把握・整理

### 解決すべき課題（1 行）

`src/main/resources/static/` 配下に AdminLTE 由来の未参照プラグインと dist 資産が大量に同梱されており、ビルド成果物（jar / zip）肥大化と保守対象の不明瞭化を招いている。これを安全に削除する。

### 関連既存資産

- テンプレート: [layout.html](src/main/resources/templates/layout.html)・[index.html](src/main/resources/templates/index.html)・[edinet.html](src/main/resources/templates/edinet.html)・[edinet-detail.html](src/main/resources/templates/edinet-detail.html)・[corporate.html](src/main/resources/templates/corporate.html)・[valuation.html](src/main/resources/templates/valuation.html)・[error.html](src/main/resources/templates/error.html)
- 静的アセット: `src/main/resources/static/dist/`・`src/main/resources/static/plugins/`
- 開発専用 Controller: [DevelopController.java](src/main/java/github/com/ioridazo/fundanalyzer/web/controller/DevelopController.java)
- CLAUDE.md の記述: 「`static/dist`, `static/plugins` の資産は AdminLTE テーマの取り込みで、原則編集対象外」

### ドキュメントとコードの整合

- CLAUDE.md は「AdminLTE 資産は編集対象外」と記すが、これは内容改変を禁ずる意図。**未参照資産の物理削除は別の問題**。本タスク完了後、CLAUDE.md の同記述に「未参照資産は削除済み」と注記する余地がある（多軸検証時に判断）。
- それ以外、ドキュメントとコードの乖離はない。

### スコープ

| 区分 | 内容 |
|---|---|
| コア | (a) 未参照プラグイン 48 ディレクトリ削除 (b) `dist/` 未参照ファイル（img/alt/非min/demo・dashboard）の削除 (c) [DevelopController.java:51-55](src/main/java/github/com/ioridazo/fundanalyzer/web/controller/DevelopController.java) の壊れた `/template` エンドポイント削除 (d) CLAUDE.md の AdminLTE 記述を本削除作業を踏まえて更新 |
| 後回し | DevelopController の他 dev 専用エンドポイント（`/edinet-list`, `/company`, `/scrape/analysis/{date}`）の整理（手動デバッグ手段を奪うため別タスクに分離） |
| 対象外 | テンプレート（`*.html`）の改変・利用中プラグインのバージョン変更・AdminLTE source map（.css.map / .js.map）の削除 |

---

## ステップ 2: プロトタイピング

**該当なし**: 外部 API・画面表示・データモデルのいずれも変更しない。利用者から見える挙動は不変。

---

## Gate 1: 影響設計の承認

### レビュアー向けサマリ

- **判断してほしいこと**: 削除対象（48 プラグイン + dist 未参照ファイル + 壊れた `/template` エンドポイント）の網羅性が妥当か、CLAUDE.md の「編集対象外」記述との整合をどう扱うか。
- **重要な変更ポイント**:
  1. `src/main/resources/static/plugins/` 配下の **48 ディレクトリ（約 49MB）を物理削除**
  2. `src/main/resources/static/dist/` 配下の **未参照ファイル（img/alt/非min/demo・dashboard）を物理削除**
  3. [DevelopController.java](src/main/java/github/com/ioridazo/fundanalyzer/web/controller/DevelopController.java) の `/template` エンドポイント（`return "template"` だがテンプレートファイル不在で 500 エラーを返す死コード）を削除
  4. 機能変更・テンプレート編集・既存テスト変更は **一切行わない**
  5. ビルド成果物サイズの削減効果は約 50MB 超
- **確認してほしい観点**:
  1. 将来 AdminLTE の他プラグインを再導入する見込みがあるか（あるなら削除を見送る判断もありうる）
  2. `DevelopController` の `/edinet-list`, `/company`, `/scrape/analysis/{date}` を本タスクから対象外として後回しにする判断の妥当性

### 重点観点

#### 影響範囲分析

変更属性チェック:

- 参照層: **該当**（`DevelopController#template()` の参照、テンプレートからの静的リソース参照）
- 状態層: **該当なし**（ステートマシン・ライフサイクルを変更しないため）
- データ層: **該当なし**（DB スキーマ・既存データ・移行戦略を変更しないため）

##### 参照層分析結果

| 対象 | 参照箇所 | 影響 |
|---|---|---|
| 削除対象プラグイン 48 ディレクトリ | テンプレート 7 ファイル全文検索で参照ゼロ（`grep -rE 'plugins/[a-zA-Z0-9_-]+' src/main/resources/templates/` で確認） | 影響なし |
| 削除対象 dist ファイル（img/alt/非min/demo/dashboard） | 同上で参照ゼロ。ただし `dist/css/adminlte.min.css` 内の `sourceMappingURL=adminlte.min.css.map` のみ `.map` を参照 → `.map` は **対象外** とし残す | 影響なし |
| `DevelopController#template()` (52-55 行) | 全コードベース・テンプレート・テストから参照ゼロ。`template.html` も存在しない（`find . -name 'template.html'` で確認） | 影響なし（むしろ死コード除去） |
| `DevelopController` の他メソッド | 同 Controller 内のみ。テストや他クラスからの参照なし。@Profile("!prod") のため prod では未読込 | 後回しスコープのため本タスクでは触れない |
| 残存使用プラグイン 12 個 | テンプレートから直接 `th:src` / `th:href` 参照あり | 削除対象外 |

リフレクション・動的ロード: jsoup・Thymeleaf 共に静的アセットを名前で動的解決する仕組みは使っていない。`spring.web.resources.static-locations` のデフォルトに依存。設定ファイル（`application.yml`）からの直接参照もなし（`grep` で確認済）。

##### 状態層分析結果

該当なし（状態遷移を変更しないため）。

##### データ層分析結果

該当なし（DB スキーマ・データを変更しないため）。

#### インフラ影響チェック

| 項目 | 判定 |
|---|---|
| 大量データ処理タイムアウト | 該当なし |
| 新規外部サービス連携 | 該当なし |
| データストアスキーマ変更 | 該当なし |
| バッチ・非同期処理追加 | 該当なし |
| 依存ライブラリの新規追加 | 該当なし（むしろ削除のみ） |
| ビルド成果物サイズ | jar 内リソース約 50MB 削減。Windows サービスのデプロイ ZIP も縮小。動作には影響なし |
| デプロイ手順 | 変更なし（[release/start.bat](release/start.bat) など触らない） |

#### 三本柱

| 柱 | 確認結果 |
|---|---|
| **テスト戦略** | 既存テスト（特に [EdinetControllerTest](src/test/java/github/com/ioridazo/fundanalyzer/web/controller/EdinetControllerTest.java) など）の **未変更通過** が完了条件。新規テスト追加は不要（外部から見える動作変更なし）。`./mvnw test` で全パス確認 |
| **セキュリティ方針** | 削除によりアタックサーフェス縮小（古い JS ライブラリの公開停止）。既存の方針強化は不要。`application.yml` の Actuator/権限設定は無変更 |
| **ドキュメント計画** | CLAUDE.md の「`static/dist`, `static/plugins` は AdminLTE テーマの取り込みで、原則編集対象外」記述は **削除実施を踏まえた更新を多軸検証段階で判断**。本 md（タスク 1 md）が削除作業の正式記録となる |

#### スコープ

§ステップ 1 の 3 区分表に従う（コア / 後回し / 対象外）。

#### 依存追加判断

該当なし（依存追加なし。むしろ未参照ファイルの削除）。

### レビュアー記入欄

- 承認者: iori-oiso（プロジェクトオーナー）
- レビュー依頼日: 2026-04-29
- 回答日: 2026-04-29
- 結論: 合格
- コメント: 削除方針を承認。ただし CLAUDE.md の「`static/dist`, `static/plugins` は AdminLTE テーマの取り込みで、原則編集対象外」の記述を、本削除作業を踏まえて同タスク内で更新すること。完了条件・スコープにも反映する。

---

## Gate 2: 完了条件の確認

### 運用ルート

**省略**（小タスク基準達成）。根拠:

- [x] 影響範囲が単一機能（フロントエンド静的資産 + dev 専用死コード 1 メソッド）
- [x] テストケース数が 0 件で済む（既存テスト未変更通過のみ確認）
- [x] ドキュメント更新は CLAUDE.md の脚注程度（必要なら検証段階で判断）
- [x] 既存仕様への影響なし（外部から見える挙動不変）
- [x] セキュリティ・性能への影響なし（むしろ縮小方向）

### 完了条件

#### 機能

- [ ] 未参照プラグイン 48 ディレクトリを `src/main/resources/static/plugins/` から削除
- [ ] dist 未参照ファイル（`dist/img/*`・`dist/css/alt/*`・`dist/css/adminlte.css(.map)`・`dist/js/adminlte.js(.map)`・`dist/js/demo.js`・`dist/js/pages/dashboard*.js`）を削除
- [ ] [DevelopController.java](src/main/java/github/com/ioridazo/fundanalyzer/web/controller/DevelopController.java) の `template()` メソッドおよび関連 `@SuppressWarnings("SameReturnValue")` を残コード状況に応じて整理

#### テスト

- [ ] `./mvnw test` 全パス（既存テスト未変更）
- [ ] `./mvnw clean package` 成功

> **環境制約**: 本作業の実行環境（macOS）に Java ランタイムが未インストールのため、AI エージェントによるローカルビルド検証は実施不可。CI（[Jenkinsfile-ci-prod.groovy](pipeline/Jenkinsfile-ci-prod.groovy)）または開発者の Windows 環境での検証を Gate 3 段階で人間レビュアが確認する。

#### ドキュメント

- [ ] 本タスク 1 md（`docs/notes/T20260429-frontend-asset-cleanup.md`）に Gate 1 / Gate 3 を記録
- [ ] CLAUDE.md の AdminLTE 記述（「`static/dist`, `static/plugins` の資産は AdminLTE テーマの取り込みで、原則編集対象外」）を、本削除作業を踏まえた表現に更新

#### スコープ外（やらないこと）

- DevelopController の他 dev エンドポイント整理
- 残存プラグイン 12 個のバージョンアップ
- テンプレート HTML 自体の改変
- AdminLTE source map（.css.map / .js.map）の削除

---

## ステップ 5: 実行サイクル

### コミット計画（カテゴリ別 4 コミット）

1. `chore: 未参照プラグイン 48 ディレクトリを削除`（Conventional Commits / 3 層構造）
2. `chore: dist 配下の未参照ファイルを削除`
3. `refactor: 死コードの DevelopController#/template を削除`
4. `docs: CLAUDE.md の AdminLTE 記述を未参照資産削除後の状態に更新`

各コミット後に `./mvnw test` を実行し、緑であることを確認してから次へ進む。

### コミット履歴

| # | コミット | 概要 |
|---|---|---|
| 1 | `221b81eb` | chore: 未参照の AdminLTE プラグインを 48 ディレクトリ削除 |
| 2 | `1a6d60bc` | chore: dist 配下の未参照ファイルを削除 |
| 3 | `7a93aeab` | refactor: 死コードの DevelopController#/template を削除 |
| 4 | （本コミット） | docs: CLAUDE.md の AdminLTE 記述を未参照資産削除後の状態に更新 |

### 削減効果

| 指標 | 削除前 | 削除後 | 差分 |
|---|---|---|---|
| `src/main/resources/static/` 総容量 | 78MB | 28MB | -50MB |
| 同ファイル数 | 1962 | 231 | -1731 |

---

## ステップ 6: 多軸検証

| 観点 | 結果 | 担当 | 確認内容 |
|---|---|---|---|
| **観点 1: コード品質** | OK | AI | DevelopController.java の編集は 1 メソッド削除と冗長 `@SuppressWarnings` 除去のみ。残メソッドは構文・依存関係を保持 |
| **観点 2: テストの構造品質** | OK | AI | 既存テストの変更ゼロ。`grep -r '/template' src/test/` で参照ゼロ確認済み。新規テスト不要（外部から見える挙動の変更がないため） |
| **観点 3: 機能完全性** | OK | AI | 完了条件 (a)〜(d) すべて達成。スコープ外（DevelopController 他メソッド・テンプレート編集・残存プラグインのバージョン変更）に手をつけていない |
| **観点 4: セキュリティ** | OK | AI | 攻撃面の縮小（公開していた未使用 JS / CSS が消滅）。新規依存・新規エンドポイント・新規外部呼び出しなし。シークレット漏洩リスクなし |
| **観点 5: ドキュメント整合性** | OK | AI | CLAUDE.md の AdminLTE 記述を本タスク内容に合わせて更新（コミット 4）。タスク 1 md（本ファイル）を一次情報源として整備済み |

### ローカルビルド検証結果（AI 実施・2026-04-29）

Homebrew 版 openjdk@17 (`/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`) を `JAVA_HOME` に指定して再試行したところ、本作業環境（macOS / Apple Silicon）でも Maven Wrapper が動作した（前回「JRE 未導入」と判断したのは `JAVA_HOME` 未設定が原因）。

| コマンド | 結果 | 所要時間 |
|---|---|---|
| `./mvnw -B -ntp compile` | BUILD SUCCESS | 約 9 秒 |
| `./mvnw -B -ntp test` | **BUILD SUCCESS** ／ Tests run: 473, Failures: 0, Errors: 0, Skipped: 0 | 約 38 秒 |
| `./mvnw -B -ntp package -DskipTests` | BUILD SUCCESS／`target/fundanalyzer-2.2.14-SNAPSHOT.jar` (70MB) ＆ `*-windows.zip` (64MB) 生成 | 約 27 秒 |

> 静的解析・カバレッジ込みの完全 CI コマンド（`test surefire-report:report pmd:pmd pmd:cpd jacoco:report spotbugs:spotbugs checkstyle:check`）は CI（[Jenkinsfile-ci-prod.groovy](pipeline/Jenkinsfile-ci-prod.groovy)）側で人間レビュアが緑であることを確認する。

> **画面表示の動作確認** は本環境では実施不可（dev 起動はファイル出力先 `C:/fundanalyzer/...` を前提としており Mac では起動できない）。Gate 3 で人間レビュアが実機確認する。

---

## Gate 3: 最終確認

### レビュアー向けサマリ

- **判断してほしいこと**: 削除後にローカル / dev 環境で画面が正常表示されるか、副次影響がないか、CI のビルド・テストが緑か
- **重要な変更ポイント**:
  1. プラグイン 48 ディレクトリ削除（コミット 1）
  2. dist 未参照ファイル削除（コミット 2）
  3. DevelopController#`/template` 削除（コミット 3、機能変更なし・死コード除去）
  4. CLAUDE.md AdminLTE 記述更新（コミット 4）
  5. 静的資産 78MB→28MB / 1962→231 ファイルへ縮小
- **確認してほしい観点**:
  1. CI（`./mvnw clean package` および `test surefire-report:report pmd:pmd pmd:cpd jacoco:report spotbugs:spotbugs checkstyle:check`）が緑であるか
  2. ローカル起動で `/v2/index`, `/v2/edinet-list`, `/v2/edinet-list-detail`, `/v2/valuation`, `/v2/corporate` の各画面が崩れず表示されるか・ブラウザの開発者ツールで 404 / Network エラーが出ていないか

### 重点観点

#### 差分レビュー

`git log --oneline 4cefdfcb..HEAD` で 4 コミットを順に確認:

```
<コミット4> docs: CLAUDE.md の AdminLTE 記述を未参照資産削除後の状態に更新
7a93aeab    refactor: 死コードの DevelopController#/template を削除
1a6d60bc    chore: dist 配下の未参照ファイルを削除
221b81eb    chore: 未参照の AdminLTE プラグインを 48 ディレクトリ削除
```

#### 動作確認結果

AI 実施分（2026-04-29）:

- [x] ローカル `./mvnw test` 緑: 473 件全パス（環境: macOS / openjdk@17 / Maven Wrapper 3.6.3）
- [x] ローカル `./mvnw package -DskipTests` 成功: jar 70MB / windows.zip 64MB
- [x] テンプレートからの全アセット参照（21 件）が削除後も解決可能であることを `find` で確認

人間レビュア実施依頼分:

- [ ] CI（Jenkinsfile-ci-prod.groovy）が緑（PMD / SpotBugs / Checkstyle / JaCoCo 含む）
- [ ] 開発者の Windows / dev 環境でアプリ起動成功
- [ ] 画面表示: `/v2/index`, `/v2/edinet-list`, `/v2/edinet-list-detail`, `/v2/valuation`, `/v2/corporate` が崩れず表示
- [ ] ブラウザ開発者ツール（Network / Console）に 404 / エラーなし

#### 副次影響

- なし想定（外部 API・スケジューラ・DAO/SQL・既存テストすべて未変更）
- 副次的な恩恵: jar 内リソース約 50MB 削減、Windows サービスデプロイ ZIP の縮小

#### ドキュメント整合性

- CLAUDE.md（コミット 4 で更新済み）
- 本タスク 1 md（一次情報源として完備）

### レビュアー記入欄

- 承認者: <氏名・役割>
- レビュー依頼日: 2026-04-29
- 回答日: -
- 結論: -
- コメント: -

---

## 添付ファイル

なし（差分は `git log` / `git show` で確認）

---

## 更新履歴

- 2026-04-29: 初版作成（ステップ 1〜4・Gate 1 セクション記載）
- 2026-04-29: Gate 1 承認記録、CLAUDE.md 更新を完了条件に追加
- 2026-04-29: 削除実行（コミット 1〜3）、ステップ 5 コミット履歴・ステップ 6 多軸検証・Gate 3 セクション記載
- 2026-04-29: ローカル `./mvnw test` (473 件全パス) ・`./mvnw package` (BUILD SUCCESS) を実施し、ステップ 6 と Gate 3 動作確認結果に AI 実施分を追記
