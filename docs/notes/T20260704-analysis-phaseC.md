# T20260704C — 評価分析ダッシュボード Phase C（指標の分布・業種間比較）

## タスク要約（1 行）
割安度・グレアム指数の母集団分布（ヒストグラム）と業種別割安度ランキングを `/v3/analysis` の分布タブに実装し、既存 `/v3/valuation` の「業種別」ビューを統合削除する。

## Gate 進捗 早見表
| Gate | 状態 | 承認者 | 回答日 |
|---|---|---|---|
| Gate 1: 設計の承認 | 合格 | iori-oiso | 2026-07-04 |
| Gate 2: 最終確認   | 合格 | iori-oiso | 2026-07-04 |

## 影響範囲タイプ 早見
- 参照層: **該当**（追加: 分布集計ロジック・`web/view/model/analysis/*` record・`fragments/analysis-distribution.html`・`AnalysisPresenter` に分布エンドポイント・`app.js` ヒストグラム。**削除**: 既存 industry ビュー一式）
- 状態層: **該当なし**（読み取り集計のみ。必要なら Caffeine キャッシュ）
- データ層: **該当なし**（既存 `valuation_view` / `industry` / `company` 読み取り。スキーマ変更なし）
- インフラ影響: **すべて該当なし**（新規依存なし）

## 関連
- 事前計画: `docs/plans/valuation-app-analysis-dashboard.md`（§Phase C）
- 前提: Phase A/B 完了（`feature/v3-analysis-phaseA`）
- プロトタイプ: `T20260704-analysis-phaseC-attachments/prototype-distribution.html`（Artifact: https://claude.ai/code/artifact/06b14f29-8a45-4eba-8a3e-3f0575da83d0 ）

---

## Gate 1: 設計の承認

### レビュアー向けサマリ
- **判断してほしいこと**: 分布タブの設計と、**既存 `/v3/valuation` 業種別ビューの削除**を承認してよいか。
- **重要な変更ポイント**:
  - `/v3/analysis` 分布タブに: ①割安度ヒストグラム ②グレアム指数ヒストグラム ③業種別割安度ランキング（**中央値＋平均**、件数）。
  - 母集団は `findAllCompanyValuationView`（`valuation_view` 全件）、業種集計は `findCompanyValuationViewList(industryId)` + `generateIndustryValuationView`（中央値対応に拡張）、`computeGrahamIndustryZScore` を分布に接続。
  - **既存 industry ビューの統合削除**（重複解消）: `ValuationPresenter` の industry 分岐 / `ViewService.findIndustryValuationTable` / `ViewValuationInteractor.viewIndustryValuation`・`getIndustryValuationView` / `IndustryValuationViewModel`・`IndustryValuationTablePage` / `valuation-v2.html` の業種別タブ / `fragments/valuation-table.html` の `industry-table`。
  - 集計値のヒストグラム化・ビン設計は純粋関数＋`app.config.analysis`（ビン数）。
- **確認してほしい観点**:
  - **削除による /v3/valuation の回帰**（業種別タブ以外が壊れないこと。`ValuationPresenterTest`・Playwright valuation）。
  - 業種別ビューの削除範囲（完全削除でよいか＝ユーザー既回答「既存は削除予定」）。
  - ヒストグラムのビン設計・分布の見せ方。

### 重点観点
- 影響範囲分析（参照/状態/データ）の網羅（**削除を含む**）
- 三本柱（テスト戦略 / セキュリティ方針 / ドキュメント計画）
- スコープ確定（コア=分布＋業種ランキング / 後回し=高度な統計 / 対象外=投資UX）
- 完了条件（機能 / テスト / ドキュメント / スコープ外）＋**削除回帰**

### レビュアー記入欄
| 項目 | 内容 |
|---|---|
| 承認者 | iori-oiso |
| レビュー依頼日 | 2026-07-04 |
| 回答日 | 2026-07-04 |
| **結論** | **合格** |
| **コメント** | 分布タブ設計＋既存業種別ビューの完全削除を承認。実装（Codex委譲・削除回帰確認込み）へ。 |

---

## Gate 2: 最終確認

