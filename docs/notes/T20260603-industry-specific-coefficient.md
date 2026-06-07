# Task T20260603: 企業価値算出係数の業種別重みづけ（拡張⑤）

- 着手日: 2026-06-03
- 完了日: —
- 担当: 計画/実装/検証エージェント（Claude） + 人間レビュア（iori-oiso）
- 関連リンク: タスク①[T20260601-analysis-coefficient-externalize.md](T20260601-analysis-coefficient-externalize.md) の `AnalysisCoefficient` を作り替える（①の YML 既定をテーブルデータへ移設）。②(RIM)とは独立。

## 解決すべき課題（1 行）

営業利益倍率・流動負債調整係数を **industry マスタの列で業種別に保持**し（既定値もソースではなくテーブルデータ＝列 DEFAULT で保持）、業種特性を反映した理論株価を算出できるようにする。

## ステップ1: 把握・整理

### 現状（一次情報）

- 企業価値式（`AnalysisResult.calculateCorporateValue`）は `AnalysisCoefficient`（[config/AnalysisCoefficient.java](../../src/main/java/github/com/ioridazo/fundanalyzer/config/AnalysisCoefficient.java)）の3係数を使う。①で `@ConfigurationProperties("app.config.analysis")`＋`defaults()`(10/1.2/4)＋`@EnableConfigurationProperties` 登録、`AnalysisResult` は2引数(=defaults委譲)/3引数の両経路、`AnalyzeInteractor` が `AnalysisCoefficient` をコンストラクタ注入。
- industry マスタ: `industry`（id/name/created_at）。最新マイグレーション **V0.3.6**（次 **V0.3.7**）。`IndustryEntity` は Doma immutable record。`IndustryDao.selectAll` は `SELECT *`（列追加で自動マップ）、`insert` は `@Insert(include={"name","createdAt"})`（新列は insert 非対象＝DB DEFAULT 適用）。
- 業種解決: `document.getEdinetCode()` → `companySpecification.findCompanyByEdinetCode` → `Company.industryId()`。未設定企業も `CompanyEntity.OTHER_INDUSTRY_ID=40`（"その他"・seed 済）に寄るため行は必ず存在する想定。
- 再分析の冪等性: `AnalysisResultSpecification.insert` は一意制約違反を catch して**スキップ**＝**既存 `analysis_result` は再分析で上書きされない**。

### 認識合わせ結果

1. **業種別にする係数**: `operatingProfitWeight` ＋ `currentLiabilitiesRatio`。`annualWeight`（年換算=「1年=4四半期」の**不変な定数**）は **YML/DB ではなくソース定数**として定義（`AnalysisResult` の `static final ANNUAL_WEIGHT=4`）。
2. **定義場所と既定値**: **industry マスタの列**（`operating_profit_weight` / `current_liabilities_ratio`、`NOT NULL DEFAULT 10 / 1.2`）。**既定値もテーブル側（列 DEFAULT）で保持し、ソース（YML/`defaults()`）からは削除**（設計(i)＋(い)）。全行に値が入るため NULL/フォールバック不要。`app.config.analysis` ブロックは丸ごと削除。
3. **既存値の扱い**: **前向きのみ（A')**。⑤は算出ロジック＋スキーマ＋設定のみ。新規分析分から反映、既存 `analysis_result` は据え置き（再分析はスキップ）。揃えるのは別タスク。

### スコープ確定

