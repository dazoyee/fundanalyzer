ALTER TABLE IF EXISTS `company` ADD COLUMN `star` CHAR (1) NOT NULL DEFAULT '0' COMMENT '注目' CHECK (`star` IN ('0', '1')) AFTER `removed`;
