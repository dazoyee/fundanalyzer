ALTER TABLE IF EXISTS `document` ADD CONSTRAINT `downloaded_check` CHECK (`downloaded` IN ('0', '1', '5', '9'));
ALTER TABLE IF EXISTS `document` ADD CONSTRAINT `decoded_check` CHECK (`decoded` IN ('0', '1', '5', '9'));