| 区分 | 内容 |
|---|---|
| **コア** | (1) Flyway V0.3.7 で `industry` に `operating_profit_weight`/`current_liabilities_ratio`（`NOT NULL DEFAULT 10/1.2`）追加。(2) `IndustryEntity` に2フィールド追加（convenience コンストラクタ併設）。(3) `AnalysisCoefficient` を **op/curr の2係数のみの純粋値オブジェクト**化（`@ConfigurationProperties`・`defaults()` 廃止）。(4) `AnalysisResult` に `static final ANNUAL_WEIGHT=4` を定義し算出式で使用。(5) `IndustrySpecification.resolveCoefficient(industryId)` で業種行から実効係数を生成。(6) `AnalyzeInteractor` が業種解決し実効係数で `AnalysisResult` 構築（`AnalysisCoefficient` 注入をやめ `IndustrySpecification` 注入へ）。(7) `application.yml` の `app.config.analysis` を丸ごと削除。`FundanalyzerApplication` の `@EnableConfigurationProperties` 削除。 |
| **後回し / 別タスク** | 既存 `analysis_result` の再計算（削除→再分析）。業種別係数の編集 UI。流動比率以外の追加係数。 |
| **対象外** | `annualWeight` のテーブル移設、`AnalysisResult` の算出式変更、再分析の冪等性仕様変更。 |

## ステップ2: プロトタイピング（外から見える形）

### スキーマ差分（V0.3.7）— 既定値を列 DEFAULT で保持
```sql
ALTER TABLE `industry`
  ADD COLUMN `operating_profit_weight`   DECIMAL(10,4) NOT NULL DEFAULT 10  COMMENT '営業利益倍率(業種別)',
  ADD COLUMN `current_liabilities_ratio` DECIMAL(10,4) NOT NULL DEFAULT 1.2 COMMENT '流動負債調整係数(業種別)';
```
- 既存全行は ALTER 時に DEFAULT(10/1.2)、新規業種 insert も DB DEFAULT 適用 → **全行が値を持つ・挙動不変**。既定値はテーブル（列 DEFAULT）に存在しソースには無い。

### YML 差分
`app.config.analysis`（op-weight / curr-ratio / annual-weight）を**丸ごと削除**。
- op-weight / curr-ratio → industry テーブルの列（既定は列 DEFAULT）
- annual-weight → ソース定数 `AnalysisResult.ANNUAL_WEIGHT = BigDecimal.valueOf(4)`（不変）

### 業種別の調整
`UPDATE industry SET operating_profit_weight=15 WHERE id=12;` → 情報・通信業の**新規分析**から企業価値が変わる。

## Gate 1: 影響設計の承認

### レビュアー向けサマリ

- **判断してほしいこと**: 既定値を含め op-weight/curr-ratio を industry マスタの列（DEFAULT 10/1.2）へ移し、`AnalysisCoefficient` を値オブジェクト化する設計が妥当か。**①の作り替え**・**スキーマ変更**・**前向きのみ**を許容できるか。
- **重要な変更ポイント**:
  1. **Flyway V0.3.7**: `industry` に `NOT NULL DEFAULT 10/1.2` の2列追加（既存行・新規行とも値が入る・挙動不変・backfill 不要）。
  2. `IndustryEntity`（record）に `operatingProfitWeight`/`currentLiabilitiesRatio` 追加。既存 `new IndustryEntity(...)`（テスト7箇所）・`of()` を壊さない **3引数 convenience コンストラクタ併設**。`selectAll`=`SELECT *` 自動マップ、`insert`=include 指定で新列非対象。
  3. **`AnalysisCoefficient` の作り替え（①変更）**: `@ConfigurationProperties`・`defaults()` を廃止し、`operatingProfitWeight`/`currentLiabilitiesRatio` の **2係数のみ**持つ純粋値オブジェクトに（annualWeight は持たない）。`FundanalyzerApplication` の `@EnableConfigurationProperties(AnalysisCoefficient.class)` 削除。
  4. `AnalysisResult`: `private static final BigDecimal ANNUAL_WEIGHT = BigDecimal.valueOf(4)` を定義。`calculateCorporateValue(fv, doc, coefficient)` は coefficient の2係数＋`ANNUAL_WEIGHT`（四半期分母の既定＋年換算倍率）を使用。2引数コンストラクタ/2引数 `calculateCorporateValue`/`defaults()` 依存を廃止し3引数経路に一本化。
  5. `IndustrySpecification.resolveCoefficient(Integer industryId)`: キャッシュ済業種から行を引き `new AnalysisCoefficient(row.opWeight, row.currRatio)` を返す。行が無い/idがnull の場合は `FundanalyzerNotExistException`（=分析スキップ・HALF_WAY、既存の値欠損ハンドリングと同様）。
  6. `AnalyzeInteractor`: `AnalysisCoefficient` 注入をやめ `IndustrySpecification` を注入。会社 `industryId` で実効係数を解決し `AnalysisResult` 構築（annualWeight 注入は不要＝定数化のため）。
  7. `application.yml` の `app.config.analysis` を丸ごと削除。
