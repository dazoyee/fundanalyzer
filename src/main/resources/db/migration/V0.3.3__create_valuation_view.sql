ALTER TABLE IF EXISTS `stock_price` MODIFY COLUMN `source_of` CHAR (1) NOT NULL COMMENT '取得元';

ALTER TABLE IF EXISTS `valuation` DROP COLUMN `goals_stock`;
ALTER TABLE IF EXISTS `valuation` DROP COLUMN `dividend_yield`;
ALTER TABLE IF EXISTS `valuation` DROP COLUMN `corporate_value`;
ALTER TABLE IF EXISTS `valuation` DROP COLUMN `stock_price_of_submit_date`;
ALTER TABLE IF EXISTS `valuation` DROP COLUMN `graham_index_of_submit_date`;
ALTER TABLE IF EXISTS `valuation` DROP FOREIGN KEY `fk_v_document_id`;
ALTER TABLE IF EXISTS `valuation` DROP COLUMN `document_id`;
ALTER TABLE IF EXISTS `valuation` DROP FOREIGN KEY `fk_v_id`;
ALTER TABLE IF EXISTS `valuation` DROP COLUMN `valuation_id_of_submit_date`;

ALTER TABLE IF EXISTS `valuation` MODIFY COLUMN `analysis_result_id` BIGINT UNSIGNED NOT NULL COMMENT '企業価値ID';

-- @formatter:off
CREATE TABLE IF NOT EXISTS `valuation_view`
(
    `code`                        CHAR(4)      NOT NULL COMMENT '企業コード',
    `name`                        VARCHAR(100) NOT NULL COMMENT '企業名',
    `target_date`                 DATE         NOT NULL COMMENT '対象日付',
    `stock_price`                 FLOAT        NOT NULL COMMENT '株価終値',
    `graham_index`                FLOAT                 DEFAULT NULL COMMENT 'グレアム指数',
    `discount_value`              FLOAT        NOT NULL COMMENT '割安値',
    `discount_rate`               FLOAT        NOT NULL COMMENT '割安度',
    `submit_date`                 DATE         NOT NULL COMMENT '提出日',
    `stock_price_of_submit_date`  FLOAT        NOT NULL COMMENT '提出日の株価終値',
    `day_since_submit_date`       INT(11)      NOT NULL COMMENT '提出日からの日数',
    `difference_from_submit_date` FLOAT        NOT NULL COMMENT '提出日との株価の差',
    `submit_date_ratio`           FLOAT        NOT NULL COMMENT '提出日との株価比率',
    `graham_index_of_submit_date` FLOAT                 DEFAULT NULL COMMENT '提出日のグレアム指数',
    `corporate_value`             FLOAT        NOT NULL COMMENT '企業価値',
    `dividend_yield`              FLOAT                 DEFAULT NULL COMMENT '予想配当利回り',
    `created_at`                  DATETIME     NOT NULL DEFAULT CURRENT_TIME() COMMENT '登録日',
    `updated_at`                  DATETIME     NOT NULL DEFAULT CURRENT_TIME() COMMENT '更新日',
    PRIMARY KEY (`code`)
);
-- @formatter:on