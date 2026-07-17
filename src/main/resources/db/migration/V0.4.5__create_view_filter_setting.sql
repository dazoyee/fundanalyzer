-- Table structure for table `view_filter_setting`(一覧表示フィルタ設定)
CREATE TABLE IF NOT EXISTS `view_filter_setting`
(
    `id`                              INT(10)      NOT NULL COMMENT 'ID',
    `discount_rate`                   DECIMAL(10, 2) NOT NULL COMMENT '割安度',
    `outlier_of_standard_deviation`   DECIMAL(10, 2) NOT NULL COMMENT '標準偏差の外れ値',
    `coefficient_of_variation`        DECIMAL(10, 3) NOT NULL COMMENT '変動係数',
    `diff_forecast_stock`             DECIMAL(10, 2) NOT NULL COMMENT '予想株価差',
    `corporate_size`                  INT(10)      NOT NULL COMMENT '提出日の表示範囲日数',
    `updated_at`                      DATETIME     NOT NULL DEFAULT CURRENT_TIME() COMMENT '更新日',
    PRIMARY KEY (`id`)
    );

INSERT INTO `view_filter_setting` (`id`, `discount_rate`, `outlier_of_standard_deviation`, `coefficient_of_variation`, `diff_forecast_stock`, `corporate_size`, `updated_at`)
VALUES (1, 120, 10000, 0.5, 100, 300, CURRENT_TIME());