- **確認してほしい観点**:
  1. **①の作り替え**（`AnalysisCoefficient` を設定Beanから値オブジェクトへ・YML 2キー削除）の許容。
  2. **スキーマ変更**（industry へ列追加・既存挙動不変）と **前向きのみ**（既存保存値据え置き）の許容。
  3. 業種行が見つからない場合に分析スキップ（HALF_WAY）とする方針（通常は OTHER_INDUSTRY_ID=40 等で必ず行が存在）。

### 重点観点

#### 影響範囲分析（参照層 / 状態層 / データ層）

- **参照層（①コードに波及）**:
  - `AnalysisCoefficient`: アノテーション/`defaults()` 削除・値オブジェクト化。参照元 = `AnalysisResult`（3引数経路のみに）、`AnalyzeInteractor`（注入変更）、`IndustrySpecification`（新規生成）。
  - `AnalysisResult`: 2引数経路削除。`AnalysisResultTest` の旧 `calculateCorporateValue`（2引数）ケースを3引数（明示係数 10/1.2/4）へ機械的更新（シグネチャ変更の許容例）。`calculateCorporateValueWithCoefficient`（既存3引数）はそのまま。
  - `AnalysisCoefficientTest`（①の binding/`defaults` テスト）: `@ConfigurationProperties`/`defaults()` 廃止に伴い**削除または値オブジェクト用に作り替え**。
  - `IndustryEntity`: フィールド追加＋convenience コンストラクタ（既存生成・`of()` 無変更）。
  - `IndustrySpecification`: `resolveCoefficient` 追加（`config.AnalysisCoefficient` を import）。
  - `AnalyzeInteractor` / `AnalyzeInteractorTest`: 注入構成変更（コンストラクタ引数の機械的更新）。
  - `FundanalyzerApplication`: `@EnableConfigurationProperties` 1行削除。
- **状態層**: なし。
- **データ層**: **スキーマ変更（V0.3.7・NOT NULL DEFAULT の2列）**。既存行は DEFAULT で埋まり企業価値不変。`analysis_result` は新規 insert のみ実効係数反映、既存据え置き（前向き）。backfill なし。

#### インフラ影響チェック
- スキーマ変更と移行戦略: 該当。`industry`（約40行）への `NOT NULL DEFAULT` 列追加。H2(MySQLモード)・MySQL ともに ALTER で既存行に DEFAULT 適用。移行（backfill）不要。`release/config` 変更不要。
- 大量データ/外部連携/バッチ/依存追加（J.1）: なし。ADR: `AnalysisCoefficient` の位置づけ変更（設定→値オブジェクト＋既定のテーブル移設）は設計判断としてアーキ判断記録（ADR）への一文記載を検討。

