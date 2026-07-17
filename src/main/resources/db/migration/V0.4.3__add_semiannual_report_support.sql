ALTER TABLE IF EXISTS `financial_statement`
    ADD COLUMN `quarter_type_tmp` CHAR (1) DEFAULT NULL COMMENT '四半期種別' CHECK (`quarter_type_tmp` IN ('1', '2', '3', '4', 'H'));
UPDATE `financial_statement`
SET `quarter_type_tmp` = `quarter_type`;
ALTER TABLE IF EXISTS `financial_statement`
    DROP COLUMN `quarter_type`;
ALTER TABLE IF EXISTS `financial_statement`
    RENAME COLUMN `quarter_type_tmp` TO `quarter_type`;

ALTER TABLE IF EXISTS `analysis_result`
    ADD COLUMN `quarter_type_tmp` CHAR (1) DEFAULT NULL COMMENT '四半期種別' CHECK (`quarter_type_tmp` IN ('1', '2', '3', '4', 'H'));
UPDATE `analysis_result`
SET `quarter_type_tmp` = `quarter_type`;
ALTER TABLE IF EXISTS `analysis_result`
    DROP COLUMN `quarter_type`;
ALTER TABLE IF EXISTS `analysis_result`
    RENAME COLUMN `quarter_type_tmp` TO `quarter_type`;

INSERT INTO `scraping_keyword` (`financial_statement_id`, `keyword`, `remarks`)
VALUES ('1', 'jpcrp_cor:SemiAnnualConsolidatedBalanceSheetTextBlock', '中間連結貸借対照表'),
       ('1', 'jpcrp_cor:SemiAnnualBalanceSheetTextBlock', '中間貸借対照表'),
       ('1', 'jpcrp_cor:Type1SemiAnnualConsolidatedBalanceSheetTextBlock', '第一種中間連結貸借対照表'),
       ('1', 'jpcrp_cor:Type1SemiAnnualBalanceSheetTextBlock', '第一種中間貸借対照表'),
       ('1', 'jpcrp_cor:SemiAnnualConsolidatedBalanceSheetUSGAAPTextBlock', '中間連結貸借対照表（USGAAP）'),
       ('1', 'jpigp_cor:CondensedSemiAnnualConsolidatedStatementOfFinancialPositionIFRSTextBlock', '要約中間連結財政状態計算書（IFRS）'),
       ('1', 'jpcrp_cor:CondensedSemiAnnualConsolidatedStatementOfFinancialPositionJMISTextBlock', '要約中間連結財政状態計算書（JMIS）'),
       ('2', 'jpcrp_cor:SemiAnnualConsolidatedStatementOfIncomeTextBlock', '中間連結損益計算書'),
       ('2', 'jpcrp_cor:SemiAnnualConsolidatedStatementOfComprehensiveIncomeSingleStatementTextBlock', '中間連結包括利益計算書'),
       ('2', 'jpcrp_cor:SemiAnnualStatementOfIncomeTextBlock', '中間損益計算書'),
       ('2', 'jpcrp_cor:Type1SemiAnnualConsolidatedStatementOfIncomeTextBlock', '第一種中間連結損益計算書'),
       ('2', 'jpcrp_cor:Type1SemiAnnualConsolidatedStatementOfComprehensiveIncomeSingleStatementTextBlock', '第一種中間連結包括利益計算書'),
       ('2', 'jpcrp_cor:Type1SemiAnnualStatementOfIncomeTextBlock', '第一種中間損益計算書'),
       ('2', 'jpcrp_cor:SemiAnnualConsolidatedStatementOfIncomeUSGAAPTextBlock', '中間連結損益計算書（USGAAP）'),
       ('2', 'jpcrp_cor:SemiAnnualConsolidatedStatementOfComprehensiveIncomeSingleStatementUSGAAPTextBlock', '中間連結包括利益計算書（USGAAP）'),
       ('2', 'jpigp_cor:CondensedSemiAnnualConsolidatedStatementOfProfitOrLossIFRSTextBlock', '要約中間連結損益計算書（IFRS）');
