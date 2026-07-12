# Gate1 レビューメモ: 本番データ分析対応（P1–P6）

本番データ分析レビュー（`docs/research/` の分析）から起票した6件の対応をまとめて実装した。本メモは Gate1（人間レビュア承認）に向けた論点集約であり、承認判断の入口とする。

## 変更セット概要

| ID | 対応 | 種別 | tracked 成果物 |
|---|---|---|---|
| P1 | 業種別係数を東証33業種PERベースに更新 | Flyway migration | `db/migration/V0.4.0__update_industry_coefficient.sql` |
| P2 | 財務取得値の乖離バリデーション＋警告通知 | ロジック追加 | `FinancialStatementSpecification` ほか |
| P3 | 指標バックフィル機構＋管理エンドポイント | ロジック追加 | `AnalysisResultDao`/`AnalysisResultSpecification`/`AnalysisController` ほか |
| P4 | 四半期の企業価値を営業利益のみ年換算（案B） | ロジック修正 | `AnalysisResult` |
| P5 | PER/グレアム指数を正値時のみ算出 | ロジック修正 | `IndicatorValue` |
| P6 | ダミー行削除の手順整備 | 手動SQL手順書のみ | なし（下記参照） |

## 検証状況

- `mvn compile -DskipTests`: 成功
- 影響テスト（P1–P6 の変更・追加クラス）: 全件成功
- 統合時に検出・修正したテスト欠陥2件（いずれもテスト側の期待値誤り、本番コードは無変更）:
  - `InvestmentIndicatorSpecificationTest` のヘルパが `AnalysisResultEntity` の位置引数を誤り、BPS値が `corporateValue` スロットに入り `bps` が null 化 → 引数マッピングを是正
  - `AnalysisControllerTest.backfillIndicator` のリダイレクトURL期待値が `:` `,` を過剰エンコード → Spring 標準エンコードの実挙動に是正

## Gate1 で判断を要する論点

### 1. P1↔P3 の遡及方針の非対称性
- P1（係数変更）は既存 `analysis_result` へ遡及しない一方、P3（指標バックフィル）は指標列を既存行へ遡及する。同一行に「係数は旧・指標列は新」の状態が併存しうる。
- 対象列は分離しており計算競合はないが、運用上の整合方針を承認事項とする。実装では両者を連結していない。

### 2. P3↔P6 の実行順序
- P3 のバックフィル対象抽出はダミー行（`document_type_code=120`）を除外しない。
- 本番適用時は **P6（ダミー行削除）→ P3（バックフィル）** の順を前提とする。両計画本文に相互参照がないため、運用手順として明記が必要。

### 3. P1 migration のプレースホルダ業種
- 暫定係数を持たない業種はプレースホルダ値を維持する設計。維持の是非を確認する。

### 4. `AnalysisController` のコンストラクタ引数数
- 新エンドポイント追加によりコンストラクタ引数が Checkstyle の `ParameterNumber` 閾値を超過。設計上許容するか、依存の集約を行うかを判断する。

## DB適用の前提

- migration 実行・手動SQL・バックフィル実走は **すべて Gate1 承認後**に行う。本変更セットはコード・手順の整備のみで、DBへの実適用は含まない。
- 手動SQL手順書（P5 クレンジング・P6 削除）は個人作業領域 `docs/plans/sql/` に置き、リポジトリには含めない。適用時に手元で参照する。
- 指標バックフィルの管理エンドポイント: `POST /v1/admin/analysis/backfill/indicator`（ドライラン: `GET .../preview`）。事前バックアップを切り戻しの前提とする。

## Gate1 通過記録

- レビュア:
- 承認日:
- 各論点の決定:
