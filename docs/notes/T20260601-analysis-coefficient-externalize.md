# Task T20260601: 企業価値算出係数の YML 外部設定化（拡張①）

- 着手日: 2026-06-01
- 完了日: —
- 担当: 計画/実装/検証エージェント（Claude） + 人間レビュア（iori-oiso）
- 関連リンク: 分析モデル高度化ロードマップ（① → ④ → ② の第1弾）。後続タスク ④業種相対評価 / ②RIMモデル の前提基盤。

## 解決すべき課題（1 行）

`AnalysisResult` にハードコードされた企業価値算出係数（営業利益重み・流動比率・四半期重み）を `application.yml` に外部化し、感度分析・業種別調整・後続モデル（RIM の資本コスト等）の土台を作る。

## ステップ1: 把握・整理

### 現状（一次情報）

企業価値の算出式は値オブジェクト `AnalysisResult.calculateCorporateValue`（[AnalysisResult.java:111-168](../../src/main/java/github/com/ioridazo/fundanalyzer/domain/value/AnalysisResult.java)）に集約。係数は `static final`:

| 定数 | 現値 | 意味 | 行 |
|---|---|---|---|
| `WEIGHTING_BUSINESS_VALUE` | 10 | 営業利益の倍率（収益力評価） | 31 |
| `AVERAGE_CURRENT_RATIO` | 1.2 | 流動負債の調整係数 | 32 |
| `WEIGHTING_QUARTER_VALUE` | 4 | 年換算の四半期重み（分母四半期重みの既定値も兼ねる） | 33 |
| `TENTH_DECIMAL_PLACE` | 10 | 除算スケール（丸め桁） | 34 |

算出式（162-167行）:
```
企業価値 = (営業利益×10 + 流動資産 − 流動負債×1.2 + 投資その他資産 − 固定負債)
         ÷ 四半期重み × 4 ÷ 株式総数
```

### 既存の設定注入パターン（2系統）

- `@ConfigurationProperties("app.config")` + Lombok（[RestClientProperties.java](../../src/main/java/github/com/ioridazo/fundanalyzer/config/RestClientProperties.java)）
- `@Value("${...}")` 直接注入（`AnalyzeInteractor.targetTypeCodes` = `app.config.view.document-type-code`）

> 本リポは Lombok 使用継続が方針（CLAUDE.md）。グローバル規約の「Lombok禁止」は本リポに適用しない。

### 重要な制約

- `AnalysisResult` は **Spring 管理外**。`AnalyzeInteractor.analyze(document)`（[AnalyzeInteractor.java:151](../../src/main/java/github/com/ioridazo/fundanalyzer/domain/interactor/AnalyzeInteractor.java)）の `new AnalysisResult(financeValue, document)` で直接 new される。
- 既存テスト `AnalysisResultTest` は `new AnalysisResult(null×7)` → `calculateCorporateValue(financeValue, document)` を呼び、**係数 10/1.2/4 前提の期待値**を検証（[AnalysisResultTest.java:19,47](../../src/test/java/github/com/ioridazo/fundanalyzer/domain/value/AnalysisResultTest.java)）。
- 命名注意: `app.config.view.discount-rate: 120` が既存。これは「割引率」でなく**表示閾値**。今回追加する係数キーは `app.config.analysis.*` に分離し混同を避ける。

### スコープ確定

| 区分 | 内容 |
|---|---|
| **コア** | 上記3係数（営業利益重み・流動比率・四半期重み）を `app.config.analysis.*` に外部化。`AnalyzeInteractor` 経由で `AnalysisResult` に注入。既存の算出結果・既存テストは不変（YML 既定値＝現行値）。 |
| **後回し** | 業種別の係数オーバーライド（④で扱う相対評価とは別物だが、係数の業種別チューニングはここでは入れない）。感度分析を回す管理UI/エンドポイント。 |
| **対象外** | `TENTH_DECIMAL_PLACE`（丸め桁）の外部化（数値精度の仕様変更になるため触らない）。BPS/EPS/ROE/ROA の計算式変更。`discount-rate` 表示閾値キーの改名。 |

