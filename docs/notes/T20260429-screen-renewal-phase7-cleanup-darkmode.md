# Task T20260429 Phase 7: error.html 移植・旧資産削除・ダークモード仕上げ

- 着手日: 2026-05-01
- 完了日: -
- 担当: AI エージェント (Claude / Opus 4.7) + iori-oiso
- 関連リンク:
  - マスタープラン: [T20260429-screen-renewal-htmx-tailwind.md](T20260429-screen-renewal-htmx-tailwind.md)
- ブランチ: `feature/screen-renewal-phase7-cleanup-darkmode`（develop から派生）

---

## ステップ 1: 把握・整理

### 解決すべき課題（1 行）

Phase 1〜6 で並走させてきた **旧 layout / 旧画面 / 旧エンドポイント / 旧静的アセット**を一括削除し、新スタック（Tailwind + htmx + layout-v2）に統一する。あわせて error.html を layout-v2 継承の新形式に移植し、ダークモードの prefers-color-scheme 自動初期化と CLAUDE.md「View / 画面」節の書き換えを行う。

### 関連既存資産（削除対象）

#### 旧テンプレート（layout-v2 で置換済み）

- `templates/layout.html`（85 行・AdminLTE 前提）
- `templates/index.html`（337 行・/v2/index で利用）
- `templates/corporate.html`（1590 行・/v2/corporate で利用）
- `templates/valuation.html`（577 行・/v2/valuation で利用）
- `templates/edinet.html`（267 行・/v2/edinet-list で利用）
- `templates/edinet-detail.html`（630 行・/v2/edinet-list-detail で利用）
- `templates/__phase2-layout-poc.html`（POC・Phase 2 で追加・本 Phase で削除）

#### 旧エンドポイント

- `IndexPresenter.corporateView`（/v2/index）
- `CorporatePresenter.corporateDetailView`（/v2/corporate）
- `ValuationPresenter.valuationView`（/v2/valuation）
- `EdinetPresenter.edinetListView`（/v2/edinet-list）
- `EdinetDetailPresenter.edinetListDetail`（/v2/edinet-list-detail）
- `DevelopController.phase2LayoutPoc / phase2LayoutPocFragment`（/v2/__phase2-layout-poc{,/fragment}）

#### 旧静的アセット

- `static/dist/`（adminlte.min.css / adminlte.min.js）
- `static/plugins/` 配下の以下ディレクトリ:
  - `bootstrap` / `datatables*` / `daterangepicker` / `fontawesome-free` / `inputmask` / `jquery` / `jszip` / `moment` / `pdfmake`
  - 新画面では一切参照していない

### 残す資産

- `templates/error.html` → **layout-v2 継承の新形式に書き換え**（Spring Boot の error テンプレート規約上ファイル名固定）
- `templates/layout-v2.html` / 各 `*-v2.html` / `fragments/*.html`
- `templates/__phase1-poc.html` は既に Phase 2 で削除済（rename で `__phase2-layout-poc.html` へ）

### スコープ

| 区分 | 内容 |
|---|---|
| **コア** | (a) `templates/error.html` を layout-v2 継承で書き換え（Tailwind ベースの 4xx/5xx エラー画面）（b) 旧テンプレート 6 ファイルを削除（layout.html / index.html / corporate.html / valuation.html / edinet.html / edinet-detail.html） (c) 旧 Presenter エンドポイント `/v2/*` 全削除（5 メソッド） (d) POC エンドポイント `/v2/__phase2-layout-poc{,/fragment}` 削除 + `__phase2-layout-poc.html` 削除 (e) 既存テストから旧 `/v2/*` メソッドのテストを削除（既存メソッド消えるため必然） (f) `static/dist/` 配下削除 (g) `static/plugins/` 配下の上記 9 ディレクトリ削除 (h) layout-v2 のダークモード初期値を `localStorage` 優先 + 未設定時に `prefers-color-scheme: dark` を初期反映 (i) CLAUDE.md「View / 画面」節を Tailwind/htmx/Alpine.js 前提に書き換え (j) frontend-asset-cleanup.md / mac-dev-startup.md 等の関連既存ノートを必要に応じて更新 |
| **後回し** | (1) Playwright スナップショット（Phase 8） (2) CSP ヘッダー導入 (3) i18n |
| **対象外** | (A) DAO / SQL / DB スキーマ・Specification の挙動変更 (B) ViewService の業務ロジック変更（旧メソッド `getCorporateView` 等は v3 から呼ばれるため残す。**メソッド削除はしない**） (C) Jenkinsfile のあらゆる変更 (D) 認証認可 (E) 業務 enum / DTO の変更 |