### レビュアー向けサマリ
- **判断してほしいこと**: 分布が正しく描画されるか / 既存 valuation 画面に副次影響（業種別削除の巻き込み）がないか
- **重要な変更ポイント**: 分布タブ（ヒストグラム＋業種別割安度ランキング）新設、既存 industry ビュー完全削除、graham z-score 保持、ViewService 単一コンストラクタ維持
- **確認してほしい観点**: 全テスト844 PASS・削除回帰なし、/v3/valuation 健全（業種別除去後）、分布描画

### 重点観点
- 差分レビュー / 動作確認結果 / 副次影響（削除回帰） / ドキュメント整合性

### レビュアー記入欄
| 項目 | 内容 |
|---|---|
| 承認者 | iori-oiso |
| レビュー依頼日 | 2026-07-04 |
| 回答日 | 2026-07-04 |
| **結論** | **合格** |
| **コメント** | 全テスト844 PASS・削除回帰なし・分布描画確認。コミット承認。 |

---

<details>
<summary>📁 AI 作業ログ（ステップ 1〜5）</summary>

### ステップ 1: 把握・整理
- 流用: `ViewSpecification.findAllCompanyValuationView`（母集団）/ `findCompanyValuationViewList(industryId)`（業種内リスト）/ `generateIndustryValuationView`（業種集計・中央値対応に拡張）/ `IndustrySpecification.inquiryIndustryList`+`isTarget` / `ViewValuationInteractor.computeGrahamIndustryZScore`（純粋関数の先例・z スコア接続）。
- 削除対象（既存 industry ビュー）: `ValuationPresenter` industry 分岐 / `ViewService.findIndustryValuationTable` / `ViewValuationInteractor.viewIndustryValuation`・`getIndustryValuationView` / `IndustryValuationViewModel`・`IndustryValuationTableQuery`・`IndustryValuationTablePage` / `valuation-v2.html` 業種別タブ / `fragments/valuation-table.html` industry-table。削除時は valuation 画面の他ビュー（stock/submit/graham-index/dividend-yield）とお気に入り等が壊れないこと。
- スコープ: コア=分布ヒストグラム＋業種別割安度ランキング / 後回し=高度な統計補正 / 対象外=投資UX。

### ステップ 2: プロトタイピング（合意済み 2026-07-04）
- 分布タブのモックを Artifact で提示・合意。
- **合意結果**: UI=3要素（割安度ヒストグラム・グレアムヒストグラム・業種別割安度ランキング）。業種列=割安度中央値/平均・グレアム中央値・件数（中央値降順）。既存 industry ビューは**完全削除して統合**。

### ステップ 3: 設計
#### 3.2 影響範囲分析
- 参照層=該当。**追加**: `DistributionUseCase`/`DistributionInteractor`、純粋 `DistributionCalculator`（ヒストグラム/中央値/業種集計）、`web/view/model/analysis/*` record、`fragments/analysis-distribution.html`、`AnalysisPresenter` 分布エンドポイント、`app.js` ヒストグラム、`ViewService` 委譲。**削除**: `ValuationPresenter` industry 分岐 / `ViewService.findIndustryValuationTable` / `ViewValuationInteractor.viewIndustryValuation`・`getIndustryValuationView`（および `ViewValuationUseCase` の対応メソッド）/ `IndustryValuationViewModel`・`IndustryValuationTableQuery`・`IndustryValuationTablePage` / `valuation-v2.html` 業種別タブ / `fragments/valuation-table.html` industry-table。
- 状態層=該当なし（読み取り集計。必要なら `@Cacheable("distribution")`）。
- データ層=該当なし（`valuation_view`/`industry`/`company` 読み取り、スキーマ変更なし）。

#### 3.3 集計アルゴリズム（純粋関数中心）
- 母集団 = `ViewSpecification.findAllCompanyValuationView()`（valuation_view 全件）。
- ヒストグラム: `DistributionCalculator.histogram(values, binBoundaries)` で割安度(discountRate×100)・グレアム指数それぞれの度数分布。ビン境界は `app.config.analysis`。
- 業種別ランキング: `IndustrySpecification.inquiryIndustryList()`+`isTarget` で対象業種 → 各業種 `findCompanyValuationViewList(industryId)` → 割安度の**中央値/平均**・グレアム中央値・件数。`MIN_INDUSTRY_SIZE=3` 未満は除外。中央値降順。中央値は純粋関数化。
- z スコア接続は補助（`computeGrahamIndustryZScore` を必要時に流用、UI 主表示は中央値ランキング）。

