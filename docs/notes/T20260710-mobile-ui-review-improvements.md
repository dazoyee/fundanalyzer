# Task T20260710: スマホ画面レビューと改善（v3 全 5 画面）

- 着手日: 2026-07-10
- 担当: iori-oiso（計画）+ Claude Code（レビュー・検証）+ Codex（実装）
- 関連リンク:
    - モバイル UI 刷新（基盤）: [T20260502-mobile-ui-renewal.md](T20260502-mobile-ui-renewal.md)
    - 分析ダッシュボード関連: 直近の valuation 統合・analysis 新設タスク群

---

## 1. レビュー結果

本番実データ（prod 環境）と dev シードデータの両方で、モバイルビューポート（375x812）にて
v3 全 5 画面＋操作系（ドロワー・アクションメニュー・ダークモード）を Playwright で確認した。
index / edinet-list / corporate 本体は T20260502 のカード化・ボトムバーが機能しており良好。
以下の問題を検出し、同一手順で 2 回検証して確定した。

| # | 優先度 | 画面 | 問題 |
|---|---|---|---|
| 1 | P1 | analysis バックテスト | 「バケット別リターン」「散布図」カードがモバイルで幅 570px 超に広がり、layout の `overflow-x-hidden` にクリップされ右側の列（的中率・収束度等）が閲覧不能。原因は `xl:grid-cols-[minmax(0,…)]` のみでモバイル側の暗黙 1 カラムに `minmax(0,1fr)` 制約が無く、グリッド子がテーブルの min-content 幅まで拡張するため |
| 2 | P1 | analysis 個別推移 | ランキング行タップで読み込む推移チャートが画面外（下方）に挿入され、スクロール誘導が無く変化に気づけない |
| 3 | P2 | analysis 全タブ | 4 テーブル（割安度ランキング / バケット別リターン / 業種内訳 / 業種別割安度ランキング）が他画面のモバイルカード化パターン未適用 |
| 4 | P2 | corporate | ⋮ アクションメニューの「お気に入り」「注目」が icon-only フラグメントの流用でラベルが無く空行に見える（タッチターゲット 32px） |
| 5 | P2 | edinet-list | 検索 placeholder が 375px で途切れる |
| 6 | P3 | edinet-list-detail | 書類メタカードの dt ラベル（書類種別等）が文字単位で折返す |
| 7 | P3 | 全画面 | 長いリスト（index は 25 件/ページで全高 11,000px 超）で先頭へ戻る手段が無い |

## 2. 実装内容

Java 本体は不変（テンプレート + フロント JS + テストのみ）。カード化は T20260502 §3.5 の規範
（`hidden sm:block` テーブル + `block sm:hidden` カード、`data-mobile-card`、dl 2 カラムグリッド）に準拠。

| ファイル | 変更 |
|---|---|
| `fragments/analysis-backtest.html` | グリッドに `grid-cols-1` を追加（#1 修正）。バケット別リターン・業種内訳をカード化（#3）。正負色分け（emerald/rose）はテーブル側と同一ロジックを維持 |
| `analysis-v2.html` | 割安度ランキングをカード化（#3）。カードにも `hx-get`（推移読込）を付与しタップ操作を維持 |
| `fragments/analysis-distribution.html` | 業種別割安度ランキングをカード化（#3） |
| `scripts/app.js` | `htmx:afterSwap` で対象が `#analysis-chart` かつ sm 未満のとき `scrollIntoView`（#2） |
| `corporate-v2.html` | メニュー行にラベル「お気に入り」「注目」を追加。ラベル span クリックでボタンへ委譲（span は button の祖先でないためバブリング再入が構造的に発生しない）（#4） |
| `edinet-list-v2.html` | placeholder を「提出日で検索（YYYY-MM-DD）」に短縮（#5） |
| `fragments/edinet-document-card.html` | dl を `grid-cols-[auto,minmax(0,1fr)]` + dt `whitespace-nowrap` に変更（#6） |
| `layout-v2.html` | モバイル専用「トップへ戻る」ボタン（44px、scrollY>600 で表示、サイドバー展開中は非表示）（#7） |
| `Phase8ScreenSnapshotTest.java` | analysis の mobile/desktop でカードとテーブルの相互排他を DOM アサーション追加 |

## 3. 検証

- `./mvnw test -DexcludedGroups=playwright` 全件成功
- `./mvnw test -Dtest=Phase8ScreenSnapshotTest` 10 件成功（新規アサーション含む）
- `npm run build`（Tailwind/esbuild）成功
- Playwright 実画面（375x812）で全項目 2 回ずつ確認: はみ出しゼロ（scrollWidth=375）・カード表示・
  タップ→チャートへのスクロール・ラベルタップでのトグル・placeholder 全表示・dt 折返し解消・
  トップへ戻るボタンの出現/動作/ドロワー展開時の非表示
- desktop（1280x800）はテーブル可視・カード非表示で不変

## 4. 継続課題（本タスクではスコープ外）

- `corporate-favorite-button` / `corporate-star-button` フラグメント単体のタッチターゲットが 32px（44px 未満）。
  index カード等の共用箇所のレイアウトに影響するため、変更する場合は全使用箇所の確認が必要
- ランキング行・カードのキーボード操作（`role="button"` / `tabindex` / Enter 発火）が未対応（既存テーブル行から踏襲した挙動）
- お気に入り/注目トグルの状態表現（`aria-pressed` の付与、可視ラベルの状態連動）
- corporate の前後ナビが証券コードのみ表示（会社名の併記は Presenter 拡張が必要）

## 5. エビデンス

`e2e-tests/ui-mobile-review/`（.gitignore 対象）に修正前後のスクリーンショットを保存。
`prod-*.png` が修正前（本番実データ）、`after-*.png` が修正後（dev）。