## ステップ2: プロトタイピング

外部から見える振る舞いは**不変**（同じ提出書類 → 同じ企業価値）。API/画面/DB スキーマ変更なし。よって UI プロトは **該当なし**。

「外から見える形」= application.yml の差分（設定インタフェース）を提示する:

```yaml
app:
  config:
    analysis:
      # 企業価値算出の係数（既定値は現行ハードコード値と一致＝挙動不変）
      operating-profit-weight: 10      # 営業利益の倍率
      current-liabilities-ratio: 1.2   # 流動負債の調整係数
      annual-weight: 4                 # 年換算の四半期重み
```

## Gate 1: 影響設計の承認

### レビュアー向けサマリ

- **判断してほしいこと**: 「Spring管理外の値オブジェクトへ係数を渡す方式」として下記**設計案C（既定値据置オーバーロード方式）**が妥当か。命名・キー構成が後続④②と整合するか。
- **重要な変更ポイント**:
  1. 新規 `@ConfigurationProperties("app.config.analysis")` クラス `AnalysisCoefficient`（係数3つ保持、Lombok `@Data`）を追加。
  2. `AnalysisResult` に係数を受け取る**新オーバーロード**を追加。既存の `calculateCorporateValue(financeValue, document)` は**係数デフォルト（10/1.2/4）を内部利用するまま残す**ため、既存テストは無変更で緑。
  3. `AnalyzeInteractor` に `AnalysisCoefficient` をコンストラクタ注入し、`new AnalysisResult(financeValue, document, coefficient)` で係数を流し込む。
  4. `application.yml` に `app.config.analysis.*` を追加（既定値＝現行値）。`release/config/application-prod.yml` は**変更不要**（既定値が現行挙動と同一のため）。
- **確認してほしい観点**:
  1. 係数を `AnalysisResult` コンストラクタに足す案A（全 new 箇所の改修＋テスト書換が必要）でなく、**デフォルト据置のオーバーロード案C**で良いか（既存テスト不変を優先）。
  2. `annual-weight`(×4) は「分母の四半期重み既定値」と「分子の年換算倍率」の2箇所で使われる。**同一キーで両方を駆動**して良いか、別キーに分けるか。

### 重点観点

#### 影響範囲分析（参照層 / 状態層 / データ層）

- **参照層**:
  - `AnalysisResult` の係数 `static final`: 直接の外部参照なし（private）。`calculateCorporateValue` 呼び出し元は `AnalysisResult` 自身（コンストラクタ53-61行）と `AnalysisResultTest`。
  - `new AnalysisResult(...)` 利用箇所: 本番は `AnalyzeInteractor.java:151` の1箇所のみ。他の `AnalysisResult` 参照（ViewModel/Entity）は別物（`AnalysisResultEntity`）で無関係。
  - 案Cなら既存シグネチャ温存のため**参照層の破壊なし**。
- **状態層**: ステートマシンなし。該当なし。
- **データ層**: DB スキーマ・マイグレーション変更なし。`analysis_result` テーブルへ保存される `corporate_value` の値は**既定値据置のため不変**。過去データ再計算も不要。

#### インフラ影響チェック

- 大量データ処理タイムアウト: 影響なし（計算量不変）。
- 新規外部サービス連携: なし。
- データストアのスキーマ変更/移行: なし。
- バッチ・非同期処理の追加: なし（既存 `AnalysisScheduler` の挙動不変）。
- 依存ライブラリ新規追加（J.1）: **なし**（Spring Boot 標準の `@ConfigurationProperties` のみ）。→ ADR 不要。

#### 品質設計の三本柱

