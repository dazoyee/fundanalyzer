# タスクノート: 当期純利益の科目名バリエーション拡充

関連: [T20260715-always-compute-phase1a.md](T20260715-always-compute-phase1a.md)（P3 skipped 調査で 4,082 件の未抽出を特定）

## 問題

`analysis_result` の指標が算出できない書類のうち大多数は、PL がスクレイピング処理済み
（`document.scraped_pl='1'`）にもかかわらず `financial_statement` に当期純利益
（`pl_subject.outline_subject_id='11'`）の値が存在しない。

## 原因

- 科目照合は `SubjectSpecification.findPlSubject()` の **`pl_subject.name` との完全一致**で行われ、
  一致しない行は例外なく黙って読み飛ばされる（`ScrapingInteractor.pl()` の flatMap）
- 科目が1件も一致しなくてもテーブル構造が定形なら処理は成功扱いとなり `scraped_pl='1'` になる
- `pl_subject` の outline='11' には「当期純利益」「当期純損失（△）」系の5表記のみが登録されており、
  連結財務諸表の標準表記（非支配株主持分との区分表示）である **「親会社株主に帰属する当期純利益」系が未登録**

## 対処（マイグレーション）

`V0.4.1__add_pl_subject_net_income_attributable_to_owners.sql` で「親会社株主に帰属する〜」系
5表記を outline='11' グループに追加。指標算出（`FinancialStatementSpecification`）は
outline グループ全体から値を検索するため、**コード変更は不要**。

> 注意: `db/dataset/V1.0.0__init_insert.sql` は dev 専用（prod の Flyway locations は
> `db/migration` のみ）。マスタ拡充は必ず `db/migration` 側に書くこと。

## 既存書類の再処理手順（別途実施）

キーワード追加は**新規スクレイピング分にのみ**自動で効く。処理済み書類の救済には以下が必要:

1. **対象抽出**: 当期純利益が欠損している `document_id` 一覧を取得
   （`scraped_pl='1'` かつ outline='11' の financial_statement 行なし）
2. **前提確認**: 本番サーバの `pathDecode` 配下にデコード済みファイルが現存するか確認
   （現存すれば EDINET 再ダウンロード不要。`XbrlScraping.findFile` はディレクトリを毎回再走査する）
3. **ステータス戻し**: 対象書類の `document.scraped_pl` を `'0'` に UPDATE
   （戻す機能はアプリに存在しないため DB 直接更新。バックアップを取ってから実施）
4. **再スクレイピング**: `POST /v2/scrape/id`（documentId 指定）を対象分実行
   （日次スケジューラは `app.scheduler.analysis.past-days` の範囲しか遡らないため使えない）
5. **検証**: 少数サンプルで 3〜4 を試行し、financial_statement に当期純利益が入ることを確認してから全量実施
6. **反映**: フェーズ1a（読み取り都度計算）により、値が入れば指標は即座に画面反映される
   （バックフィル不要。rim_value は非遡及方針により埋めない）

- 重複 INSERT は `financial_statement` の一意制約で自動スキップされるため安全
- 本番 `pl_subject` の outline='11' には手動追加とみられる detail_subject_id='6' の行が存在する
  （マイグレーションの採番はこれを避けて '7' 以降とした）。マスタが Flyway 管理外で手動運用されて
  きた環境があるため、マスタ系マイグレーション追加時は本番の現行データ確認を必須とする
- 実施前の残確認: 対象書類の実ファイルで実際の表記を grep で一次確認すること

## スコープ外（同根の別課題）

- IFRS 採用企業（`jpigp_cor:*` 科目）の当期純利益表記は outline='11' に未登録の可能性があり別途調査
