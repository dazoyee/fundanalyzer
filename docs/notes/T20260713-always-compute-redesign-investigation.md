# 調査ノート: 指標・理論株価「常に計算する方式」への再設計検討

- ステータス: 調査（コード変更なし）
- 起票理由: [`T20260713-gate1-production-analysis-fixes.md`](T20260713-gate1-production-analysis-fixes.md) 論点1のフォローアップ。P1（係数変更）が既存行へ遡及しない一方、P3（指標バックフィル）は既存行へ遡及するため、同一 `analysis_result` 行に「係数は算出時点のもの・指標列は後日補完されたもの」という provenance の非対称性が生じている。これを保存＋バックフィル方式ではなく「算出時/表示時に都度計算する方式」で構造的に解消できるかを検討する。
- 関連計画: [`docs/plans/indicator-backfill.md`](../plans/indicator-backfill.md)

---

## 1. 現状アーキテクチャ

### 1.1 三段階の永続化パイプライン

現状は次の3段階すべてで値を永続化している。

1. **`analysis_result`（書類単位・1行=1提出書類）**
   - `AnalysisResult`（`src/main/java/github/com/ioridazo/fundanalyzer/domain/value/AnalysisResult.java:68-77`）が `FinanceValue`（＝`financial_statement` テーブルの値）・`Document`・`AnalysisCoefficient`（＝`industry` テーブルの係数）から `corporateValue`・`bps`・`eps`・`roe`・`roa`・`rimValue` を算出する。
   - `calculateBps/Eps/Roe/Roa`（同ファイル `221-318`）は **`coefficient` を一切参照しない**。純粋に `financial_statement` の値と `document.quarterType` だけで決まる。
   - `calculateCorporateValue`（`160-219`）と `calculateRimValue`（`119-127`、`costOfEquity` 経由）は **`coefficient`（業種別の営業利益倍率・流動負債比率・資本コスト）に依存する**。この係数は `industry` マスタの列であり、P1 のように後日更新され得る。
   - 書き込みは `AnalysisResultSpecification.insert()`（`domain/domain/specification/AnalysisResultSpecification.java:104-142`）。一意キー違反（既存行あり）の場合は **ログのみでスキップ**し、既存行は一切書き換えない（`122-141`）。これが P1 非遡及の実体。
   - P3 バックフィル用に新設された `upsert()`（同 `321-367`）は、既存行があれば `corporate_value`・`documentTypeCode` 等は現状値を維持したまま **指標列（rim/bps/eps/roe/roa）だけ**を上書きする（`333-348` のコンストラクタ引数を参照。`current.getCorporateValue()` をそのまま引き継ぐコメントが `315-317` にあり意図的な設計）。

2. **`investment_indicator`（株価取得日単位・履歴テーブル、約300万件〔`docs/plans/indicator-backfill.md:165` 記載の規模感〕）**
   - `IndicatorValue`（`domain/value/IndicatorValue.java:53-60`）が `stockPrice` と `AnalysisResultEntity`（＝その時点で「最新」だった `analysis_result` 行）から `per`・`pbr`・`grahamIndex` を算出。
   - `InvestmentIndicatorSpecification.insert()`（`domain/domain/specification/InvestmentIndicatorSpecification.java:113-167`）が呼ばれるのは `AnalyzeInteractor.analyze(Document)` 経由で「この書類がその企業の最新書類のとき」だけ（`AnalyzeInteractor.java:164-170` の `indicate()` 呼び出し）。一意キー違反時はスキップのみで **upsert 経路が存在しない**。
   - **重要な非対称性**: P3 バックフィルは `analysis_result` の bps/eps を事後的に埋めるが、`investment_indicator` の過去行はその時点の（NULL だった）bps/eps から計算済みの値のまま凍結される。`docs/plans/indicator-backfill.md:165` に明記の通り、`investment_indicator` の遡及再構築は今回のバックフィル計画の**スコープ外**。つまり P3 実施後も「`analysis_result.bps` は埋まったが、過去の `investment_indicator.per` は古い（NULL のまま or 別値の）まま」という**第二の provenance 非対称**が残る。