| 柱 | 方針 |
|---|---|
| **テスト戦略** | 値オブジェクト＝ユニットテスト（JUnit5標準アサーションのみ、AssertJ禁止）。新規: ①係数注入時に式へ反映されること ②既定オーバーロードが現行値で従来通り計算すること ③`AnalysisCoefficient` バインドの確認。`AnalyzeInteractorTest` は係数注入経路の結線を1ケース追加。**既存 `AnalysisResultTest` は変更しない**（案Cにより緑のまま）。カバレッジ目標80%以上維持。 |
| **セキュリティ方針** | 設定値は数値係数のみ・外部入力経路なし。シークレット非該当。`@Value(":default")` 禁止規約に従い**既定値はYMLに集約**。段階: 影響軽微。 |
| **ドキュメント計画** | 本 md（Gate記録）。CLAUDE.md「§算出ロジック」または `docs/` に係数の所在変更を追記。`application.yml` のコメントで各係数の意味を明記。drawio 構成図は係数移動のみで構造不変のため更新不要。 |

#### スコープ確定（再掲）

コア=3係数の外部化（挙動不変）／後回し=業種別係数・感度分析UI／対象外=丸め桁・他指標式・discount-rate改名。

### レビュアー記入欄

- 承認者: iori-oiso（人間レビュア）
- レビュー依頼日: 2026-06-01
- 回答日: 2026-06-01
- 結論: **合格**
- コメント: 設計方式は案C（既存シグネチャ温存・既存テスト不変）で承認。`annual-weight`(×4) は (a) 同一キー1つで分母既定値・分子年換算の両方を駆動する方式で承認。

## ステップ4: テスト設計

### テストケース（自然言語）

**新規（`AnalysisResultTest` に @Nested 追加 — 既存ケースは変更しない）**

1. 係数を明示指定したオーバーロードに `operating-profit-weight=20` を渡すと、企業価値が営業利益×20 で計算される（係数が式に反映されることの確認）。
2. 係数オーバーロードに現行値(10/1.2/4)を渡した結果が、既存の係数なしメソッドの結果と一致する（デフォルト据置の同値性）。
3. `current-liabilities-ratio` を変えると流動負債の調整項のみが変わる（係数の独立性）。

**新規（`AnalysisCoefficient` のバインドテスト — 設定クラス）**

4. `app.config.analysis.*` の値が `AnalysisCoefficient` の各フィールドに正しくバインドされる（`@SpringBootTest` もしくは `Binder` による軽量バインド検証）。

**新規（`AnalyzeInteractorTest` — 結線1ケース）**

5. `AnalyzeInteractor.analyze(document)` が注入された `AnalysisCoefficient` を用いて `AnalysisResult` を構築し、`AnalysisResultSpecification.insert` に渡す企業価値が係数を反映している（注入経路の結線確認。Mockito）。

### 既存テストとの重複・補完

- 既存 `AnalysisResultTest.calculateCorporateValue`（正常系・各値欠損系）は **無変更で温存**。新規ケース2が「デフォルト経路＝既存挙動」の橋渡しを担い、回帰を二重に担保する。
- 状態遷移なし → 状態遷移マトリクスは該当なし。

## Gate 2: 完了条件の確認

### 運用ルート

**インライン**（完了条件が短く提示と承認がほぼ同期）。小タスク省略基準は「単一ファイル・単一関数」を満たさない（新規設定クラス＋AnalysisResult＋AnalyzeInteractor＋yml の複数ファイル）ため省略不可。

### 完了条件（機能）

- [ ] `AnalysisCoefficient`（`@ConfigurationProperties("app.config.analysis")`, Lombok `@Data`）を新規追加し3係数を保持
- [ ] `application.yml` に `app.config.analysis.{operating-profit-weight, current-liabilities-ratio, annual-weight}` を追加（既定値 10 / 1.2 / 4）
- [ ] `AnalysisResult` に係数を受け取るオーバーロードを追加し、既存メソッドはデフォルト値(10/1.2/4)を維持
- [ ] `AnalyzeInteractor` に `AnalysisCoefficient` をコンストラクタ注入し `new AnalysisResult(financeValue, document, coefficient)` で利用

### 完了条件（テスト）