#### 品質設計の三本柱
| 柱 | 方針 |
|---|---|
| **テスト戦略** | ユニット（JUnit5標準のみ）。`IndustrySpecification.resolveCoefficient`（業種行の値採用・annualWeight 合成・行なし/null→例外）、`AnalyzeInteractor` が業種に応じた実効係数で企業価値を算出する結線（Mockito）。`IndustryEntity` 新列マッピングは @SpringBootTest（H2・V0.3.7適用）で担保。改修する既存テスト（`AnalysisResultTest` 2→3引数、`AnalysisCoefficientTest` 作り替え、`AnalyzeInteractorTest` setUp）は計画承認のうえ機械的更新。カバレッジ80%以上。 |
| **セキュリティ方針** | 参照系＋マスタ列の数値のみ・外部入力経路なし・シークレット非該当。SQL は Doma パラメータ化。影響軽微。 |
| **ドキュメント計画** | 本 md。CLAUDE.md の係数記述を更新（既定値はテーブル列 DEFAULT・業種別オーバーライド・前向き反映）。①の md には後日談リンク。drawio ER 図に industry の2列追記。 |

#### スコープ確定（再掲）
コア=industry 列追加（既定値=列DEFAULT）＋`AnalysisCoefficient`値オブジェクト化＋実効係数マージ＋AnalyzeInteractor適用（前向き）／後回し=既存値再計算・編集UI・追加係数／対象外=年換算重みのテーブル移設・AnalysisResult算出式変更。

### レビュアー記入欄

- 承認者: iori-oiso（人間レビュア）
- レビュー依頼日: 2026-06-03
- 回答日: 2026-06-03
- 結論: **合格**
- コメント: industry 列追加（既定=列DEFAULT）＋`AnalysisCoefficient` 2係数値オブジェクト化＋annualWeight 定数化＋`app.config.analysis` 全削除、スキーマ変更・前向きのみ・行なし時スキップを承認。①の作り替えを含む。

## ステップ4: テスト設計

### テストケース（自然言語）

**新規: `IndustrySpecification.resolveCoefficient(industryId)`**
1. 業種行が存在 → その行の op-weight/curr-ratio を持つ `AnalysisCoefficient` を返す。
2. 異なる業種ID → それぞれの行の値を返す（取り違えない）。
3. 業種ID が null → `FundanalyzerNotExistException`。
4. 業種行が見つからない（未知ID）→ `FundanalyzerNotExistException`。

**新規: `AnalyzeInteractor` 結線**
5. ある書類を分析すると、会社の業種行の係数で企業価値が算出される（業種倍率を反映。Mockito で `findCompanyByEdinetCode`→Company(industryId)、`resolveCoefficient`→係数 をスタブし、`insert` に渡る企業価値を検証）。

**新規: `IndustryEntity` 新列マッピング（H2 結合）**
6. `@SpringBootTest`（V0.3.7 適用）で `industry` 行を読むと op-weight/curr-ratio が DEFAULT(10/1.2) で取得できる。

**改修: 既存テスト（機械的）**
- `AnalysisResultTest`: 2引数 `calculateCorporateValue` ケースを3引数（明示係数）に。`calculateCorporateValueWithCoefficient` の `new AnalysisCoefficient(...,...,4)` を2引数 `new AnalysisCoefficient(op, curr)` に（期待値は ANNUAL_WEIGHT 定数で同値）。
- `AnalysisCoefficientTest`: `@ConfigurationProperties` binding・`defaults()` テストを廃止（2係数値オブジェクトの簡易テストに作り替え）。
- `AnalyzeInteractorTest`: setUp の生成引数を `IndustrySpecification` 注入に更新。`document_usesInjectedCoefficient` の係数生成を2引数化＋経路調整。

### 状態遷移
なし（該当なし）。

## Gate 2: 完了条件の確認

### 運用ルート
**インライン**（複数ファイル・①作り替えを含むため省略不可）。