3. **`corporate_view`（企業単位・一覧画面用マテリアライズドキャッシュ）**
   - `ViewCorporateInteractor.parallelUpdateView()`（`domain/interactor/ViewCorporateInteractor.java:549-580`）が、企業ごとに
     - `analysisResultSpecification.findLatestAnalysisResult()`（永続化済み最新分析結果）
     - `analyzeInteractor.calculateCorporateValue()`（`AnalyzeInteractor.java:215-272`。`analysis_result` の複数行を読んで平均・標準偏差・変動係数をその場で集計 = ここは既に「都度計算」層）
     - `investmentIndicatorSpecification.findIndicatorValue()`（永続化済み最新投資指標）
     を集めて `ViewSpecification.generateCorporateView()`（`domain/domain/specification/ViewSpecification.java:274-325`）で `CorporateViewModel` を組み立て、`viewSpecification.upsert()`（`ViewSpecification.java:192-198` → `CorporateViewDao`）で `corporate_view` に書き戻す。
   - `updateView()` は **管理操作・スケジューラ起点の明示的なバッチ**（`ViewCorporateInteractor.java:452-503`）でのみ実行され、画面表示リクエストのたびには走らない。
   - 画面表示側（`viewMain()`/`viewQuart()`/`viewAll()`/`viewFavorite()`/`viewStar()`、`ViewSpecification.java:120-214`）は例外なく `viewSpecification.findAllCorporateView()`（→ `CorporateViewDao.selectAll()`）で **`corporate_view` から読むだけ**で、`analysis_result`/`investment_indicator`/`financial_statement` には一切触れない。フィルタ（`ViewCorporateInteractor.filter()`, `:505-547`）とソートはこのマテリアライズド行の集合に対してインメモリで行う。

### 1.2 まとめ表

| 値 | 係数依存 | どこで算出 | どこに永続化 | どこから参照（表示） |
|---|---|---|---|---|
| BPS/EPS/ROE/ROA | なし（純粋関数） | `AnalysisResult` コンストラクタ | `analysis_result` | `corporate_view`（生成時に埋め込み）、`ViewSpecification.viewSummaryChart` 系（`AnalysisResultViewModel::of`） |
| corporate_value | あり（係数） | 同上 | `analysis_result` | `AnalyzeInteractor.calculateCorporateValue()` が集計、`corporate_view` に反映、`BacktestInteractor.backtest()` も直接参照 |
| RIM | あり（costOfEquity） | 同上 | `analysis_result` | `corporate_view` 生成経路では未使用（`generateCorporateView` の引数に rim 無し）。画面詳細系のみ |
| PER/PBR/グレアム指数 | 間接的（bps/eps 経由） | `IndicatorValue` コンストラクタ | `investment_indicator` | `corporate_view`（最新分のみ）、詳細画面の履歴表示 |
| 3/5/10/全年平均・標準偏差・変動係数 | あり（corporate_value 経由） | `AnalysisResultSpecification` の集計メソッド（`151-230`） | **永続化なし**（都度計算） | `AnalyzeInteractor.calculateCorporateValue()`、`corporate_view` 生成時にのみ結果が書き戻される |

平均・標準偏差・変動係数は既に「都度計算 → 結果だけを corporate_view にキャッシュ」という設計になっており、今回検討する「常に計算する方式」の要素はすでに部分的に存在する。論点は「そのキャッシュ書き戻しを一切やめて完全に都度計算に倒せるか」である。

---

## 2. 「常に計算する方式」の設計案

### 2.1 核となる分離軸: 係数非依存 vs 係数依存

調査の結果、BPS/EPS/ROE/ROA と corporate_value/RIM は provenance 面で性質が異なることが分かった。