- [ ] 単体テスト: 上記テストケース 1〜5（JUnit5標準アサーションのみ）
- [ ] 既存 `AnalysisResultTest` を変更していない
- [ ] `./mvnw test` 全緑・カバレッジ80%以上維持

### 完了条件（ドキュメント）

- [ ] 本 md の Gate 2 / Gate 3 を記入
- [ ] `application.yml` に各係数の意味コメント
- [ ] CLAUDE.md（または docs）の算出ロジック記述へ「係数は app.config.analysis.* に外部化」を追記

### スコープ外（やらないこと）

- 業種別の係数オーバーライド、感度分析の管理UI/エンドポイント
- `TENTH_DECIMAL_PLACE`（丸め桁）の外部化、BPS/EPS/ROE/ROA の式変更
- `app.config.view.discount-rate` キーの改名
- prod 設定（release/config/application-prod.yml）の変更 ← 既定値が現行挙動と同一のため不要

### レビュアー記入欄

- 承認者: iori-oiso（人間レビュア）
- レビュー依頼日: 2026-06-01
- 回答日: 2026-06-01
- 結論: **合格**（インライン承認）
- コメント: 機能・テスト・ドキュメント・スコープ外の完了条件で承認。TDD 実装着手可。

## ステップ5・6: 実装と多軸検証

### 実装サマリ

| ファイル | 変更 |
|---|---|
| `config/AnalysisCoefficient.java` | 新規。`@ConfigurationProperties("app.config.analysis")` + `@Getter` + `@AllArgsConstructor` + `final`（イミュータブル）。`defaults()`=10/1.2/4 |
| `FundanalyzerApplication.java` | `@EnableConfigurationProperties(AnalysisCoefficient.class)` を追加（登録） |
| `domain/value/AnalysisResult.java` | 係数オーバーロード（コンストラクタ・`calculateCorporateValue`）追加。既存2引数は `defaults()` 委譲で温存 |
| `domain/interactor/AnalyzeInteractor.java` | `AnalysisCoefficient` をコンストラクタ注入し3引数構築で利用 |
| `application.yml` | `app.config.analysis.*`（既定値 10/1.2/4） |
| 各テスト | `AnalysisResultTest`（既存8件無変更＋新規3件）/ `AnalysisCoefficientTest`（新規2件）/ `AnalyzeInteractorTest`（新規結線1件・setUp 係数追加） |

### テスト結果

- `./mvnw test -DexcludedGroups=playwright` → **727 件全緑・回帰なし**。

### 実装中に判明した重要点（ユニットテストで捕捉できなかった起動時不具合）

イミュータブル化（`final` フィールド＝コンストラクタバインド）と `@Configuration` 自己登録は**両立しない**。当初 `@Configuration` のまま `final` 化したところ、ユニットテスト 727 件は緑だったが **`spring-boot:run` の実機起動で `UnsatisfiedDependencyException`（BigDecimal を autowire しようとして失敗）** が発生した。

- 原因: コンストラクタバインド対象を `@Configuration` で通常 Bean 登録すると、Spring が係数の `BigDecimal` 引数を DI しようとする。
- 修正: `@Configuration` を外し、`FundanalyzerApplication` に `@EnableConfigurationProperties(AnalysisCoefficient.class)` を付与（リポジトリ既存の `CacheConfig` と同方式）。
- 教訓: `@ConfigurationProperties` のイミュータブル（コンストラクタバインド）型は `@EnableConfigurationProperties` / `@ConfigurationPropertiesScan` で登録する。**この種の起動時 DI 不具合はユニットテストをすり抜けるため、Gate 3 の実機起動確認が有効だった。**

### 多軸検証（観点別）