### 完了条件（機能）
- [ ] Flyway `V0.3.7__add_industry_coefficient.sql`: `industry` に `operating_profit_weight`/`current_liabilities_ratio`（`NOT NULL DEFAULT 10/1.2`）追加
- [ ] `IndustryEntity` に2フィールド追加＋3引数 convenience コンストラクタ（既存生成・`of()` 無変更）
- [ ] `AnalysisCoefficient` を2係数の純粋値オブジェクト化（`@ConfigurationProperties`/`defaults()` 廃止）。`FundanalyzerApplication` の `@EnableConfigurationProperties` 削除
- [ ] `AnalysisResult` に `ANNUAL_WEIGHT=4` 定数定義・3引数経路一本化・2引数経路廃止
- [ ] `IndustrySpecification.resolveCoefficient(industryId)` 追加
- [ ] `AnalyzeInteractor` を `IndustrySpecification` 注入に変更し業種別実効係数を適用
- [ ] `application.yml` の `app.config.analysis` 削除

### 完了条件（テスト）
- [ ] 上記テストケース1〜6（JUnit5標準のみ）
- [ ] 改修した既存テスト以外は無変更／`./mvnw test -DexcludedGroups=playwright` 全緑・カバレッジ80%以上
- [ ] dev 実機で起動成功＋業種別係数の企業価値反映を確認（Gate3。一時 seed で業種列 UPDATE→新規分析の企業価値変化）

### 完了条件（ドキュメント）
- [ ] 本 md・CLAUDE.md（係数の置き場所＝テーブル列/ソース定数）更新。①md に後日談リンク。drawio ER 図に industry 2列追記

### スコープ外
- 既存 `analysis_result` の再計算、編集UI、追加係数、annualWeight のテーブル移設

### レビュアー記入欄
- 承認者: iori-oiso（人間レビュア）
- レビュー依頼日: 2026-06-03
- 回答日: 2026-06-03
- 結論: **合格**（インライン承認）

## ステップ5・6: 実装と多軸検証

### 実装サマリ
| ファイル | 変更 |
|---|---|
| `db/migration/V0.3.7__add_industry_coefficient.sql` | 新規。industry に `operating_profit_weight`/`current_liabilities_ratio`（NOT NULL DEFAULT 10/1.2） |
| `entity/master/IndustryEntity` | 2フィールド追加＋3引数 convenience コンストラクタ |
| `dao/master/IndustryDao` | `@Insert(include)` の意図コメント追記（係数列は DB DEFAULT） |
| `config/AnalysisCoefficient` | op/curr の2係数値オブジェクト化（`@ConfigurationProperties`/`defaults()` 廃止） |
| `value/AnalysisResult` | `ANNUAL_WEIGHT=4` 定数・2引数経路廃止・3引数一本化 |
| `specification/IndustrySpecification` | `resolveCoefficient(industryId)` 追加（null係数行は除外） |
| `interactor/AnalyzeInteractor` | `IndustrySpecification` 注入・業種別実効係数で算出・Fs無しNotExistはスキップ |
| `FundanalyzerApplication` / `application.yml` | `@EnableConfigurationProperties` 削除 / `app.config.analysis` 削除 |
| CLAUDE.md | 係数の置き場所（テーブル列/ソース定数）更新 |

### テスト結果
- `./mvnw test -DexcludedGroups=playwright` → **742 件全緑・回帰なし**（@SpringBootTest が V0.3.7 適用・Bean削除後もコンテキスト起動）。
- 新規/改修: `IndustrySpecification.resolveCoefficient`（行あり/取り違えない/null/未知ID）、`AnalyzeInteractor`（業種別係数の反映・解決不可時スキップ）、`AnalysisResultTest`（2→3引数・係数2引数化）、`AnalysisCoefficientTest` 削除。

### 多軸検証（観点別）
| 観点 | 結果 |
|---|---|
| コード品質（code-reviewer） | **CRITICAL を1件検出→修正**: 業種/会社未整備時の Fs無し NotExist が `e.getFs().orElseThrow` で RuntimeException 化し `parallelStream.forEach` を中断し得る問題 → catch で Fs 無しはスキップ（バッチ継続）に修正。併せて null係数行除外・`@Insert` 意図コメント・解決不可スキップのテスト追加・新規テストの var 解消を反映。 |
| テスト構造・機能完全性 | 完了条件テスト網羅。既存テストは convenience コンストラクタ＋機械的更新で対応。 |
| セキュリティ（security-reviewer） | 本変更起因の重大リスクなし（LOW）。内部バッチのみ・外部入力なし・新規シークレットなし・Doma パラメータ化。null係数 NPE 懸念は resolveCoefficient の null 除外で対処。 |
| ドキュメント整合性 | CLAUDE.md・本 md 更新。 |

