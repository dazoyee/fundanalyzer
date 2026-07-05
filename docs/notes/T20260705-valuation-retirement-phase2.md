# T20260705-2 — valuation 廃止 Phase 2（/v3/valuation 削除）

## タスク要約（1 行）
株価評価画面 `/v3/valuation` と valuation 専用コード・ナビ・テストを削除し、旧 URL を `/v3/analysis` へリダイレクト。共有メソッドは残す。

## Gate 進捗 早見表
| Gate | 状態 | 承認者 | 回答日 |
|---|---|---|---|
| Gate 1: 設計の承認 | 合格（計画 valuation-retirement.md をユーザー承認） | iori-oiso | 2026-07-05 |
| Gate 2: 最終確認   | 合格 | iori-oiso | 2026-07-05 |

## 影響範囲タイプ 早見
- 参照層: **該当**（valuation 専用の Presenter/テンプレ/Service メソッド/ViewModel/テスト削除、ナビ更新、旧URLリダイレクト追加）
- 状態層: **該当なし**（新規永続化なし）
- データ層: **該当なし**
- インフラ影響: **すべて該当なし**

## 関連
- 計画: `docs/plans/valuation-retirement.md`（承認済み）
- 前提: Phase 1（アクション移設）完了（`b7d8c7f7`）

## Gate 1: 設計の承認
### レビュアー向けサマリ
- **判断してほしいこと**: valuation 削除範囲（下記）と旧URLリダイレクトの承認 → **計画承認により合格扱い**。
- **重要な変更ポイント**: valuation 専用一式の削除、共有メソッド存置、ナビ削除、旧URL→/v3/analysis リダイレクト。
### レビュアー記入欄
| 項目 | 内容 |
|---|---|
| 承認者 | iori-oiso |
| レビュー依頼日 | 2026-07-05 |
| 回答日 | 2026-07-05 |
| **結論** | **合格**（計画承認による） |
| **コメント** | 計画 valuation-retirement.md 承認。§8 既定（旧URL→/v3/analysis リダイレクト）で進行。 |

## Gate 2: 最終確認
### レビュアー向けサマリ
- **判断してほしいこと**: valuation 削除後、他画面（index/analysis/corporate/edinet）・共有機能・スケジューラが健全か
- **重要な変更ポイント**: valuation 一式削除、旧URL→/v3/analysis リダイレクト、graham z-score等の共有機能保持
- **確認してほしい観点**: 全テスト822・Playwright10 PASS、旧URL実動作確認（302→/v3/analysis）
### レビュアー記入欄
| 項目 | 内容 |
|---|---|
| 承認者 | iori-oiso |
| レビュー依頼日 | 2026-07-05 |
| 回答日 | 2026-07-05 |
| **結論** | **合格** |
| **コメント** | 全テスト822・Playwright10 PASS、旧URLリダイレクト実動作確認。コミット承認。 |

---

<details>
<summary>📁 AI 作業ログ（削除マップ）</summary>

### 削除
- `web/presenter/ValuationPresenter.java`、`templates/valuation-v2.html`、`templates/fragments/valuation-table.html`
- `ViewService`: `findCompanyValuationTable` / 引数なし `getValuationView()` / `getFavoriteValuationView()` / private `replaceGrahamWithIndustryZScore` / 関連 import・comparator（company用 comparator は残す）
- `ViewValuationUseCase` / `ViewValuationInteractor`: 引数なし `viewValuation()`・`viewFavoriteValuation()`（getValuationView()無/ getFavoriteValuationView 経由のみ）
- `web/view/model/valuation/CompanyValuationTablePage.java`, `CompanyValuationTableQuery.java`
- テスト: `ValuationPresenterTest`、`ViewServiceTest` の valuation テーブル系ケース、`SecurityConfigIntegrationTest` の /v3/valuation 疎通（/v3/analysis に差し替え）、スクショテストの valuation ターゲット

### 残す（共有・削除禁止）
- `getAllValuationView`/`viewAllValuation`、`getValuationView(code)`/`viewValuation(code)`、`getGrahamIndustryZScore(code)`/`findGrahamIndustryZScore`/`computeGrahamIndustryZScore`、`updateValuationView(*)`/`updateView`、`CompanyValuationViewModel`、`/v2/evaluate`（corporate 個社）、`AnalysisScheduler`

### 追加
- 旧URL `/v3/valuation` → `redirect:/v3/analysis`（小さな Controller/Presenter マッピング）
- ナビ削除: `layout-v2.html`（サイドバー株価評価・モバイル株価）、`corporate-v2.html` fallback ナビ株価
- CLAUDE.md 画面一覧から /v3/valuation 行削除

### ステップ 5: 多軸検証
- **削除**: ValuationPresenter/valuation-v2.html/valuation-table.html/CompanyValuationTablePage・Query/ValuationPresenterTest を削除。ViewService/ViewValuationUseCase/Interactor から valuation テーブル専用メソッド削除。ナビ（layout-v2・corporate-v2）から株価評価リンク削除。CLAUDE.md 画面表更新。
- **共有保持**: getAllValuationView/getValuationView(code)/updateValuationView/graham z-score系（computeGrahamIndustryZScore等）は grep で残存確認済み。dangling 参照ゼロ確認済み。
- **旧URL**: `AnalysisPresenter.legacyValuationRedirect()` で `/v3/valuation` → `redirect:/v3/analysis`。curlで実動作確認（302→/v3/analysis、200）。
- **テスト（Java 17）**: 全テスト回帰 822/0/0（Phase C時844から、削除した valuation テーブル系テスト分の減）。Playwright Phase8 10/10。
- **発見・修正した不具合**: Playwright `backtestTabRenders`/`distributionTabRenders` が10テスト同時実行の負荷で `waitForTimeout(1500)` 不足によりフレーキー（1回失敗、単体では1/1 PASS）。`waitForFunction` によるポーリング待機に修正し、再実行で10/10 PASS 確認（valuation削除とは無関係の品質改善）。
- **環境**: 新規 Codex スレッドが前回2時間超停滞したため `--fresh` で再委譲し成功。Claude が全検証を実施。

### コミット・検証履歴
- 環境: `JAVA_HOME=openjdk@17` 必須。
</details>

## 更新履歴
| 日付 | 概要 |
|---|---|
| 2026-07-05 | 起票。計画承認により Gate 1 合格。Phase 2 実装着手。 |
