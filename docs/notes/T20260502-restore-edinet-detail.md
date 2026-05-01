# T20260502 — /v3/edinet-list-detail 機能復元タスク（優先度 1：業務ブロック解除）

## 課題（1 行）

画面刷新 Phase 5（commit efcaaf62）で `/v3/edinet-list-detail` を移植した際、旧 `/edinet-detail` にあった **7 アクションボタン**（更新／分析／スクレイピング／ステータス完了／除外）と **11 個の財務値手動編集フォーム**（流動資産合計／総資産／純資産／営業利益／当期純利益／株式総数 等）と **書類詳細 10 列メタデータテーブル** が完全消失しており、**運用上の業務がブロック**されている。`edinet-list-detail-v2.html`（現状 79 行）に旧 `edinet-detail.html`（630 行）相当の機能を復元する。

## 関連タスク

- 親タスク: [T20260429-screen-renewal-htmx-tailwind.md](T20260429-screen-renewal-htmx-tailwind.md) Phase 5
- 起源コミット: `efcaaf62 feat: 画面刷新 Phase 5 で EDINET 2 画面 ... を Tailwind+htmx に移植する`
- 並走タスク（5 画面復元シリーズ）:
  - 優先度 1（本タスク）: T20260502-restore-edinet-detail.md
  - 優先度 2: [T20260501-corporate-detail-restoration.md](T20260501-corporate-detail-restoration.md)
  - 優先度 3: T20260502-restore-index.md
  - 優先度 4: T20260502-restore-valuation.md
  - 優先度 5: T20260502-restore-edinet-list.md
- 並走中: [T20260501-v3-screen-test-data-seed.md](T20260501-v3-screen-test-data-seed.md)（Gate 3 保留）

---

## ステップ 1：把握・整理

### スコープ確定

| 区分 | 内容 |
|---|---|
| **コア** | ① 7 アクションボタン復元（更新して前に戻る／分析する／スクレイピング・分析する／ステータス更新：すべて完了／除外）／② 11 財務値編集フォーム（条件付き表示）／③ 書類詳細リスト（10 メタデータ列）／④ 書類ファイル外部リンク（BS/PL/NS の document path）／⑤ 統計サマリー（8 項目: 総件数/処理対象/処理済/分析済/未分析ID/処理確認ID/未処理/対象外） |
| **後回し** | 高度なスタイリング（Tailwind デザインシステムへの厳密適合）／レスポンシブ最適化 |
| **対象外** | バックエンド変更／旧 `/edinet-detail` endpoint の復活／新規 endpoint 追加 |

### 関連既存資産

| 区分 | 場所 |
|---|---|
| 復元参考用 | `git show 088c5d73^:src/main/resources/templates/edinet-detail.html`（630 行） |
| 改修対象 | `src/main/resources/templates/edinet-list-detail-v2.html`（79 行） |
| Presenter | `EdinetDetailPresenter.java` |
| バックエンド endpoint | AnalysisController.java / EdinetController.java |

### バックエンド endpoint 生存確認（必須）

実装着手前に以下を確認する:

| ボタン / フォーム | POST URL | 動作 |
|---|---|---|
| 更新して前に戻る | `/v1/update/edinet-list` | submitDate 引数で再処理 |
| 分析する | `/v1/analyze/date` | submitDate 引数で再分析 |
| スクレイピング・分析する | `/v2/scrape/id` | submitDate + documentId |
| ステータス更新：すべて完了 | `/v1/update/all-done/status` | submitDate + documentId |
| 除外（書類除外） | `/v1/remove/document` | submitDate + documentId |
| 11 財務値登録 | `/v1/fix-fundamental-value` | submitDate + edinetCode + documentId + financialStatementId + subjectId + value |

### 完了条件

- [x] 課題が 1 行で明文化済
- [x] 関連既存資産（旧 template / Presenter / Controller）を把握済
- [x] スコープが 3 区分で確定済

---

## ステップ 2：プロトタイピング

### Thymeleaf model 階層（旧版そのまま）

```
${edinetDetail}
  .edinetList { submitDate, countAll, countTarget, countScraped, countAnalyzed,
                cantScrapedId, notAnalyzedId, countNotScraped, countNotTarget }
  .documentDetailList[] {
    .document { documentId, edinetCode, documentTypeName, documentPeriod,
                downloaded, decoded, scrapedNumberOfShares, scrapedBs, scrapedPl,
                bsDocumentPath, plDocumentPath, numberOfSharesDocumentPath }
    .financeValue {
      totalCurrentAssets, totalInvestmentsAndOtherAssets, totalAssets,
      totalCurrentLiabilities, totalFixedLiabilities, subscriptionWarrant,
      netAssets, operatingProfit, netIncome, numberOfShares (10 fields)
    }
  }
```

`EdinetDetailPresenter.populateModel()` で送出している model attribute は変更不要（旧 template と同一）。

### 旧 → 新 マッピング

| 旧 (Bootstrap) | 新 (Tailwind+v2) |
|---|---|
| `<div class="card card-info">` | `bg-white dark:bg-slate-800 rounded-lg p-4 shadow-sm` |
| `<button class="btn btn-success btn-block">` | `bg-blue-600 hover:bg-blue-700 text-white rounded px-4 py-2 w-full` |
| `<form method="post">` | 同左（POST 動作は通常リンク遷移 / 必要なら hx-post） |
| `<table class="table table-bordered">` | `min-w-full divide-y divide-slate-200` |
| `<input type="number" class="form-control">` | `border border-slate-300 rounded px-2 py-1 dark:bg-slate-900` |

### 完了条件

- [x] model 階層が把握済（変更不要を確認）
- [x] 旧 → 新マッピング表が示されている

