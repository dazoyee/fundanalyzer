# Task T20260621: Yahoo!ファイナンス株価スクレイピング停止の原因調査・対応（無効化＋停止検知）

- 着手日: 2026-06-21
- 完了日: 2026-06-22
- 担当: iori-oiso
- 状態: **完了（Gate 1 / 2 / 3 すべて合格）**
- 関連タスク: T20260621-stock-price-split-adjustment（株式分割調整。本タスクはそこから分離）

---

## ステップ 1: 把握・整理

### 解決する課題

Yahoo!ファイナンス由来の株価データ（`stock_price.source_of = '3'`）が **2025-01-31 を最後に取り込まれていない**。本番DBの実データで判明。Yahoo はスクレイピング結果に調整後終値（`adjustedPrice` 相当）を持つ唯一の有効ソース候補だったため、停止が株価品質に影響している。

### 判明している事実（本番DB実データ・2026-06-21 確認）

- 証券コード 1798（守谷商会, `company_code='17980'`）で確認:

| source_of | サイト | 件数 | 最小 | 最大 | 最古 | 最新 |
|---|---|---|---|---|---|---|
| 1 | 日経 | 170 | 1003 | 7130 | 2021-07-09 | **2026-06-08** |
| 3 | **Yahoo** | 235 | 2012 | 4010 | 2021-06-21 | **2025-01-31（停止）** |
| 4 | みんかぶ | 694 | 985 | 7180 | 2022-12-08 | **2026-06-05** |

- 日経・みんかぶは直近まで継続取得できているのに、**Yahoo だけ 2025-01-31 で止まっている**。
- `app.config.stock.yahoo-finance.enabled: true`（本番設定）であり、設定上は有効。
- enum 対応: `SourceOfStockPrice` … 1=日経, 2=kabuoji3, 3=Yahoo, 4=みんかぶ（`SourceOfStockPrice.java`）。

### 調査対象（コード）

| 対象 | 場所 | 観点 |
|---|---|---|
| Yahoo 取得 | `JsoupClient#yahooFinance` | URL・取得処理・例外 |
| Yahoo パース | `StockPriceResultBean#ofYahooFinanceResultBean` ほか | HTML構造変化でセレクタ/列数がズレていないか（`recordList.size()==7` 前提など） |
| 取得制御 | `StockInteractor#importStockPrice` | Yahoo 分岐・例外ハンドリング・握りつぶし |
| レジリエンス | `application.yml` の `resilience4j.*.yahoo-finance` | サーキットブレーカーが open のままになっていないか |
| スケジューラ | 株価取得スケジューラ・対象選定 | Yahoo だけ対象外になっていないか |

### 想定される原因仮説（調査で検証）

1. Yahoo!ファイナンスのHTML構造変更でセレクタ/列数が変わり、パース例外 or 空結果 → 例外が握りつぶされ無言で停止
2. URL/エンドポイント変更（`yahoo-finance.base-uri: https://finance.yahoo.co.jp`）
3. サーキットブレーカーが open のまま復帰せず、Yahoo 呼び出しがスキップされ続けている
4. レート制限/アクセスブロック（403 等）

### スコープ確定（2026-06-22 認識合わせで更新）

| 区分 | 内容 |
|---|---|
| **コア** | (1) 実ページで根本原因を確定（完了）。(2) **Yahoo を無効化**（`yahoo-finance.enabled: false`）。(3) **ソース別の取得停止検知**（最新取得日が一定日数以上停滞したら Slack 通知）を追加し再発防止。 |
| **対象外** | **Yahoo パースの復旧（取得再開）はしない**（P1 で分割調整は有報ベース化し Yahoo 不要。実ページは Next.js 化＋日付/URL 変更で復旧コスト高）。株式分割の調整ロジック（T20260621-stock-price-split-adjustment）。コーポレートアクション常時検知（P3）。 |

> 方針決定（2026-06-22）: P1 で Yahoo の必要性が解消したため、復旧ではなく「無効化＋停止検知」を採用。

---

## ステップ 2: プロトタイピング

### コード調査結果（2026-06-21・読み取り専用・再確認済み）

