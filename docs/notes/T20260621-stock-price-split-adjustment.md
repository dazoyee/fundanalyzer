# Task T20260621: 株価の株式分割調整（表示系＋理論株価比較の整合）

- 着手日: 2026-06-21
- 完了日: 2026-06-22
- 担当: iori-oiso
- 状態: **完了（Gate 1 / 2 / 3 すべて合格）**
- 関連タスク: T20260621-yahoo-finance-scraping-stopped（Yahoo 取得停止調査。本タスクから分離）

---

## ステップ 1: 把握・整理

### 解決する課題

株価チャート・最新株価・平均株価、および理論株価との比較（割安度・グレアム指数・バリュエーション評価）が、**株式分割／併合（コーポレートアクション）の調整を一切行っていない**ため、分割をまたぐと誤った値・不連続なグラフ・破綻した割安判定を表示する。

### 原因（本番DB実データで確定・2026-06-21）

証券コード 1798（守谷商会, `company_code='17980'`）で確認:

- 2021〜2025 で株価は約2,000→約7,000円へ上昇（正常）。
- **2026-03-27(金) 6,280円 → 03-30(月) 1,227円** と週末を挟み **約5.1倍の不連続（クリフ）**。以降~1,000円台。
- 日経（source 1）・みんかぶ（source 4）の **2独立ソースが同値**を示す → スクレイピング誤りではなく **実際の株式分割（約1:5、2026年3月末施行）**。
- 同一日付の複数ソースは値が完全一致（ratio=1.00）→ 「サイト混在による汚染」は**否定済み**。
- システムに分割・併合を調整する機構が皆無（`stock_price` は生の終値のみ保存。`closingPriceAdjustment` 列は `StockPriceResultBean` で受けるだけで未使用）。

「現在~5,000円が正しい」というユーザー認識は、分割後の生値 ~1,000円 を**旧株基準（最新有報の株式数基準）に戻した値（×5）**と一致する。

### スコープ確定（2026-06-22 ロック）

**✅ コア（今回やる）**

| # | 項目 | 補足 |
|---|---|---|
| 1 | 分割係数の**自動導出** | 比率＝有報 `TOTAL_NUMBER_OF_SHARES` の変化、施行日＝クリフ検知。**両者が同時期に整合した時のみ確定**（暫定→次有報で確定置換）。分割・併合の双方を比率で汎用に扱う |
| 2 | **読み出し経路での有報基準補正** | 最新株価・平均株価・株価チャート・割安度（差/比率）・グレアム/PER/PBR を補正後の値で算出 |
| 3 | 単一企業詳細の時系列は**都度算出** | `valuation` を詳細表示の「正」にしない（生データから算出） |
| 4 | **今後分のみ修正（fix-forward）／履歴は再構築しない** | 補正済みの読み出し・評価経路により今後の評価点・表示は正しい。`valuation`/`investment_indicator` に delete系DAOが無く evaluate は月次累積のため、既存の過去履歴行（分割企業のみ）は陳腐化したまま残す。**データ削除・上書きはしない**（リスク回避）。`corporate_view` は update-view 次回実行で補正値に更新（上書き型・Gate3で確認） |
| 5 | 補助系列の補正 | `goals_stock`（みんかぶ予想株価）も同係数で補正 |
| 6 | 基準シフトのUX対策 | チャートに分割マーカー/注記（推奨） |

**❌ 対象外（やらない／別タスク）**

| 項目 | 扱い |
|---|---|
| 派生値テーブルの**全面非永続化（都度算出アーキテクチャ）** | 別タスクに切り出し（性能・スケジューラ/Slack通知の意味づけを含む大きな設計変更のため。混在禁止） |
| `stock_price` のスキーマ変更・**生値の書き換え** | やらない（生値保持が安全の要） |
| `analysis_result`（企業価値/BPS/EPS）の変更 | やらない（既に有報基準で正しい・FK波及大） |
| Yahoo スクレイピング停止の調査・復旧 | 別タスク T20260621-yahoo-finance-scraping-stopped |

> 既存履歴の扱い（2026-06-22 決定: 案B fix-forward）: 過去の月次 valuation/指標の履歴行は再計算せず据え置く。delete+全再評価は新規インフラかつデータ削除を伴いリスクが高いため不採用。主要な誤り（株価表示・最新値・現在の割安度）は読み出し補正で解消済み。過去の分割跨ぎ点は時間経過で相対化される。

