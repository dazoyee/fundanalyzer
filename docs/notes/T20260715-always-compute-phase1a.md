# タスクノート: 常に計算する方式 フェーズ1a（BPS/EPS/ROE/ROA 読み取り側都度計算化）

関連調査: [T20260713-always-compute-redesign-investigation.md](T20260713-always-compute-redesign-investigation.md)

## 目的

係数非依存指標（BPS/EPS/ROE/ROA）の消費経路を `analysis_result` の永続列読み取りから、
`financial_statement` 由来の `FinanceValue` による都度計算へ切り替える（調査ノート §6.4 フェーズ1a）。
スキーマ変更なし・書き込み経路維持（二重計算＝安全弁）・即時ロールバック可能の構成とする。

## Go 判断の根拠（調査ノート §6.5 の条件に対応）

- **(a) 一致検証**: 本番DBの対象行（document_type_code 120/130、bps 非NULL）全件に対し、
  SQL で財務諸表値から BPS/EPS/ROE/ROA を再計算し永続値と照合した結果、
  相対誤差 2e-5 を超える不一致は **0件**、算出不能も 0件（検証時の対象は 31,776 行）。
- **(b) rim_value バックフィルの扱い**: フェーズ1a のスコープ外として分離（フェーズ1b の Gate 1 論点）。
- **(c) parallelUpdateView 性能**: 追加呼び出しはバッチ文脈に閉じており per-request 経路への影響なし。
  書類ID一致時は取得済み Document を再利用して DB 往復を削減する実装とした。
  実運用のバッチ実行時間は Gate 2 以降の運用で確認する。

## 実装内容

| ファイル | 変更 |
|---|---|
| `AnalysisResult` | `of(entity, financeValue, document)` を追加（旧 `of(entity)` を置換）。corporate_value/rim_value は永続値を凍結、指標は都度計算。入力欠損（`FundanalyzerNotExistException`）・ゼロ除算（`ArithmeticException`）は該当指標のみ null。`calculateBps/Eps/Roe/Roa` を static 化 |
| `IndicatorValue` | 入力を `AnalysisResultEntity` から `AnalysisResult`（都度計算値）へ変更 |
| `InvestmentIndicatorSpecification.insert` | 3引数化（entity=ID/キー用、computed=指標値用、stockPrice） |
| `AnalyzeInteractor.indicate` | analyze 経路は取得済み `FinanceValue` を使い回し、単独経路は document/financeValue を解決して委譲。ガードは都度計算値ベース |
| `ViewCorporateInteractor.parallelUpdateView` | 分析結果に対応する書類の財務諸表値から再構築。`FundanalyzerBadDataException` は該当企業のみスキップ |

## Gate 1（設計の承認）

- 影響範囲: 参照層のみ（indicate 経路・corporate_view 生成バッチ）。状態層・データ層の変更なし
- テスト戦略: 新ファクトリの単体テスト（凍結値/都度計算値の分離・欠損時の null）、
  parallelUpdateView の都度計算経路と例外継続のテストを追加
- 完了条件: 全テストグリーン・検証エージェントの CRITICAL/HIGH 指摘ゼロ
- スコープ外: 書き込み停止（フェーズ1b）・列削除（フェーズ1c）・investment_indicator 都度計算化（フェーズ2）・rim_value バックフィルの扱い
- 通過: 人間レビュアが AskUserQuestion 経由で設計を承認

## 検証（実装作業後）

- code-reviewer エージェントによるレビューで HIGH 2件（例外伝播の拡大・新規分岐のテスト欠如）、
  MEDIUM 3件（N+1・例外種別・書類紐付けの前提）を検出し、すべて修正済み
- 全テストスイート（playwright 除外）860件グリーン
- checkstyle はリポジトリ既存ベースラインと同傾向（新規クラス追加なし）

## 付随調査: P3 バックフィル skipped の性質（本番DB）

バックフィル対象条件に残る行の内訳（調査時点の本番実測）:

| 分類 | 件数 | 性質 |
|---|---|---|
| 5指標すべて NULL（= skipped 母集団） | 4,401 | 指標算出の必須科目欠損 |
| うち当期純利益のみ欠損 | 4,082 | PL はスクレイピング済み（scraped_pl='1'）だが「当期純利益」科目が抽出されていない。**再スクレイピングではなくキーワード照合の拡充が必要な領域** |
| うち入力が揃っているのに skip | 312 | corporate_value 計算の入力欠損が原因と推定（別クエリで確定） |
| rim のみ NULL | 2,392 | ROE≤0 による構造的 NULL（正常） |
| roa のみ NULL（+rim） | 65 | 総資産欠損による構造的 NULL（ROA は例外を投げない設計） |

- skip 対象書類はすべて scraped_bs/pl/ns='1' であり、書類未処理による欠損は存在しない
- 企業マスタ欠損による skip は 0件
- 提出年分布は 2018〜2022 に分散（特定期の障害ではなくデータ品質の恒常的性質）
