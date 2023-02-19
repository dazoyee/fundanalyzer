ALTER TABLE IF EXISTS `document` MODIFY COLUMN `downloaded` CHAR (1) NOT NULL DEFAULT '0' COMMENT 'ダウンロードステータス' CHECK (`downloaded` IN ('0', '1', '5', '9'));
ALTER TABLE IF EXISTS `document` MODIFY COLUMN `decoded` CHAR (1) NOT NULL DEFAULT '0' COMMENT 'ファイル解凍ステータス' CHECK (`decoded` IN ('0', '1', '5', '9'));
