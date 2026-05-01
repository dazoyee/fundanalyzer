# T20260501 — /v3/corporate 詳細画面の機能復元タスク

## 課題（1 行）

画面刷新 Phase 6（commit 04ee48fa）で `/v3/corporate` を Tailwind+htmx に移植した際、旧 `/corporate` にあった主要機能（表示情報ダッシュボード／4 期間平均カード／6 タブ群／3 アクションボタン／外部リンク）の約 90% が抜け落ちており、業務上利用に堪えない状態。旧 `corporate.html`（1594 行）と同等の機能を `corporate-v2.html`（現状 201 行）に復元する。

## 関連タスク

- 親タスク: [T20260429-screen-renewal-htmx-tailwind.md](T20260429-screen-renewal-htmx-tailwind.md) Phase 6
- 起源コミット: `04ee48fa feat: 画面刷新 Phase 6 で銘柄詳細 (/v3/corporate) を Tailwind+htmx に移植し Chart.js 14 個をローカルバンドル化する`
- 旧テンプレート削除: `088c5d73 chore: 画面刷新 Phase 7 で旧資産削除…`（git history で復活可能）
- **本タスクは 5 画面復元シリーズの優先度 2**:
  - 優先度 1: [T20260502-restore-edinet-detail.md](T20260502-restore-edinet-detail.md)（業務ブロック解除）
  - **優先度 2（本タスク）**: T20260501-corporate-detail-restoration.md
  - 優先度 3: [T20260502-restore-index.md](T20260502-restore-index.md)
  - 優先度 4: [T20260502-restore-valuation.md](T20260502-restore-valuation.md)
  - 優先度 5: [T20260502-restore-edinet-list.md](T20260502-restore-edinet-list.md)
- 並走中: [T20260501-v3-screen-test-data-seed.md](T20260501-v3-screen-test-data-seed.md)（Gate 3 保留中、本タスクの検証データソースを兼ねる）

---

## ステップ 1：把握・整理

### 認識合わせ結果（2026-05-01 / iori-oiso）

| 観点 | 確定 |
|---|---|
| **復元範囲** | 旧版と同等の全機能を復元 |
| **本件 T20260501-seed の扱い** | Gate 3 保留、本タスクと並走しつつ同 seed を流用 |
| **品質ポリシー** | 業務価値優先（ダッシュボード機能・アクション・データ表）／スタイリング微調整は後回し |

### スコープ確定

| 区分 | 内容 |
|---|---|
| **コア** | 旧 `corporate.html` の以下を `corporate-v2.html` に Tailwind 風で復元: ① 会社情報カード（業種/資本金/PER/PBR/ROE/配当利回り/株式優待/外部リンク）／② 表示情報ダッシュボード（20+ 指標）／③ 3年/5年/10年/全平均カード（4 期間 × 4 指標）／④ その他情報 6 タブ群（分析情報/投資指標 4 サブタブ/財務諸表/株価予想 3 サブタブ/株価 5 サブタブ/評価 3 サブタブ）／⑤ アクション 3 ボタン（お気に入り/評価/株価取得）／⑥ 前後ページナビ／⑦ 各データ表（OHLC/評価 9 列等） |
| **後回し** | 高度なデザイン磨き込み（角丸・shadow・余白の微調整）／レスポンシブ最適化／ダークモード時の chart カラー再調整 |
| **対象外** | バックエンド変更（`CorporatePresenter` / `ViewCorporateInteractor` / model attribute は既に全件送出済）／旧 v2 endpoint（`/v2/corporate`）の復活／新規グラフ追加 |

### 関連既存資産

| 区分 | 場所 | 概要 |
|---|---|---|
| 復元参考用 | `git show 088c5d73^:src/main/resources/templates/corporate.html` | 削除前の 1594 行。`/tmp/legacy-corporate.html` に展開済 |
| 改修対象 | `src/main/resources/templates/corporate-v2.html` | 現状 201 行 |
| Presenter | `src/main/java/.../web/presenter/CorporatePresenter.java` | 既に全 model attribute（41 件）を送出。**変更不要** |
| バックエンド endpoint | AnalysisController.java | `/v2/favorite/company` / `/v2/evaluate` / `/v1/import/stock/code` がすべて生存。**変更不要** |

### バックエンド model 全件（既に送出済）

```
corporate, corporateView, backwardCode, forwardCode, target,
analysisResults, analysisLabelAll, analysisPointAll,
indicators, indicatorLabel{30,180,365,All}, indicatorPoint{30,180,365,All},
financialStatements,
forecastStocks, forecastStockLabel{180,365,All}, forecastStockPoint{180,365,All},
stockPrices, stockLabel{30,90,180,365,All}, stockPoint{30,90,180,365,All},
valuations, valuationLabel{180,365,All}, valuationPoint{180,365,All}
```

### 完了条件