---

## ステップ 2: プロトタイピング

### 認識合わせ結果（2026-06-21）

| 論点 | 決定 |
|---|---|
| スコープ | 表示系＋理論株価比較の整合 |
| 調整基準 | **A. 有報基準**（時点依存。比較の正確性を最優先） |
| コーポレートアクション情報の取得 | **自動導出（人手ゼロ）**。比率＝有報の発行株式数変化、施行日＝クリフ（価格急変）検知。**手動マスタは作らない** |
| 既存データの扱い | `stock_price` は**生値を保持**し、調整は係数として比較・表示時に適用。**マイグレーションは valuation（割安度等）の再計算**に充てる |

### 設計判断: 調整基準と自動導出（確定）

**(1) 基準A＝有報基準**
理論株価（企業価値・BPS・EPS・RIM）はすべて **有報の発行株式数 `TOTAL_NUMBER_OF_SHARES` で割って算出**（`AnalysisResult.java:199-211, 214-236, 239-259`）、割安度は `corporateValue ÷ stockPrice`（`ValuationSpecification.java:190-191`）。理論株価が乗る株式数基準は「その有報提出時点の株式数」であり**時点依存**。比較する株価も同じ有報の株式数基準に揃える。

**(2) 調整は相対補正（生値は焼き込まない）**
日Dの株価を有報の株式数 `N_filing` 基準に直す:
```
price(D, 有報基準) = price_raw(D) × ( N_D / N_filing )
discountRate       = corporateValue_pershare(有報) ÷ price(D, 有報基準)
```
係数 `N_D/N_filing` は有報ごとに変わるため、`stock_price` の値は**生値のまま保持**し、係数を比較・表示時に算出する。

**(3) 表示は「最新有報の株式数」を基準に統一 → チャートもヘッドラインも連続・整合**
チャート・最新株価・平均株価は最新有報の株式数基準で表示。1798 で検算: 直近生値 1,003 ×5（旧株基準）= **5,015 ≒ 5,000円**（ユーザー認識と一致）、分割前 6,280 とも連続。よって「チャートは直近基準／比較は有報基準」と分ける必要はなく、**全表示を有報基準で統一**できる（新しい分割後有報が提出されたら基準が新株数へ自動シフト）。

**(4) 自動導出（手動マスタ廃止）**
- **比率**: 連続する有報の `TOTAL_NUMBER_OF_SHARES` の比から分割比率を推定（人手ゼロ・基準Aと同源）。
- **施行日**: 価格クリフ（前日比が閾値超）を検知して日次特定 → 有報間のチャート段差を解消。
- 比率（有報）と施行日（クリフ）を突き合わせて**係数タイムライン**を構築。手入力テーブルは設けない（派生キャッシュとして保持するかは実装で判断）。
- 増資・自己株消却など分割以外の株式数変動と区別するため、**クリフと有報変化が同時期に整合した場合のみ**コーポレートアクションと判定。

---

## Gate 1: 影響設計の承認

### レビュアー向けサマリ

- **判断してほしいこと**:
  1. 調整基準＝**A 有報基準**、自動導出（有報株式数変化＋クリフ）、手動マスタ廃止の設計を承認するか。
  2. 既存 `stock_price` は生値保持、マイグレーションは **valuation 再計算**に充てる方針の承認。
  3. 影響範囲（参照層/状態層/データ層）・インフラ影響・品質設計三本柱に漏れがないか。
- **重要な変更ポイント**:
  - コーポレートアクション自動導出ロジック新設（比率＝有報 `TOTAL_NUMBER_OF_SHARES` の変化、施行日＝クリフ検知）。**手入力テーブルなし**
  - 係数タイムラインから「有報基準株価」を算出する補正層を新設
  - `StockSpecification` の最新株価・平均株価算出に補正適用（`findStock`/`getAverageStockPriceOfLatestSubmitDate`）
  - `ValuationSpecification#evaluate` の株価比較を有報基準で整合（`:174,188-191`）
  - 既存 `stock_price` は生値のまま。Flyway 移行は対象企業の `valuation` 再計算に使用
- **確認してほしい観点**:
  - 有報の株式数変化＋クリフで分割を「増資等と誤判定しない」判定条件が妥当か
  - 施行日〜次有報提出までの期間の扱い（係数の暫定適用）に漏れがないか

### 変更属性チェック（最初に必ず実施）

