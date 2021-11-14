-- industry
INSERT INTO `industry` (`id`, `name`)
VALUES (1, '水産・農林業'),
       (2, '建設業'),
       (3, '卸売業'),
       (4, '非鉄金属'),
       (5, '石油・石炭製品'),
       (6, '内国法人・組合（有価証券報告書等の提出義務者以外）'),
       (7, 'サービス業'),
       (8, '機械'),
       (9, '鉱業'),
       (10, '金属製品'),
       (11, '不動産業'),
       (12, '情報・通信業'),
       (13, '食料品'),
       (14, '小売業'),
       (15, '化学'),
       (16, '繊維製品'),
       (17, '輸送用機器'),
       (18, '証券、商品先物取引業'),
       (19, 'ガラス・土石製品'),
       (20, '電気機器'),
       (21, 'その他製品'),
       (22, 'パルプ・紙'),
       (23, 'その他金融業'),
       (24, '医薬品'),
       (25, 'ゴム製品'),
       (26, '精密機器'),
       (27, '鉄鋼'),
       (28, '銀行業'),
       (29, '保険業'),
       (30, '陸運業'),
       (31, '倉庫・運輸関連'),
       (32, '海運業'),
       (33, '空運業'),
       (34, '電気・ガス業'),
       (35, '外国法人・組合'),
       (36, '外国法人・組合（有価証券報告書等の提出義務者以外）'),
       (37, '外国政府等'),
       (38, '個人（組合発行者を除く）'),
       (39, '個人（非居住者）（組合発行者を除く）'),
       (40, 'その他（仮登録用）')
;

-- scraping_keyword
insert into `scraping_keyword` (`financial_statement_id`, `keyword`, `remarks`)
values ('1', 'jpcrp_cor:ConsolidatedBalanceSheetTextBlock', '連結貸借対照表'),
       ('1', 'jpcrp_cor:BalanceSheetTextBlock', '貸借対照表'),
       ('2', 'jpcrp_cor:ConsolidatedStatementOfIncomeTextBlock', '連結損益計算書'),
       ('2', 'jpcrp_cor:StatementOfIncomeTextBlock', '損益計算書'),
       ('4', 'jpcrp_cor:IssuedSharesTotalNumberOfSharesEtcTextBlock', '株式総数'),
       ('1', 'jpcrp_cor:QuarterlyConsolidatedBalanceSheetTextBlock', '四半期連結貸借対照表'),
       ('2', 'jpcrp_cor:YearToQuarterEndConsolidatedStatementOfComprehensiveIncomeSingleStatementTextBlock',
        '四半期連結損益及び包括利益計算書'),
       ('2', 'jpcrp_cor:YearToQuarterEndConsolidatedStatementOfIncomeTextBlock', '四半期連結損益計算書及び四半期連結包括利益計算書'),
       ('1', 'jpcrp_cor:QuarterlyBalanceSheetTextBlock', '四半期財務諸表'),
       ('2', 'jpcrp_cor:YearToQuarterEndStatementOfIncomeTextBlock', '四半期損益計算書'),
       ('1', 'jpigp_cor:CondensedQuarterlyConsolidatedStatementOfFinancialPositionIFRSTextBlock', '要約四半期連結財務諸表'),
       ('2', 'jpigp_cor:CondensedYearToQuarterEndConsolidatedStatementOfProfitOrLossIFRSTextBlock', '要約四半期連結損益計算書')
;

-- bs_subject
insert into `bs_subject` (`outline_subject_id`, `detail_subject_id`, `name`)
values ('1', '3', '流動資産'),
       ('1', '1', '流動資産合計'),
       ('2', null, '有形固定資産'),
       ('3', null, '無形固定資産'),
       ('4', '2', '投資その他の資産'),
       ('4', '1', '投資その他の資産合計'),
       ('5', '1', '固定資産合計'),
       ('6', null, '繰延資産'),
       ('7', null, '資産合計'),
       ('8', '3', '流動負債'),
       ('8', '1', '流動負債合計'),
       ('9', '3', '固定負債'),
       ('9', '1', '固定負債合計'),
       ('10', null, '負債合計'),
       ('11', null, '投資主資本'),
       ('12', null, '剰余金'),
       ('13', null, '投資主合計'),
       ('14', null, '純資産合計'),
       ('15', null, '負債純資産合計'),
       ('1', '2', '流動資産計'),
       ('5', '2', '固定資産計'),
       ('8', '2', '流動負債計'),
       ('9', '2', '固定負債計'),
       ('9', '4', '非流動負債合計')
--        ('4', '3', 'その他の金融資産')
;

-- pl_subject
insert into `pl_subject` (`outline_subject_id`, `detail_subject_id`, `name`)
values ('1', null, '営業収益'),
       ('2', null, '営業費用'),
       ('3', '1', '営業利益'),
       ('3', '2', '営業利益又は営業損失（△）'),
       ('3', '3', '営業利益又は営業損失(△)'),
       ('3', '4', '営業損失（△）'),
       ('3', '5', '営業損失(△)'),
       ('3', '6', '営業利益（△は損失）'),
       ('3', '7', '営業利益（△損失）'),
       ('3', '8', '営業活動に係る利益'),
       ('3', '9', '営業損失'),
       ('3', '10', '営業損失(△）'),
       ('3', '11', '営業利益(△損失)'),
       ('3', '12', '営業利益又は損失（△）'),
       ('3', '13', '営業利益(△は損失)'),
       ('3', '14', '営業損失（△)'),
       ('4', null, '営業外収益'),
       ('5', null, '営業外費用'),
       ('6', null, '経常利益'),
       ('7', null, '特別損失'),
       ('8', null, '税引前当期純利益'),
       ('9', null, '法人税、住民税及び事業税'),
       ('10', null, '法人税等合計'),
       ('11', null, '当期純利益'),
       ('12', null, '前期繰越利益'),
       ('13', null, '当期未処分利益又は当期未処理損失（△）')
;
