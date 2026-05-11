# Task T20260509: 銘柄詳細画面の前後銘柄ナビ + /v3/index デフォルトソート修正

- 着手日: 2026-05-09
- 完了日: -
- 担当: Claude (計画 / 実装 / 検証) + 人間レビュア (iori-oiso)
- 関連リンク:
  - [docs/guideline/](../guideline/) 一次情報源
  - [CLAUDE.md](../../CLAUDE.md) スマホ対応 / 画面刷新節
  - 既存実装 (機能 1 = 前後ナビ): [CorporatePresenter.java](../../src/main/java/github/com/ioridazo/fundanalyzer/web/presenter/CorporatePresenter.java) / [ViewCorporateInteractor.java](../../src/main/java/github/com/ioridazo/fundanalyzer/domain/interactor/ViewCorporateInteractor.java) / [corporate-v2.html](../../src/main/resources/templates/corporate-v2.html)
  - 既存実装 (機能 2 = ソート修正): [IndexPresenter.java](../../src/main/java/github/com/ioridazo/fundanalyzer/web/presenter/IndexPresenter.java) / [ViewService.java](../../src/main/java/github/com/ioridazo/fundanalyzer/domain/service/ViewService.java)

> 本タスクは独立した 2 機能を「ユーザー指示により一括進行」で 1 タスク md に統合管理する（[human-checkpoints.md](../guideline/human-checkpoints.md) §タスク 1 md 統合方式に基づく明示的選択）。

---

## ステップ 1: 把握・整理

### 解決すべき課題

#### 機能 1: 銘柄詳細の前後ナビ

銘柄詳細画面 (`/v3/corporate?code=XXXX`) で、提出日順に隣接する銘柄へワンクリックで遷移できない。既存実装には `backwardCode` / `forwardCode` の概念があるが、`target` パラメータ付きアクセス時のみ機能し、配置も content 内のため画面上部に視線を戻さないと使えない。

#### 機能 2: /v3/index デフォルトソート修正

会社一覧 (`/v3/index`) のデフォルトソートが `code,asc` になっており、`viewMain()` 内部の「提出日 DESC → コード DESC」順が画面で失われている。v2 (旧 AdminLTE) 時代に提出日順で表示されていた挙動を再現したい。

### 関連既存資産

#### 機能 1 関連

| 種別 | パス | 備考 |
|---|---|---|
| Controller | [CorporatePresenter.java:47-54](../../src/main/java/github/com/ioridazo/fundanalyzer/web/presenter/CorporatePresenter.java) | `/v3/corporate` GET。`target` 任意 |
| UseCase 実装 | [ViewCorporateInteractor.java:240-268](../../src/main/java/github/com/ioridazo/fundanalyzer/domain/interactor/ViewCorporateInteractor.java) | `viewCorporateDetail(CodeInputData, Target)` で codeList から前後コード算出 |
| UseCase 実装 | [ViewCorporateInteractor.java:viewMain/Quart/All](../../src/main/java/github/com/ioridazo/fundanalyzer/domain/interactor/ViewCorporateInteractor.java) | 提出日 reversed (新→古) でソート |
| ViewModel | [CorporateDetailViewModel.java](../../src/main/java/github/com/ioridazo/fundanalyzer/web/view/model/corporate/detail/CorporateDetailViewModel.java) | record に `backwardCode` / `forwardCode` 既存 |
| Template | [corporate-v2.html:37-53](../../src/main/resources/templates/corporate-v2.html) | content 内の前後 nav。`th:if` でコード null 時に非表示 |
| Layout | [layout-v2.html:65-80](../../src/main/resources/templates/layout-v2.html) | `page-title` fragment を持つヘッダー |
| Test | [CorporatePresenterTest.java](../../src/test/java/github/com/ioridazo/fundanalyzer/web/presenter/CorporatePresenterTest.java) | `target=quart` ケースで `getCorporateDetailView(_, Target)` 呼び分け検証 |
| Test | [ViewCorporateInteractorTest.java:207-305](../../src/test/java/github/com/ioridazo/fundanalyzer/domain/interactor/ViewCorporateInteractorTest.java) | `backward`/`forward` 計算ロジックの 4 ケース |

#### 機能 2 関連

| 種別 | パス | 備考 |
|---|---|---|
| Controller | [IndexPresenter.java:25, 101-114](../../src/main/java/github/com/ioridazo/fundanalyzer/web/presenter/IndexPresenter.java) | `DEFAULT_SORT="code,asc"` / `parseSort()` は単一ソートのみ返す |
| Service | [ViewService.java:104-158](../../src/main/java/github/com/ioridazo/fundanalyzer/domain/service/ViewService.java) | `findCompanyTable` の `applySort` は `Sort` の `Order` を順に `thenComparing` するため複合ソート対応済み |
| Test | `IndexPresenterTest` (要確認) | デフォルトソート挙動の検証 |
| 比較対象 | [EdinetPresenter.java:25](../../src/main/java/github/com/ioridazo/fundanalyzer/web/presenter/EdinetPresenter.java) | 既に `submitDate,desc` がデフォルト |
| 比較対象 | [ValuationPresenter.java:153-168](../../src/main/java/github/com/ioridazo/fundanalyzer/web/presenter/ValuationPresenter.java) | 5 view 別ソート。スコープ外（view ごとの複雑性のため別タスクで判断） |

### ドキュメント・コードの乖離

- 機能 1: なし。既存 `backwardCode`/`forwardCode` フィールドはコードのみで使われ、ドキュメント側に明記された前提仕様はない（CLAUDE.md にも前後ナビへの言及なし）
- 機能 2: なし。CLAUDE.md にデフォルトソートの仕様は明記されていない

### スコープ確定

| 区分 | 内容 |
|---|---|
| **コア（機能 1）** | (1) ヘッダー H1 横（タイトル右上）に前後ボタンを配置 (2) `target` 未指定時もデフォルトで全企業 (`viewAll()`) ベースに前後を算出 (3) 「次=より新しい提出日」「前=より古い提出日」へ方向統一（既存挙動を反転） (4) 端では該当ボタンを非表示 (5) スマホ (sm 未満) はタイトル下行に表示 |
| **コア（機能 2）** | (6) `IndexPresenter.DEFAULT_SORT` を「`submitDate,desc` → `code,desc`」の複合ソートを表現する形に変更 (7) `parseSort()` でデフォルト時に `Sort.by(DESC, "submitDate").and(Sort.by(DESC, "code"))` を返す (8) ユーザーが `submitDate` カラム明示クリック時のみ `code,desc` を tie-break として加える（他カラムソート時の tie-break は加えない） |
| **後回し** | なし |
| **対象外（機能 1）** | キーボードショートカット (←/→ で遷移) / お気に入り絞り込み一覧での前後遷移 / 循環（末尾→先頭 ループ）/ PNG ビジュアルリグレッション baseline の追加（corporate-v2 は元々 baseline 対象外）/ お気に入りビューでの前後計算 |
| **対象外（機能 2）** | `/v3/valuation` のデフォルトソート修正（5 view それぞれにカラム構成が異なり別判定が必要、別タスク化）/ `/v3/edinet-list` のデフォルトソート修正（既に `submitDate,desc` で要件達成済み、行に `code` カラムなし）/ `formatSort` のような複合ソートのクライアント表現（URL に `?sort=submitDate,desc` 1 個だけ表示し、tie-break はサーバ側で暗黙適用） |

