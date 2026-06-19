ALTER TABLE IF EXISTS `industry` ADD COLUMN `operating_profit_weight` DECIMAL(10, 4) NOT NULL DEFAULT 10 COMMENT '営業利益倍率(業種別)';
ALTER TABLE IF EXISTS `industry` ADD COLUMN `current_liabilities_ratio` DECIMAL(10, 4) NOT NULL DEFAULT 1.2 COMMENT '流動負債調整係数(業種別)';
