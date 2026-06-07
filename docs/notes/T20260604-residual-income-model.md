# Task T20260604: 残余利益モデル(RIM)の理論株価追加（拡張②）

- 着手日: 2026-06-04
- 完了日: —
- 担当: 計画/実装/検証エージェント（Claude） + 人間レビュア（iori-oiso）
- 関連リンク: 分析モデル高度化ロードマップ（①→④→②、⑤を経て）最終タスク。①の係数基盤・⑤の業種別 industry 列パターンを踏襲。

## 解決すべき課題（1 行）

既存の BPS・ROE と業種別資本コスト r から **残余利益モデル(RIM)の理論株価**を算出・保存し、既存の企業価値モデルとの **2モデル合意度**を銘柄詳細で示す。

## ステップ1: 把握・整理

### 認識合わせ結果

1. **RIM 式**: 無成長・一定 ROE 版 `RIM理論株価 = BPS × (ROE/100) ÷ r`（ROE は % 保存のため /100。BPS・ROE は既存）。
2. **資本コスト r**: **業種別**。`industry` に `cost_of_equity` 列（`NOT NULL DEFAULT 0.08`）。⑤の industry 列パターン踏襲。
3. **永続化(B)**: RIM 値を `analysis_result` に列保存（分析時に算出）。**前向きのみ**＝既存行は NULL（「-」）、新規分析分から RIM が入る（再分析は一意制約スキップ）。
4. **表示**: `/v3/corporate` に「RIM理論株価」＋「2モデル合意度（割安票数/2）」。index 展開は後回し。

### 現状（一次情報）

- `analysis_result`（[V0.1.0](../../src/main/resources/db/migration/V0.1.0__init_create.sql) + bps/eps/roe/roa は [V0.3.0](../../src/main/resources/db/migration/V0.3.0__create_investment_indicator.sql) で `ALTER ADD COLUMN`）。`AnalysisResultEntity`（Lombok `@Value`）に bps/roe 等を保持。`AnalysisResultDao.insert` は `@Insert`（include 指定なし＝全項目）。
- 算出: `AnalysisResult`（[value](../../src/main/java/github/com/ioridazo/fundanalyzer/domain/value/AnalysisResult.java)）が bps/eps/roe/roa/corporateValue を計算。⑤で `AnalysisCoefficient`（op/curr の2係数）を受け取り `AnalyzeInteractor` が `IndustrySpecification.resolveCoefficient(industryId)` で解決。
- 企業価値モデルの理論株価（per share）= `corporateValue`（`analysis_result.corporate_value`）。`AnalyzeInteractor.calculateCorporateValue(company)` で最新値・平均を集計。
- 銘柄詳細: `CorporatePresenter` が `corporateView`（latestCorporateValue 等）・analysis 一覧・株価を model に積む。`corporate-v2.html` の「表示情報」カードに最新企業価値・割安度・グレアム指数（④の業種内z併記）等。
- 最新マイグレーション **V0.3.7**（⑤）→ 次 **V0.3.8**。

### スコープ確定

| 区分 | 内容 |
|---|---|
| **コア** | (1) Flyway V0.3.8: `industry` に `cost_of_equity`（NOT NULL DEFAULT 0.08）、`analysis_result` に `rim_value`（FLOAT NULL）追加。(2) `IndustryEntity`/`AnalysisCoefficient` に r を追加し `resolveCoefficient` で解決。(3) `AnalysisResult` に RIM 算出＋ `rimValue` 保持、`AnalysisResultEntity`/`of`/`insert` に伝播。(4) `/v3/corporate` に RIM理論株価＋合意度（割安票数/2）表示。 |
| **後回し / 別タスク** | 既存 analysis_result の RIM 再計算（delete→再分析）。成長項つき RIM。index への合意度列展開。RIM の業種内zスコア化。 |
| **対象外** | 企業価値モデル式の変更、CAPM による r 算出、再分析の冪等性仕様変更。 |

## ステップ2: プロトタイピング