| 変更属性 | 判定 | 理由 |
|---|---|---|
| シンボル参照の変更/追加 | **該当** | 自動導出・補正層の新設、`StockSpecification`/`ValuationSpecification` のメソッド変更（株価取得に補正適用） |
| 状態遷移・ライフサイクルの変更/追加 | **該当なし** | 状態機械・ライフサイクルは扱わない |
| データの形（スキーマ・既存値）の変更 | **該当** | 係数導出のための派生データ（任意でキャッシュ表）＋ 既存 `valuation` の再計算（移行戦略が必要。`stock_price` 生値は不変） |

### 影響範囲分析（参照層 / 状態層 / データ層）

> 行番号は 2026-06-21 時点。★=本人確認済み、（要確認）=未検証で実装前に再確認。

**データ層**
| 対象 | 場所 | 影響 |
|---|---|---|
| `stock_price` スキーマ | `db/migration/V0.1.0__init_create.sql` ほか（最新 `V0.3.8`）★ | **変更なし（生値を保持）**。新規 Flyway は `V0.4.0` 以降 |
| StockPriceEntity / Dao | `entity/transaction/StockPriceEntity.java`★ / `dao/transaction/StockPriceDao.java`★ | 生値は維持。補正は読み出し時に適用 |
| 係数導出の入力 | `financial_statement`（`TOTAL_NUMBER_OF_SHARES`）＋ `stock_price`（クリフ検知） | 既存データから自動導出。手入力テーブルなし |
| 係数キャッシュ（任意） | （新設可）派生テーブル | 導出済みの分割係数タイムラインを保持する場合のみ。`industry`（`V0.3.7`）の構成を参考 |
| valuation 再計算 | （新設）`V0.4.x` Flyway ＋ 再評価バッチ | 分割が検知された企業の `valuation`（割安度・差・比率・グレアム）を有報基準で再計算 |

**状態層**
| 対象 | 場所 | 影響 |
|---|---|---|
| 最新株価 | `StockSpecification#findStock` `:118-148`★ / `findLatestStock` `:96-100`★ | 調整適用 |
| 平均株価 | `StockSpecification#getAverageStockPriceOfLatestSubmitDate` `:359-377`★ | 期間内株価に調整適用後に平均 |
| 理論株価 | `AnalysisResult#calculateCorporateValue/Bps/Eps` `:199-211, 214-236, 239-259`★ | 有報株式数基準＝時点依存。基準選択に応じ比較側の整合が必要 |
| 割安度・提出日比較 | `ValuationSpecification#evaluate` `:174,188-191`★ | `stockPrice` と `stockPriceOfSubmitDate`・`corporateValue` の基準を揃える |
| グレアム指数 | `IndicatorValue`（PER=株価/EPS, PBR=株価/BPS）（要確認） | 株価とEPS/BPSの基準不整合で急変。整合が必要 |

**参照層**
| 対象 | 場所 | 影響 |
|---|---|---|
| 株価チャート | `web/presenter/CorporatePresenter#setStockPriceView` `:236-285`★ | 全レコードをそのままプロット。調整後系列に差し替え |
| 株価ViewModel | `view/model/corporate/detail/StockPriceViewModel`★ | 調整後値を渡す |
| 割安度表示・ソート | `CorporateViewModel` / `IndexPresenter` / `ValuationPresenter`（要確認） | 調整後の整合値を表示・ソート |

### インフラ影響チェック

- A. 処理時間: 調整適用は軽量な算術。移行は一回限りのバッチ。影響軽微
- B. 外部サービス連携: 新規呼び出しなし（マスタは手入力）
- C. データストア・スキーマ: **該当**（新規マスタ＋既存値再計算）。Flyway 移行で対応
- D. バッチ・非同期処理: 株価取得スケジューラの保存値の扱いに影響（生値保存は維持予定）
- E. リソース: 影響なし
- F. 可用性: 移行はダウンタイム最小（追加テーブル＋UPDATE）。要バックアップ
- G. セキュリティ: 機密情報なし。手入力値の検証（比率>0・施行日妥当性）必要
- H. 監視: 分割未登録銘柄でのクリフ検知アラート（任意・再発防止）
- I. デプロイ: Flyway 移行を含むため通常リリースフローに乗る
- J. 互換性・依存関係: 新規ライブラリ不要

### 品質設計の三本柱

