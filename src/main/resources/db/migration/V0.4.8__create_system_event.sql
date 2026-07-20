-- Table structure for table `system_event`(システムイベント)
-- @formatter:off
CREATE TABLE IF NOT EXISTS `system_event`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `event_type`  VARCHAR(10)     NOT NULL COMMENT 'イベント種別' CHECK (`event_type` IN ('ERROR', 'WARNING')),
    `source`      VARCHAR(100)    NOT NULL COMMENT '発生元',
    `message`     VARCHAR(1000)   NOT NULL COMMENT 'メッセージ',
    `occurred_at` DATETIME        NOT NULL DEFAULT CURRENT_TIME() COMMENT '発生日時',
    PRIMARY KEY (`id`),
    KEY `idx_system_event_occurred_at` (`occurred_at` DESC)
);
-- @formatter:on
