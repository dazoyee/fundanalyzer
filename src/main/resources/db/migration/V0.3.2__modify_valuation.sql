ALTER TABLE IF EXISTS `valuation` MODIFY COLUMN `difference_from_submit_date` FLOAT NOT NULL COMMENT '提出日との株価の差';
ALTER TABLE IF EXISTS `valuation` MODIFY COLUMN `submit_date_ratio` FLOAT NOT NULL COMMENT '提出日との株価比率';

ALTER TABLE IF EXISTS `valuation` ADD COLUMN `stock_price_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '株価ID' AFTER `target_date`;
ALTER TABLE IF EXISTS `valuation` ADD COLUMN `dividend_yield` FLOAT DEFAULT NULL COMMENT '予想配当利回り' AFTER `goals_stock`;
ALTER TABLE IF EXISTS `valuation` ADD COLUMN `investment_indicator_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '投資指標ID' AFTER `dividend_yield`;
ALTER TABLE IF EXISTS `valuation` ADD COLUMN `valuation_id_of_submit_date` BIGINT UNSIGNED DEFAULT NULL COMMENT '提出日の企業価値評価ID' AFTER `corporate_value`;
ALTER TABLE IF EXISTS `valuation` ADD COLUMN `analysis_result_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '企業価値ID' AFTER `graham_index_of_submit_date`;

ALTER TABLE IF EXISTS `valuation` DROP INDEX `uk_v`;
ALTER TABLE IF EXISTS `valuation` ADD UNIQUE `uk_v` (`company_code`, `submit_date`, `target_date`);

ALTER TABLE IF EXISTS `valuation` ADD CONSTRAINT `fk_v_stock_price` FOREIGN KEY (`stock_price_id`) REFERENCES `stock_price`(`id`) ON DELETE SET NULL;
ALTER TABLE IF EXISTS `valuation` ADD CONSTRAINT `fk_v_investment_indicator` FOREIGN KEY (`investment_indicator_id`) REFERENCES `investment_indicator` (`id`) ON DELETE SET NULL;
ALTER TABLE IF EXISTS `valuation` ADD CONSTRAINT `fk_v_analysis_result` FOREIGN KEY (`analysis_result_id`) REFERENCES `analysis_result` (`id`);
ALTER TABLE IF EXISTS `valuation` ADD CONSTRAINT `fk_v_id` FOREIGN KEY (`valuation_id_of_submit_date`) REFERENCES `valuation` (`id`);