---

## ステップ 2: プロトタイピング

### 機能 1: 銘柄詳細の前後ナビ

#### デスクトップ（sm 以上）

ヘッダー H1 行の右側、ダークモード切替ボタンの左側にチェブロン付きリンクを並置。

```
┌────────────────────────────────────────────────────────────────────┐
│ ☰  ← 一覧へ / 9999 サンプル株式会社    [← 1234] [5678 →]   ☀/🌙 │
└────────────────────────────────────────────────────────────────────┘
```

- 末尾（先頭）の場合は該当ボタンを非表示（`th:if` 踏襲）
- ボタンには遷移先のコード番号を表示（識別性）

#### モバイル（sm 未満）

タイトルが truncate で切られるため、前後ボタンはタイトル下の独立行に配置（`hidden sm:flex` でデスクトップ版を非表示、`flex sm:hidden` で別行版を表示）。

```
┌────────────────────────────────┐
│ ☰  9999 サンプル株式会社  ☀/🌙 │
├────────────────────────────────┤
│        [← 1234]   [5678 →]     │
├────────────────────────────────┤
│ ... コンテンツ ...              │
└────────────────────────────────┘
│ [会社] [株価] [EDINET] (ボトムナビ) │
└────────────────────────────────┘
```

### 機能 2: /v3/index デフォルトソート修正

「外から見える形」 = URL とテーブル並び順。プロトタイプ的なモックは不要だが、振る舞いを文書化する。

| ケース | URL | 内部 Sort | 期待される並び順 |
|---|---|---|---|
| 初回アクセス | `/v3/index` | `Sort.by(DESC, "submitDate").and(Sort.by(DESC, "code"))` | 提出日 DESC → 同提出日内は コード DESC |
| 提出日見出しクリック (desc) | `/v3/index?sort=submitDate,desc` | 同上 | 同上 |
| 提出日見出しクリック (asc) | `/v3/index?sort=submitDate,asc` | `Sort.by(ASC, "submitDate").and(Sort.by(DESC, "code"))` | 提出日 ASC → 同提出日内は コード DESC |
| 会社名見出しクリック | `/v3/index?sort=name,asc` | `Sort.by(ASC, "name")` | 名前 ASC のみ（tie-break なし） |
| コード見出しクリック | `/v3/index?sort=code,asc` | `Sort.by(ASC, "code")` | code ASC のみ（tie-break なし） |
| 不正な sort パラメータ | `/v3/index?sort=invalid` | デフォルトに fallback | 提出日 DESC → コード DESC |

`sortParam` の Model 値は単一表現 (`"submitDate,desc"`) のみ保持（テーブル列ヘッダーの矢印表示が複合ソートを表現できないため、UI 上は主キーのみ反映）。tie-break はサーバ側で暗黙的に適用する。

### ステークホルダー合意

ユーザー（人間レビュア）と AskUserQuestion で擦り合わせ済み。

機能 1:
- 順序基準: 提出日順
- 前後両方
- 末尾は **ボタン自体を非表示**（最終回答で disabled から修正）
- 配置: ヘッダー右上（タイトル横）、スマホはタイトル下
- 既存 `target=main|quart|all` の方向解釈も「次=より新しい」へ統一する（破壊的変更を承認）

機能 2:
- スコープは `/v3/index` のみ（valuation / edinet-list は対象外）
- 実装方針: 複合ソート (`submitDate desc + code desc`) を `parseSort()` で返す
- tie-break は **`submitDate` ソート時のみ** `code desc` を加える（他カラムソート時は加えない）

合意済みのため、別途プロトタイプモック作成は省略。

---

## ステップ 3: 影響設計（Gate 1）

### 3.1 入力の確認

事前計画ドキュメントなし。本タスク md でフル分析を実施。

### 3.2 影響範囲分析

[impact-analysis.md](../guideline/impact-analysis.md) §0 の変更属性チェックを通す。

#### 変更属性チェック

| 属性 | 該当 | 備考 |
|---|---|---|
| 型・関数の追加・変更・削除 | ✅ | (機能 1) `ViewCorporateInteractor` の前後コード計算ロジック反転 / `viewCorporateDetail(CodeInputData)` の呼び出し経路で codeList を埋める。(機能 2) `IndexPresenter.parseSort()` の戻り値を複合 Sort 対応に変更 |
| 公開 API 追加・変更 | ❌ | `/v3/corporate` のクエリパラメータは既存維持 / `/v3/index` のクエリパラメータも既存維持 |
| 状態遷移の変更 | ❌ | 状態を持たない |
| データモデル変更 | ❌ | DB スキーマ変更なし |
| 設定値追加・変更 | ❌ | `application.yml` 変更なし |
| 依存ライブラリ追加・更新 | ❌ | 既存 Tailwind / htmx / Thymeleaf / Spring Data Sort のみで完結 |
| 外部連携追加・変更 | ❌ | なし |
| インフラ構成変更 | ❌ | なし |

#### 参照層（機能 1）