- **係数非依存（BPS/EPS/ROE/ROA）**: `financial_statement`（不変な原始データ）と `document.quarterType` だけで決まる純粋関数。**バックフィルという概念自体が本質的に不要になる**——保存しなければ「保存し忘れた過去行」が発生しようがない。ドメインサービス（例: `FinancialRatioCalculationService`、既存の `AnalysisResult.calculateBps/Eps/Roe/Roa` をそのまま抽出）に集約し、表示直前（`ViewSpecification.generateCorporateView()` や `AnalysisResultViewModel::of` 相当の箇所）で `FinanceValue` + `Document` から都度計算すれば、保存列自体を `analysis_result` から削除でき、P3 のようなバックフィルタスクは概念ごと消滅する。
- **係数依存（corporate_value/RIM）**: `industry` マスタという「後から変わり得る外部状態」に依存する。ここを「常に現在の係数で計算」に倒すと、**P1 のような係数改定のたびに全期間の理論株価が黙って書き換わる**——これは実は望ましくない可能性が高い（§3 で詳述）。

### 2.2 PER/PBR/グレアム指数（investment_indicator）

BPS/EPS が係数非依存の純粋関数である以上、PER/PBR も「`stock_price` の実測値」×「その書類時点の `financial_statement` から都度計算した bps/eps」の組み合わせで都度計算可能。`investment_indicator` に `per`/`pbr`/`grahamIndex` を保存する代わりに、`stock_price` と `document_id`（または `analysis_result_id`）の対応関係だけを保持し、表示時に `IndicatorValue` を都度構築する設計に変更すれば、**「analysis_result はバックフィルしたが investment_indicator は凍結されたまま」という第二の非対称性も構造的に解消**できる。

### 2.3 corporate_view（一覧画面）

一覧画面（`/v3/index` 等）は全社を対象にソート・フィルタ・ページングを行うため、`corporate_view` のようなマテリアライズド行の存在自体は性能上の要請（§3 参照）であり、「都度計算方式」に完全移行しても**この層だけは何らかのキャッシュが必要になる可能性が高い**。ただし、キャッシュの更新契機を「バックフィルで個別行を書き換える」のではなく、「係数非依存の値は保存せず生成のたびに都度計算 → 生成結果を丸ごと使い捨てキャッシュとして書き戻す（既存の `updateView()` と同じ形）」に統一すれば、保存済み行に新旧の計算ロジックが混在する状態そのものは発生しなくなる（キャッシュは常に「最後に `updateView()` を実行した時点の、そのときの計算ロジック・係数で全行を再生成したもの」という単一の一貫したスナップショットになる）。

### 2.4 ドメインサービスへの集約（実装イメージ、コード変更は今回含まない）

- `AnalysisResult`/`IndicatorValue` の計算メソッド群（`calculateBps` 等）はすでに `package-private` でテスト可能な純粋関数として切り出されている（`AnalysisResult.java:119,160,221,246,269,298`、`IndicatorValue.java:94,99,106,113`）。これは好都合で、「保存する/しない」の判断とロジック自体は疎結合。既存メソッドをそのまま無状態ドメインサービスに移し、Interactor 層は「保存」ではなく「表示直前に呼び出す」役割に変えるだけで移行できる可能性が高い。
- 一方 `calculateCorporateValue`/`calculateRimValue` は `coefficient` を都度解決する必要があり、`IndustrySpecification.resolveCoefficient()`（`domain/domain/specification/IndustrySpecification.java:91`）の呼び出しコストが N 件（全企業×全書類）に比例して増える点は§3で評価する。

---

## 3. トレードオフ

### 3.1 性能（バッチ集計・画面応答・N+1）

