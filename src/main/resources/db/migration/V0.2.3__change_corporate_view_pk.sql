ALTER TABLE IF EXISTS `corporate_view` MODIFY COLUMN `latest_document_type_code` CHAR (3) NOT NULL;
ALTER TABLE IF EXISTS `corporate_view` DROP PRIMARY KEY;
ALTER TABLE IF EXISTS `corporate_view` ADD PRIMARY KEY (`code`, `latest_document_type_code`);