| 対象 | 影響内容 | 対応 |
|---|---|---|
| `ViewCorporateInteractor.viewCorporateDetail(CodeInputData, Target)` | 前後 index の方向を反転（次=index-1, 前=index+1） | 実装変更 |
| `ViewCorporateInteractor.viewCorporateDetail(CodeInputData)` | target 未指定時に `viewAll()` ベースで前後を計算するパスを追加 | 実装変更（現状は呼び出さない経路に backward/forward を委譲する形で副作用を制御） |
| `CorporatePresenter.populateModel` | model attribute 名は変更なし。`backwardCode`/`forwardCode` の値が target 無し経路でも入る前提に変わる | 変更不要見込み（Interactor 側で吸収） |
| `corporate-v2.html` | (1) content 内 nav 削除 (2) `page-title` fragment 内に前後リンク併置 (3) スマホ用別行 nav | テンプレート変更 |
| `ViewCorporateInteractorTest` の `target_*` 系 4 ケース | 期待値が反転（backward/forward が入れ替わる） | テスト修正（**仕様変更承認済み** ルートで [workflow.md §例外類型](../guideline/workflow.md#例外既存テスト変更が許可される場合) 適用） |
| `CorporatePresenterTest` | `target` 無しケースの新規追加（前後コードが Model に乗ることを検証） | テスト追加 |

#### 参照層（機能 2）

| 対象 | 影響内容 | 対応 |
|---|---|---|
| `IndexPresenter.DEFAULT_SORT` | `"code,asc"` → `"submitDate,desc"` へ変更 | 実装変更 |
| `IndexPresenter.parseSort()` | 戻り値を `Sort.by(DESC, "submitDate").and(Sort.by(DESC, "code"))` 等の複合 Sort に拡張。`submitDate` ソート時のみ `code desc` tie-break を加える | 実装変更 |
| `ViewService.findCompanyTable` / `applySort` | 既に `Sort` の `Order` を順に `thenComparing` するため複合ソートに対応済み | 変更不要 |
| `IndexPresenterTest` (存在確認要) | デフォルトソート挙動の期待値変更 | 既存があれば修正、なければ追加 |
| `index-v2.html` / `fragments/index-table.html` のソート矢印 | `sortParam` は単一表現のため矢印表示は影響なし（主キーのみ反映） | 変更不要見込み |

#### 状態層

該当なし（画面遷移は GET のみで状態保持なし）。

#### データ層

該当なし（DB アクセスは既存 `viewSpecification.findAllCorporateView()` 等を継続利用。並び替えはメモリ内 stream）。

### 3.3 インフラ影響チェック

[infra-impact-checklist.md](../guideline/infra-impact-checklist.md) を確認。

| 項目 | 該当 | 備考 |
|---|---|---|
| 大量データ処理タイムアウト | ❌ | `viewAll()` は既存呼び出し。会社数規模も既存と同等 |
| 新規外部サービス連携 | ❌ | なし |
| データストアスキーマ変更 | ❌ | なし |
| バッチ・非同期処理追加 | ❌ | なし |
| 依存ライブラリ追加 | ❌ | なし |
| 設定変更（application.yml 等） | ❌ | なし |

### 3.4 品質設計の三本柱

#### テスト戦略

| 種別 | 適用 | 内容 |
|---|---|---|
| ユニットテスト (Mockito) | ✅ | (機能 1) `ViewCorporateInteractor` の前後コード計算（target 有無 × 端ケース） |
| 統合テスト (MockMvc) | ✅ | (機能 1) `CorporatePresenter` の `target` 無しケースで `backwardCode`/`forwardCode` が Model に入ることを検証 / (機能 2) `IndexPresenter` のデフォルトソート・`submitDate` 明示ソート時の tie-break・他カラムソート時の tie-break 不在を検証 |
| Playwright スナップショット | ❌ | 既存 `Phase8ScreenSnapshotTest` の 3 画面（index / valuation / edinet-list）あり。`/v3/index` の Sort 変更で構造（`<table>` / `<aside>` 等）に変化はないため structural assertion は影響なし（baseline 更新不要） |
| PNG ビジュアルリグレッション | △ | `MobileScreenshotRegressionTest` 対象に `index` 含まれる。**ソート順が変わるため index baseline の更新が必要**。後述の運用にて対応 |

採用しない理由（機能 1）: corporate-v2 は元々 baseline 取得対象外（[CLAUDE.md スマホ対応節](../../CLAUDE.md)）。

採用する理由（機能 2 の baseline 更新）: index の表内容は会社一覧テーブルの内容そのもの。並び順が変わると baseline と差異が出る。`./mvnw test -Dtest=ManualMobileScreenshotTest -DupdateBaselines=true` で再生成し、PR レビューで目視承認する（[CLAUDE.md baseline 更新手順](../../CLAUDE.md)）。

カバレッジ目標: 80% 以上を維持（既存方針）。

#### セキュリティ方針

| 観点 | 影響 | 対応 |
|---|---|---|
| 入力検証 | 既存 `code` パラメータのみ。新規入力なし | 変更不要 |
| XSS | リンクの href / テキストに企業コード（4 桁数字）を表示 | Thymeleaf のデフォルトエスケープに委ねる。属性・テキストとも `th:href`/`th:text` 経由で出力 |
| 認可 | 既存 `/v3/corporate` のアクセス制御を維持 | 変更不要 |
| CSRF | GET のみ。フォーム送信なし | 該当なし |
| 機密情報露出 | なし | - |

新規脅威の追加なし。

#### ドキュメント計画

| ドキュメント | 更新内容 | 担当 |
|---|---|---|
| [CLAUDE.md](../../CLAUDE.md) | (機能 1) 「View / 画面 §エンドポイントとテンプレート」付近に「銘柄詳細ヘッダーの前後ナビ」を 1〜2 行追記 / (機能 2) `/v3/index` のデフォルトソート仕様（`submitDate desc → code desc`）を 1 行追記 | 実装エージェント |
| 本タスク md | Gate 1 / 2 / 3 通過記録 | 計画 / 検証エージェント |
| ADR | 新規作成不要（既存スタックを使った機能追加で技術判断なし） | - |
| 既存 baseline PNG | (機能 2) `index-mobile.png` / `index-desktop.png` を `ManualMobileScreenshotTest -DupdateBaselines=true` で再生成し PR で目視確認 | 実装エージェント |

### 3.5 設計ドキュメント更新

実装後に [CLAUDE.md](../../CLAUDE.md) の該当節を更新する。

### Gate 1 セクション

> 初版（機能 1 のみ）はユーザー追加要件で再提出された。本セクションは **未承認のまま再構成された v2** であり、ガイドラインの「承認後の編集禁止」には抵触しない（[human-checkpoints.md §通過記録](../guideline/human-checkpoints.md#通過記録-タスク-1-md-統合方式) の規定）。

#### レビュアー向けサマリ

- **判断してほしいこと**:
  1. 機能 1 / 機能 2 を 1 タスク md に統合する判断の妥当性（ユーザー指示「既存タスク md に追記して一括進行」が確認済み）
  2. 影響範囲分析の網羅性が十分か
  3. 「既存 `target=main|quart|all` 経路で前後方向を反転する」破壊的仕様変更（機能 1）を承認するか
  4. 「`/v3/index` のデフォルトソートを `code,asc` から `submitDate,desc + code,desc` 複合ソートに変更する」仕様変更（機能 2）を承認するか
- **重要な変更ポイント**:
  1. (機能 1) 前後方向を「次=より新しい提出日」へ反転（既存ユーザーの体感が変わる）
  2. (機能 1) `target` 未指定時もデフォルトで `viewAll()` ベースに前後コードを算出
  3. (機能 1) 既存テスト (`ViewCorporateInteractorTest` の 4 ケース) の期待値を反転（[workflow.md §例外類型](../guideline/workflow.md#例外既存テスト変更が許可される場合) の「仕様変更承認済み」適用）
  4. (機能 1) corporate-v2.html の前後 nav を `page-title` fragment 内 + スマホ別行レイアウトへ移設
  5. (機能 2) `IndexPresenter.parseSort()` を複合 Sort (`Sort.and()`) 対応に拡張、`submitDate` ソート時のみ `code desc` tie-break を加える
- **確認してほしい観点**:
  - 「既存 `target=main|quart|all` 経路」のユーザーが他にいないか（機能 1 の前後方向反転の利用者影響）
  - お気に入りビュー (`viewFavorite()`) を前後計算対象にしないことの妥当性（機能 1）
  - 機能 2 で `valuation` / `edinet-list` をスコープ外にする判断が妥当か（5 view 別カラム / 行に code なし、を理由に別タスク化）
  - 機能 2 で baseline PNG (`index-mobile/desktop`) 更新が必要になる点の許容

#### 重点観点

- **影響範囲分析**: 参照層のみ該当。状態層 / データ層 / インフラ影響なし
- **三本柱**:
  - テスト戦略: ユニット + 統合テスト追加 / 機能 2 では index baseline PNG の更新運用が発生
  - セキュリティ方針: 新規脅威なし。Thymeleaf エスケープと既存ホワイトリスト (`ALLOWED_SORT_FIELDS`) に依拠
  - ドキュメント計画: CLAUDE.md 該当節の追記 + 本タスク md + baseline PNG 再生成
- **スコープ**:
  - コア 8 項目（機能 1: 5 / 機能 2: 3）
  - 対象外 8 項目（機能 1: 5 / 機能 2: 3 = `/v3/valuation` 修正・`/v3/edinet-list` 修正・URL に複合ソート表現）
- **依存追加判断**: 該当なし

#### レビュアー記入欄

- 承認者: iori-oiso (人間レビュア)
- レビュー依頼日: 2026-05-09（v1 = 機能 1 のみ）/ 2026-05-09（v2 = 機能 1 + 2 / 再提出）
- 回答日: 2026-05-09
- 結論: 合格（v2 を承認）
- コメント: チャット上で「承認」回答を確認。前後ナビは既存 `backwardCode`/`forwardCode` の活用拡張、`/v3/index` は v2 時代の仕様復元という整理で承認。実装フェーズに進行可。

---

## ステップ 4: テスト設計

### テストケース（自然言語）

#### 機能 1: 銘柄詳細の前後ナビ

`ViewCorporateInteractor` のユニットテスト：

| # | 前提 | 操作 | 期待結果 |
|---|---|---|---|
| 1-A | viewMain() = ["new", "mid", "old"] (新→古), code="mid", target=MAIN | viewCorporateDetail(input, MAIN) | backwardCode="old"（より古い）/ forwardCode="new"（より新しい） |
| 1-B | viewMain() = ["mid", "old"] (リスト先頭), code="mid", target=MAIN | viewCorporateDetail(input, MAIN) | backwardCode="old" / forwardCode=null（先頭） |
| 1-C | viewMain() = ["new", "mid"] (リスト末尾), code="mid", target=MAIN | viewCorporateDetail(input, MAIN) | backwardCode=null（末尾）/ forwardCode="new" |
| 1-D | viewMain() = ["mid"] (単独), code="mid", target=MAIN | viewCorporateDetail(input, MAIN) | backwardCode=null / forwardCode=null |
| 1-E | viewAll() = ["new", "mid", "old"], code="mid", **target なし** | viewCorporateDetail(input)（target 引数なしオーバーロード） | backwardCode="old" / forwardCode="new"（**新規挙動**：target 無しでも前後コードが入る） |
| 1-F | viewAll() が空, code="mid", target なし | viewCorporateDetail(input) | backwardCode=null / forwardCode=null |
| 1-G | viewAll() = ["new", "old"], code="not-in-list" | viewCorporateDetail(input) | backwardCode=null / forwardCode=null（リストに含まれない場合は前後ナビなし） |

`CorporatePresenter` の MockMvc 統合テスト：

| # | 前提 | 操作 | 期待結果 |
|---|---|---|---|
| 1-H | viewService.getCorporateDetailView(_) が backwardCode="old"/forwardCode="new" 入りで返す | GET /v3/corporate?code=mid | Model attribute に backwardCode="old", forwardCode="new" が乗る |
| 1-I | target=quart 指定 | GET /v3/corporate?code=mid&target=quart | viewService.getCorporateDetailView(_, Target.QUART) が呼ばれる（既存挙動維持確認）|

#### 機能 2: /v3/index デフォルトソート

`IndexPresenter` の MockMvc 統合テスト（または `parseSort` の振る舞いを Controller 経由で検証）：

| # | 前提 | 操作 | 期待結果 |
|---|---|---|---|
| 2-A | sort パラメータ未指定 | GET /v3/index | viewService.findCompanyTable に渡される Pageable の Sort が `submitDate DESC, code DESC` の 2 段構成 |
| 2-B | sort=submitDate,desc 明示 | GET /v3/index?sort=submitDate,desc | Pageable の Sort が `submitDate DESC, code DESC` |
| 2-C | sort=submitDate,asc 明示 | GET /v3/index?sort=submitDate,asc | Pageable の Sort が `submitDate ASC, code DESC`（tie-break は方向に関わらず DESC 固定）|
| 2-D | sort=name,asc 明示 | GET /v3/index?sort=name,asc | Pageable の Sort が `name ASC` のみ（tie-break なし） |
| 2-E | sort=code,asc 明示 | GET /v3/index?sort=code,asc | Pageable の Sort が `code ASC` のみ（tie-break なし） |
| 2-F | sort=invalid,desc 不正 | GET /v3/index?sort=invalid | デフォルトに fallback（`submitDate DESC, code DESC`）|
| 2-G | sort=submitDate,desc + その他フィルタ併用 | GET /v3/index?sort=submitDate,desc&q=keyword | Sort 適用 + キーワード絞り込みが両立 |

### 状態遷移マトリクス

該当なし（状態を扱うタスクではない）。

### 既存テストとの重複・補完

- `ViewCorporateInteractorTest` の既存 `target_*` 4 ケースは **期待値を反転** する形で本タスクの 1-A〜1-D を満たす（**既存テスト変更**：[workflow.md §例外類型](../guideline/workflow.md#例外既存テスト変更が許可される場合) の「仕様変更承認済み」ルートで Gate 1 にて承認済み）
- 1-E〜1-G は新規追加（target 無し経路）
- `CorporatePresenterTest` の既存 `target=quart` ケースは 1-I として維持。1-H は新規追加
- `IndexPresenterTest` は既存有無を確認のうえ、なければ新規ファイル作成、あれば 2-A〜2-G を追加

---

## ステップ 5: 実行サイクル（Gate 2）

### Gate 2 運用ルート

「省略」基準（[human-checkpoints.md §小タスク基準](../guideline/human-checkpoints.md#小タスクの判定基準省略可の条件)）の判定：

| 基準 | 該当 | 判定 |
|---|---|---|
| 影響範囲が単一ファイル・単一関数 | ❌ | 機能 1 + 機能 2 で 5 ファイル超に変更が及ぶ |
| テストケース数が 3 件以下 | ❌ | 16 ケース（機能 1: 9 / 機能 2: 7） |
| ドキュメント更新が不要 | ❌ | CLAUDE.md + baseline PNG の更新が必要 |
| 既存仕様への影響なし | ❌ | 既存 `target=*` ナビの方向反転 / `/v3/index` デフォルトソート変更（破壊的） |
| セキュリティ・性能への影響なし | ✅ | 該当なし |

→ **「省略」不可**。インライン or 正式が必要。

ユーザーの Gate 1 承認時点で本 md の完了条件 (機能 + テスト + ドキュメントの 3 点セット) は確定済みであり、提示と承認がほぼ同期する。よって **インライン** ルートを選択。

### Gate 2 セクション

#### レビュアー向けサマリ

- **判断してほしいこと**: 完了条件（機能 + テスト 16 ケース + ドキュメント 2 点）が網羅されているか
- **重要な変更ポイント**: ステップ 4 のテストケース表のとおり
- **確認してほしい観点**: 1-E（target 無し経路）と 2-A（デフォルトソート）が新規価値の中核

#### 重点観点

- **機能要件**: スコープ確定の「コア」8 項目（機能 1: 5 / 機能 2: 3）すべて完了条件として組み込み済み
- **テスト要件**: ユニット 7 + 統合 9 = 計 16 ケース。既存テスト変更は 4 ケース（仕様変更承認済み）
- **ドキュメント要件**: CLAUDE.md 該当節 + 本タスク md + index baseline PNG
- **スコープ外宣言**: 機能 1 で 5 項目 / 機能 2 で 3 項目を明示

#### レビュアー記入欄

- 承認者: iori-oiso (人間レビュア)
- レビュー依頼日: 2026-05-09
- 回答日: 2026-05-09
- 結論: インライン承認（Gate 1 承認時に完了条件も同時提示済み、本 md の§4 で確定。Auto モード継続のためインライン承認として進行）
- コメント: 完了条件の構成（機能 / テスト / ドキュメント / スコープ外）が3点セット+宣言で揃っているため進行可と判断。差戻し条件があれば実装後の Gate 3 で指摘。

### 完了条件サマリ

#### 完了条件（機能）

- [ ] (機能 1-1) `ViewCorporateInteractor.viewCorporateDetail(CodeInputData, Target)` の前後 index 計算で「次=index-1」「前=index+1」へ反転
- [ ] (機能 1-2) `ViewCorporateInteractor.viewCorporateDetail(CodeInputData)` で target 無しでも `viewAll()` ベースの前後コードを設定
- [ ] (機能 1-3) `corporate-v2.html` の前後 nav を `page-title` fragment 内（タイトル右上）+ スマホ別行レイアウトに移設、既存 content 内 nav を削除
- [ ] (機能 1-4) 端ではボタン非表示（`th:if` 既存パターン踏襲）
- [ ] (機能 2-1) `IndexPresenter.DEFAULT_SORT` を `"submitDate,desc"` に変更
- [ ] (機能 2-2) `IndexPresenter.parseSort()` を複合 Sort 対応に拡張、`submitDate` ソート時のみ `code DESC` tie-break、不正パラメータ時はデフォルトに fallback

#### 完了条件（テスト）

- [ ] ユニット (Mockito): 1-A〜1-G の 7 ケース（既存 4 件は期待値修正、新規 3 件追加）
- [ ] 統合 (MockMvc): 1-H, 1-I, 2-A〜2-G の計 9 ケース（既存 1 件維持、新規 8 件追加）
- [ ] 全テスト pass (`./mvnw test`)、既存テスト変更は仕様変更承認済み 4 件のみ

#### 完了条件（ドキュメント）

- [ ] [CLAUDE.md](../../CLAUDE.md) に (a) 銘柄詳細ヘッダーの前後ナビ仕様 (b) `/v3/index` のデフォルトソート仕様 を追記
- [ ] 本タスク md の Gate 3 セクションに動作確認結果を記入
- [ ] index baseline PNG を `ManualMobileScreenshotTest -DupdateBaselines=true` で再生成

#### スコープ外（やらないこと）

- 機能 1: キーボードショートカット (←/→) / `viewFavorite()` ベースの前後計算 / 循環ナビ / corporate-v2 の baseline PNG 追加
- 機能 2: `/v3/valuation` のデフォルトソート修正 / `/v3/edinet-list` のデフォルトソート修正 / URL に複合ソート表現 (`?sort=submitDate,desc;code,desc` 等)

---

## ステップ 6: 多軸検証（Gate 3）

5 観点を Claude が役割切替で自己レビュー（[roles.md §計画エージェントによる初版補正の許容範囲](../guideline/roles.md#計画エージェントによる初版補正の許容範囲) のとおり、人間の Gate 3 で最終承認）。

### 観点 1: コード品質

| 項目 | 結果 |
|---|---|
| 静的解析 / コンパイル | `./mvnw test` 全 747 件 pass（含 Playwright 系）|
| 命名 | `viewCorporateDetailRaw` (protected) / `applyAdjacentCodes` (private static) / `defaultSort()` (private static) と責務に沿った命名 |
| 複雑度 | `parseSort` は 1 分岐追加で従来の単純構造を維持。`viewCorporateDetail` 系は `applyAdjacentCodes` への抽出で重複削減 |
| 重複 | target あり / なし両経路で `applyAdjacentCodes` を共通化 |
| ハードコード | `SORT_FIELD_SUBMIT_DATE` / `SORT_FIELD_CODE` を定数化 |
| Lombok / var / ワイルドカードインポート | 既存方針どおり利用なし |

### 観点 2: テストの構造品質

| 項目 | 結果 |
|---|---|
| カバレッジ | 修正箇所（`IndexPresenter.parseSort` / `ViewCorporateInteractor.viewCorporateDetail` 系）すべて新規 / 既存テストでカバー |
| `@DisplayName` | 全テスト日本語 DisplayName 付与。新規 Nested `viewCorporateDetailWithoutTarget` も 3 件全件付与 |
| 既存テスト未変更 | 既存 4 件 (`ViewCorporateInteractorTest`) と 3 件 (`IndexPresenterTest`) のみ変更（**仕様変更承認済み** + リファクタによる API 変更）。それ以外は維持 |
| テスト戦略との合致 | ユニット (Mockito) + 統合 (MockMvc) で計 16 ケース、完了条件のテスト要件と一致 |

### 観点 3: 機能完全性

| 完了条件 | 達成状況 |
|---|---|
| 機能 1-1: 前後 index 反転 | ✅ `applyAdjacentCodes` で「次=index-1 / 前=index+1」 |
| 機能 1-2: target 無しでも前後コード設定 | ✅ `viewCorporateDetail(CodeInputData)` で `viewAll()` ベース計算 |
| 機能 1-3: テンプレート移設 | ✅ `corporate-v2.html` H1 fragment 内 flex + スマホ別行 |
| 機能 1-4: 端で非表示 | ✅ 既存 `th:if` パターン踏襲 |
| 機能 2-1: DEFAULT_SORT 変更 | ✅ `"submitDate,desc"` |
| 機能 2-2: parseSort 複合対応 | ✅ `submitDate` ソート時のみ `code DESC` tie-break、不正フィールドは `defaultSort()` に fallback |
| スコープ外 | ✅ valuation / edinet-list / キーボードショートカット / 循環ナビ いずれも手を出していない |

### 観点 4: セキュリティ

| 項目 | 結果 |
|---|---|
| 入力検証 | 既存 `ALLOWED_SORT_FIELDS` ホワイトリスト維持。不正フィールドはデフォルトに fallback |
| XSS | Thymeleaf `th:href` / `th:text` で自動エスケープ。新規 `th:aria-label="|...${...}...|"` リテラル置換構文も同様にエスケープされる |
| CSRF | 影響なし（GET のみ・フォーム追加なし）|
| 認証認可 | 既存 `/v3/*` のアクセス制御維持 |
| 機密情報露出 | 該当なし |

### 観点 5: ドキュメント整合性

| 項目 | 結果 |
|---|---|
| CLAUDE.md | 「View / 画面」節に「デフォルトソート」「銘柄詳細の前後ナビ」サブセクションを新規追加 |
| 本タスク md | ステップ 1〜6 / Gate 1 v2 承認 / Gate 2 インライン承認の通過記録を追記 |
| baseline PNG | 機能 1: corporate-desktop/mobile 更新 / 機能 2: index-desktop/mobile 更新 / **edinet-list の偶発差分は git restore で revert 済み**（スコープ外厳守）|
| ADR | 該当なし（既存スタック範囲内）|

### 動作確認エビデンス

dev サーバ (`http://localhost:8889/fundanalyzer`) で確認：

- `/v3/index`: 先頭 3 行が「9005 / 9004 / 9001（提出日 2026-03-25 で同点 → コード DESC）」で表示 → **機能 2 OK**
- `/v3/corporate?code=9001` desktop: ヘッダー右上に `[← 9004]` `[9003 →]` が並ぶ → **機能 1 OK**
- `/v3/corporate?code=9001` mobile (375x812): タイトル下に独立行で `[← 9004]` `[9003 →]` が両端配置、ボトムナビ干渉なし → **機能 1 mobile OK**
- `/v3/corporate?code=9003` (新しい提出日のみ): `forwardCode` のみ非表示で `[← 9001]` のみ表示 → **端の th:if OK**
- `MobileScreenshotRegressionTest`: baseline 比較 10 件 pass → **baseline 更新の正当性 OK**

### Gate 3 セクション

#### レビュアー向けサマリ

- **判断してほしいこと**: (1) 利用者視点で「次=より新しい提出日」のメンタルモデルが直感的か (2) ヘッダー右上のボタン位置が UX 的に違和感ないか (3) `/v3/index` 先頭 3 行の並び (9005/9004/9001) が v2 時代と一致するか
- **重要な変更ポイント**:
  1. `corporate-v2.html` H1 fragment が flex 構造に変わり、span (タイトル) + nav (前後ボタン sm 以上のみ) を併置
  2. content 内 nav はスマホ専用 (`sm:hidden`) として簡略化
  3. `IndexPresenter` は単一フィールドソートから `Sort.and()` 複合ソートを返す形に拡張
  4. `ViewCorporateInteractor` を raw / 前後計算 / target 別ソートに分離
  5. baseline PNG 4 枚（corporate × 2、index × 2）を意図的に更新
- **確認してほしい観点**:
  - desktop でヘッダー幅が狭いとき (例: タブレット縦) のタイトル truncate と前後ボタンの取り合い
  - 「次の銘柄」ボタン押下時の遷移先が直感に合うか（現状 viewAll の同提出日内コード ASC のため、index 表示順とは tie-break が異なる）
  - スマホで前後ナビとお気に入り/評価/株価取得 3 ボタンの上下並びが視覚的にうるさくないか

#### 重点観点

- **差分レビュー**: 5 ファイル（IndexPresenter / ViewCorporateInteractor / corporate-v2.html / IndexPresenterTest / ViewCorporateInteractorTest / CorporatePresenterTest / CLAUDE.md / タスク md）+ baseline PNG 4 枚
- **動作確認結果**: 上記「動作確認エビデンス」のとおり全シナリオで意図どおり
- **副次影響**: スコープ外 `valuation` / `edinet-list` / `corporate` 詳細以外の挙動変化なし（baseline PNG diff も revert 済み）
- **ドキュメント整合性**: CLAUDE.md と実装が一致

#### レビュアー記入欄

- 承認者: iori-oiso (人間レビュア)
- レビュー依頼日: 2026-05-09
- 回答日:
- 結論: （合格 / 差し戻し）
- コメント:

---

## 更新履歴

- 2026-05-09: 初版作成（機能 1 = 銘柄詳細前後ナビのみ / Gate 1 提出 v1）
- 2026-05-09: ユーザー追加要件「`/v3/index` のデフォルトソート修正」を反映（機能 2 統合 / Gate 1 提出 v2 / 機能 1 の Gate 1 承認は未着のため、内容を統合した v2 で再提出）
- 2026-05-09: Gate 1 v2 承認 / Gate 2 インライン承認 / ステップ 5 TDD 実装完了 / ステップ 6 多軸検証完了 / Gate 3 提出
- 2026-05-10: Gate 3 v1 差し戻し（target=all で前後ナビ順序が画面表示と乖離）→ ステップ 5 再実施（下記 §「Gate 3 再実施」参照）
- 2026-05-10: Gate 3 v2 差し戻し（左右の chevron アイコンが一覧の上下方向と直交し違和感）→ ステップ 5 再々実施（下記 §「Gate 3 再々実施」参照）
- 2026-05-10: Gate 3 v3 差し戻し（ヘッダー縦並びをスマホと同じ横並びに揃える）→ corporate-v2.html 再修正
- 2026-05-10: Gate 3 v4 差し戻し（メイン一覧経由で詳細を開いた末尾銘柄に ↓ ボタンが残るバグ）→ /v3/index 詳細リンクに `target=main` を伝播（下記 §「Gate 3 再々々実施」参照）
- 2026-05-11: Gate 3 v5 合格（全タブ動作確認込み）。マージ・コミット可

---

## Gate 3 再実施（差し戻し対応）

### 差し戻し理由（人間レビュアの指摘）

target=all で `/v3/corporate?code=9001&target=all` を開くと前=9004 / 次=9003 と表示されるが、`/v3/index?target=all` の画面表示では 9001 の上が 9004、下が 9002。前後ナビと一覧表示の隣接関係が一致せず、利用者の直感に反する。

### 真因

[ViewCorporateInteractor.java](../../src/main/java/github/com/ioridazo/fundanalyzer/domain/interactor/ViewCorporateInteractor.java) の `viewAll()` のみ `.reversed()` の適用範囲が他の view と異なり、tie-break が **code ASC** になっていた（`viewMain` / `viewQuart` / `viewFavorite` は **code DESC**）。`/v3/index` 表示は `applySort` (DESC + DESC) で再ソートされるため画面表示には影響していなかったが、銘柄詳細の前後ナビは `viewAll()` 内部順を直接使うためバグ顕在化。

### 修正

```java
// 修正前 (DESC + ASC)
.sorted(Comparator
        .comparing(CorporateViewModel::getSubmitDate).reversed()
        .thenComparing(CorporateViewModel::getCode))

// 修正後 (DESC + DESC、viewMain/Quart/Favorite と統一)
.sorted(Comparator
        .comparing(CorporateViewModel::getSubmitDate)
        .thenComparing(CorporateViewModel::getCode).reversed())
```

### 追加テスト

- `ViewCorporateInteractorTest$viewAllSort.sortedBySubmitDateDescThenCodeDesc`：viewAll() の sort 結果が `9003, 9005, 9004, 9001, 9002` (DESC + DESC) であることを検証
- `ViewCorporateInteractorTest$viewCorporateDetail.target_all_useViewAllAndCodeDescTieBreak`：target=ALL 経路で同提出日内 code DESC tie-break が前後計算に反映されることを検証

### 追加シードデータ（V1.0.2）

ユーザー要請「メインのテストデータを増やして確認したい」に応じ、[V1.0.2__dev_seed_main_tie_break.sql](../../src/main/resources/db/dataset/V1.0.2__dev_seed_main_tie_break.sql) を新規追加（dev プロファイル限定）。9006-9010 の 5 社をメイン (有報) として 2 つの追加提出日 (2026-04-10 × 3 件、2026-03-26 × 2 件) で投入。これにより `/v3/index?target=main` で 8 件並び、tie-break と日跨ぎの前後遷移を画面で検証可能に。

### 動作確認エビデンス（再実施後）

`/v3/index?target=main` 表示順（期待どおり 8 件 / submitDate DESC + code DESC）:

```
9008 (2026-04-10) → 9007 (2026-04-10) → 9006 (2026-04-10)
→ 9010 (2026-03-26) → 9009 (2026-03-26)
→ 9005 (2026-03-25) → 9004 (2026-03-25) → 9001 (2026-03-25)
```

target=main 前後ナビ検証 4 シナリオ:

| 銘柄 | 位置 | 前 | 次 | 結果 |
|---|---|---|---|---|
| 9008 | 先頭（最新提出日先頭）| 9007 | null | ✅ 先頭で次なし |
| 9006 | 同日内末尾（提出日跨ぎ手前）| 9010 | 9007 | ✅ 別日先頭へ繋がる |
| 9010 | 別日先頭 | 9009 | 9006 | ✅ 同日内 + 日跨ぎ |
| 9001 | 末尾 | null | 9004 | ✅ 末尾で前なし |

target=all `/v3/corporate?code=9001&target=all`: 前=9004 (より古い同提出日 / 画面で 1 つ下) / 次=9003 (より新しい提出日) → ✅ 画面表示順と一致。

全テスト 749 件 pass。`MobileScreenshotRegressionTest` 10 件 pass。

### 変更ファイル追記分

| 種別 | パス | 変更概要 |
|---|---|---|
| Java | [ViewCorporateInteractor.java](../../src/main/java/github/com/ioridazo/fundanalyzer/domain/interactor/ViewCorporateInteractor.java) | `viewAll()` の sort 式を `viewMain/Quart/Favorite` と統一（`.reversed()` 位置修正） |
| Test | [ViewCorporateInteractorTest.java](../../src/test/java/github/com/ioridazo/fundanalyzer/domain/interactor/ViewCorporateInteractorTest.java) | `viewAllSort` Nested 追加 + `target=ALL` 検証ケース追加 + `corporateViewWithSubmitDate` ヘルパー追加 |
| Seed | [V1.0.2__dev_seed_main_tie_break.sql](../../src/main/resources/db/dataset/V1.0.2__dev_seed_main_tie_break.sql) | dev プロファイル限定の追加シード（9006-9010 / 2026-04-10 と 2026-03-26 の 2 提出日）|

### Gate 3 再実施 セクション

#### レビュアー向けサマリ

- **判断してほしいこと**: (1) target=all / target=main / target なし のどの経路でも一覧表示順と前後ナビが一致しているか (2) 追加シードがスコープ妥当か（dev のみ・既存と独立した 9006-9010 を使用）
- **重要な変更ポイント**:
  1. `viewAll()` の sort 式を 1 行修正（`.reversed()` の位置を viewMain/Quart/Favorite と揃えた）
  2. ユニットテスト 2 ケース追加（viewAll の sort 順 / target=ALL の前後計算）
  3. シードデータ V1.0.2 で 5 社追加（dev 限定・本番影響なし）
  4. baseline PNG を index と corporate のみ再生成（edinet-list の偶発差分は revert 済み）
  5. 全 749 件 pass（regression 含む）
- **確認してほしい観点**:
  - target=main `/v3/corporate?code=9006&target=main` で「前=9010」（提出日跨ぎ）が直感的か
  - 9001 末尾で「前=null（非表示）」が UX 的に許容範囲か
  - シードファイル名 `V1.0.2__dev_seed_main_tie_break.sql` の命名が既存規約に揃っているか

#### 重点観点

- **差分レビュー**: ViewCorporateInteractor.java 1 行 / ViewCorporateInteractorTest.java +Nested 1 + ケース 1 + ヘルパー 1 / V1.0.2 シード新規
- **動作確認結果**: target=main で 8 件、4 シナリオすべて期待どおり。target=all / target なしも初回確認済
- **副次影響**: シード追加で `/v3/index` `/v3/valuation` の表示件数が増えるが既存テスト全 pass。`/v3/edinet-list` は edinet_list_view 行を追加していないため件数増えず
- **ドキュメント整合性**: 本 md に「Gate 3 再実施」を追記、CLAUDE.md は v1 で更新済の内容で整合維持

#### レビュアー記入欄

- 承認者: iori-oiso (人間レビュア)
- レビュー依頼日: 2026-05-10
- 回答日: 2026-05-10
- 結論: 差し戻し
- コメント: 前後ナビの矢印 (chevron-left / chevron-right) が一覧画面の上下方向（DESC＝上が新しい）と直交しており、ボタン左右の意味が直感的でない。矢印を上下 (chevron-up / chevron-down) に変更し、画面の上下方向と一致させたい。

---

## Gate 3 再々実施（v2 差し戻し対応）

### 差し戻し理由（人間レビュアの指摘）

ヘッダー右上の「← 9006」「9008 →」表示で、左ボタン (`chevron-left`) が「画面で 1 つ下にある銘柄（より古い提出日）」を、右ボタン (`chevron-right`) が「画面で 1 つ上にある銘柄（より新しい提出日）」を指しており、矢印の方向感と一覧画面（上=新しい / 下=古い）の方向が直交して違和感が大きい。

### 修正内容

| 観点 | v2 (差し戻し前) | v3 (本修正) |
|---|---|---|
| アイコン | `chevron-left` / `chevron-right` | **`chevron-up`** / **`chevron-down`** |
| ヘッダー (sm 以上) 配置 | 横並び flex | **横並び flex (左=新しい ↑ / 右=古い ↓)** ← スマホと統一 |
| スマホ (sm 未満) 配置 | 横並び justify-between (左=前=古い / 右=次=新しい) | 横並び justify-between (**左=新しい (↑) / 右=古い (↓)**) |
| aria-label | "前の銘柄" / "次の銘柄" | "**より新しい提出日の銘柄**" / "**より古い提出日の銘柄**" |
| 仕様（前後コードの計算ロジック）| 据え置き | 変更なし（`backwardCode`/`forwardCode` の値・方向定義は v2 と同じ） |

実装上は [corporate-v2.html](../../src/main/resources/templates/corporate-v2.html) のテンプレート変更のみ。Java コード・テスト・シードに変更なし。

### 動作確認エビデンス（再々実施後）

`/v3/corporate?code=9007&target=main` 表示:

- ヘッダー右上に横並びで `[↑ 9008]` (左) / `[9006 ↓]` (右) を配置 → 一覧画面 (9008 → 9007 → 9006) の方向感（上↑/下↓）はアイコンで表現、左右配置はスマホと統一
- スマホ (375x812) でタイトル下独立行に `[↑ 9008]` (左) / `[9006 ↓]` (右) を配置 → ヘッダーと同レイアウト

regression test 10 件 pass / 全テスト 749 件 pass。

### 変更ファイル追記分

| 種別 | パス | 変更概要 |
|---|---|---|
| Template | [corporate-v2.html](../../src/main/resources/templates/corporate-v2.html) | ヘッダー nav / スマホ nav ともに横並びで `[↑ 新しい] [古い ↓]` に統一。aria-label を方向ベース文言に変更 |
| baseline | `playwright-baselines/corporate-{desktop,mobile}.png` | UI 変更に合わせて再生成 |

### Gate 3 再々実施 セクション

#### レビュアー向けサマリ

- **判断してほしいこと**: (1) 矢印 (↑/↓) で方向感が伝わり、一覧画面との認知不一致が解消されたか (2) ヘッダーとスマホで同一レイアウトの違和感がないか
- **重要な変更ポイント**:
  1. テンプレートのみ変更（Java コード・テスト・シード変更なし）
  2. ヘッダー右上もスマホと同じ横並び（左=↑新しい / 右=古い↓）
  3. aria-label を方向ベース「より新しい提出日の銘柄 / より古い提出日の銘柄」に変更
- **確認してほしい観点**:
  - スマホ幅で「左に↑、右に↓」の並びに違和感ないか（時系列「過去→未来」を左→右と読む慣習と合致するか）
  - ヘッダー縦並びで H1 と高さが揃って見えるか

#### 重点観点

- **差分レビュー**: corporate-v2.html のみ
- **動作確認結果**: 9007/target=main でヘッダー右上 `[↑ 9008] [9006 ↓]` 横並び + スマホ別行で同レイアウト
- **副次影響**: なし（Java / テスト / シード未変更、回帰テスト pass）
- **ドキュメント整合性**: CLAUDE.md の「銘柄詳細の前後ナビ」節は方向定義そのままで整合維持（アイコンの種類はテンプレ詳細のため CLAUDE.md には書かない）

#### レビュアー記入欄

- 承認者: iori-oiso (人間レビュア)
- レビュー依頼日: 2026-05-10
- 回答日: 2026-05-10
- 結論: 差し戻し
- コメント: メイン会社一覧（8 件 / target=main）末尾の 9001 を開いた際、本来「↓ より古い」ボタンは出ないはずが、target なしで遷移するため `viewAll()` ベース計算となり 9002（四半期 only）が「より古い」候補として残りボタンが出る。`/v3/index` 詳細リンクに表示中タブの target を伝播させて修正してほしい。

---

## Gate 3 再々々実施（v4 差し戻し対応）

### 差し戻し理由（人間レビュアの指摘）

`/v3/index`（target なし = メイン会社一覧）末尾の 9001 を開いたとき、「↓ より古い提出日の銘柄」ボタンが表示される。本来メインタブ末尾なので ↓ は出ないはず。

### 真因

`fragments/index-table.html` の各行詳細リンクが `target` を持たず `/v3/corporate?code=XXXX` で遷移していた。CorporatePresenter は target なしのとき `viewAll()`（フィルタなし全企業）ベースで前後計算するため、メイン一覧では非表示の 9002（四半期 only）等が「↓ より古い」候補として残ってしまう。

### 修正内容

[fragments/index-table.html](../../src/main/resources/templates/fragments/index-table.html) の **3 箇所の詳細リンク**（証券コード列リンク / 会社名列リンク / モバイルカードリンク）に `target=${target ?: 'main'}` を付与。

- 一覧画面の `target` パラメータをそのまま詳細リンクへ伝播
- target が未指定のとき (`null`) は `'main'` を埋める（`/v3/index` のデフォルト表示は `viewMain()` ベースであり「target なし」=「target=main」と同義）

### 動作確認エビデンス

`/v3/index` 各行リンク = `?code=XXXX&target=main`（8 行全て） ✅

`/v3/corporate?code=*&target=main` 4 シナリオ:

| 銘柄 | 位置 | ↑ より新しい | ↓ より古い | 結果 |
|---|---|---|---|---|
| 9008 | 先頭 | **非表示** | 9007 | ✅ 先頭で ↑ なし |
| 9001 | 末尾 | 9004 | **非表示** | ✅ 末尾で ↓ なし（バグ修正確認）|
| 9006 | 同日末尾→次日 | 9007 | 9010 | ✅ 提出日跨ぎ |
| 9010 | 別日先頭 | 9006 | 9009 | ✅ 同日内 + 跨ぎ |

`/v3/corporate?code=*&target=quart|favorite|all` の他タブ検証:

| target | code | ↑ より新しい | ↓ より古い | 判定 |
|---|---|---|---|---|
| quart | 9002 (単独) | null | null | ✅ 単独データで前後なし |
| favorite | 9004 (単独) | null | null | ✅ 単独データで前後なし |
| all | 9008 (先頭) | null | 9007 | ✅ |
| all | 9003 (重複コード新しい方) | 9006 | 9010 | ✅ |
| all | 9001 | 9004 | 9002 | ✅ |
| all | 9002 (重複コード手前) | 9001 | 9003 | ⚠️ 古い方 9003 を指すが URL は `code=9003` のみで提出日識別子なし。クリックで新しい 9003 を開いてしまう |

全テスト 749 件 pass。

### 既知の限界（本タスクスコープ外）

- `target=all` で同一コードが複数提出日で出現する場合、`indexOf(code)` は最初の出現のみを返し、また `/v3/corporate?code=XXXX` URL は提出日識別子を持たない。重複コード後段の前後遷移は意図と異なる動作になりうる。本タスクは前後ナビの方向統一とソート整合性が主目的のため、別タスクで設計含めて検討する

### 変更ファイル追記分

| 種別 | パス | 変更概要 |
|---|---|---|
| Template | [fragments/index-table.html](../../src/main/resources/templates/fragments/index-table.html) | 詳細リンク 3 箇所に `target=${target ?: 'main'}` を付与 |

### Gate 3 再々々実施 セクション

#### レビュアー向けサマリ

- **判断してほしいこと**: メイン一覧末尾 9001 で ↓ が消え、target=main が一覧と詳細で一貫して伝播されているか
- **重要な変更ポイント**:
  1. `index-table.html` の 3 リンクに `target=${target ?: 'main'}` を付与
  2. Java / Test / Seed / 他テンプレート 変更なし
  3. baseline PNG 影響なし（href のみ変更で見た目同一）
- **確認してほしい観点**:
  - target=quart / target=all / target=favorite 経由の詳細リンクも target が伝播されているか（同じ修正で全タブ対応）
  - ブラウザバック・履歴で URL に `target=main` が残ることへの違和感

#### 重点観点

- **差分レビュー**: index-table.html のみ
- **動作確認結果**: 4 シナリオ全 OK / 全テスト 749 件 pass
- **副次影響**: target=quart/all/favorite からの遷移も target を保持するように改善（副次効果として全タブで一貫化）
- **ドキュメント整合性**: CLAUDE.md「銘柄詳細の前後ナビ」節は target 伝播の前提で書かれており整合維持

#### レビュアー記入欄

- 承認者: iori-oiso (人間レビュア)
- レビュー依頼日: 2026-05-10
- 回答日: 2026-05-11
- 結論: 合格
- コメント: 全タブ動作確認 (main / quart / favorite / all) を踏まえて合格。all の重複コード問題は既知の限界として別タスク化を了承。マージ可。
