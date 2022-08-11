ALTER TABLE IF EXISTS `corporate_view` DROP INDEX `uk_cv_company_code`;
ALTER TABLE IF EXISTS `corporate_view` ADD UNIQUE `uk_cv` (`code`, `latest_document_type_code`);