確認したファイル:
- `client/jsoup/JsoupClient.java`（`yahooFinance` / `getForHtml` / `readYahooFinanceThOrder` / `RecordFailurePredicate`）
- `client/jsoup/result/StockPriceResultBean.java`（`ofYahooFinance` ほか）
- `domain/interactor/StockInteractor.java`（`importStockPrice` の Yahoo 分岐・例外ハンドリング）
- `domain/service/AnalysisService.java`（取得対象ソースの並び・呼び出し）
- `web/scheduler/StockScheduler.java`（取得トリガ）
- `application.yml` / `application-prod.yml`（rest-client / resilience4j / stock 設定）

#### 0. 結論サマリ

Yahoo だけ停止しているのは **スケジューラやサーキットブレーカーの問題ではなく、Yahoo 分岐のパース／日付フォーマット前提が他ソースより著しく壊れやすく、かつ失敗がすべて握りつぶされて 0 件取得が「正常終了」扱いになる構造**であることがコードから強く示唆される。最有力は **(A) Yahoo!ファイナンスの HTML/URL 構造変更により取得 0 件が継続している**。コードからは「どの段階で 0 件になっているか」までは断定できず、実 HTTP レスポンスとログでの確定が必要。

#### 1. 各仮説の評価

**棄却できる仮説**

- **(仮説3) サーキットブレーカーが open のまま復帰しない** → 棄却寄り。
  - `recordFailurePredicate` は `FundanalyzerCircuitBreakerRecordException` のみを失敗計上する（`JsoupClient.java:383-389`）。これは HTTP 200 以外（`RestClientResponseException`）とタイムアウト（`ResourceAccessException`）でのみ送出される（`JsoupClient.java:249-264`）。
  - **パース 0 件・空テーブルはサーキットブレーカーの失敗としてカウントされない**（例外すら出ない）。よって「403 等で open しっぱなし」なら CB がトリップするが、`waitDurationInOpenState: 300s`（`application.yml:114`）で 5 分後に必ず HALF_OPEN に戻るため、1 年以上 open のまま固着する設計ではない。Yahoo だけ CB 設定が他と異なる点もない（nikkei/minkabu と同一: `application.yml:107-116`）。
  - したがって CB 固着は本事象（恒久停止）の説明として弱い。

- **(仮説: スケジューラが Yahoo だけ対象外)** → 明確に棄却。
  - `AnalysisService.importStock(CodeInputData)` は `KABUOJI3, MINKABU, YAHOO_FINANCE, NIKKEI` を等しく回す（`AnalysisService.java:213-218`）。`StockScheduler#insert` → `analysisService.importStock` 経由で Yahoo も毎回呼ばれる（`StockScheduler.java:122-128`）。対象企業選定（`findTargetCodeForStockScheduler`）はソース非依存。Yahoo だけ除外する分岐は存在しない。
  - 設定も `yahoo-finance.enabled: true`（`application.yml:227` / `application-prod.yml:163`）。

- **(仮説: kabuoji3 と同様に無効化されている)** → 棄却。kabuoji3 のみ `enabled: false`（`application.yml:225`）。Yahoo は true。

**最有力の仮説**

- **(仮説A) Yahoo!ファイナンスの HTML/URL 構造変更でパース結果が 0 件になり続けている**（最有力）。根拠は「Yahoo 分岐だけが構造変化に極端に弱い」こと:

  1. **行抽出が他ソースと別実装で脆い**: `yahooFinance()` は `document.select("table")`（**クラス指定なしの全 table**）→ 各行で `th` 先頭セル＋`td` を結合して 1 行を作り、`size()==7` のみ通す（`JsoupClient.java:197-208`）。kabuoji3 は `.table_wrap table`、minkabu は `.md_table_wrapper table` と**特定クラスを選択**しているのに対し、Yahoo はページ内の table 構造・列数が変わると即 0 件になる。
  2. **ヘッダーラベルが「調整後終値*」固定**: `readYahooFinanceThOrder` は `thList.indexOf("調整後終値*")`（末尾アスタリスク付き）を要求（`JsoupClient.java:364`）。ラベル文言（アスタリスク有無・全角空白等）が変わると `indexOf` が `-1` を返し、`Map.of` 自体は通っても後段の `tdList.get(-1)` で `IndexOutOfBoundsException` 系になり得る。なお `Map.of` に同一 value(-1) が複数入ると重複キー扱いにはならない（キーは文字列なので例外にはならない）。
  3. **日付パースが Yahoo だけ和暦表記前提**: `StockInteractor.java:155` は `DateTimeFormatter.ofPattern("yyyy年M月d日")` で `targetDate` をパースする。Yahoo のセルが「YYYY/MM/DD」等に変わると `DateTimeParseException` になり、`StockInteractor.java:169-178` で **INFO ログだけ出してスキップ**（=0 件登録）。minkabu は `"uuuu/MM/dd"`（`:144`）と別フォーマットで、フォーマット前提がソースごとにバラバラ。
  4. **URL パスが旧構造の可能性**: `base-uri: https://finance.yahoo.co.jp` + `/quote/{4桁コード}/history?from=&to=&timeFrame=d&page={1..13}`（`JsoupClient.java:184-185`、`application.yml:191-192`）。`from/to` は「1 年前〜本日」で `uuuuMMdd`。現行 Yahoo!ファイナンスの履歴ページが SSR で同テーブルを返さない（JS 描画化・URL 体系変更・ページング廃止等）場合、jsoup は静的 HTML しか取れないため**テーブルが空 or 別構造 → 0 件**。停止時期（2025-01-31）と Yahoo 側リニューアルが一致する可能性が高い。

  これら 1〜4 のいずれが起きても、HTTP は 200 で返る限り **例外も CB 計上もされず、ただ取得 0 件**になる。`importStockPrice(DateInputData, place)` は最後に必ず「最新の株価を正常に取り込みました」を INFO 出力する（`StockInteractor.java:99-108`）ため、**0 件でも“正常”ログが出て無言で停止**する。

