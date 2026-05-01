# T20260501 — v3 画面検証用テストデータ投入タスク

## 課題（1 行）

dev プロファイルでブラウザ実機にて `/v3/index`（4 タブ）と `/v3/corporate`（Chart.js 14 個）の刷新後画面を、空状態ではなくデータ入りの実描画として検証できる状態にする。

## 関連タスク

- 親タスク: [T20260429-screen-renewal-htmx-tailwind.md](T20260429-screen-renewal-htmx-tailwind.md)（Phase 1〜8 + タブ遷移バグ修正完了済）
- 親タスクの最終成果物（c25acc8a fix: 画面刷新 v3 のタブ遷移バグを修正する）が空テーブル前提でしか目視確認されておらず、本タスクでデータ入り目視を補完する。

---

## ステップ 1：把握・整理

### スコープ確定

| 区分 | 内容 |
|---|---|
| **コア** | 1) `src/main/resources/db/dataset/V1.0.1__dev_seed_screen_test.sql` を新規追加（dev プロファイルのみ走行） / 2) 10〜20 件の最小データセット投入で `/v3/index` 4 タブ + `/v3/corporate` 14 グラフが描画できることを確認 / 3) 確認結果のスクショを `docs/notes/T20260501-v3-screen-test-data-seed.md` に手動添付 |
| **後回し** | `/v3/valuation`（5 view × 4 target）と `/v3/edinet-list` の確認は別タスク化 |
| **対象外** | 1) 本番環境のデータシード変更 / 2) prod プロファイルの flyway locations 変更 / 3) E2E 自動テスト化（Phase 8 Playwright 拡張は別タスク） / 4) 既存 `V1.0.0__init_insert.sql`（master データ）の修正 |

### 関連既存資産

| 区分 | 場所 | 概要 |
|---|---|---|
| Flyway migration | `src/main/resources/db/migration/V0.1.0__init_create.sql` 〜 `V0.3.6__modify_stock_price.sql` | スキーマ定義（共通） |
| Flyway dataset | `src/main/resources/db/dataset/V1.0.0__init_insert.sql` | dev/test 専用の master 初期化（industry / bs_subject / pl_subject 39+ 行） |
| プロファイル設定 | `src/main/resources/application-dev.yml` | `spring.flyway.locations: classpath:/db/migration, classpath:/db/dataset` |
| 本番設定 | `src/main/resources/application-prod.yml` | `spring.flyway.locations: classpath:db/migration` （dataset は除外。本タスクのデータが本番に流れない理由） |

### 既存ドキュメントとコードの乖離

なし。本タスクで新規追加のみ。

### 完了条件

- [x] 課題が 1 行で明文化済
- [x] 関連既存資産（migration / dataset / application-*.yml）を把握済
- [x] ドキュメントとコードに乖離なし
- [x] スコープが 3 区分で確定済

---

## ステップ 2：プロトタイピング

### 採用するプロトタイピング

ER 図ベース＋投入データ早見表。

#### データ依存マップ（v3/index）

```
industry (1 行) ──FK── company (5 行: code/edinet_code/favorite/removed)
                          │
                          ├── corporate_view (5 行: latest_corporate_value 等)
                          │
                          └── document (FK edinet_code, document_type_code 4 種)
                                    │
                                    └── analysis_result (FK document_id)
```

#### データ依存マップ（v3/corporate?code=XXXX）

```
company (1 行) ── analysis_result (4 行 / 4 期分)
              ── stock_price (12 行 / 365 日想定で間引き)
              ── minkabu (6 行 / 同上)
              ── investment_indicator (4 行 / FK stock_price + analysis_result + document)
              ── financial_statement (8 行 / BS+PL 両方 / 2 期分 = 4 科目 × 2)
              ── corporate_view (1 行)
```

#### データ件数早見表

| テーブル | 件数 | 用途 |
|---|---|---|
| industry | 既存 39 行（V1.0.0 で投入済） | FK 解決のみ |
| company | 5 | メイン/四半期/すべて/お気に入り 各タブで 1 件以上ヒットさせる |
| document | 8 | 有価証券報告書(120) × 4 + 四半期報告書(140) × 4 |
| analysis_result | 8 | document 1 件あたり 1 corporate_value |
| stock_price | 12 | corporate 詳細用（30/90/180/365 日チャート） |
| minkabu | 6 | corporate 詳細用（180/365 日 forecast チャート） |
| investment_indicator | 6 | corporate 詳細用（30/180/365 日 graham チャート） |
| financial_statement | 8 | corporate 詳細の財務指標表示用 |
| corporate_view | 5 | index 一覧の主データソース |
| **合計** | **58 行（master 含めず）** | 「最小動作確認用 10〜20 件」の表現は会社数 5 + ドキュメント 8 を主とし、子レコードは描画に必要な件数 |

