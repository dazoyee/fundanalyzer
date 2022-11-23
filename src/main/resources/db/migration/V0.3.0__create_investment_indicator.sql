ALTER TABLE IF EXISTS `analysis_result` ADD COLUMN `bps` FLOAT DEFAULT NULL COMMENT '1株当たり純資産' AFTER `corporate_value`;
ALTER TABLE IF EXISTS `analysis_result` ADD COLUMN `eps` FLOAT DEFAULT NULL COMMENT '1株当たり当期純利益' AFTER `bps`;
ALTER TABLE IF EXISTS `analysis_result` ADD COLUMN `roe` FLOAT DEFAULT NULL COMMENT '自己資本利益率' AFTER `eps`;
ALTER TABLE IF EXISTS `analysis_result` ADD COLUMN `roa` FLOAT DEFAULT NULL COMMENT '総資産利益率' AFTER `roe`;
