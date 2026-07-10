# T20260704A — 評価分析ダッシュボード Phase A（個別銘柄の評価推移）

## タスク要約（1 行）
評価データのトレンドが見えない課題に対し、独立ダッシュボード `/v3/analysis` を新設し、その第1歩として個別銘柄の評価推移チャートを実装する。

## Gate 進捗 早見表
| Gate | 状態 | 承認者 | 回答日 |
|---|---|---|---|
| Gate 1: 設計の承認 | 合格 | iori-oiso | 2026-07-04 |
| Gate 2: 最終確認   | 合格 | iori-oiso | 2026-07-04 |

## 影響範囲タイプ 早見
- 参照層: **該当**（新規 `AnalysisPresenter` / `analysis-v2.html` / `fragments/analysis-chart` / `web/view/model/analysis/*`、`layout-v2.html`・`app.js` 追記）
- 状態層: **該当なし**（新規永続化・状態遷移なし。既存ビューの読み取り表示のみ）
- データ層: **該当なし**（DB スキーマ変更なし。既存 `getSummaryChartView` / `viewValuation(code)` / `findAllCompanyValuationView` を読むのみ）
- インフラ影響: **すべて該当なし**（新規依存なし。単一銘柄取得のため大量データ処理なし）

## 関連
- 事前計画: `docs/plans/valuation-app-analysis-dashboard.md`
- 後続: Phase B（バックテスト）, Phase C（分布・業種統合削除）
- プロトタイプ: `T20260704-analysis-phaseA-attachments/prototype-analysis.html`（Artifact: https://claude.ai/code/artifact/569edb71-71be-4e87-8573-80dd29ac3760 ）

---

## Gate 1: 設計の承認

### レビュアー向けサマリ
- **判断してほしいこと**: `/v3/analysis` Phase A の画面設計（3タブ構成＋割安度ランキング＋個別推移チャート①②）・影響範囲・完了条件を承認してよいか。
- **重要な変更ポイント**:
  - 新規画面 `/v3/analysis`（3タブの器。Phase A では「個別推移」タブを実装、バックテスト/分布は後続 Phase のプレースホルダ）
  - サイドナビに「分析」項目を追加（`layout-v2.html` デスクトップ/モバイルの2箇所）
  - チャート①「企業価値 vs 調整後株価」は **`getSummaryChartView` を流用**、チャート②「割安度/グレアム/提出日比率 推移」は **`viewValuation(code)` を流用**（新規データ経路を作らない）
  - 既存 `/v3/valuation` 画面・DB は不変更
- **確認してほしい観点**:
  - プロトタイプのレイアウト・情報設計が意図に合うか（→ 合意済み）
  - 既存資産の流用方針（新規経路を増やさない）

### 重点観点
- 影響範囲分析（参照層 / 状態層 / データ層）の網羅 → 上記「早見」＋ステップ3.2
- 三本柱（テスト戦略 / セキュリティ方針 / ドキュメント計画）→ ステップ3.4
- スコープ確定（コア=個別推移 / 後回し=バックテスト・分布 / 対象外=投資UX）
- 完了条件（機能 / テスト / ドキュメント / スコープ外）→ ステップ3.6

### レビュアー記入欄
| 項目 | 内容 |
|---|---|
| 承認者 | iori-oiso |
| レビュー依頼日 | 2026-07-04 |
| 回答日 | 2026-07-04 |
| **結論** | **合格** |
| **コメント** | プロトタイプ合意後の設計を承認。Phase A 実装へ。 |

---

## Gate 2: 最終確認

### レビュアー向けサマリ
- **判断してほしいこと**: 実装が個別推移の意図どおりに動くか / 既存 valuation 画面に副次影響がないか
- **重要な変更ポイント**: `/v3/analysis` 新設（3タブ・割安度ランキング・チャート①②）、ナビ2箇所追加、既存 valuation 不変更
- **確認してほしい観点**: 実レンダリング（Playwright 10/10 PASS・スクショ）、割安度×100表示の妥当性、検証で修正した3不具合

### 重点観点
- 差分レビュー / 動作確認結果 / 副次影響 / ドキュメント整合性

### レビュアー記入欄
| 項目 | 内容 |
|---|---|
| 承認者 | iori-oiso |
| レビュー依頼日 | 2026-07-04 |
| 回答日 | 2026-07-04 |
| **結論** | **合格** |
| **コメント** | 実レンダリング確認済み（Playwright 10/10）。検証で3不具合を修正。チャート実描画はユニットテスト+実績パターンで担保しコミット承認。 |