- **(仮説4) 403/アクセスブロック** → 可能性は残るが単独では弱い。403 なら `FundanalyzerCircuitBreakerRecordException`（`:249-256`）→ CB 計上 → 10 回連続で open（`failureRateThreshold:100`, `minimumNumberOfCalls:10`）。ただし 300s で HALF_OPEN 復帰するので恒久停止にはならず、毎回 WARN ログが残るはず。ログに 403/CB open が出ていなければ仮説 A、出ていれば仮説 4。

#### 2. 「無言停止」を生む構造（重要）

- `getForHtml` の例外は最終的に `StockInteractor#importStockPrice(CodeInputData,...)` の catch 群で握られ、いずれも **INFO/WARN ログのみ**・Slack 通知なし（`StockInteractor.java:169-195`）。
- パース 0 件（例外すら出ないケース）は catch すら通らず、完全に無痕跡。
- `StockScheduler` が Slack に投げるのは「対象企業数」だけで、**ソース別の取得件数 0 を検知する仕組みがない**（`StockScheduler.java:130-132`）。
- 結果、Yahoo が 1 年以上 0 件でも誰も気づけない。これが「2025-01-31 で止まったまま放置」の直接原因。

#### 3. コードからは断定できず、実行時に確認すべき項目

1. **実 HTTP レスポンス**: `https://finance.yahoo.co.jp/quote/<4桁>/history?from=<1年前>&to=<本日>&timeFrame=d&page=1` を実際に GET し、(a) ステータスコード、(b) `<table>` が静的 HTML に含まれるか、(c) ヘッダー文言が「調整後終値*」のままか、(d) 日付セルの表記（「2025年1月31日」か「2025/01/31」か）を確認。
2. **本番ログ**: `Category.STOCK / Process.IMPORT` で「yahoo-financeの表形式に問題」「株価取得の対象日を認識できなかった」「200以外のHTTPステータス」「サーキットブレーカーがオープン」のいずれが出ているか。どれが出るかで仮説 A の 1〜4 を切り分け可能。
3. **サーキットブレーカー状態**: Actuator `circuitBreakers`（`yahoo-finance` インスタンス）の現在 state と直近失敗率。open 固着でないことの確認。
4. **`page` ループの妥当性**: `yahooPages=13`（`JsoupClient.java:55`）・`from/to` 1 年・`store-stock-price-for-last-days: 99999`（実質無制限）の前提が現行ページのページング仕様と合うか。

#### 4. 復旧の方向性（変更候補・本タスクでは未実施）

- **URL/セレクタ/ラベルの現行化**（仮説 A 本命）:
  - `JsoupClient#yahooFinance` の URL パス・クエリを現行 Yahoo 履歴ページに合わせる（`:184-185`）。
  - 行抽出を `select("table")` から**特定クラス/構造指定**に変更し誤抽出を防ぐ（`:197`）。
  - `readYahooFinanceThOrder` のラベル `"調整後終値*"` を現行文言へ、`indexOf == -1` 時に明示的にスクレイピング例外化（`:364`、現状は -1 を握ってしまう）。
  - `StockInteractor.java:155` の日付フォーマットを現行セル表記に合わせる（和暦固定をやめる）。