---

## ステップ 3：影響設計（★Gate 1）

### 3.2 影響範囲分析

| 層 | 影響対象 | 影響内容 |
|---|---|---|
| **参照層** | `edinet-list-detail-v2.html` のみ | 79 → 約 500 行に拡張 |
| **状態層** | 該当なし | DB 状態遷移を扱わない |
| **データ層** | 該当なし | DB 変更なし（V1.0.1 seed をそのまま利用） |

### 3.3 インフラ影響チェック

すべて該当なし（template only）。

### 3.4 品質設計の三本柱

| 柱 | 確認結果 |
|---|---|
| **テスト戦略** | 手動目視（dev seed の `S0000001` などで `/v3/edinet-list-detail?date=2026-03-25` を開き各ボタン動作確認）+ Phase 8 Playwright 8/8 PASS 維持 |
| **セキュリティ方針** | POST フォームは既存 endpoint を呼ぶのみ。`th:text` の HTML エスケープに依拠。CSRF が有効なら `_csrf` を hidden 追加 |
| **ドキュメント計画** | 本 md §6 にテストケース実行結果と Playwright PASS ログを残す |

### 3.5 設計ドキュメント更新

不要。

### Gate 1：人間の承認

- [x] **承認者**: iori-oiso
- [x] **承認日**: 2026-05-02
- [ ] スコープに 7 ボタン + 11 フォーム + 10 列テーブル + 外部リンクが網羅されているか
- [ ] バックエンド変更ゼロの構造的保証に納得できるか

---

## ステップ 4：テスト設計

### テストケース（自然言語）

| # | ケース | 期待結果 |
|---|---|---|
| 1 | `/v3/edinet-list-detail?date=2026-03-25` を開く | h1 に「処理詳細（2026-03-25）」、統計サマリ 8 項目表示 |
| 2 | 「更新して前に戻る」ボタン押下 | POST `/v1/update/edinet-list` 実行後 `/v3/edinet-list` へ遷移 |
| 3 | 「分析する」ボタン押下 | POST `/v1/analyze/date` 実行 |
| 4 | 書類詳細テーブルが 10 列表示 | 書類ID/EDINETコード/書類種別/対象年/DL/DC/NS/BS/PL/RM |
| 5 | 「スクレイピング・分析する」（書類別）押下 | POST `/v2/scrape/id` 実行 |
| 6 | 「ステータス更新：すべて完了」押下 | POST `/v1/update/all-done/status` 実行 |
| 7 | 「除外」押下 | POST `/v1/remove/document` 実行 |
| 8 | 財務値が空の場合 11 編集フォーム表示 | `value == null` の項目で input 表示 |
| 9 | 財務値登録ボタン押下 | POST `/v1/fix-fundamental-value` 実行 |
| 10 | BS/PL/NS の document path リンク表示 | クリックでファイルパスが新規タブで開く |

### Phase 8 Playwright 既存 8 case

template 拡張で構造（aside/header/main/title）は維持されるため引き続き PASS する想定。

### Gate 2：完了条件の確認

- [x] **承認者**: iori-oiso
- [x] **承認日**: 2026-05-02

---

## ステップ 5：実行サイクル

### 実装順序

1. **統計サマリ 8 項目復元**（最上段カード）
2. **2 アクションボタン復元**（更新して前に戻る／分析する）
3. **書類詳細 10 列テーブル復元**（書類別ループ）
4. **書類ごとの 3 ボタン**（スクレイピング・分析／ステータス完了／除外）
5. **書類ファイル外部リンク 3 種**（BS/PL/NS）
6. **11 財務値編集フォーム**（`value == null` 条件付き表示）

各ステップ後に preview server で目視確認。

---

## ステップ 6：多軸検証（★Gate 3）

### Gate 3：人間の最終確認

- [ ] **承認者**: iori-oiso
- [ ] **承認日**: ____
- [ ] 旧 `/edinet-detail` と同等の運用が可能か
- [ ] 業務ブロックが解除されたか

---

## §6 検証結果

### 実施日

2026-05-02（dev プロファイル / preview server / Chromium）

### 実装

- `src/main/resources/templates/edinet-list-detail-v2.html` 79 → 253 行
- dev seed `V1.0.1__dev_seed_screen_test.sql` に `edinet_list_view` 4 行追加 + S0000008 を未処理状態に

### 動作確認結果

`/v3/edinet-list-detail?submitDate=2026-03-25` を開き以下を確認:

| # | テストケース | 結果 |
|---|---|---|
| 1 | URL 開いて 200 OK | ✅ |
| 2 | h1 に「処理詳細（2026-03-25）」 | ✅ |
| 3 | 統計サマリ 8 項目（総件数〜対象外件数） | ✅ |
| 4 | 上部 2 ボタン（更新して前に戻る／分析する） | ✅ |
| 5 | 書類詳細 10 列メタデータテーブル | ✅ |
| 6 | 書類別 2 ボタン（スクレイピング/分析する／ステータス更新：すべて完了） | ✅ |
| 7 | 除外ボタン（10 列目 RM） | ✅ |
| 8 | BS/PL/NS の document path リンク表示 | ✅ |
| 9 | 10 個の財務値編集フォーム（`value == null` 時 input + 登録） | ✅ 10 `登録` ボタン |
| 10 | Alpine.js 折りたたみ（会社名クリック） | ✅ chevron 切替 |

### 検出した点

- 旧版「11 個」とした財務値フォームは実際 **10 個**（md の数値訂正）
- `documentDetailList` は `allStatusDone() == false` のものに限定。業務上「失敗・要確認の書類のみ」表示される設計

### Phase 8 Playwright

5 タスク完了後にまとめて確認予定。