---

<details>
<summary>📁 AI 作業ログ（ステップ 1〜5）</summary>

### ステップ 1: 把握・整理
- 関連ドキュメント: `docs/plans/valuation-app-analysis-dashboard.md`（精査4観点を反映した決定版）
- 関連コード:
  - チャート①: `ViewService.getSummaryChartView` → `ViewCorporateInteractor.viewSummaryChart`（`SummaryChartData`：企業価値時系列＋株価調整済み時系列）
  - チャート②: `ViewValuationInteractor.viewValuation(CodeInputData)`（targetDate 降順の `CompanyValuationViewModel`：discountRate/grahamIndex/submitDateRatio 保持）
  - ランキング: `ViewSpecification.findAllCompanyValuationView`（`valuation_view` の最新1点/社）
  - 画面追加の型: `IndexPresenter`（`@Controller`＋定数化＋2エンドポイント）、`layout-v2.html`（ナビ2箇所）、`app.js`（`htmx:load` でチャート bootstrap）
- スコープ確定: コア=個別推移タブ / 後回し=バックテスト(B)・分布(C) / 対象外=投資UX(A/D/E)・期間絞り込み(F)

### ステップ 2: プロトタイピング（合意済み）
- 静的モック `prototype-analysis.html` を Artifact で提示（上記 URL）。
- **認識合わせ結果（2026-07-04 合意）**:
  - レイアウト: **左右2ペイン**（左=割安度ランキング、右=詳細＋チャート）
  - チャート②: **3指標を1グラフに重ねて表示**（割安度/グレアム/提出日比率）
  - ランキング列: **現状（割安度・グレアム・対象日）**
- 実装上の申し送り: チャート②は3指標が別スケールのため、**正規化または複数 y 軸**で読みやすさを担保する（重ね表示の合意を尊重しつつ実装で対処）。

### ステップ 3: 設計（影響設計＋テスト設計）
#### 3.2 影響範囲分析
- **参照層（該当）**: 追加 = `web/presenter/AnalysisPresenter`（`@Controller`・定数化・2エンドポイント）、`templates/analysis-v2.html`（`layout:decorate`）、`templates/fragments/analysis-chart.html`、`web/view/model/analysis/`（チャート整形用 record）。追記 = `layout-v2.html` ナビ2箇所、`scripts/app.js` のチャート bootstrap。
- **状態層（該当なし）**: 新規永続化・状態遷移なし。読み取り表示のみ。
- **データ層（該当なし）**: DB スキーマ変更なし。既存の read 系メソッド（`getSummaryChartView`/`viewValuation`/`findAllCompanyValuationView`）のみ利用。新規 SQL 不要。

#### 3.3 インフラ影響チェック
- 全カテゴリ **該当なし**（新規依存追加なし、単一銘柄取得で大量データ処理なし、外部 I/O なし、スキーマ不変）。

#### 3.4 品質設計の三本柱
- **テスト戦略**: `AnalysisPresenter` を MockMvc 統合テスト（既存 `IndexPresenterTest`/`ValuationPresenterTest` と同型）。チャート整形（JSON 化・ソート・フォールバック）はロジックを純粋メソッドに寄せてユニットテスト。カバレッジ 80% 目標。**dev(H2) は評価データが空**のため、Playwright/手動確認用に valuation/stock_price/analysis_result 数銘柄分の **シードデータ**（test/dev リソース）を用意。
- **セキュリティ方針**: 認証必須（`SecurityConfig` の `.anyRequest().authenticated()`）で自動保護、新規認可要件なし。ユーザー入力は `code`（ルーティング正規表現 `[0-9]{4,5}` で制約）と検索キーワード（既存 valuation と同じく Thymeleaf エスケープ）。CSP は既存 `script-src` で data 属性方式が通る。SQL は Doma パラメータ化。→ 新規リスクなし。
- **ドキュメント計画**: プロジェクト `CLAUDE.md` 画面一覧に `/v3/analysis`→`AnalysisPresenter` を追記。`develop/document/アプリ構成図.drawio` は Phase A では新規 UseCase を追加しない（既存流用＋Presenter 追加のみ）ため更新は軽微／任意。ER 図は不変。

#### 3.5 設計ドキュメント更新
- 実装前更新は不要（スキーマ・構成に大きな変更なし）。CLAUDE.md 画面表は実装と同時に更新。