- [x] 課題が 1 行で明文化済
- [x] 関連既存資産（旧 template / Presenter / Controller）を把握済
- [x] スコープが 3 区分で確定済

---

## ステップ 2：プロトタイピング

### 採用する手段

旧 `corporate.html` を 1 次情報源とし、構造を Tailwind+Alpine.js で書き直すマッピング表。

### 旧 → 新マッピング（要旨）

| 旧 (Bootstrap) | 新 (Tailwind+v2) |
|---|---|
| `.info-box` | `bg-white dark:bg-slate-800 rounded-lg p-4 shadow-sm` |
| `.row .col-md-3` | `grid grid-cols-1 md:grid-cols-4 gap-4` |
| `<dl class="row">` | `dl class="grid grid-cols-2 gap-x-4 gap-y-2"` |
| `.nav-tabs` + `.tab-pane` | Alpine.js `x-data` + `x-show` |
| `<button class="btn btn-primary">` | `bg-blue-600 text-white rounded px-4 py-2 hover:bg-blue-700` |
| Bootstrap modal （未使用）| 該当なし |

### グラフ canvas

既に `app.js` 内に Chart.js 16 個の定義があり、canvas ID も整合済。**JS 変更は不要、HTML 側で canvas 要素を表示する DIV を増やすだけ**。

---

## ステップ 3：影響設計（★Gate 1）

### 3.2 影響範囲分析

#### 参照層

| 影響対象 | 影響内容 | 確認方法 |
|---|---|---|
| `corporate-v2.html` | 全面書き換え（201 → 1000 行程度） | git diff |
| `CorporatePresenter.java` | 変更なし | grep で確認済（model 41 件はすべて新 template が利用） |
| `app.js` Chart.js 定義 | 変更なし（canvas ID は既に整合） | grep で確認済 |
| `app.css` Tailwind | 変更なし（既存 utility class で記述可能） | 確認不要 |

#### 状態層

該当なし。

#### データ層

該当なし（DB 変更なし、`T20260501-v3-screen-test-data-seed.md` の V1.0.1 seed をそのまま利用）。

### 3.3 インフラ影響チェック

| 項目 | 該当 |
|---|---|
| 大量データ処理 | × |
| 新規外部サービス | × |
| スキーマ変更 | × |
| バッチ追加 | × |
| 依存ライブラリ追加 | × |

### 3.4 品質設計の三本柱

| 柱 | 確認結果 |
|---|---|
| **テスト戦略** | ① 手動目視で 6 タブすべて表示 / 3 アクションボタン動作 / 4 期間平均表示を確認 ② Phase 8 Playwright 8/8 PASS 維持 ③ 新規 JUnit テスト不要（template 変更のみ） |
| **セキュリティ方針** | アクションフォームは既存 endpoint を呼ぶのみ。新規 XSS/CSRF リスクなし。`th:text` のデフォルト HTML エスケープに依拠 |
| **ドキュメント計画** | 本 md に復元対象の全項目を列挙、§6 検証結果に旧 vs 新スクショ比較を貼付 |

### 3.5 設計ドキュメント更新

不要。Phase 6 の T20260429-screen-renewal-phase6-corporate-htmx.md は「Phase 6 の最初の実装」を記録するもので、本復元タスクの md と独立して扱う。

### Gate 1：人間の承認

- [x] **承認者**: iori-oiso
- [x] **承認日**: 2026-05-02
- [ ] スコープに「旧版と同等の全機能」が網羅されているか
- [ ] 後回し対象（デザイン微調整 / レスポンシブ最適化）が妥当か
- [ ] バックエンド変更ゼロの構造的保証に納得できるか

---

## ステップ 4：テスト設計

### テストケース（自然言語）

#### 表示確認

| # | ケース | 期待結果 |
|---|---|---|
| 1 | `/v3/corporate?code=9001` を開く | 会社情報カードに業種(情報・通信業)/資本金(¥100M)/決算月(03-31)/PER(10.83)/PBR(1.63)/ROE(0.12) 等が表示 |
| 2 | 表示情報ダッシュボードに 20+ 指標表示 | 最新企業価値/割安値/平均株価/グレアム指数/標準偏差/変動係数/対象年カウント/最新株価/予想株価/BPS/EPS/ROA/株価企業価値率 が確認可能 |
| 3 | 3年/5年/10年/全平均カードが 4 つ並ぶ | 各カードに 平均企業価値/標準偏差/変動係数/割安比率 が並ぶ。データなしは空表示 |
| 4 | 「分析情報」タブクリック | analysisChartAll 表示 + 提出日/対象年/企業価値の表 |
| 5 | 「投資指標」タブ + 6ヶ月サブタブ | indicatorChart180 表示 + 対象日付/株価企業価値率/PER/PBR/グレアム指数の表 |
| 6 | 「財務諸表」タブクリック | 書類種別 × 期間でグループ化された BS/PL 表 |
| 7 | 「株価予想」タブ + 6ヶ月サブタブ | forecastStockChart180 + 目標株価/理論株価表 |
| 8 | 「株価」タブ + 1ヶ月サブタブ | stockChart30 + OHLC 5 列表（終値/始値/高値/安値）|
| 9 | 「評価」タブ + 6ヶ月サブタブ | valuationChart180 + 9 列表（増減値の色付き）|