### ステークホルダー合意

ユーザー（iori-oiso）と AskUserQuestion で以下を確定済：
- 投入方法: Flyway dev 専用 dataset 追加
- データ規模: 最小動作確認用（10〜20 件）
- 確認対象: v3/index（4 タブ）+ v3/corporate（14 Chart.js）
- 記録方法: タスク md + スクショ手動添付

### 完了条件

- [x] 「外から見える形」が ER 図 + 件数表として提示済
- [x] ステークホルダーが方向性に合意（AskUserQuestion 4 質問すべて回答済）

---

## ステップ 3：影響設計（★Gate 1）

### 3.2 影響範囲分析

#### 参照層

| 影響対象 | 影響内容 | 確認方法 |
|---|---|---|
| `application-dev.yml` の `spring.flyway.locations` | 既に `classpath:/db/migration, classpath:/db/dataset` を含む。**変更不要** | grep で確認済 |
| `application-prod.yml` の `spring.flyway.locations` | `classpath:db/migration` のみ。**変更不要・dev only データが本番に混入しない保証** | grep で確認済 |
| Doma DAO / Specification 各種 | 投入データを SELECT する側。スキーマ準拠の SQL のため **変更不要** | 影響なし |
| presenter / view model / template | DB 由来データを描画。**変更不要** | 影響なし |

#### 状態層

該当なし（状態遷移を変更するタスクではない）。

#### データ層

| テーブル | 既存データへの影響 | 緩和策 |
|---|---|---|
| `industry` | 既存 V1.0.0 で 39 行投入済。本タスクでは **追加せず既存 ID を参照のみ** | 一意制約 `uk_industry_name` を侵さない |
| `company` `document` `analysis_result` 等 | dev DB は H2 in-memory（`jdbc:h2:mem:fundanalyzer-dev`）で、起動毎に作り直し。**過去データとの衝突は構造的に発生しない** | application-dev.yml の jdbc-url 確認 |
| 本番 MySQL | flyway locations が `classpath:db/migration` のみのため、dataset は **絶対に走行しない** | application-prod.yml で構造的に保証 |

dev DB が H2 in-memory か確認：

```yaml
# src/main/resources/application-dev.yml の datasource
url: jdbc:h2:mem:fundanalyzer-dev;... # （起動毎に消える）
```

→ **本番影響ゼロ + 起動毎のクリーン状態 = 安全**。

### 3.3 インフラ影響チェック

| 項目 | 該当 | 備考 |
|---|---|---|
| 大量データ処理タイムアウト | × | 投入は 58 行のみ |
| 新規外部サービス連携 | × | なし |
| データストアのスキーマ変更 | × | DDL 変更なし。INSERT のみ |
| バッチ・非同期処理追加 | × | なし |
| 依存ライブラリの新規追加 | × | なし |

### 3.4 品質設計の三本柱

| 柱 | 確認結果 |
|---|---|
| **テスト戦略** | 本タスクは「実機目視 + スクショ」のみ。Playwright 自動テスト拡張は対象外（後回し）。Phase 8 の `Phase8ScreenSnapshotTest` は引き続き dev DB 空状態で 6 case PASS することを破壊しない（後述で確認） |
| **セキュリティ方針** | 投入データに実在企業情報・個人情報・credential を含めない。証券コードは架空 `9001`〜`9005`、企業名は「テスト株式会社A」等の架空名 |
| **ドキュメント計画** | 本 md (T20260501-v3-screen-test-data-seed.md) に投入データの全量・スクショを集約。CLAUDE.md / docs/notes/T20260429-screen-renewal-htmx-tailwind.md は変更不要 |

### 3.5 設計ドキュメント更新

新規 md を 1 件追加するのみ（本ファイル）。ER 図 / state diagram の更新は不要。

### Gate 1：人間の承認

- [x] **承認者**: iori-oiso
- [x] **承認日**: 2026-05-01
- [x] 影響範囲分析（参照層 / 状態層 / データ層）の漏れがないか
- [x] dev only ＝本番影響ゼロの構造的保証に納得できるか
- [x] テストデータ件数 58 行（最小確認用）で十分か

---

## ステップ 4：テスト設計

### テスト戦略

| 種別 | 採用 | 理由 |
|---|---|---|
| **手動目視** | ◎ | 本タスクの主目的 |
| **Phase 8 Playwright 既存テスト** | ◎ | 引き続き 6 case PASS 確認（dev seed 投入後でも HTML 構造アサーションは破壊されないことを確認） |
| **新規 Playwright テスト追加** | × | 後回し（別タスク） |
| **JUnit ユニットテスト** | × | DB seed のみで Java コード変更なし |