### 削除順序（リスク回避）

1. **新画面が新エンドポイントを使っていることを最終確認**（`grep '/v2/' templates/*-v2.html`）
2. POC エンドポイント・テンプレートを削除
3. 旧 Presenter `/v2/*` メソッドを削除
4. 旧テンプレート 6 ファイルを削除
5. 旧テスト（/v2/* 用）を削除
6. `static/dist` / `static/plugins/*` 削除
7. ビルド + テストで全緑を確認
8. ダークモード仕上げ + CLAUDE.md 書き換え

---

## ステップ 2: プロトタイピング

実機ブラウザで以下を確認する（Gate 3 §動作確認結果に記録・実機確認は Phase 5/6 同様省略可）:

- [ ] `/v3/index`, `/v3/corporate`, `/v3/valuation`, `/v3/edinet-list` すべて 200 OK で動作
- [ ] 存在しない URL（旧 `/v2/*`）が 404 を返す
- [ ] error.html が Tailwind スタイルで描画される
- [ ] ダークモード初期化: localStorage 未設定時に prefers-color-scheme を反映

---

## Gate 1: 影響設計の承認

### レビュアー向けサマリ

- **判断してほしいこと**:
  1. 旧 6 テンプレート + 旧 5 エンドポイント + 旧 9 plugin ディレクトリ + dist の **一括削除** で問題ないか
  2. error.html を layout-v2 継承で書き換える方針（Spring Boot のファイル名規約上 error.html は残す必要がある）
  3. ダークモード初期化を localStorage 優先 + 未設定時に prefers-color-scheme で初期反映する変更
- **重要な変更ポイント**:
  1. 旧テンプレート 6 ファイル削除（layout / index / corporate / valuation / edinet / edinet-detail）
  2. 旧 Presenter エンドポイント /v2/* 5 メソッド削除（IndexPresenter / CorporatePresenter / ValuationPresenter / EdinetPresenter / EdinetDetailPresenter）
  3. POC エンドポイント /v2/__phase2-layout-poc 削除
  4. 旧テスト（/v2/* 用）削除
  5. static/dist / static/plugins 配下 9 ディレクトリ削除
  6. error.html を layout-v2 継承で再構成
  7. layout-v2 のダークモード初期化に prefers-color-scheme フォールバック追加
  8. CLAUDE.md「View / 画面」節を書き換え
  9. ViewService の旧業務メソッド（getCorporateView 等）は v3 から呼ばれるため **残す**
- **確認してほしい観点**:
  1. 旧 /v2/* エンドポイントへの外部依存がないか（ブックマーク・リンク・スクリプト等）
  2. 旧 plugin の中に新画面で間接利用しているものがないか（grep で参照ゼロを確認）
  3. error.html 移植で Spring Boot のエラーマッピング（4xx / 5xx / status code）が壊れないか

### 重点観点

#### 影響範囲分析

- **参照層: 該当**（旧テンプレート / 旧エンドポイント / 旧テスト / 旧静的アセットの全削除）
- **状態層: 該当なし**
- **データ層: 該当なし**

##### 参照層分析結果

| 対象 | 参照箇所 | 影響 |
|---|---|---|
| `templates/layout.html` 6 ファイル | 旧 /v2/* メソッドからのみ参照 | 削除可 |
| 旧 Presenter /v2/* メソッド 5 個 | 外部からの URL リクエストのみ | 削除可（既存テストも併せて削除） |
| `static/dist/` | 旧 layout.html からのみ参照 | 削除可 |
| `static/plugins/*` 9 ディレクトリ | 旧テンプレートからのみ参照 | 削除可 |
| ViewService の旧業務メソッド | v3 メソッドから呼ばれる | **保持** |

#### インフラ影響チェック

| カテゴリ | 判定 | 内容 |
|---|---|---|
| **A. 処理時間** | 該当 | 旧アセット削除で jar サイズ大幅縮小 |
| **B. 外部サービス** | 該当なし | |
| **C. データストア** | 該当なし | |
| **D. バッチ・非同期** | 該当なし | |
| **E. リソース** | 該当 | jar に含まれる静的アセットが約 28MB → 数百 KB に縮小（既存削除分は T20260429-frontend-asset-cleanup で実施済・本 Phase で残り削除） |
| **F. 可用性** | 該当 | 旧 /v2/* URL を叩いていた利用者がいれば 404 になる。社内管理画面のため運用者にアナウンスが必要 |
| **G. セキュリティ** | 該当 | 攻撃面の縮小（jQuery / 旧 plugin の脆弱性経路ゼロ化） |
| **H〜J** | 該当なし | |

#### 三本柱

| 観点 | 採用 |
|---|---|
| 既存テスト維持 | 部分的に削除（/v2/* 用テストは必然的に削除） |
| 残った v3 系テスト全パス確認 | ✅ 必須 |
| `./mvnw clean package` 成功 | ✅ |
| Playwright | Phase 8 |

#### スコープ確定

§ステップ 1 のスコープ表に従う。

### レビュアー記入欄

- 承認者: iori-oiso（プロジェクトオーナー）
- レビュー依頼日: 2026-05-01
- 回答日: 2026-05-01
- 結論: 合格
- コメント: 旧資産の一括削除・error.html 移植・ダークモード仕上げすべて承認。Phase 7 実装着手して可。

---

## Gate 2: 完了条件の確認

### 運用ルート

**インライン**。

### 完了条件

#### 機能

- [ ] `templates/error.html` を layout-v2 継承で書き換え
- [ ] 旧テンプレート 6 ファイル削除
- [ ] 旧 Presenter /v2/* メソッド 5 個削除（IndexPresenter / CorporatePresenter / ValuationPresenter / EdinetPresenter / EdinetDetailPresenter）
- [ ] DevelopController から POC `/v2/__phase2-layout-poc{,/fragment}` 削除
- [ ] `templates/__phase2-layout-poc.html` 削除
- [ ] `static/dist/` 配下削除
- [ ] `static/plugins/` 配下の 9 ディレクトリ削除（bootstrap / datatables* / daterangepicker / fontawesome-free / inputmask / jquery / jszip / moment / pdfmake）
- [ ] layout-v2 のダークモード初期化に prefers-color-scheme フォールバック追加
- [ ] CLAUDE.md「View / 画面」節を書き換え

#### テスト

- [ ] 旧 /v2/* 用のテストを削除（メソッド削除に伴う必然対応）
- [ ] 残った v3 系テスト全緑（`./mvnw clean test`）
- [ ] `./mvnw clean package` 成功（jar サイズ縮小確認）

#### ドキュメント

- [ ] 本 Phase 7 サブタスク md に Gate 1 / Gate 2 / Gate 3 通過記録
- [ ] マスタープラン §サブタスク追跡表 Phase 7 行更新
- [ ] CLAUDE.md「View / 画面」節を Tailwind/htmx/Alpine.js 前提に書き換え

#### スコープ外

- DAO / SQL / DB / Specification 挙動変更
- ViewService の業務メソッド削除（v3 から呼ばれるため保持）
- Jenkinsfile / 認証認可
- Playwright 導入（Phase 8）

### レビュアー記入欄

- 承認者: iori-oiso（プロジェクトオーナー）
- レビュー依頼日: 2026-05-01
- 回答日: 2026-05-01
- 結論: 合格（インライン承認）
- コメント: 完了条件・スコープ外宣言いずれも承認。

---

## ステップ 5: 実行サイクル

### コミット計画

| # | コミット要約 | カテゴリ |
|---|---|---|
| 0 | `docs: Phase 7 サブタスク md を起票する` | docs |
| 1 | `refactor: POC エンドポイント /v2/__phase2-layout-poc を削除する` | refactor |
| 2 | `refactor: 旧 Presenter /v2/* メソッドを削除する` | refactor |
| 3 | `chore: 旧テンプレート 6 ファイル（layout / index / corporate / valuation / edinet / edinet-detail）を削除する` | chore |
| 4 | `chore: static/dist と static/plugins 配下の旧資産を削除する` | chore |
| 5 | `feat: layout-v2 のダークモード初期化に prefers-color-scheme を追加する` | feat |
| 6 | `feat: error.html を layout-v2 継承で書き換える` | feat |
| 7 | `docs: CLAUDE.md「View / 画面」節を Tailwind/htmx 前提に書き換える` | docs |
| 8 | `docs: Phase 7 実装ログとマスタープラン追跡表を反映する` | docs |

最終的な Squash Merge 時の 1 コミット要約: `chore: 画面刷新 Phase 7 で旧資産削除・error.html 移植・ダークモード仕上げを行う`

---

## ステップ 5 §コミット履歴

| # | コミット | 概要 |
|---|---|---|
| 0 | `86d8204c` | docs: Phase 7 サブタスク md を起票する |
| 1 | `0bc5dca7` | refactor: POC エンドポイント /v2/__phase2-layout-poc を削除する |
| 2 | `4d17e63c` | refactor: 旧 Presenter /v2/* メソッドを削除する |
| 3 | `8207b7fb` | chore: 旧テンプレート 6 ファイルを削除する |
| 4 | `（コミット 4）` | chore: static/dist と static/plugins 配下の旧資産を削除する |
| 5 | `ec561e4e` | feat: layout-v2 のダークモード初期化に prefers-color-scheme を追加する |
| 6 | `22ee4f99` | feat: error.html を layout-v2 継承で書き換える |
| 7 | `b705de2c` | docs: CLAUDE.md「View / 画面」節を Tailwind/htmx 前提に書き換える |

ブランチ: `feature/screen-renewal-phase7-cleanup-darkmode`（develop から派生）

最終的な Squash Merge 時の 1 コミット要約: `chore: 画面刷新 Phase 7 で旧資産削除・error.html 移植・ダークモード仕上げを行う`

---

## ステップ 6: 多軸検証

| 観点 | 結果 | 担当 | 確認内容 |
|---|---|---|---|
| **観点 1: コード品質** | OK（AI 実施） | AI | 旧 Presenter /v2/* メソッド 5 個と未使用 import / 未使用定数を整理。EdinetDetailPresenter から不要な EDINET_DETAIL 定数も削除。CorporatePresenter / IndexPresenter 等も整然 |
| **観点 2: テスト構造品質** | OK（AI 実施） | AI | 旧 nested クラスを sed で範囲削除（5 ファイル）し、削除されたメソッドを参照するテストを除去。DevelopControllerTest の redirect 先 assertion を /v2/index → /v3/index に修正。`./mvnw test` 711 件全パス |
| **観点 3: 機能完全性** | OK（AI 実施） | AI | Gate 2 §完了条件 §機能 達成（旧テンプレート 6 削除 / 旧エンドポイント 5 削除 / POC 削除 / 旧 plugins 全削除 / dist 削除 / error.html 移植 / ダークモード prefers-color-scheme / CLAUDE.md 書き換え）。ViewService の業務メソッドは残存（v3 から呼び出される） |
| **観点 4: セキュリティ** | OK（AI 実施） | AI | jQuery / 旧 plugin の脆弱性経路ゼロ化。攻撃面大幅縮小。新スタックに統一されサプライチェーン最小化 |
| **観点 5: ドキュメント整合性** | OK（AI 実施） | AI | 本 md / マスタープラン追跡表 / CLAUDE.md「View / 画面」節 / ADR-001（採用技術不変）整合 |

---

## Gate 3: 最終確認

### レビュアー向けサマリ

- **判断してほしいこと**: 旧資産大規模削除（コミット 1〜4）後の挙動が新画面で完全動作するか / error.html の Tailwind 化が問題ないか / ダークモード prefers-color-scheme 自動初期化が期待通りか
- **重要な変更ポイント**:
  1. 旧 Presenter /v2/* メソッド 5 個削除
  2. POC エンドポイント + テンプレート削除
  3. 旧テンプレート 6 ファイル削除（合計 3486 行削除）
  4. static/dist と static/plugins 配下の 9 ディレクトリ + dist 全削除（数十 MB の資産削除）
  5. error.html を layout-v2 継承で完全書き換え
  6. layout-v2 のダークモード初期化に prefers-color-scheme フォールバック
  7. CLAUDE.md「View / 画面」節を Tailwind/htmx 前提に書き換え
  8. テスト 711 件全パス（既存 + Phase 1〜7 全テスト・旧 v2 用テストは削除済）

### 重点観点

#### 動作確認結果（AI 実施・2026-05-01）

- [x] `./mvnw test` 711 件全パス（19 秒前後）
- [ ] Claude Preview 実機確認は省略・人間レビュアに委ねる（Phase 5/6 と同様）
- [ ] `/v3/index`, `/v3/corporate`, `/v3/valuation`, `/v3/edinet-list` の動作確認は人間レビュア
- [ ] 404 エラー画面の Tailwind スタイル描画確認は人間レビュア
- [ ] ダークモード prefers-color-scheme 反映の確認は人間レビュア（OS のダークモード設定を切替して localStorage クリア状態で確認）

#### 副次影響

- jar サイズ大幅縮小（旧 plugins と dist 削除で数十 MB レベル）
- 既存 ViewService 業務メソッドは v3 から呼ばれるため保持
- DAO / SQL / DB スキーマは無変更

### レビュアー記入欄

- 承認者: iori-oiso（プロジェクトオーナー）
- レビュー依頼日: 2026-05-01
- 回答日: 2026-05-01
- 結論: 合格
- コメント: 旧資産大規模削除・error.html Tailwind 化・ダークモード prefers-color-scheme 追加すべて承認。Phase 8（Playwright）へ進める。実機確認は人間レビュア側で実施。

---

## 添付ファイル

`docs/notes/T20260429-screen-renewal-phase7-attachments/` 配下に配置予定。

---

## 更新履歴

- 2026-05-01: 初版作成（ステップ 1〜2・Gate 1・Gate 2 セクション記載・コミット計画策定・削除順序の明記）
- 2026-05-01: Gate 1 / Gate 2（インライン）承認記録（iori-oiso・合格）
- 2026-05-01: 7 コミット（86d8204c→b705de2c）を `feature/screen-renewal-phase7-cleanup-darkmode` ブランチで実装。旧 Presenter /v2/* 5 個 + POC + 旧テンプレート 6 + 旧 plugins 9 ディレクトリ + dist 削除 + error.html 移植 + ダークモード prefers-color-scheme + CLAUDE.md 書き換え。`./mvnw test` 711 件全パス。Claude Preview 実機確認は省略・人間レビュアに委ねる
- 2026-05-01: コミット 8（c5c14f46）で実装ログ反映 + DevelopControllerTest redirect 修正。Gate 3 承認記録（iori-oiso・合格、Phase 8 へ進める指示）