| 柱 | 方針 |
|---|---|
| **テスト戦略** | Specification/AnalysisResult/ValuationSpecificationのユニットテストをMockitoで。分割前後をまたぐデータで「調整後の最新/平均/割安度/グレアム」が連続・整合することを検証（境界=施行日前後）。移行SQLはTestcontainers想定 |
| **セキュリティ方針** | 手入力マスタの入力検証（Bean Validation）。SQLはDoma/パラメータ化で injection 対策済み |
| **ドキュメント計画** | 本md（T20260621-stock-price-split-adjustment）。ER図 `develop/document/*.drawio` に新規マスタを追記。調整基準の判断は ADR 相当として本mdに記録 |

### レビュアー記入欄

- 承認者: iori-oiso
- レビュー依頼日: 2026-06-21
- 回答日: 2026-06-22
- 結論: 合格
- コメント: 調整基準A（有報基準）・自動導出（有報株式数変化＋クリフ）・手動マスタ廃止・`stock_price` 生値保持・valuation 再計算の設計を承認。確定アクションのみ補正適用、暫定は印のみ。実装（ステップ5）へ進行可。

---

## ステップ 4: テスト設計（TDD・テストファースト）

> 方針: 新規導出ロジックはユニット（Mockito）、補正適用は Specification 単位、移行は Testcontainers。アサーションは JUnit5 標準のみ。`@Nested`/`@DisplayName` 日本語。

### 新規 `CorporateActionSpecification`（分割係数の自動導出）
| ケース | 入力 | 期待 |
|---|---|---|
| 分割検知（整合） | 有報株式数が 1,000,000→5,000,000（×5）かつ同時期に価格クリフ（前日比≈1/5） | ratio=5、施行日=クリフ日で確定 |
| 併合検知 | 株式数 5,000,000→1,000,000（×0.2）＋価格クリフ（≈5倍） | ratio=0.2 |
| 増資との区別 | 株式数変化あり・価格クリフなし | コーポレートアクションと判定しない |
| クリフのみ（暫定） | 価格クリフあり・有報未更新 | 暫定 ratio をクリフから推定、未確定フラグ |
| 暫定→確定置換 | 暫定後に分割後有報提出 | 確定 ratio に置換 |
| 係数タイムライン | 複数回の分割 | 累積係数 `sharesFactorAt(code,date)` が単調・正しい積 |
| アクションなし | 通常の値動き | factor=1.0（無補正） |

### 補正適用（基準算出）
| 対象 | ケース | 期待 |
|---|---|---|
| `priceOnFilingBasis` | 分割後の生値1,003・最新有報=分割前 | 1,003×(N_D/N_filing)=約5,015 |
| 跨がない期間 | 有報も株価も同一基準 | 係数1.0で生値と一致（不変） |

### `StockSpecification`（補正後の最新/平均/チャート）
| メソッド | ケース | 期待 |
|---|---|---|
| `findStock(Company)` | 分割を含む系列 | latestStockPrice/averageStockPrice/チャート系列が最新有報基準で連続 |
| `getAverageStockPrice` | 平均期間が分割を跨ぐ | 各日を基準補正後に平均（混在しない） |

### `ValuationSpecification#evaluate`（割安度の整合）
| ケース | 期待 |
|---|---|
| 有報=分割前・対象日=分割後 | stockPrice/stockPriceOfSubmitDate を有報基準へ補正後に discountRate 算出（幻の割安が消える） |
| 跨がない | 既存値と一致（係数1.0） |

### グレアム/PER/PBR（`IndicatorValue`）
| ケース | 期待 |
|---|---|
| 株価=分割後・EPS/BPS=有報基準 | 株価を有報基準補正後に PER/PBR 算出 |

### 移行（再構築の冪等性）
| ケース | 期待 |
|---|---|
| 対象企業の再評価を2回実行 | 結果が一致（冪等。UK `company_code,target_date`） |
| 跨がない行の再評価 | 値が変わらない（正当履歴保全） |

---

## ステップ 5: 実装計画（計画→実装→検証）

