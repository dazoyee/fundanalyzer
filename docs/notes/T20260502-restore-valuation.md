# T20260502 — /v3/valuation 機能復元タスク（優先度 4）

## 課題（1 行）

画面刷新 Phase 4（commit 3b38c81c）で `/v3/valuation` を移植した際、旧 `/valuation` にあった **「企業の株価を取得する（提出日指定）」フォーム（POST `/v2/import/stock/date`）** と **「株価を評価する」フォーム（POST `/v2/evaluate`）**、4 サブタブのカラム差分（提出日情報/グレアム指数 で日付関連カラム不足）が消失している。`valuation-v2.html` と `fragments/valuation-table.html` を旧版相当に拡張する。

## 関連タスク

- 親: Phase 4（commit 3b38c81c）
- 並走優先度: 1=edinet-detail / 2=corporate / 3=index / **4=本タスク** / 5=edinet-list

---

## ステップ 1：把握・整理

### スコープ確定

| 区分 | 内容 |
|---|---|
| **コア** | ① 「企業の株価を取得する」フォーム（POST `/v2/import/stock/date` + 日付レンジ `fromToDate`）／② 「株価を評価する」フォーム（POST `/v2/evaluate`）／③ 4 サブタブのカラム旧版相当に揃える（特に提出日情報・グレアム指数の日付関連列） |
| **後回し** | DataTables export 機能 |
| **対象外** | バックエンド変更／業種別タブの仕様変更 |

### バックエンド endpoint

- `POST /v2/import/stock/date` — 提出日指定の株価取得
- `POST /v2/evaluate` — 株価評価更新

---

## ステップ 3：影響設計（★Gate 1）

### 3.2 影響範囲分析

| 層 | 影響 |
|---|---|
| 参照層 | `valuation-v2.html`（73 → 約 110 行）/ `fragments/valuation-table.html`（243 → 約 300 行） |
| 状態層 | 該当なし |
| データ層 | 該当なし |

### Gate 1：人間の承認

- [x] **承認者**: iori-oiso
- [x] **承認日**: 2026-05-02

---

## ステップ 4：テスト設計

### テストケース

| # | ケース | 期待結果 |
|---|---|---|
| 1 | `/v3/valuation` 上部に 2 フォームカード表示 | 株価取得フォーム + 評価フォーム |
| 2 | 「企業の株価を取得する」フォーム送信 | POST `/v2/import/stock/date` 実行 |
| 3 | 「株価を評価する」フォーム送信 | POST `/v2/evaluate` 実行 |
| 4 | 株価タブ（view='stock'）で 6 列表示 | code/name/date/price/difference/ratio |
| 5 | 提出日情報タブ（view='submit'）で 6 列表示 | code/name/date/avgStockPrice/grahamIndex/corporateValue |
| 6 | グレアム指数タブ（view='graham-index'）で 6 列表示 | code/name/date/grahamIndex/submitDate/grahamIndexAtSubmit |
| 7 | 配当利回りタブ（view='dividend-yield'）で 5 列表示 | code/name/date/stockPrice/dividendYield |

### Gate 2

- [x] **承認者**: iori-oiso
- [x] **承認日**: 2026-05-02

---

## ステップ 5：実行サイクル

1. valuation-v2.html: 上部に 2 フォームカード
2. fragments/valuation-table.html: 各 view fragment のカラム数を旧版に合わせる

---

## ステップ 6：多軸検証（★Gate 3）

- [ ] **承認者**: iori-oiso
- [ ] **承認日**: ____

---

## §6 検証結果（実装後に追記）
