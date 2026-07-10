# T20260705-1 — valuation 廃止 Phase 1（アクションを /v3/index へ移設）

## タスク要約（1 行）
株価評価画面の2アクション（株価取得・全件評価）を会社一覧 `/v3/index` の取得エリアへ移設し、valuation 依存を切る（valuation 画面自体は Phase 2 で削除）。

## Gate 進捗 早見表
| Gate | 状態 | 承認者 | 回答日 |
|---|---|---|---|
| Gate 1: 設計の承認 | 合格（計画 valuation-retirement.md をユーザー承認） | iori-oiso | 2026-07-05 |
| Gate 2: 最終確認   | 合格 | iori-oiso | 2026-07-05 |

## 影響範囲タイプ 早見
- 参照層: **該当**（`index-v2.html` にフォーム追加、`AnalysisController` のリダイレクト先変更、`AnalysisControllerTest` 更新）
- 状態層: **該当なし**（集計・取得ロジックは不変。呼び出し経路のみ）
- データ層: **該当なし**
- インフラ影響: **すべて該当なし**

## 関連
- 計画: `docs/plans/valuation-retirement.md`（承認済み）
- 後続: Phase 2（valuation 削除）

## Gate 1: 設計の承認
### レビュアー向けサマリ
- **判断してほしいこと**: アクション移設方式（index 取得エリアへ・リダイレクト先変更）の承認 → **計画承認により合格扱い**。
- **重要な変更ポイント**:
  - `/v3/index` 取得エリアに「株価を取得（提出日指定）」`POST /v2/import/stock/date` と「株価を評価（全件）」`POST /v2/evaluate`（code無）を追加。
  - `AnalysisController.importStockBySubmitDate` / `evaluate`(code無) のリダイレクトを `/v3/valuation` → `/v3/index`。
  - valuation 画面のフォームは残置（Phase 2 で画面ごと削除）。
- **確認してほしい観点**: index 画面の取得エリアの体裁、全件評価の実行導線。

### レビュアー記入欄
| 項目 | 内容 |
|---|---|
| 承認者 | iori-oiso |
| レビュー依頼日 | 2026-07-05 |
| 回答日 | 2026-07-05 |
| **結論** | **合格**（計画承認による） |
| **コメント** | 計画 valuation-retirement.md を承認。§8 既定（旧URLリダイレクト/valuationフォーム残置/配当非スコープ/index取得エリア）で進行。 |

## Gate 2: 最終確認
### レビュアー向けサマリ
- **判断してほしいこと**: index からの株価取得・全件評価が動作し、valuation の他機能に影響がないか
- **重要な変更ポイント**: （実装後に記載）
- **確認してほしい観点**: （実装後に記載）
### レビュアー記入欄
| 項目 | 内容 |
|---|---|
| 承認者 | iori-oiso |
| レビュー依頼日 | 2026-07-05 |
| 回答日 | 2026-07-05 |
| **結論** | **合格** |
| **コメント** | AnalysisControllerTest 7 / Playwright 12 PASS。フォームは index 取得メニューに移設・リダイレクト /v3/index。コミット承認。 |

---

<details>
<summary>📁 AI 作業ログ</summary>

### ステップ 1: 把握
- 移設元: `valuation-v2.html:19-43` の2フォーム。POST 先 `/v2/import/stock/date`（fromToDate）・`/v2/evaluate`（code無=全件）。
- 移設先: `index-v2.html` 取得エリア（既存の財務諸表取得フォーム等が並ぶ場所）。
- リダイレクト: `AnalysisController.java` importStockBySubmitDate(:130-145) / evaluate code無(:168-190) の `redirect:/v3/valuation` を `/v3/index` に。code有 evaluate（corporate）は不変。
- テスト: `AnalysisControllerTest.java:82`（importStock1 の redirect 期待）を `/v3/index` に更新。

### ステップ 5: 多軸検証
- 変更: index-v2.html（取得メニューに2フォーム移植・既存カード様式）／AnalysisController（株価取得・全件評価のリダイレクトを /v3/index、code有 corporate は不変）／AnalysisControllerTest（redirect 期待更新＋全件 evaluate 確認追加）。
- テスト（Java 17）: AnalysisControllerTest 7/0、Playwright Phase8 12/12。valuation 画面・他機能は無影響（残置）。
- Codex 委譲＋Claude 検証（Codex 側は JAVA_HOME が Java8 に解決されテスト未通過だったため Claude が Java17 で再検証）。

### コミット・検証履歴
- 環境: `JAVA_HOME=openjdk@17` 必須。
</details>

## 更新履歴
| 日付 | 概要 |
|---|---|
| 2026-07-05 | 起票。計画承認により Gate 1 合格。Phase 1 実装着手。 |