#### アクション確認

| # | ケース | 期待結果 |
|---|---|---|
| 10 | お気に入りハート押下 | POST `/v2/favorite/company` 成功、ハートが青く塗りつぶされ、`/v3/index?target=favorite` に表示される |
| 11 | 「評価する」ボタン押下 | POST `/v2/evaluate` 成功、画面再描画 |
| 12 | 「株価取得」ボタン押下 | POST `/v1/import/stock/code` 成功（dev は外部接続失敗で 500 でも、リダイレクト動作を確認）|

#### ナビゲーション確認

| # | ケース | 期待結果 |
|---|---|---|
| 13 | 前社ナビ（chevron 左）クリック | `backwardCode` の会社コードに遷移 |
| 14 | 次社ナビ（chevron 右）クリック | `forwardCode` の会社コードに遷移 |
| 15 | 外部リンク 3 種クリック | 日経/みんかぶ/ヤフーファイナンスに新規タブで遷移 |

### Phase 8 Playwright 既存 8 case の維持

- 既存 `Phase8ScreenSnapshotTest` は HTML 構造（aside/header/main/title）の存在のみアサート → template 拡張で破壊されない
- 復元後も `./mvnw test -Dtest=Phase8ScreenSnapshotTest` で 8/8 PASS を確認

### Gate 2：完了条件の確認

- [x] **承認者**: iori-oiso
- [x] **承認日**: 2026-05-02
- [ ] テストケース 15 件で旧版機能の網羅が確認できるか
- [ ] Playwright 既存 8 case PASS の確認手順が明示されているか

---

## ステップ 5：実行サイクル

### 実装順序

業務価値優先のため以下の順で着手し、各ステップ後にユーザー確認可能な状態を維持する。

1. **アクション 3 ボタン復元**（お気に入り/評価/株価取得）
2. **会社情報カード復元**（業種/資本金/PER/PBR/ROE/配当利回り/株式優待 + 外部リンク 3 種）
3. **表示情報ダッシュボード復元**（4 列インフォボックス + 20+ 指標 dl）
4. **3年/5年/10年/全平均カード復元**（4 期間 × 4 指標）
5. **6 タブ + サブタブ骨組み復元**（Alpine.js x-show）
6. **タブ内コンテンツ復元**（順番: 分析情報 → 株価 → 投資指標 → 評価 → 株価予想 → 財務諸表）
7. **前後ページナビ復元**

各ステップ後に preview server で目視確認、最後に Phase 8 Playwright で回帰確認。

### TDD 適用可否

該当なし。template only 変更で動作テストは手動目視 + 既存 Playwright で十分。

### 既存テスト変更

なし。

---

## ステップ 6：多軸検証（★Gate 3）

### 5 観点

| 観点 | 確認内容 |
|---|---|
| **コード品質** | template 内に Thymeleaf アンチパターンなし（深いネスト/重複ロジック等） |
| **テスト構造品質** | Phase 8 Playwright 8/8 PASS 維持 |
| **機能完全性** | 15 テストケースすべてが目視で確認できる |
| **セキュリティ** | XSS リスクなし（th:text は HTML エスケープ）／POST フォームに `_csrf` token が必要なら追加 |
| **ドキュメント整合性** | 本 md §6 に旧 vs 新の比較スクショが貼付済 |

### Gate 3：人間の最終確認

- [ ] **承認者**: iori-oiso
- [ ] **承認日**: ____
- [ ] 旧 `/corporate` と同等の機能が `/v3/corporate` で再現されているか
- [ ] 業務上の利用に支障がないか

---

## §6 検証結果（実装後に追記）

> 実装エージェントが §5 を完走後、ここに目視確認結果と Phase 8 Playwright PASS ログを記載する。

- [ ] 1〜15 のテストケース実行結果
- [ ] 旧 `/corporate` (1594 行) との機能対比表
- [ ] Phase 8 Playwright 8/8 PASS ログ

---

## チェックリスト

- [ ] §5 のテンプレート変更が 1 回のコミットで完結している
- [ ] テスト（Phase 8 Playwright 8/8 PASS）確認済
- [ ] 関連ドキュメント（本 md）更新済
- [ ] Gate 1・Gate 2・Gate 3 がすべて本 md に通過記録として残る
- [ ] スコープ外（バックエンド変更／旧 v2 endpoint 復活／新規グラフ）に手を出していない