### スキーマ差分（V0.3.8）
```sql
ALTER TABLE IF EXISTS `industry`        ADD COLUMN `cost_of_equity` DECIMAL(6,4) NOT NULL DEFAULT 0.08 COMMENT '資本コスト(業種別・RIM用)';
ALTER TABLE IF EXISTS `analysis_result` ADD COLUMN `rim_value`      FLOAT                 DEFAULT NULL COMMENT 'RIM理論株価' AFTER `corporate_value`;
```
- industry 既存行は r=0.08。analysis_result 既存行は rim_value=NULL（前向き）。

### 表示（/v3/corporate 表示情報カード・モック）
```
┌─ 最新企業価値 ─┐ ┌─ RIM理論株価 ─┐ ┌─ 2モデル合意度 ─┐
│ 3000.000       │ │ 1500.000       │ │ 割安 2 / 2  👍   │  ← 企業価値>株価 & RIM>株価
└────────────────┘ └────────────────┘ └──────────────────┘
```
- 合意度 = (企業価値モデル理論株価>株価 ? 1:0) + (RIM理論株価>株価 ? 1:0)、表示 `n/2`。
- RIM が算出不能（既存行 NULL・ROE/BPS なし・r=0・赤字 ROE<0 等）は「-」、合意度は企業価値モデルのみで `n/1` 相当か「-」。

## Gate 1: 影響設計の承認

### レビュアー向けサマリ

