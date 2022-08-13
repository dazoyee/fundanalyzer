ALTER TABLE IF EXISTS `scraping_keyword` ADD COLUMN `priority` INT (2) DEFAULT NULL COMMENT '優先度' AFTER `keyword`;