#### 3.6 テスト設計（自然言語ケース）＋完了条件
- `GET /v3/analysis` → 200 / `analysis-v2` / 3タブ / 割安度ランキング（`findAllCompanyValuationView` を discountRate 降順）
- `GET /v3/analysis/{code}/chart` → 200 / fragment / チャート①②の JSON が model に格納
- `code` 不正形式 → ルーティング不一致（`[0-9]{4,5}`）で 404
- データ無し銘柄 → チャート JSON が `"[]"` フォールバック、空表示メッセージ
- 検索キーワードでランキング絞り込み
- ナビ「分析」が active

**完了条件**:
- 機能: `/v3/analysis` 表示、3タブ、割安度ランキング（降順・検索）、行選択で個別推移チャート①②、ナビ「分析」追加、ダーク対応
- テスト: `AnalysisPresenter` MockMvc（上記ケース）、Playwright 表示確認、dev シードで実データ確認、カバレッジ 80%、Checkstyle 通過、ビルド（`npm run build` 含む）成功
- ドキュメント: `CLAUDE.md` 画面表更新、本タスクmd の Gate 2 記入
- スコープ外（宣言）: バックテスト(B)・分布(C)・投資UX(A/D/E)・期間絞り込み/エクスポート(F)

### ステップ 4: 実行サイクル
- 実装順: ①器（`AnalysisPresenter` + `analysis-v2.html` + ナビ + タブ骨格）→ ②ランキング（`findAllCompanyValuationView` 降順）→ ③チャート①（`getSummaryChartView` 流用 + `app.js` bootstrap）→ ④チャート②（`viewValuation(code)` 流用、正規化/複数軸）→ ⑤シードデータ + テスト。
- TDD: Presenter は MockMvc を先に用意。既存テストの変更なし（新規追加のみ）。

### ステップ 5: 多軸検証
- **コード品質**: 既存 `IndexPresenter`/`index-v2.html`/`app.js` の作法を忠実に踏襲（ワイルドカードimportなし・明示的型・コンストラクタDI・Javadoc・例外catch・`@Value` 設定注入）。
- **テスト構造**: `AnalysisPresenterTest` は Mockito+直接呼び出し（既存流儀）、`@Nested`/`@DisplayName` 日本語、JUnit5標準アサーション。**Tests run: 3, Failures: 0, Errors: 0（Java 17）**。
- **機能完全性**: `/v3/analysis`（ランキング）・`/{code}/chart`（チャート①②JSON）・存在しないコードの空JSON分岐を網羅。
- **セキュリティ**: 認証必須（`.anyRequest().authenticated()`）で自動保護、`code` は正規表現制約、SQLはDoma、CSPは既存で許容。新規リスクなし。
- **ドキュメント整合性**: CLAUDE.md 画面表への追記は Gate 2 前に対応予定。
- **検証で発見・修正した不具合（Codex出力レビュー結果）**:
  1. ランキングのソート `nullsLast(naturalOrder()).reversed()` は null が先頭に来る → `nullsLast(reverseOrder())` に修正（降順・null末尾）。
  2. 割安度の表示が生比率（例 2.12）→ 既存 corporate 画面に合わせ `×100`（212%）表示に修正。
  3. `AnalysisPresenterTest` の空JSONテストに `throws JsonProcessingException` 欠落 → 追加。

### コミット・検証履歴
- **環境の重要事項**: Maven ビルドは **`JAVA_HOME=openjdk@17` 必須**（CLAUDE.md 記載）。Java 25 だと Lombok の注釈処理が失敗し `CorporateViewModel` の getter 未生成 →無関係な `CorporateViewBean` がコンパイルエラーになる。今回この誤用で一時ハマった。
- **検証結果（Java 17）**: `clean` main+test コンパイル成功 / `AnalysisPresenterTest` 3/3 PASS・BUILD SUCCESS / frontend `npm`(Maven plugin) ビルドで app.css・app.js に analysis のクラス・チャートコードを確認。
- **checkstyle**: `checkstyle:check` は生成コード込みで全体8566違反＝リポジトリ全体が未通過（phase/goal未バインド・除外未設定）で実質ゲート機能なし。新規コードは既存慣習に一致。
- コミットは未実施（Gate 2 の動作確認後）。

</details>

---

## 更新履歴
| 日付 | 概要 |
|---|---|
| 2026-07-04 | 起票。Gate 1 準備。影響範囲3層・スコープ確定。 |
| 2026-07-04 | プロトタイプ合意（左右2ペイン/チャート②重ね/ランキング現状列）。設計・三本柱・テスト設計・完了条件を記入し Gate 1 レビュー依頼。 |