### 新規・変更コンポーネント
| 区分 | 対象 | 内容 |
|---|---|---|
| 新規 | `CorporateActionSpecification` | 有報株式数変化＋クリフから分割係数タイムラインを導出。`sharesFactorAt(code,date)` / `adjustToBasis(price, code, priceDate, basisDate)` を提供。依存: `FinancialStatementSpecification`（株式数）・`StockPriceDao`（クリフ） |
| 変更 | `StockSpecification#findStock(Company)` `:118-149` | latest/average/チャート系列を最新有報基準で補正 |
| 変更 | `StockSpecification#getAverageStockPriceOfLatestSubmitDate` `:359-377` | 各日を基準補正後に平均 |
| 変更 | `ValuationSpecification#evaluate` `:171-195` / `getStockPriceOfSubmitDate` `:197-218` | 株価を有報（analysisResult）基準へ補正 |
| 変更 | `IndicatorValue`（PER/PBR/グレアム） | 株価を有報基準へ補正 |
| 変更 | 詳細チャート供給（`ViewCorporateInteractor`/`CorporateDetailViewModel`/`CorporatePresenter`/`corporate-v2.html`） | 補正後系列をプロット。分割マーカー（確定分割日に縦線） |
| 不採用 | 係数キャッシュ表 + Flyway | 都度導出で十分。スキーマ追加なし（性能問題なし） |
| 不採用 | 再構築バッチ | 案B（fix-forward）採用。delete系DAO無し＋月次累積のため履歴は据え置き。データ削除しない |

### 実装バッチ（各バッチ後に `./mvnw compile -DskipTests` 確認）
1. ✅ `CorporateActionSpecification` ＋ ユニットテスト（係数導出の核）
2. ✅ `StockSpecification` への補正適用 ＋ テスト
3. ✅ `ValuationSpecification` / `InvestmentIndicatorSpecification` への補正適用 ＋ テスト
4. ✅ 4a チャート/明細系列の補正、4b 分割マーカー
5. （案B採用により不要）対象企業の再構築バッチ

---

## Gate 2: 完了条件の確認

### 運用ルート

正式（本タスクは複数ファイル・スキーマ/移行を伴う中規模のため省略不可）。実装完了時に下記完了条件で確認する。

### 重点観点（完了条件 = 機能 + テスト + ドキュメント）

**機能要件**（コード実装済み。1798 実データの視覚確認は Gate 3）
- [x] チャート・最新株価・平均株価を有報基準・確定のみで補正（バッチ2,4a）
- [x] 割安度・グレアム/PER/PBR を有報基準で補正（バッチ3）
- [x] 分割を跨がない企業・期間は係数1.0で不変（無回帰。テストで担保）
- [x] 分割マーカー（確定分割日の縦線）実装（バッチ4b。視覚確認は Gate 3）

**テスト要件**
- [x] ステップ4の主要ケースを実装し合格（CorporateAction 8 / Stock 53 / Valuation・Indicator 46 / ViewCorporate・Presenter 31）
- [x] P1変更による既存テスト無回帰（全テスト 775 中、失敗は後述の外部要因のみ・P1起因ゼロ）

**ドキュメント要件**
- [x] 本md更新（ER図はスキーマ変更なしのため更新不要）

**スコープ外宣言**: 全面非永続化(P4)・`stock_price`書換え・`analysis_result`変更・Yahoo復旧(P2)・過去valuation/指標履歴の再構築（案B採用で据え置き）

### Gate 2 検証エビデンス（2026-06-22）

- **P1テスト**: 新規/補正テスト全合格（8+53+46+31）。
- **全体無回帰**: `./mvnw test -DexcludedGroups=playwright,manual-screenshot` で 775 件中失敗3のみ、いずれも `SecurityConfigIntegrationTest`（未認証302期待）。原因は **P1スコープ外の `SecurityConfig#httpBasic` 追加（別途対応中）** で未認証応答が401化したため。**P1起因の失敗はゼロ**。
- **manual-screenshot 10件**: `@Tag("manual-screenshot")` のPlaywrightブラウザ試験で環境依存 `PlaywrightException`。P1非関連。
- **Checkstyle**: `checkstyle:check` は**プロジェクト全体で 4085 件の既存違反**（元々グリーンではない）。新規 `CorporateActionSpecification` の69件は全て sun_checks の超厳格項目（80文字行・privateフィールドJavadoc・文末ピリオド形式）で、既存コードも未充足。新規コードは周辺規約（var無し/ワイルドカード無し/メソッドJavadoc有り/イミュータブル/コンストラクタインジェクション）に準拠。**要レビュア判断**（新規ファイルを sun_checks 厳格対応するか、リポジトリ慣習に合わせ据え置くか）。

### レビュアー記入欄

