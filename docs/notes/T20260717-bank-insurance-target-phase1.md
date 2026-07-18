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

1. 過去提出分の取り込み: 銀行・保険の書類は `document` テーブルに登録済みだが未ダウンロードのため、過去の提出日（有報は毎年6月に集中）について `POST /v1/document/analysis` を期間指定で実行する。`fromToDate` パラメータの形式は **`MM/dd/uuuu ~ MM/dd/uuuu`（米国式）**。処理は `@Async` のため即時に 302 が返り、実体はサーバ側で日付ループ（一覧取得→DL→スクレイプ→分析→ビュー更新）が走る
2. `GET /v1/admin/analysis/recalculate/preview` の件数は**テーブル全件数（走査対象）であり「変更される件数」ではない**（`AnalyzeInteractor.previewRecalculation` は countAll を返す）。既存業種のリグレッション確認には使えないため、テストスイートと翌日の定時バッチ確認で担保する
3. 株価取得の開始: 対象化時点から取得が始まるため、平均株価・バリュエーションが安定するまで取得期間を要する
4. スクレイピングエラーログから IFRS・特殊様式の残存提出者を検出し、後続フェーズの対象リストに繰り入れる

## 本番反映で顕在化した潜在バグと hotfix

- v2.5.0 反映後の初回取り込み（提出日 2026-06-15）で、分析・ビュー更新が `NullPointerException` で全企業について失敗した
- 原因: `SubjectSpecification.findBsSubjectList` / `findPlSubjectList` のソート `Comparator.comparing(detailSubjectId)` が null 非対応で、**同一 outline 内に detail_subject_id が NULL の行と非 NULL の行が混在すると `String.compareTo(null)` になる**。V0.4.4 が outline 7/10/14 に業法様式ラベル行を追加したことで混在が初めて発生し顕在化した（それまでは各 outline 単一行でソート比較自体が走らなかった）
- 修正: `Comparator.nullsFirst(Comparator.naturalOrder())` 化（NULL＝標準ラベル行を先頭に維持するため科目解決の優先順位は不変）。リグレッションテストを `SubjectSpecificationTest` に追加
- 教訓: **科目マスタに「同一 outline への行追加」を行うマイグレーションは、detail_subject_id の NULL 混在ソートを踏む**。以後の科目追加時は findXxSubjectList の挙動をテストで確認する
- 調査手順の教訓: 本ログ（ECS 形式）の `error.stack_trace` は1行 JSON 内に `\r\n` エスケープで格納される。`Select-String` の行抽出で足りるが、フィールド抽出の正規表現が改行エスケープを跨げず「スタックなし」に見えることがある。生行をファイル経由で取得して解析するのが確実

## 過去分取り込みの実施記録（v2.5.0〜v2.5.3）

全期間（2017-01-01〜2026-07-17）の提出日について `POST /v1/document/analysis` を期間チャンクで投入し、銀行・保険の過去有報のダウンロード・スクレイピング・分析・ビュー更新を完走した。

### 実施中に顕在化した既存潜在バグと対処（時系列）

| リリース | 事象 | 原因と対処 |
|---|---|---|
| v2.5.1 | 初回取り込みで全企業の分析・ビュー更新がNPE | `SubjectSpecification.findBsSubjectList/findPlSubjectList` のソートがdetail_subject_idのNULL非対応（V0.4.4で同一outline内にNULL/非NULL混在が発生し顕在化）。`Comparator.nullsFirst` 化 |
| v2.5.2 | 期間バッチが `NoSuchElementException` で全体停止 | `EdinetDocumentSpecification.parsePeriod` のparentDocId判定が `map(...).isPresent()`（Optional二重包み）でNULL親に `orElseThrow` が発火。`flatMap` 化＋`AnalysisService.executePartOfMain` に日次try-catchを追加し1日の失敗で期間全体が止まらないようにした |
| v2.5.3 | DB瞬断後にバッチスレッドが無限ブロック（2回発生・サービス再起動で暫定復旧） | Connector/Jの `socketTimeout` 既定が無限のため、死んだTCP接続の読み待ちで永久停止（HikariCPの `connection-timeout` はプール取得のみが対象）。JDBC URLに `connectTimeout=10000&socketTimeout=180000` を設定 |

### 取り込み結果の検証

- 地銀（七十七銀行）: 企業ページに最新企業価値・平均企業価値・RIMが表示され、複数年の分析結果が生成されている（NET_ASSETモデルの実働確認）
- JGAAP保険（T&D HD）: 同様に企業価値が算出されている
- IFRS移行済み保険（東京海上HD）: **IFRS移行前のJGAAP年度分は正常に取り込まれ**、IFRS年度分の書類のみ既存のエラー→処理対象外フローに乗る（設計どおり）。IFRS移行済み提出者の最新年度は企業価値が更新されない点に留意
- 業務エラーは把握済みの事象のみ（毒データ・DB瞬断起因）で、データ欠損が疑われる日付はビュー全体更新（`GET /v1/update/corporate/view`）で回復させた

### 運用上の知見

- 期間バッチはEDINETレートリミッタが実効ボトルネックのため、チャンクの並行投入による短縮効果は限定的。DB負荷観点でも逐次投入を推奨
- 書類の少ない期間は高速、四半期報告書期（廃止前の2・5・8・11月）と有報期（6月）・半期報告書期（11月）は低速という処理ペース特性がある
