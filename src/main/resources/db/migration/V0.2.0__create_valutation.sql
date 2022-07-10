-- @formatter:off
CREATE TABLE IF NOT EXISTS `valuation`
(
    `id`                          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `company_code`                CHAR(5)         NOT NULL COMMENT '企業コード',
    `target_date`                 DATE            NOT NULL COMMENT '対象日付',
    `stock_price`                 FLOAT           NOT NULL COMMENT '株価終値',
    `goals_stock`                 FLOAT                    DEFAULT NULL COMMENT '予想株価',
    `day_since_submit_date`     INT(11)         NOT NULL COMMENT '提出日からの月数',
    `difference_from_submit_date` FLOAT           NOT NULL COMMENT '提出日との差',
    `submit_date_ratio`           FLOAT           NOT NULL COMMENT '提出日比率',
    `discount_value`              FLOAT           NOT NULL COMMENT '割安値',
    `discount_rate`               FLOAT           NOT NULL COMMENT '割安度',
    `submit_date`                 DATE            NOT NULL COMMENT '提出日',
    `corporate_value`             FLOAT           NOT NULL COMMENT '企業価値',
    `stock_price_of_submit_date`  FLOAT           NOT NULL COMMENT '提出日の株価終値',
    `document_id`                 CHAR(8)         NOT NULL COMMENT '書類ID',
    `created_at`                  DATETIME        NOT NULL DEFAULT CURRENT_TIME() COMMENT '登録日',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_v` (`company_code`, `target_date`),
    CONSTRAINT `fk_v_document_id` FOREIGN KEY (`document_id`) REFERENCES `document` (`document_id`)
    );
-- @formatter:on