- 承認者: iori-oiso
- レビュー依頼日: 2026-06-22
- 回答日: 2026-06-22
- 結論: 合格
- コメント: P1起因の回帰ゼロを確認し承認。失敗13件はP1外（SecurityConfig httpBasic=別途対応中、manual-screenshot=環境依存）。Checkstyleは既存債務（4085件）でP1非起因、新規ファイルはリポジトリ慣習に準拠で据え置き。

---

## Gate 3: 最終確認

### レビュアー向けサマリ

- 判断してほしいこと: 利用者視点で株価/割安度が正しく表示されるか・副次影響がないか。
- 重要な変更ポイント: 生データ保持＋読み出し時の有報基準補正（確定アクションのみ）。チャート/明細/最新/平均/割安度/指標に適用。分割マーカー。
- 確認してほしい観点: 1798 のチャート連続性（~5000基準）と分割マーカー、既存企業（分割なし）の無変化。

### 重点観点

**差分レビュー（code-reviewer 2026-06-22 実施・指摘の切り分け済み）**
- 検出 HIGH は**すべてP1由来ではなく既存パターン**と確認:
  - テストの `var` 使用 → 既存（StockSpecificationTest HEAD=47/現行=47、Codexは新規varを追加せず）。リポジトリ慣習。
  - `evaluate` のゼロ除算 → HEAD で既に同一 `divide` パターン（株価 NOT NULL・>0 のため実害なし）。P1は変数名変更のみ。
  - 3 SpecクラスのクラスJavadoc欠如 → HEAD で元々無し（既存）。
- P1由来の新規欠陥なし。基準日の一貫性（Stock=最新有報、Valuation/Indicator=各有報提出日）・confirmedOnly徹底・無回帰（係数1.0）をレビュアーも確認。
- MEDIUM（cosmetic）: `ViewCorporateInteractor` の `basisDate.orElseThrow()`（到達後）等。実害なし。

**動作確認結果（実機 dev / 2026-06-22）**
- dev(H2) を別ポート（server 8899 / management 8999）で新コード起動（ユーザーの 8889 既存インスタンスは不干渉）。`Started FundanalyzerApplication`。
- フォームログイン（admin）成功 → `/v3/corporate?code=9001` **HTTP 200・204KB・エラー痕跡ゼロ**（whitelabel/exception/500 なし）。
- 新テンプレの健全性確認: `const splitDates = []`（分割なし企業は空配列）、`afterDraw` 縦線プラグイン＋「分割」ラベルが描画コードに存在、`stockChart30/90/180/365/All` の全 `buildLine` に `splitDates` 引数が配線（指標/予測チャートには非配線＝スコープ通り）。株価系列は連続値（クリフなし）。
- → **新チャート/マーカーのテンプレ・JS配線はサーバサイド描画で破損しない**ことを確認。分割なし企業の表示は不変（無回帰）。
- **分割シナリオ実機確認（seed `V1.0.3` 投入・2026-06-22）**: dev限定 dataset で分割企業 91110（`?code=9111`）を投入（有報株式数 1,000,000→5,000,000 の×5、株価 2024-12-31:5900→2025-01-31:1180 の÷5クリフ）。
  - `/v3/corporate?code=9111` HTTP 200・エラーゼロ。
  - `const splitDates = ["2025-01-31"]` ← 確定分割（有報株式数変化＋クリフ整合）を検知し施行日をマーカー供給。
  - stockChartAll 系列が補正で連続化: 生値 6100/6000/5900（分割前）→ 表示 **1220/1200/1180**（÷5）→ 分割後 1180/1200/…/1320。**クリフ消失・連続**（6100/5=1220 ほか一致）。
  - → 「確定分割の検知 → 有報基準補正でチャート連続化 → 分割マーカー描画」が実機でエンドツーエンド動作することを確認。
  - **スクショエビデンス**（Playwright・dev）: `e2e-tests/ui-split-adjustment/step-3-stockchart.png`（株価チャート全期間：連続線＋2025-01-31の赤「分割」縦線マーカー）、`step-2-corporate-full.png`（詳細ページ全体）。生データのクリフ（5900→1180）が補正で消え連続表示されることを視覚的に確認済み。
  - 補正ロジックの数値正当性はユニット/統合テストでも担保（1798 検算: 1003×5=5015）。本番 1798 の最終視覚確認はデプロイ後にレビュアが実施。

**副次影響**
- `stock_price`/`analysis_result` 不変。分割なし企業は係数1.0で表示不変（テスト担保）。

**ドキュメント整合性**
- 本md・ロードマップ更新済み。スキーマ変更なしのためER図更新不要。

