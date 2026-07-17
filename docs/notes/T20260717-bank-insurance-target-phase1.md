# 銀行業・保険業の分析対象化 フェーズ1（純資産ベース企業価値モデル）

- 調査の一次資料: [`T20260717-excluded-industry-target-feasibility.md`](T20260717-excluded-industry-target-feasibility.md)（対象化可否の実XBRL検証）

## 何を変えたか

`app.config.scraping.no-industry` から銀行業・保険業を外し、両業種に**純資産ベース企業価値モデル**（`AnalysisResult.CorporateValueModel.NET_ASSET`）を適用して分析対象化した。

```
NET_ASSET: corporate_value = ( 経常利益 × operating_profit_weight（年換算） ＋ 純資産 − 新株予約権 ) ÷ 株式総数
STANDARD : corporate_value = ( 営業利益 × operating_profit_weight（年換算） ＋ 流動資産 − 流動負債×係数 ＋ 投資その他の資産 − 固定負債 ) ÷ 株式総数
```

- 銀行・保険の業法様式（銀行法・保険業法）の財務諸表には流動/固定区分と営業利益が存在しないため、収益力項を経常利益に、資産項を純資産に置き換えた（現行式の「収益力＋ネット資産」構造の移植）。
- 新株予約権の控除は ROE の分母と整合（欠損時はゼロ扱い）。年換算は利益項のみに適用（STANDARD と同一の weighting 構造）。

## 設計判断（恒久的なWHY）

| 判断 | 理由 |
|---|---|
| モデル選択は業種IDで分岐（`IndustrySpecification.isNetAssetModel`、対象業種は `app.config.analysis.net-asset-model-industry` で設定） | 明示的で影響範囲が限定され、既存企業の算出結果に影響しない。科目存在による自動判定は既存対象企業の一時的な取得失敗でも発動し得るため不採用 |
| 同一業種内は単一モデル（一般様式で提出する保険代理店系にも NET_ASSET を適用） | 業種内でモデルが混在すると業種別係数の検証（モデル検証の意味論）が崩れる。NET_ASSET の入力（経常利益・純資産・株式総数）は一般様式にも存在する |
| 係数は既存の `operating_profit_weight`（銀行業・保険業とも設定済み）を経常利益に流用 | 倍率の由来（業種別PER×(1−実効税率)）は税前利益に掛けるロジックであり経常利益とも整合。割引率分布を見て再調整の余地あり（変更時は一括再計算ルールに従う） |
| IFRS提出者（東京海上・MS&AD・SOMPO・ライフネット生命ほか）は対象外のまま | IFRSのPLに経常利益が存在せず入力が取れない。科目が取れないため既存の all-error → removeDocument フローで処理対象外化される（この暫定挙動を許容）。jpigp 対応・代替入力設計は後続フェーズ |
| 日本郵政・信金中央金庫は `no-company` 除外を維持 | 両社とも業法様式だが業種が「サービス業」「その他金融業」のため業種ID分岐に乗らない。対象化には企業単位のモデル指定の仕組みが必要（後続フェーズで検討）。信金中央金庫は上場証券が優先出資証券で株式総数・株価の意味論も特殊 |
| `bs_subject` に「資産の部合計」「負債の部合計」「純資産の部合計」を追加（V0.4.4） | 業法様式のBSは部単位の合計ラベルを使い、科目照合は完全一致のため未登録だと読み飛ばされる。これにより銀行・保険でも純資産等が取得でき、RIM・BPS・EPS・ROE・ROA は既存コードのまま算出される |

## 変更ファイル

- `src/main/resources/db/migration/V0.4.4__add_bs_subject_statutory_format_totals.sql`
- `src/main/resources/application.yml`（no-industry 縮小・net-asset-model-industry 追加）
- `domain/value/PlSubject.java`（ORDINARY_INCOME）・`domain/value/FinanceValue.java`（ordinaryIncome）
- `domain/domain/specification/FinancialStatementSpecification.java`（経常利益の取得）
- `domain/domain/specification/IndustrySpecification.java`（isNetAssetModel）
- `domain/value/AnalysisResult.java`（CorporateValueModel・NET_ASSET 算出式）
- `domain/interactor/AnalyzeInteractor.java`（analyze・recalculate のモデル選択）
- テスト: `AnalysisResultTest`・`IndustrySpecificationTest`・`AnalyzeInteractorTest` ほか `FinanceValue.of` シグネチャ追従

## リリース後に必要な運用

1. 過去提出分の取り込み: 銀行・保険の書類は `document` テーブルに登録済みだが未ダウンロードのため、過去の提出日（有報は毎年6月に集中）について `POST /v1/document/analysis` を提出日ごとに実行する
2. `GET /v1/admin/analysis/recalculate/preview` で既存業種の変更が 0 件であることを確認する（リグレッション確認）
3. 株価取得の開始: 対象化時点から取得が始まるため、平均株価・バリュエーションが安定するまで取得期間を要する
4. スクレイピングエラーログから IFRS・特殊様式の残存提出者を検出し、後続フェーズの対象リストに繰り入れる