#### 3.4 実行方式・三本柱
- htmx 遅延ロード（分布タブ初回クリックで `/v3/analysis/distribution` を取得）。集計は母集団1回読みで軽量、必要なら `@Cacheable`。
- テスト: `DistributionCalculator`（histogram/median/業種集計）をユニット。`DistributionInteractor` は specification モック。**削除回帰**: `ValuationPresenterTest`・`ViewValuationInteractorTest`・`ViewServiceTest` から industry 関連ケースを除去/調整し全 PASS を維持。Playwright valuation 画面（業種別タブ消失後）健全。カバレッジ80%。
- セキュリティ: 認証必須、新規入力なし。ドキュメント: アプリ構成図.drawio 更新（DistributionUseCase 追加・industry ビュー削除）。

### ステップ 4: 実行サイクル
- 実装順: ①`DistributionCalculator`＋ユニット → ②`DistributionUseCase`/`Interactor`＋ViewService＋config → ③Presenter/fragment/タブ配線/app.js → ④**既存 industry ビュー削除**＋回帰テスト調整。各バッチ Java 17 で検証。

### ステップ 5: 多軸検証
- **実装バッチ**: ①DistributionCalculator（純粋）②DistributionUseCase/Interactor＋ViewService＋設定 ③表示層（Presenter/fragment/タブ配線/ヒストグラムJS）④既存 industry ビュー削除＋テスト回帰調整。各バッチ Codex 委譲→Claude 検証。
- **テスト（Java 17）**: DistributionCalculatorTest 20／DistributionInteractorTest＋ViewServiceTest 52／削除後4クラス（ValuationPresenter12・ViewService47・ViewValuationInteractor14・ViewSpecification11）全PASS／**全テスト回帰 844/0/0**／Playwright 12/12。
- **実データ描画**: 分布タブでヒストグラム（割安度・グレアム）とサマリが描画。業種別ランキングは dev シードが1業種3社未満のため空状態を正しく表示（本番の多数銘柄で populate）。
- **削除の安全性**: 削除シンボル（IndustryValuation*/viewIndustryValuation/findIndustryValuationTable/generateIndustryValuationView/getIndustryValuationView）への **dangling 参照ゼロ**。**graham z-score 系（computeGrahamIndustryZScore 等）は保持**（誤削除なし）。/v3/valuation は業種別タブ除去後も他ビュー・お気に入り健全（Playwright）。
- **ViewService**: 単一コンストラクタ維持（DistributionUseCase を引数追加のみ）。
- **ドキュメント整合性**: アプリ構成図.drawio 更新（DistributionUseCase 追加・industry ビュー削除）は Gate 2 後の別対応。

### コミット・検証履歴（追記）
- 環境: `JAVA_HOME=openjdk@17` 必須。全テスト回帰は Gate 2 直前に 844 件 PASS で実施済み。

### 完了条件
- 機能: 分布タブ（割安度/グレアムヒストグラム＋業種別割安度ランキング）、既存 industry ビュー削除。
- テスト: `DistributionCalculator`/`DistributionInteractor` ユニット、削除回帰（valuation 関連テスト調整で全 PASS）、カバレッジ80%、Playwright で分布タブ描画＆valuation 画面健全。
- ドキュメント: タスクmd Gate2、アプリ構成図.drawio 更新。
- スコープ外（宣言）: 高度な統計補正、投資UX、ヒートマップ等の追加可視化。

### コミット・検証履歴
- 環境: `JAVA_HOME=openjdk@17` 必須。

</details>

---

## 更新履歴
| 日付 | 概要 |
|---|---|
| 2026-07-04 | 起票。Gate 1 準備（影響範囲3層・既存 industry ビュー削除範囲・流用先を記載）。 |