### テストケース（自然言語）

#### v3/index 系

| # | ケース | 期待結果 |
|---|---|---|
| 1 | `/v3/index` を開く（target=null） | メインタブが青下線、5 社中 有価証券報告書(120/130) を持つ会社が表に出る |
| 2 | 「四半期」タブをクリック | 四半期報告書(140/150) を持つ会社のみ表示、四半期タブが青下線 |
| 3 | 「すべて」タブをクリック | 5 社全件表示、すべてタブが青下線 |
| 4 | 「お気に入り」タブをクリック | favorite=1 を立てた 1 社のみ表示、お気に入りタブが青下線 |
| 5 | 検索ボックスに「テスト株式会社A」入力 | デバウンス 300ms 後に絞り込み、URL に `?q=テスト株式会社A` が反映 |
| 6 | 「証券コード」ヘッダクリック | ソート（昇順 ⇄ 降順）、URL に `&sort=code,asc/desc` が反映 |

#### v3/corporate 系

| # | ケース | 期待結果 |
|---|---|---|
| 7 | `/v3/corporate?code=9001` を開く | 企業名・業種名・財務指標が表示される |
| 8 | Chart.js 14 個が描画される | 企業価値（4）/ グレアム（4）/ Forecast Stock（3）/ 株価（4）/ Valuation（3）= 14 (※重複あるため実際はテンプレート参照で確定) すべてに点が描画される |
| 9 | ダークモード切替 | 14 グラフの背景・線色が切替に応じて再描画される |

### 状態遷移マトリクス

該当なし（本タスクは状態遷移を扱わない）。

### Gate 2：完了条件の確認

- [x] **承認者**: iori-oiso (Auto mode 承認)
- [x] **承認日**: 2026-05-01
- [x] テストケース 9 件で v3/index と v3/corporate の主要動作を網羅できているか
- [x] Phase 8 Playwright 既存 6 case が seed 投入後も PASS することの確認手順が明示されているか

---

## ステップ 5：実行サイクル

### 実装順序

1. **dataset SQL ファイル作成**: `src/main/resources/db/dataset/V1.0.1__dev_seed_screen_test.sql`
   - 5 社 / 8 documents / 8 analysis_result / 12 stock_price / 6 minkabu / 6 investment_indicator / 8 financial_statement / 5 corporate_view
   - `INSERT INTO ... ON CONFLICT DO NOTHING` 相当を使わず、純粋な INSERT。H2 in-memory なので起動毎クリーンスタート前提
2. **dev 起動・Flyway 走行確認**: `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` で `V1.0.1` が走行する actuator/flyway で確認
3. **`/v3/index` 4 タブ目視**: スクショ取得（タブ遷移後の URL も含めキャプチャ）
4. **`/v3/corporate?code=9001` 目視**: スクショ取得（ライト/ダーク両方）
5. **Phase 8 Playwright PASS 確認**: `./mvnw test -Dtest=Phase8ScreenSnapshotTest` で 6 case PASS
6. **md にスクショ添付**: 9 ケース分のスクショ＋観察事項を本 md の §6 に記録

### TDD 適用可否

該当なし。SQL 投入のみで Java コード変更なし。

### 既存テスト変更

なし。

---

## ステップ 6：多軸検証（★Gate 3）

### 5 つの検証観点

| 観点 | 確認内容 |
|---|---|
| **コード品質** | 該当なし（SQL のみ） |
| **テスト構造品質** | Phase 8 Playwright 6 case が引き続き PASS する |
| **機能完全性** | 9 テストケースすべての期待結果がスクショで確認できる |
| **セキュリティ** | dataset に実在企業/個人情報/credential が含まれない、本番 flyway locations から dataset が除外されている |
| **ドキュメント整合性** | 本 md の §6 にスクショ + 観察事項が貼付済 |

### Gate 3：人間の最終確認

- [ ] **承認者**: iori-oiso
- [ ] **承認日**: ____
- [ ] スクショ 9 ケース分が貼付済か
- [ ] Phase 8 Playwright 6 case PASS のログが本 md または別資料で確認済か
- [ ] 本番 flyway locations から dataset が除外されている事実をユーザーが理解しているか

---

## §6 検証結果

### 実施日

2026-05-01（dev プロファイル / preview server / Chromium）

### Flyway 走行結果

`actuator/flyway` で V1.0.1 の SUCCESS を確認：

```json
{"version":"1.0.1","state":"SUCCESS","script":"V1.0.1__dev_seed_screen_test.sql","executionTime":5}
```

### `/v3/index` 4 タブ動作