- **失敗の可視化（再発防止の本丸）**:
  - パース 0 件を「正常」と見なさない: `importStockPrice` で取得件数 0 を WARN/Slack 通知（現状 `:99-108` は常に成功ログ）。
  - ソース別の最終取得日が一定日数以上停滞したら Slack アラート（`stock_price.source_of` ごとの max(target_date) 監視）。`StockScheduler#insert` 完了時にソース別件数を集計して通知する案。
  - パース失敗を CB 失敗計上対象に含めるかは要検討（誤計上で他ソースに影響しないようインスタンス分離は既に満たす）。
- いずれも実装前に **Gate 1（影響設計の承認）** を要する。

### 実ページ確認による根本原因確定（2026-06-22・実HTTP取得）

現行 Yahoo!ファイナンス履歴ページを実取得し、原因を確定:
- `https://finance.yahoo.co.jp/quote/1798.T/history` → **HTTP 200**（価格表は静的HTMLに存在＝終値/始値/調整後終値ラベルあり、Next.js だが SSR でテーブル描画）。
- `https://finance.yahoo.co.jp/quote/1798/history`（`.T` 無し）→ **HTTP 301**。
- **確定原因（二重）**:
  1. **URL に `.T` が無い**: `JsoupClient#yahooFinance` は `code.substring(0,4)` で `/quote/1798/history` を組む（`JsoupClient.java:185-188`）→ 301 リダイレクトで取得失敗。
  2. **日付形式の変更**: 現行ページは `2026/6/19`（`yyyy/M/d`）。コードは `DateTimeFormatter.ofPattern("yyyy年M月d日")`（`StockInteractor.java:155`）でパース → `DateTimeParseException` → 行スキップ（INFO ログ）→ **0件登録の無言停止**。
- → 仮説A（HTML/URL 構造変更による無言停止）を実データで確定。CB固着・スケジューラ除外は前述のとおり棄却済み。
- **判断**: 復旧（パース現行化）は可能だが、P1 で Yahoo 不要化済み・Next.js 化で再発リスク高のため、**無効化＋停止検知**を採用（スコープ確定参照）。

---

## ステップ 3: 実装計画（無効化＋停止検知）

### 新規・変更コンポーネント
| 区分 | 対象 | 内容 |
|---|---|---|
| 変更 | `application.yml` / `application-prod.yml` の `app.config.stock.yahoo-finance.enabled` | `true` → **`false`**（Yahoo 取得を停止） |
| 新規 | `StockPriceDao` に最新取得日をソース別取得するクエリ | `source_of` ごとの `max(target_date)` |
| 新規 | `StockSpecification`（または専用 Specification）に停止検知メソッド | 有効ソースごとに最新取得日を評価し、閾値超の停滞を抽出 |
| 変更 | スケジューラ（`StockScheduler`）or 新規定時ジョブ | 日次で停止検知を実行し、停滞ソースを Slack 通知（`NoticeUseCase`） |
| 新規 | `application.yml` 設定 | 停滞判定の閾値日数（例 `app.config.stock.staleness-alert-days`）。`@Profile(prod)` でのみ通知 |

### テスト設計（ステップ4 兼）
- 停止検知ロジックのユニット: 有効ソースの最新取得日が閾値内→通知なし／閾値超→通知あり／無効ソース(Yahoo)は対象外。
- Slack 通知の呼び出し検証（Mockito）。

---

## Gate 1: 影響設計の承認

### レビュアー向けサマリ

- 判断してほしいこと: 「Yahoo 無効化＋ソース別停止検知（Slack通知）」の設計を承認するか。復旧しない判断でよいか。
- 重要な変更ポイント:
  - `yahoo-finance.enabled: false`（dev/prod）
  - `stock_price` の `source_of` 別 `max(target_date)` を見る停止検知を追加し、有効ソース（日経/みんかぶ）の無言停止を Slack 通知
  - 閾値日数は YML 集約（コードに既定値を持たせない）
- 確認してほしい観点:
  - 検知対象は「有効ソースのみ」でよいか（無効化した Yahoo は対象外）
  - 通知経路は既存 `NoticeUseCase`/Slack を流用、prod 限定でよいか

### 重点観点