| 観点 | 結果 |
|---|---|
| コード品質（code-reviewer） | CRITICAL/HIGH なし。指摘反映: `@Data`→`@Getter`+final でイミュータブル化（global規約 setter禁止に準拠）／annual-weight 二重利用のインラインコメント追加。 |
| テスト構造・機能完全性 | 完了条件のテスト1〜5を実装。既存テスト無変更。挙動不変（既定値＝現行値、BigDecimal scale 一致）を新規ケース2で二重担保。 |
| セキュリティ（security-reviewer） | 本変更起因の重大リスクなし。係数は非機密・外部入力経路なし。`annualWeight=0` のゼロ除算は運用設定ミスの範疇（原コードも未ガード、Validation 依存追加はスコープ外のため見送り）。 |
| ドキュメント整合性 | CLAUDE.md「永続化」節に係数外部化を追記。yml にコメント。本 md を更新。 |

### スコープ外として切り出す既存課題（本変更とは無関係・別タスク推奨）

security-reviewer が検出した**既存の**設定リスク（今回は触らない）:
- `application-prod.yml` / `release/config/application-prod.yml` の DB パスワード・Slack トークン平文 → 環境変数化
- Actuator `exposure.include: "*"` / `env.enabled: true`（prod は別ポート 8990 で緩和されているが YML レベル保護なし）

> 環境設定系（application.yml / release/*）の変更は本番起動直結でリスク高のため、本タスクのスコープ外。別タスク化を推奨。

## Gate 3: 最終確認

### レビュアー向けサマリ

- **判断してほしいこと**: 挙動不変（外部化前後で同一書類→同一企業価値）が利用者視点で担保されているか。副次影響がないか。
- **重要な変更ポイント**:
  1. 係数3つを `app.config.analysis.*` へ外部化（既定値＝現行値、画面表示の企業価値・割安度は不変）。
  2. `AnalysisResult` 係数オーバーロード追加＋既存メソッドはデフォルト委譲（既存テスト8件無変更で緑）。
  3. `AnalyzeInteractor` への注入のみが本番の挙動接点。スケジューラ等の処理時刻・フローは不変。
- **確認してほしい観点**:
  1. 既定値据置のため `/v3/index`・`/v3/corporate` 等の数値が従来と一致すること（実機確認）。
  2. prod 設定を変更していない（既定値が現行挙動と同一のため不要）こと。

### 重点観点

#### 差分レビュー
本番コード5ファイル（`AnalysisCoefficient`新規 / `AnalysisResult` / `AnalyzeInteractor` / `FundanalyzerApplication` / `application.yml`）＋テスト2ファイル＋新規テスト＋CLAUDE.md。

#### 動作確認結果（dev 実機起動）
- アプリ起動: **成功**（`Started FundanalyzerApplication in 4.274s`）。係数3つは `/actuator/configprops` にバインド確認。
- `/v3/index`・`/v3/valuation`・`/v3/edinet-list`: いずれも **HTTP 200**（[index スクショ](T20260601-attachments/gate3-v3-index.png)）。
- `/v3/corporate?code=9008`: 最新企業価値 4500.000 / 割安度 136.8 / グレアム指数 21.600 / BPS 950.000 / EPS 150.000 / PER 12.000 / PBR 1.800 を表示（seed データの期待値どおり＝**挙動不変を実機確認**）（[詳細スクショ](T20260601-attachments/gate3-v3-corporate-9008.png)）。

#### 副次影響
なし（DB/スキーマ/外部連携/依存追加なし）。スケジューラ等のフロー・時刻も不変。

#### ドキュメント整合性
CLAUDE.md・yml コメント・本 md を更新済み。

### レビュアー記入欄

- 承認者: iori-oiso（人間レビュア）
- レビュー依頼日: 2026-06-01
- 回答日: 2026-06-01
- 結論: **合格**
- コメント: dev 実機起動で挙動不変を確認。イミュータブル化に伴う起動時 DI 不具合（ユニットテストすり抜け）を実機確認で捕捉・修正済み。承認。

## 更新履歴

- 2026-06-01: 初版作成（ステップ1把握・Gate1 影響設計）
- 2026-06-01: Gate1 合格記録。ステップ4 テスト設計・Gate2 完了条件を追記
- 2026-06-01: Gate2 合格記録。ステップ5・6 実装＋多軸検証完了（727件緑）。Gate3 最終確認待ち
