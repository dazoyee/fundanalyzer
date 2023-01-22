ALTER TABLE IF EXISTS `valuation` ADD COLUMN `graham_index` FLOAT DEFAULT NULL COMMENT 'グレアム指数' AFTER `goals_stock`;
ALTER TABLE IF EXISTS `valuation` ADD COLUMN `graham_index_of_submit_date` FLOAT DEFAULT NULL COMMENT '提出日のグレアム指数' AFTER `stock_price_of_submit_date`;
