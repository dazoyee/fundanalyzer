ALTER TABLE IF EXISTS `stock_price` MODIFY COLUMN `source_of` CHAR (1) NOT NULL COMMENT '取得元';

ALTER TABLE IF EXISTS `valuation` DROP COLUMN `goals_stock`;
ALTER TABLE IF EXISTS `valuation` DROP COLUMN `dividend_yield`;
ALTER TABLE IF EXISTS `valuation` DROP COLUMN `corporate_value`;
ALTER TABLE IF EXISTS `valuation` DROP COLUMN `stock_price_of_submit_date`;
ALTER TABLE IF EXISTS `valuation` DROP COLUMN `graham_index_of_submit_date`;
ALTER TABLE IF EXISTS `valuation` DROP COLUMN `document_id`;
ALTER TABLE IF EXISTS `valuation` DROP COLUMN `valuation_id_of_submit_date`;
ALTER TABLE IF EXISTS `valuation` DROP COLUMN `analysis_result_id`;
