ALTER TABLE IF EXISTS `valuation` DROP INDEX `uk_v`;
ALTER TABLE IF EXISTS `valuation` ADD UNIQUE `uk_v` (`company_code`, `target_date`, `submit_date`);
