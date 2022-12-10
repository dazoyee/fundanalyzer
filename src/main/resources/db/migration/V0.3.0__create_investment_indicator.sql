ALTER TABLE IF EXISTS `analysis_result` ADD COLUMN `bps` FLOAT DEFAULT NULL COMMENT '1株当たり純資産' AFTER `corporate_value`;
ALTER TABLE IF EXISTS `analysis_result` ADD COLUMN `eps` FLOAT DEFAULT NULL COMMENT '1株当たり当期純利益' AFTER `bps`;
ALTER TABLE IF EXISTS `analysis_result` ADD COLUMN `roe` FLOAT DEFAULT NULL COMMENT '自己資本利益率' AFTER `eps`;
ALTER TABLE IF EXISTS `analysis_result` ADD COLUMN `roa` FLOAT DEFAULT NULL COMMENT '総資産利益率' AFTER `roe`;

-- @formatter:off
CREATE TABLE IF NOT EXISTS `investment_indicator`
(
    `id`                          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `stock_id`                    BIGINT UNSIGNED NOT NULL COMMENT '株価ID',
    `analysis_result_id`          BIGINT UNSIGNED NOT NULL COMMENT '企業価値ID',
    `company_code`                CHAR(5)         NOT NULL COMMENT '企業コード',
    `target_date`                 DATE            NOT NULL COMMENT '対象日付',
    `price_corporate_value_ratio` FLOAT           NOT NULL COMMENT '株価企業価値率',
    `per`                         FLOAT                    DEFAULT NULL COMMENT '株価収益率',
    `pbr`                         FLOAT                    DEFAULT NULL COMMENT '株価純資産倍率',
    `graham_index`                FLOAT                    DEFAULT NULL COMMENT 'グレアム指数',
    `document_id`                 CHAR(8)         NOT NULL COMMENT '書類ID',
    `created_at`                  DATETIME        NOT NULL DEFAULT CURRENT_TIME() COMMENT '登録日',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ii` (`company_code`, `target_date`),
    CONSTRAINT `fk_ii_stock_id` FOREIGN KEY (`stock_id`) REFERENCES `stock_price` (`id`),
    CONSTRAINT `fk_ii_analysis_result_id` FOREIGN KEY (`analysis_result_id`) REFERENCES `analysis_result` (`id`),
    CONSTRAINT `fk_ii_document_id` FOREIGN KEY (`document_id`) REFERENCES `document` (`document_id`)
);
-- @formatter:on

ALTER TABLE IF EXISTS `corporate_view` ADD COLUMN `price_corporate_value_ratio` FLOAT DEFAULT NULL COMMENT '株価企業価値率' AFTER `forecast_stock`;
ALTER TABLE IF EXISTS `corporate_view` ADD COLUMN `per` FLOAT DEFAULT NULL COMMENT '株価収益率' AFTER `price_corporate_value_ratio`;
ALTER TABLE IF EXISTS `corporate_view` ADD COLUMN `pbr` FLOAT DEFAULT NULL COMMENT '株価純資産倍率' AFTER `per`;
ALTER TABLE IF EXISTS `corporate_view` ADD COLUMN `graham_index` FLOAT DEFAULT NULL COMMENT 'グレアム指数' AFTER `pbr`;
ALTER TABLE IF EXISTS `corporate_view` ADD COLUMN `bps` FLOAT DEFAULT NULL COMMENT '1株当たり純資産' AFTER `graham_index`;
ALTER TABLE IF EXISTS `corporate_view` ADD COLUMN `eps` FLOAT DEFAULT NULL COMMENT '1株当たり当期純利益' AFTER `bps`;
ALTER TABLE IF EXISTS `corporate_view` ADD COLUMN `roe` FLOAT DEFAULT NULL COMMENT '自己資本利益率' AFTER `eps`;
ALTER TABLE IF EXISTS `corporate_view` ADD COLUMN `roa` FLOAT DEFAULT NULL COMMENT '総資産利益率' AFTER `roe`;
