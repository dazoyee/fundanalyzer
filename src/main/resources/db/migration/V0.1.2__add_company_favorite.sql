ALTER TABLE IF EXISTS `company` ADD COLUMN `favorite` CHAR (1) NOT NULL DEFAULT '0' COMMENT 'お気に入り' CHECK (`favorite` IN ('0', '1')) AFTER `settlement_date`;
