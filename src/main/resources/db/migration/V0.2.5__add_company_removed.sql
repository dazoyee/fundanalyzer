ALTER TABLE IF EXISTS `company` ADD COLUMN `removed` CHAR (1) NOT NULL DEFAULT '0' COMMENT '除外フラグ' CHECK (`removed` IN ('0', '1')) AFTER `favorite`;