- **判断してほしいこと**: RIM を `analysis_result` に永続化（前向き）し、r を industry 列で持ち、銘柄詳細に2モデル合意度を出す設計が妥当か。**スキーマ2列追加**・**前向きのみ（既存行 RIM は当面 NULL）**・`AnalysisResult`/`AnalysisCoefficient`/`AnalysisResultEntity` のコンストラクタ拡張に伴う既存生成箇所の機械的更新を許容できるか。
- **重要な変更ポイント**:
  1. **Flyway V0.3.8**: industry.cost_of_equity（NOT NULL DEFAULT 0.08）＋ analysis_result.rim_value（NULL）。
  2. `IndustryEntity` に `costOfEquity` 追加（⑤の convenience コンストラクタを null 補完で維持）。`AnalysisCoefficient` を op/curr/**r** の3係数に拡張。`resolveCoefficient` が r も解決。
  3. `AnalysisResult`: `rimValue` フィールド＋ `calculateRimValue(bps, roe, r)`（`bps × roe/100 ÷ r`、null/ゼロ/負ガードで Optional）。`AnalysisResult` の各コンストラクタ・`of()`・`of(entity)` に rimValue を伝播（既存 `new AnalysisResult(...)` 生成箇所・テストを機械的更新）。
  4. `AnalysisResultEntity`（@Value）に `rimValue` 追加＋ `of(...)` 引数追加。`AnalysisResultSpecification.insert` で受け渡し。`@Insert`（全項目）で新列を保存。
  5. 表示: `CorporatePresenter`（or `ViewCorporateInteractor`）で最新 RIM・合意度を解決し model 属性追加。`corporate-v2.html` に RIM理論株価カード＋合意度（desktop/mobile）。
- **確認してほしい観点**:
  1. **前向きのみ**: 既存銘柄は当面 RIM「-」・合意度は企業価値モデルのみ。新規分析から RIM が入る。
  2. ROE が % 保存である点（RIM=BPS×ROE/100÷r）。四半期報告は ROE 未算出 → RIM「-」。
  3. 合意度の定義（理論株価>株価で割安1票、n/2）。RIM 無し時の表示。

### 重点観点

#### 影響範囲分析（参照層 / 状態層 / データ層）
- **参照層（広め）**:
  - `AnalysisCoefficient`: 2→3係数（r追加）。生成箇所＝`resolveCoefficient`、テスト（`AnalysisResultTest`/`IndustrySpecificationTest`/`AnalyzeInteractorTest` の `new AnalysisCoefficient(...)`）を機械的に3引数化。
  - `AnalysisResult`: `rimValue` 追加。7引数コンストラクタ・3引数コンストラクタ・`of()`・`of(entity)` と全 `new AnalysisResult(...)` 生成箇所（本番＋テスト）を更新。
  - `AnalysisResultEntity`: `rimValue` 追加。`of()`・`insert` 経路・`AnalysisResult.of(entity)` のマッピング更新。
  - `IndustryEntity`: `costOfEquity` 追加（⑤ convenience コンストラクタを null 補完で維持）。`SELECT *` 自動マップ。
  - 表示: `CorporatePresenter`/`corporate-v2.html`（加算）。
- **状態層**: なし。
- **データ層**: **スキーマ2列追加**。industry 既存行は DEFAULT 0.08（企業価値モデルには無影響、RIM 用）。analysis_result.rim_value は既存行 NULL・新規分析のみ設定（前向き）。backfill なし。

#### インフラ影響チェック
- スキーマ変更と移行: 該当。industry（約40行）へ NOT NULL DEFAULT 列、analysis_result（多数行）へ NULL 列追加。NULL 列追加は既存行に即時・移行不要。H2/MySQL とも ALTER 可。`release/config` 変更不要。
- 新規外部連携・バッチ・依存追加（J.1）: なし。RIM は分析時の算出（既存 `AnalyzeInteractor` 内）。ADR: 2モデル併記の設計判断を一文記載検討。

#### 品質設計の三本柱
| 柱 | 方針 |
|---|---|
| **テスト戦略** | ユニット（JUnit5標準のみ）。`calculateRimValue`（正常・ROE%換算・r別・ROE負/0・BPS/ROE/r null → Optional.empty）、`resolveCoefficient`（r を含む解決）、`AnalysisResult` の rimValue 伝播、合意度ロジック（2/1/0票・RIM無し時）、`AnalysisResultEntity` 新列マッピング（H2 結合）。改修する既存テスト（コンストラクタ拡張）は計画承認のうえ機械的更新。カバレッジ80%以上。 |
| **セキュリティ方針** | 参照系＋マスタ/分析の数値のみ・外部入力なし・シークレット非該当。Doma パラメータ化。影響軽微。 |
| **ドキュメント計画** | 本 md。CLAUDE.md に RIM・合意度・cost_of_equity を追記。drawio ER 図に industry.cost_of_equity / analysis_result.rim_value 追記。 |

#### スコープ確定（再掲）
コア=RIM算出・永続化（前向き）・r業種列・合意度表示／後回し=既存RIM再計算・成長項・index展開／対象外=企業価値式変更・CAPM・再分析仕様変更。

### レビュアー記入欄

- 承認者: iori-oiso（人間レビュア）
- レビュー依頼日: 2026-06-04
- 回答日: 2026-06-05
- 結論: **合格**
- コメント: RIM永続化(前向き)・業種別r(industry列)・銘柄詳細の2モデル合意度・スキーマ2列追加・コンストラクタ拡張の波及を承認。

## ステップ4: テスト設計

### テストケース（自然言語）
**新規: `AnalysisResult.calculateRimValue`（または同等の算出）**
1. BPS=1000・ROE=12(%)・r=0.08 → RIM=1500（BPS×0.12÷0.08）。
2. r が異なると比例して変わる（r=0.10 → 1200）。
3. ROE が 0 または負（赤字）→ RIM は算出しない（Optional.empty／「-」）。
4. BPS が null / ROE が null（四半期）/ r が null・0 → Optional.empty。

**新規: `IndustrySpecification.resolveCoefficient`（r 拡張）**
5. 業種行の cost_of_equity を含む3係数を返す。

**新規: `AnalysisResult` 伝播**
6. 3引数コンストラクタで rimValue がセットされ、`of(entity)` で entity.rim_value がマッピングされる。

**新規: 合意度ロジック（`CorporatePresenter` or 算出ヘルパ）**
7. 企業価値・RIM とも株価超 → 2/2。片方のみ → 1/2。両方以下 → 0/2。RIM 無し → 企業価値のみ（1/1 or 0/1）で表示。

**新規: `AnalysisResultEntity` 新列マッピング（H2 結合）**
8. `@SpringBootTest`（V0.3.8 適用）で rim_value/cost_of_equity が読める。

**改修: 既存テスト（機械的）**
- `new AnalysisCoefficient(...)`（2→3引数）、`new AnalysisResult(...)`（rimValue 追加）、`AnalysisResultEntity.of(...)` 呼び出しの引数追加。

## Gate 2: 完了条件の確認

### 運用ルート
**インライン**（複数ファイル・コンストラクタ拡張波及のため省略不可）。

### 完了条件（機能）
- [ ] Flyway V0.3.8: industry.cost_of_equity（NOT NULL DEFAULT 0.08）＋ analysis_result.rim_value（NULL）
- [ ] `IndustryEntity` に cost_of_equity 追加（convenience コンストラクタ維持）
- [ ] `AnalysisCoefficient` を op/curr/r の3係数に拡張・`resolveCoefficient` で r 解決
- [ ] `AnalysisResult` に rimValue 算出（BPS×ROE/100÷r・null/0/負ガード）＋全コンストラクタ/of/of(entity) 伝播
- [ ] `AnalysisResultEntity` に rimValue 追加・of/insert 伝播
- [ ] `/v3/corporate` に RIM理論株価＋合意度（割安票数/2、desktop/mobile）
- [ ] CLAUDE.md・ER図 更新

### 完了条件（テスト）
- [ ] 上記テストケース1〜8（JUnit5標準のみ）／既存改修以外は無変更／全緑・カバレッジ80%以上
- [ ] dev 実機で V0.3.8 適用・起動・銘柄詳細に RIM/合意度表示を確認（Gate3。一時 seed で新規分析 or rim_value 投入）

### 完了条件（ドキュメント）
- [ ] 本 md・CLAUDE.md・drawio ER 図

### スコープ外
- 既存 analysis_result の RIM 再計算、成長項つき RIM、index への合意度展開、RIM の zスコア化

### レビュアー記入欄
- 承認者: iori-oiso（人間レビュア）
- レビュー依頼日: 2026-06-04
- 回答日: 2026-06-05
- 結論: **合格**（インライン承認）

## ステップ5・6: 実装と多軸検証

### 実装サマリ
| ファイル | 変更 |
|---|---|
| `db/migration/V0.3.8__add_rim.sql` | 新規。industry.cost_of_equity（NOT NULL DEFAULT 0.08）＋ analysis_result.rim_value（FLOAT NULL） |
| `config/AnalysisCoefficient` | op/curr/r の3係数化（+2引数互換コンストラクタ） |
| `entity/master/IndustryEntity` | cost_of_equity 追加（convenience コンストラクタ維持） |
| `specification/IndustrySpecification` | resolveCoefficient が r も解決（null除外） |
| `value/AnalysisResult` | calculateRimValue（BPS×ROE/100÷r・null/0/負ガード）＋rimValue 伝播（+7引数互換コンストラクタ） |
| `entity/transaction/AnalysisResultEntity` | rimValue 追加（@AllArgsConstructor明示＋13引数互換コンストラクタ） |
| `specification/AnalysisResultSpecification` | insert で rimValue 受け渡し |
| `web/.../AnalysisResultViewModel` | rimValue 追加 |
| `web/presenter/CorporatePresenter` | setRimAndAgreement（最新RIM＋合意度を model 属性化） |
| `corporate-v2.html` / `fragments/tooltip.html` | RIM理論株価＋2モデル合意度カード、用語2件追加 |
| CLAUDE.md | RIM・合意度・cost_of_equity を追記 |

### テスト結果
- `./mvnw test -DexcludedGroups=playwright` → **747 件全緑・回帰なし**（@SpringBootTest が V0.3.8 適用・Entity拡張OK）。互換コンストラクタにより既存 50+ 生成箇所は無変更。
- 新規: `calculateRimValue`（正常・r別・ROE0/負・null/0）、`resolveCoefficient`（r含む）、`CorporatePresenter` 合意度（rimValue/agreement 属性）。

### 多軸検証（観点別）
| 観点 | 結果 |
|---|---|
| コード品質（code-reviewer） | CRITICAL/HIGH なし。RIM式（ROE%換算）・合意度（total=1フォールバック）・前向きのみ・互換コンストラクタ×Doma整合・null安全 を確認。MEDIUM 反映: IndustryDao コメント更新・新規テスト var/FQN 解消。Lombok→Record は repo方針(Lombok維持)により据え置き。 |
| テスト構造・機能完全性 | 完了条件テスト網羅。互換コンストラクタで既存テスト無変更。 |
| セキュリティ（security-reviewer） | 本変更起因の重大リスクなし（LOW）。内部算出・表示のみ・XSS は th:text・Doma パラメータ化。LOW指摘の**負の資本コストガード**を反映（`signum()<=0`）。FLOAT 型は既存 bps/roe 等と統一のため据え置き。 |
| ドキュメント整合性 | CLAUDE.md・本 md 更新。drawio ER 図は別途更新予定。 |

## Gate 3: 最終確認

### レビュアー向けサマリ
- **判断してほしいこと**: RIM永続化（前向き）＋業種別r＋銘柄詳細の2モデル合意度が利用者視点で妥当か。既存値不変か。
- **重要な変更ポイント**:
  1. RIM理論株価＋2モデル合意度を /v3/corporate に表示。
  2. スキーマ2列追加（既存行 RIM は NULL・前向き）。
  3. 互換コンストラクタで既存生成箇所・既存テスト無変更。
- **確認してほしい観点**:
  1. 既存銘柄は当面 RIM「-」（合意度は企業価値モデルのみ）。新規分析から RIM。
  2. 既存企業価値・他指標が不変であること。

### 重点観点
#### 差分レビュー
本番10ファイル＋マイグレーション1＋テスト4＋CLAUDE.md。互換コンストラクタで既存50+生成箇所は無変更。

#### 動作確認結果（dev 実機起動）
- **V0.3.8 マイグレーション適用・起動成功**（`Migrating ... 0.3.8`／`Started`）。Entity拡張・Bean構成OK。
- 実値デモ（一時 seed `V1.0.4`・コミットせず撮影後削除）で 90010 の `rim_value=1500` を投入 → `/v3/corporate?code=9001` に **RIM理論株価 1500.0（GOOD）＋2モデル合意度 2 / 2 割安（GOOD）** を表示（企業価値3000・株価1280・RIM1500 がいずれも株価超）（[スクショ](T20260604-attachments/gate3-corporate-rim.png)）。一時 seed 削除済み（git 痕跡なし）。
- 既存の最新企業価値・割安度・グレアム指数は従来どおり（既存値不変）。

#### 副次影響
スキーマは industry へ NOT NULL DEFAULT 列・analysis_result へ NULL 列追加（既存行 RIM は NULL）。他画面・他指標は不変。

#### ドキュメント整合性
CLAUDE.md・本 md 更新済み。

### レビュアー記入欄
- 承認者: iori-oiso（人間レビュア）
- レビュー依頼日: 2026-06-05
- 回答日: 2026-06-07
- 結論: **合格**
- コメント: dev 実機で RIM理論株価1500.0＋2モデル合意度2/2、既存値不変を確認。レビュー指摘（負の資本コストガード）反映済み。追加要望でRIM未算出時はGOOD/BADバッジを非表示（中立）に調整（9002でRIM「-」・バッジなしを確認）。前向きのみ・スキーマ2列追加を承認。

## 更新履歴

- 2026-06-04: 初版作成（認識合わせ: 無成長RIM／業種別r／永続化(B)／銘柄詳細表示・合意度n/2）。Gate1 承認待ち。
