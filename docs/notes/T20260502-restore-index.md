# T20260502 — /v3/index 機能復元タスク（優先度 3）

## 課題（1 行）

画面刷新 Phase 3（commit 05959502）で `/v3/index` を移植した際、旧 `/index` にあった **データテーブルカラム 8→4 に削減**（変動係数／株価平均／割安比率／対象年数 が消失）、**「財務諸表の取得と分析をする」フォーム（POST `/v1/document/analysis`）**、**「書類IDから取得と分析する」フォーム（POST `/v1/scrape/id`）**、**みんかぶ外部リンク** が消失している。`fragments/index-table.html` と `index-v2.html` を旧版相当に拡張する。

## 関連タスク

- 親: Phase 3（commit 05959502）
- 並走優先度: 1=edinet-detail / **2=corporate** / 3=本タスク / 4=valuation / 5=edinet-list

---

## ステップ 1：把握・整理

### スコープ確定

| 区分 | 内容 |
|---|---|
| **コア** | ① テーブルカラム 4 → 8 に拡張（変動係数 `coefficientOfVariationToDisplay` / 株価平均 `averageStockPrice` / 割安比率 `discountRateToDisplay` / 対象年数 `countYear` を追加）／② フォーム「財務諸表の取得と分析をする」（POST `/v1/document/analysis` + 日付レンジ `fromToDate`）／③ フォーム「書類IDから取得と分析する」（POST `/v1/scrape/id` + `documentId`）／④ 証券コードのみんかぶリンク（`https://minkabu.jp/stock/{code}` target=_blank） |
| **後回し** | DataTables の export（PDF/Excel/CSV/Print/colvis）— Tailwind では別途 export ライブラリ要検討。htmx ソート + ページネーションは既に動作 |
| **対象外** | バックエンド変更／旧 v2 endpoint 復活 |

### バックエンド endpoint（生存確認）

- `POST /v1/document/analysis` — 財務諸表取得・分析
- `POST /v1/scrape/id` — 書類 ID から取得・分析

---

## ステップ 3：影響設計（★Gate 1）

### 3.2 影響範囲分析

| 層 | 影響 |
|---|---|
| 参照層 | `fragments/index-table.html`（89 → 約 130 行）/ `index-v2.html`（58 → 約 90 行） |
| 状態層 | 該当なし |
| データ層 | 該当なし |

### 3.3 インフラ影響チェック

すべて該当なし。

### 3.4 品質設計の三本柱

| 柱 | 確認結果 |
|---|---|
| テスト戦略 | 手動目視 + Phase 8 Playwright 8/8 PASS 維持 |
| セキュリティ方針 | POST フォームは既存 endpoint。`th:href` の URL 組み立てに XSS リスクなし（コードは数値） |
| ドキュメント計画 | 本 md §6 |

### Gate 1：人間の承認

- [x] **承認者**: iori-oiso
- [x] **承認日**: 2026-05-02

---

## ステップ 4：テスト設計

### テストケース

| # | ケース | 期待結果 |
|---|---|---|
| 1 | `/v3/index` を開く | テーブルが 8 列（コード/会社名/提出日/最新企業価値/変動係数/株価平均/割安比率/対象年数）|
| 2 | 「財務諸表の取得と分析をする」フォーム表示 | 日付レンジピッカー + Submit ボタン |
| 3 | 同フォーム送信 | POST `/v1/document/analysis` 実行 |
| 4 | 「書類IDから取得と分析する」フォーム表示 | テキスト input + Submit ボタン |
| 5 | 同フォーム送信 | POST `/v1/scrape/id` 実行 |
| 6 | 証券コード（例 9001）クリック | みんかぶリンク `https://minkabu.jp/stock/9001` が新規タブで開く（または `/v3/corporate?code=9001` への内部リンクと併存） |

### 既存 Playwright

引き続き 8/8 PASS（HTML 構造アサートのみ）。

### Gate 2

- [x] **承認者**: iori-oiso
- [x] **承認日**: 2026-05-02

---

## ステップ 5：実行サイクル

1. fragments/index-table.html: 4 列追加（変動係数/株価平均/割安比率/対象年数）
2. index-v2.html: 上部に 2 フォームカード追加
3. みんかぶ外部リンク追加（既存 corporate リンクの隣 or 置換）

---

## ステップ 6：多軸検証（★Gate 3）

- [ ] **承認者**: iori-oiso
- [ ] **承認日**: ____
- [ ] 旧版相当のカラム数・フォーム機能の確認

---

## §6 検証結果（実装後に追記）