### 既知の MEDIUM（フォローアップ候補・本タスク対象外）
- `AnalysisCoefficient` を `config/` から `domain/value/` へ移設（値オブジェクト化に伴う適正パッケージ）。影響範囲が広いため別タスク。

## Gate 3: 最終確認

### レビュアー向けサマリ
- **判断してほしいこと**: ①の作り替え（AnalysisCoefficient 値オブジェクト化・YML 全削除・annualWeight 定数化）＋industry 列追加＋業種別係数適用が利用者視点で妥当か。前向きのみ（既存値据え置き）で良いか。
- **重要な変更ポイント**:
  1. 業種別係数を industry 列（既定=列DEFAULT 10/1.2）で保持。新規分析から反映。
  2. CRITICAL（バッチ中断リスク）をレビューで検出し修正済み。
  3. 既存の保存値・既存画面表示は不変（前向きのみ・再分析スキップ）。
- **確認してほしい観点**:
  1. 既存企業価値が不変であること（9001=3000.000 で従来どおり）。
  2. 業種別係数の効果は新規分析から（既存データには delete→再分析の別タスクが必要）。

### 重点観点
#### 差分レビュー
本番8ファイル＋マイグレーション1＋テスト4（1削除）＋CLAUDE.md。

#### 動作確認結果（dev 実機起動）
- **V0.3.7 マイグレーション適用・起動成功**（`Migrating ... 0.3.7`／`Started FundanalyzerApplication`）。`@EnableConfigurationProperties` 削除後もコンテキスト起動。
- 業種別タブ `/v3/valuation?target=industry`：**HTTP 200**（`SELECT *` で新列マッピング正常）。
- `/v3/index`・`/v3/corporate?code=9001`：**HTTP 200**。9001 最新企業価値 = **3000.000**（seed 値のまま＝前向きのみで既存値不変を確認）（[スクショ](T20260603-attachments/gate3-corporate-9001.png)）。
- 業種別係数の算出反映はユニットテストで厳密に担保（新規分析の end-to-end は EDINET 取込が必要なため実機デモは省略）。

#### 副次影響
DBスキーマは industry へ nullable でない列追加（既存行 DEFAULT で挙動不変）。他機能・他画面は不変。

#### ドキュメント整合性
CLAUDE.md・本 md 更新済み。

### レビュアー記入欄
- 承認者: iori-oiso（人間レビュア）
- レビュー依頼日: 2026-06-03
- 回答日: 2026-06-04
- 結論: **合格**
- コメント: マイグレーション適用・起動・既存値不変（9001=3000.000）を実機確認。CRITICAL（バッチ中断リスク）は修正済み。前向きのみ・既存値据え置きを承認。

## 更新履歴

- 2026-06-03: 初版（YML業種マップ案）→ 業種IDキー → industry 列追加（V0.3.7）と推移。
- 2026-06-03: 既定値もテーブル（列 DEFAULT）で保持し YML/`defaults()` から削除する方針（設計(i)＋(い)）に確定。`AnalysisCoefficient` を値オブジェクト化（①作り替え）。Gate1 承認待ち。
- 2026-06-07: **ER図更新は別タスク化**。`develop/document/Entity-Relationship-Diagram.drawio` は2020年作成で `company` 周辺のみ・現行スキーマの大半（`industry`/`analysis_result` 等）が未反映と判明。本タスクの「industry 列追記」は対象テーブルが図に無く成立しないため見送り。ER図は現行 `db/migration/V*.sql` からの**全面再生成を別タスク**で扱う。
