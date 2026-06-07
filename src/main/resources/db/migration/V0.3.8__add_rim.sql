ALTER TABLE IF EXISTS `industry` ADD COLUMN `cost_of_equity` DECIMAL(6, 4) NOT NULL DEFAULT 0.08 COMMENT '資本コスト(業種別・RIM用)';
ALTER TABLE IF EXISTS `analysis_result` ADD COLUMN `rim_value` FLOAT DEFAULT NULL COMMENT 'RIM理論株価' AFTER `corporate_value`;