- **一覧画面（index/corporate/edinet-list）**: `viewMain()` 等は現在 `corporate_view` 1テーブルへの `selectAll()` のみで完結し（`ViewSpecification.java:122,136,155,172,199`）、フィルタ・ソートは取得済みリストに対するインメモリ処理。もし corporate_value を都度計算に倒すと、画面表示のたびに **全社×直近書類 の `financial_statement` 読み取り + 係数解決 + 平均/標準偏差集計（`analysisTargetList` は企業ごとに `analysis_result` を再スキャン、`AnalysisResultSpecification.java:286-301`）** を実行することになり、リクエストスレッドで N+1 相当の負荷が発生する。現状の `corporate_view` 材質化は明らかにこの負荷を避けるための意図的な設計であり、完全な都度計算移行はこの層には適用しない方が安全。
- **BPS/EPS/ROE/ROA の都度計算**: これらは `financial_statement` 1行の読み取り＋ BigDecimal 演算のみで、係数解決や複数行集計を伴わない。詳細画面（企業1件表示）のような低頻度・低N数の経路であれば都度計算のオーバーヘッドは軽微。
- **バックテスト（`BacktestInteractor.backtest()`）**: `@Cacheable("backtest")`（`domain/interactor/BacktestInteractor.java:66-67`）で全社×全評価履歴を1回のバッチとして計算しキャッシュする設計。ここは `analysisResultSpecification`（永続化済み `corporate_value`）を直接参照しており、都度計算に倒すとキャッシュ生成そのものが重くなる。

### 3.2 既存画面・ソート・ページングへの影響

- 一覧画面のソートキー（提出日・企業コード、`ViewCorporateInteractor` 各 `view*()` 内の `Comparator`）は `corporate_view` の列に対して行われている。都度計算方式へ全面移行し `corporate_view` を廃止すると、DBレベルのソート・絞り込みができなくなり、全件をアプリ側でロード→計算→ソートする必要が生じる。現状のページング・件数絞り込み（`filter()`）は既に「全件ロード後にインメモリでフィルタ」の設計のため、ここを悪化させないためには **corporate_view 相当のキャッシュ層は維持**が現実的。

### 3.3 キャッシュ要否

- 係数非依存の値（BPS/EPS/ROE/ROA）は保存をやめても実害が小さいため、**キャッシュ不要**で都度計算に倒せる候補。
- 係数依存の値（corporate_value/RIM）は、一覧画面用途では性能上のキャッシュが必要（§3.1）。ただし「キャッシュ＝バックフィル対象になる保存列」ではなく「使い捨てで全行再生成するスナップショット」として扱えば、非対称性の温床にはならない。

### 3.4 バックテスト機能への影響（最重要トレードオフ）

- `BacktestInteractor.backtest()` は `valuationSpecification.findAllValuationEntities()`（過去の評価判定=`ValuationEntity`、割安判定の履歴）と `analysisResultSpecification`（過去の `corporate_value`）を突き合わせて「その時点で下した判定が後にどうなったか」を検証する。これは本質的に**「判定を下した瞬間に使われていた理論株価」を凍結して参照する**ことで初めて意味を持つ（後から係数が変わって理論株価が書き換わってしまうと、過去の判定の正しさを検証するという目的そのものが壊れる）。
- P1（係数変更を既存行へ遡及しない）は、実はバグではなく **バックテストの再現性を守るために必要な性質**だったと解釈できる。corporate_value/RIM を「常に現在の係数で計算」に倒すと、係数マスタが更新されるたびにバックテストの母集団（過去の理論株価）が静かに変わり、過去の検証結果が再現不能になる。
- 結論: **corporate_value/RIM に関しては「常に計算する方式」を額面通り適用すべきではない**。むしろ必要なのは「どの係数バージョンで計算したか」を明示的に記録する（例: `analysis_result` に係数のスナップショットまたはバージョンIDを持たせる）ことで、"常に計算" ではなく "計算時の入力を凍結して記録する" 方向の設計が本来の解決策に近い。

---

## 4. 移行方針（段階案）と全面移行の妥当性

### 4.1 段階案