### レビュアー記入欄

- 承認者: iori-oiso
- レビュー依頼日: 2026-06-22
- 回答日: 2026-06-22
- 結論: 合格
- コメント: 差分レビュー（指摘は全て既存パターン）・実機動作確認（dev分割企業でチャート連続化＋分割マーカーをスクショで確認）を確認し承認。P1クローズ。本番1798の最終視覚確認はデプロイ後に実施。

---

## 更新履歴

- 2026-06-21: タスク起票。本番DB実データで原因（2026年3月末の約1:5株式分割の未調整）を確定。認識合わせ完了。影響設計（Gate 1）作成、レビュー依頼。
- 2026-06-22: 認識合わせ追加。調整基準＝A 有報基準に確定。手動マスタ廃止し自動導出（比率＝有報株式数変化、施行日＝クリフ検知）へ。`stock_price` は生値保持、マイグレーションは valuation 再計算に変更。Gate 1 を更新。
- 2026-06-22: スコープロック。ロードマップ（T20260622-stock-data-quality-roadmap）作成。ステップ4（テスト設計）・ステップ5（実装計画/Gate2完了条件）記載。
- 2026-06-22: 実装バッチ1完了。`CorporateActionSpecification`（係数自動導出）＋テスト8件を追加・全合格（Codex委譲分のリフレクション実装を `CompanySpecification` 注入へ是正）。`application.yml` に `corporate-action` 設定追加。併せて既存破損 `IndexPresenterTest`（`ObjectMapper` 引数欠落・本変更とは無関係）を最小修正してビルドを復旧。
  - 留意（バッチ2へ）: 暫定アクション（クリフのみ）を表示/評価に適用すると正当な暴落を誤補正しうる。バッチ2で「確定のみ補正適用・暫定は印のみ」等を設計する。
- 2026-06-22: Gate 1 合格（回答日 2026-06-22）。実装フェーズへ移行。
- 2026-06-22: 実装バッチ2完了。`StockSpecification`（最新株価・平均株価）に有報基準・確定アクションのみの補正を適用（基準日＝最新有報提出日）。`CorporateActionSpecification` に `confirmedOnly` オーバーロード追加。`StockSpecificationTest`・`CorporateActionSpecificationTest` 全合格（53件）。既存テスト無回帰（分割なしは係数1.0で不変）。チャート系列の補正は予定どおりバッチ4へ。スコープ逸脱なし・var/reflectionなし。
- 2026-06-22: 実装バッチ3完了。`ValuationSpecification#evaluate`（割安度/差/比率/割安値）と `InvestmentIndicatorSpecification#insert`（PER/PBR/グレアム/株価企業価値率）に、各analysisResultの有報基準（提出日）・確定のみの株価補正を適用。`ValuationSpecificationTest`・`InvestmentIndicatorSpecificationTest` 全合格（46件）。既存テスト無回帰。スコープ逸脱なし・var/reflectionなし。
- 2026-06-22: 実装バッチ4a完了。`ViewCorporateInteractor` のチャート/明細系列マッピングを有報基準・確定のみで補正（終値＋OHLC、null安全）。チャートと株価明細テーブルが同時に連続化。`ViewCorporateInteractorTest` 全合格（25件）。テスト生成の `Stock.of` 引数順取り違えを是正。スコープ逸脱なし・var/reflectionなし。
- 2026-06-22: Gate 2 合格・Gate 3 合格。全テスト無回帰（失敗13件はP1外＝SecurityConfig httpBasic[Prometheus用の意図的コミット]・manual-screenshot[環境依存]）。差分レビュー指摘は全て既存パターン。dev分割企業（91110）でチャート連続化＋分割マーカーをPlaywrightスクショでE2E確認（`e2e-tests/ui-split-adjustment/`）。**P1 クローズ**。
- 2026-06-22: 実装バッチ4b完了。分割マーカー（確定分割日に縦線）を追加。`CorporateDetailViewModel` に `splitDates`（既存9引数 of() は空デフォルトで互換）、`ViewCorporateInteractor` が確定分割日を供給、`CorporatePresenter` が model 配布、`corporate-v2.html` の `buildLine` に縦線プラグイン（株価チャートのみ）。`ViewCorporateInteractorTest`/`CorporatePresenterTest` 全合格（31件）。スコープ逸脱なし・var/reflectionなし。フロント視覚確認は Gate 3 で実施。