- 影響範囲分析（参照層: なし / 状態層: 停止検知ロジック新設 / データ層: 参照クエリ追加のみ・スキーマ変更なし）
- 三本柱（テスト: 検知ロジックのユニット＋通知検証 / セキュリティ: 影響なし / ドキュメント: 本md）
- スコープ確定（コア: 無効化＋検知 / 対象外: Yahoo復旧・P3常時検知）

### レビュアー記入欄

- 承認者: iori-oiso
- レビュー依頼日: 2026-06-22
- 回答日: 2026-06-22
- 結論: 合格
- コメント: 実ページ確定原因（URL .T欠落＋日付形式変更）を踏まえ、復旧せず「無効化＋ソース別停止検知」で進める設計を承認。検知は有効ソースのみ・Slack通知prod限定。

---

## Gate 2: 完了条件の確認

### 運用ルート

インライン（中規模・スキーマ変更なし）。

### 重点観点（完了条件）

**機能要件**
- [x] Yahoo を無効化（`yahoo-finance.enabled: false`、dev/prod）
- [x] 有効ソース別の最新取得日が閾値（`staleness-alert-days`=7）超で停滞検知（`StockSourceStalenessSpecification#findStaleSources`）
- [x] 停滞時に Slack 通知（`StockScheduler#notifyStaleSources` → `messages_ja.properties` の staleness キー）
- [x] 無効ソース（Yahoo）は検知対象外

**テスト要件**
- [x] `StockSourceStalenessSpecificationTest` 4件（閾値内/超/null/無効除外）合格
- [x] 全体無回帰：`./mvnw test -DexcludedGroups=playwright` で 779件全合格

**ドキュメント要件**
- [x] 本md更新（スキーマ変更なしのためER図不要）

**スコープ外宣言**: Yahooパース復旧・P3常時検知

### Gate 2 エビデンス（2026-06-22）
- `selectMaxTargetDateBySource` 追加（Doma `@Select`＋SQL）、`StockSourceStalenessSpecification`（有効ソースのみ・閾値YML集約）、`StockScheduler#notifyStaleSources`（日次・prod）配線。
- テスト 779件全合格（新規4件含む）。var/reflection なし。スコープ逸脱なし。

### レビュアー記入欄

- 承認者: iori-oiso
- レビュー依頼日: 2026-06-22
- 回答日: 2026-06-22
- 結論: 合格
- コメント: 無効化＋停止検知の完了条件（テスト779件全合格・無回帰）を確認し承認。

---

## Gate 3: 最終確認

### レビュアー向けサマリ

- 判断してほしいこと: Yahoo無効化と停止検知の方針・実装が利用者/運用視点で妥当か。
- 重要な変更ポイント: Yahoo取得停止（無効化）、有効ソースの無言停止をSlack通知する仕組みの新設。
- 確認してほしい観点: 既存の株価取得（日経/みんかぶ）に副次影響がないか、通知のprod限定。

### 重点観点

- 差分レビュー: 新規 `StockSourceStalenessSpecification`・DAO1メソッド・scheduler1メソッド・config/messages。プロダクションの既存取得ロジックは不変。
- 動作確認結果: ユニットテスト4件で検知ロジックを担保。スケジューラ/Slack通知は `@Profile(prod)` のため dev では非起動 → 実通知は本番デプロイ後に観測。Yahoo無効化はdev起動で取得非実行を確認可能（UIなし）。
- 副次影響: 日経/みんかぶの取得は不変。`stock_price` スキーマ不変。
- ドキュメント整合性: 本md更新済み。

### レビュアー記入欄

- 承認者: iori-oiso
- レビュー依頼日: 2026-06-22
- 回答日: 2026-06-22
- 結論: 合格
- コメント: 差分レビュー・無回帰・方針妥当性を確認し承認。実Slack通知は本番デプロイ後に観測。P2クローズ。

---

## 更新履歴

- 2026-06-21: タスク起票。本番DB実データで Yahoo が 2025-01-31 停止を確認。バックグラウンド調査開始。
- 2026-06-22: 実ページ実取得で原因確定（URL .T欠落→301＋日付形式 yyyy/M/d 変更→無言0件）。認識合わせで「無効化＋停止検知」に方針確定。Gate1合格。
- 2026-06-22: 実装完了。Yahoo無効化（dev/prod）＋停止検知（`StockSourceStalenessSpecification`/`selectMaxTargetDateBySource`/`StockScheduler#notifyStaleSources`/messages・config）。テスト779件全合格・無回帰。Gate2合格・Gate3合格。**P2クローズ**。