1. **フェーズ1（低リスク・即応可）**: 係数非依存の BPS/EPS/ROE/ROA を `analysis_result` への保存対象から外し、表示直前（`ViewSpecification.generateCorporateView()`、詳細画面の `AnalysisResultViewModel::of` 系）で `financial_statement` から都度計算する。この範囲は provenance 非対称の心配がそもそも構造的に生じない。P3 バックフィルのうち BPS/EPS/ROE/ROA 部分は都度計算化により**恒久的に不要**になる。
2. **フェーズ2（要検討）**: `investment_indicator` の PER/PBR/グレアム指数も同様に都度計算へ。ただし約300万件規模の履歴テーブルへの参照経路（詳細画面のチャート等）を洗い出し、都度計算に切り替えても応答時間が許容範囲か検証が必要。
3. **corporate_value/RIM は対象外とし、代わりに係数バージョニングを検討する**（別タスク）。既存の非対称性（P1 非遡及）は「バグ」ではなく「意図した凍結」であることを明文化し、Gate1 論点1の結論を「常に計算する方式への全面移行」ではなく「係数依存部分は現状維持＋係数変更の監査性強化」に修正する。
4. **corporate_view は材質化キャッシュとして維持**。ただし「個別行の部分バックフィル」という操作自体を今後は作らず、変更が必要な場合は必ず `updateView()`（全件再生成）で一貫したスナップショットに置き換える運用を徹底する。

### 4.2 全面移行の妥当性

**全面移行は妥当ではない**、というのが調査結果の結論。理由:

- corporate_value/RIM は係数依存であり、「常に最新の計算式・最新の係数で計算する」ことが正しさの基準ではなく、**むしろ「判定時点の値を凍結する」ことがバックテスト・過去実績表示の正しさの基準**になっている。
- 一覧画面は性能上マテリアライズドキャッシュ（corporate_view）が必要であり、これは「常に計算」とは原理的に矛盾する（キャッシュ＝過去のある時点の計算結果を保持すること）。

**部分適用が妥当**: 係数に依存しない BPS/EPS/ROE/ROA（および将来的には PER/PBR/グレアム指数）に限定して「保存しない・都度計算する」方式を適用し、バックフィルという概念自体を不要にする。係数依存の corporate_value/RIM は保存を維持しつつ、「非遡及」を意図的な仕様として明文化し、必要なら係数のバージョン管理を別途検討する。

---

## 5. 推奨と未解決の要確認点

### 推奨

1. Gate1 論点1のフォローアップ着地点を「常に計算する方式への全面移行」から「係数非依存指標（BPS/EPS/ROE/ROA、将来的に PER/PBR/グレアム指数）に限定した都度計算化」に絞り込むことを推奨する。
2. corporate_value/RIM は現状の永続化＋非遡及方針を維持し、P1 の「非遡及」は仕様として `docs/guideline` 等に明文化する（バックテストの再現性が根拠）。
3. `corporate_view` はキャッシュとして維持しつつ、今後の変更は「個別行への部分書き込み」ではなく「`updateView()` による全件再生成」に統一するルールを徹底し、キャッシュ内での新旧ロジック混在を防ぐ。

### 未解決の要確認点

1. フェーズ1（BPS/EPS/ROE/ROA 都度計算化）を実施した場合、`analysis_result` の該当4列をスキーマから削除するか、後方互換のため残すか（残す場合は「保存はするが読み取りには使わない」という別の非対称が生まれない設計にする必要がある）。
2. `investment_indicator`（約300万件規模）を都度計算に倒した場合の詳細画面チャート描画の応答時間は未計測。フェーズ2着手前に検証環境での実測が必要。
3. corporate_value/RIM の係数依存性をどう扱うか（バージョニング案）は本調査のスコープ外であり、着手する場合は別タスクとして Gate1 影響範囲分析（3属性チェック）から起票する必要がある。
4. `investment_indicator` は「新規追加」の履歴テーブルであり、既存の巨大データ（約300万件）の扱い（都度計算へ移行する場合、既存の保存列をどう扱うか）は移行コストの見積もりが必要。