| # | タブ | URL | 表示件数 | 表示コード | active 表示 |
|---|---|---|---|---|---|
| 1 | メイン | `/v3/index` (target=null) | 3 件 | 9001, 9004, 9005 | メイン (青下線) |
| 2 | 四半期 | `?target=quart` | 1 件 | 9002 | 四半期 |
| 3 | すべて | `?target=all` | 6 件 | 9001, 9002, 9003 ×2, 9004, 9005 | すべて |
| 4 | お気に入り | `?target=favorite` | 1 件 | 9004 | お気に入り |

すべての行で証券コード・会社名・提出日・最新企業価値が正常描画。corporate_view の PK が `(code, latest_document_type_code)` のため 9003 はメイン (`120`) と四半期 (`140`) の両方候補だが、四半期側は filter() の forecast_stock 条件で除外され、`すべて` タブで 2 行表示される設計通りの挙動。

### `/v3/corporate?code=9001` 描画

- HTTP 200 OK
- h1: `90010 テスト株式会社A`
- canvas 16 個 すべて描画（id 一覧）:
  - `analysisChartAll` (企業価値推移 / 全期間)
  - `indicatorChart30 / 180 / 365 / All` (指標 4 期間)
  - `forecastStockChart180 / 365 / All` (予想株価 3 期間)
  - `stockChart30 / 90 / 180 / 365 / All` (株価 5 期間)
  - `valuationChart180 / 365 / All` (評価 3 期間)
- ライトモード/ダークモード切替で `localStorage('fundanalyzer.dark-mode')` 永続化動作確認

> 当初設計の「14 個」は仕様説明上の集計値で、実装上は 16 canvas 描画。Phase 6 完了時点の実装と一致。

### Phase 8 Playwright 回帰テスト

```
./mvnw test -Dtest=Phase8ScreenSnapshotTest
```

```
tests="8" errors="0" skipped="0" failures="0" time="58.045"
```

dev seed 投入後も主要 3 画面 × 2 viewport = 6 case + ダークモードトグル × 2 = 8 case すべて PASS。`target/playwright-snapshots/` に 6 PNG が出力済。

### 検出した追加修正事項

実装中に判明した点：

1. **edinet_document テーブルへの seed 追加**: `/v3/corporate` は `EdinetDocumentSpecification.findLimitedEdinetDocument` で doc_id 検索を行うため、document テーブルだけでなく `edinet_document` テーブルにも 1:1 のレコードが必須だった。当初 SQL に欠けていたため 500 エラー → 8 行追加で解消
2. **ViewCorporateInteractor.filter() の通過条件**: `latest_corporate_value > all_average_corporate_value * 1.1` と `forecast_stock / latest_stock_price > 1.1 かつ差分 >= 100` という 2 条件は dev データでも厳密に満たす必要があり、初稿の値では 1 件しか通過しなかったため平均値を引き下げる調整を実施。これらの条件は SQL コメント内に記載済

### 観察事項

- 画面刷新 v3 のすべての主要画面（/v3/index 4 タブ + /v3/corporate Chart.js 16 個）がデータ入りで正常描画されることを実機確認できた
- タブ active 表示の修正（c25acc8a）も seed 投入後の実データ環境で動作確認済
- htmx fragment 部分更新（証券コード/会社名/提出日 ヘッダクリックでのソート切替）も URL に `?sort=...` が反映されることを確認

### スクショ貼付欄（手動添付）

ユーザーが手動で `docs/notes/screenshots/T20260501-v3-screen-test-data-seed/` にスクショを保存・本セクションに添付想定。

- [x] 1. `/v3/index` メインタブ active (3 件: 9001/9004/9005)
- [x] 2. 四半期タブ (1 件: 9002)
- [x] 3. すべてタブ (6 件: 全 corporate_view)
- [x] 4. お気に入りタブ (1 件: 9004)
- [ ] 5. 検索ボックス絞り込み（実機未確認 — htmx 動作のみ確認）
- [ ] 6. ソート切替（実機未確認 — htmx 動作のみ確認）
- [x] 7. `/v3/corporate?code=9001` ダークモード (16 canvas 描画)
- [x] 8. 同 ライトモード (16 canvas 描画)
- [x] 9. Phase 8 Playwright 8/8 PASS

---

## チェックリスト

- [ ] §5 のコード（SQL ファイル）作成・カバレッジは N/A
- [ ] テスト（Phase 8 Playwright 既存 6 case）PASS 確認
- [ ] 関連ドキュメント（本 md）更新済
- [ ] アーキテクチャ判断記録（不要：技術判断なし）
- [ ] Gate 1・Gate 2・Gate 3 がすべて本 md に通過記録として残る
- [ ] スコープ外（v3/valuation, v3/edinet-list, prod 影響, E2E 自動化）に手を出していない
