-- Table structure for table `industry`(業種)
-- DROP TABLE IF EXISTS `industry`;
CREATE TABLE IF NOT EXISTS `industry`
(
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `name`       VARCHAR(100)    NOT NULL COMMENT '業種名',
    `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIME() COMMENT '登録日',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_industry_name` (`name`)
);

-- Table structure for table `company`(企業)
-- DROP TABLE IF EXISTS `company`;
CREATE TABLE IF NOT EXISTS `company`
(
    `code`            CHAR(5)               DEFAULT NULL COMMENT '企業コード',
    `company_name`    VARCHAR(100) NOT NULL COMMENT '企業名',
    `industry_id`     INT(10)      NOT NULL COMMENT '業種ID',
    `edinet_code`     CHAR(6)      NOT NULL COMMENT 'EDINETコード',
    `list_categories` CHAR(1)               DEFAULT NULL COMMENT '上場区分' CHECK (`list_categories` IN ('0', '1', '9')),
    `consolidated`    CHAR(1)               DEFAULT NULL COMMENT '連結の有無' CHECK (`consolidated` IN ('0', '1', '9')),
    `capital_stock`   INT(11)               DEFAULT NULL COMMENT '資本金',
    `settlement_date` VARCHAR(6)            DEFAULT NULL COMMENT '提出日',
    `favorite`        CHAR(1)      NOT NULL DEFAULT '0' COMMENT 'お気に入り' CHECK (`favorite` IN ('0', '1')),
    `removed`         CHAR(1)      NOT NULL DEFAULT '0' COMMENT '除外フラグ' CHECK (`removed` IN ('0', '1')),
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIME() COMMENT '登録日',
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIME() COMMENT '更新日',
    PRIMARY KEY (`edinet_code`),
    UNIQUE KEY `uk_company_code` (`code`),
    CONSTRAINT `fk_industry_id` FOREIGN KEY (`industry_id`) REFERENCES `industry` (`id`)
);

-- Table structure for table `scraping_keyword`(スクレイピングキーワード)
-- DROP TABLE IF EXISTS `scraping_keyword`;
CREATE TABLE IF NOT EXISTS `scraping_keyword`
(
    `id`                     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `financial_statement_id` VARCHAR(10)     NOT NULL COMMENT '財務諸表ID',
    `keyword`                VARCHAR(256)    NOT NULL COMMENT 'キーワード',
    `priority`               INT(2)                   DEFAULT NULL COMMENT '優先度',
    `remarks`                VARCHAR(100)             DEFAULT NULL COMMENT '備考',
    `created_at`             DATETIME        NOT NULL DEFAULT CURRENT_TIME() COMMENT '登録日',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_scraping_keyword` (`keyword`)
);

-- Table structure for table `bs_subject`(貸借対照表)
-- DROP TABLE IF EXISTS `bs_subject`;
CREATE TABLE IF NOT EXISTS `bs_subject`
(
    `id`                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `outline_subject_id` VARCHAR(10) DEFAULT NULL COMMENT '大科目ID',
    `detail_subject_id`  VARCHAR(10) DEFAULT NULL COMMENT '小科目ID',
    `name`               VARCHAR(100)    NOT NULL COMMENT '科目名',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_bs_subject_id` (`outline_subject_id`, `detail_subject_id`)
);

-- Table structure for table `pl_subject`(損益計算書)
-- DROP TABLE IF EXISTS `pl_subject`;
CREATE TABLE IF NOT EXISTS `pl_subject`
(
    `id`                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `outline_subject_id` VARCHAR(10) DEFAULT NULL COMMENT '大科目ID',
    `detail_subject_id`  VARCHAR(10) DEFAULT NULL COMMENT '小科目ID',
    `name`               VARCHAR(100)    NOT NULL COMMENT '科目名',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pl_subject_id` (`outline_subject_id`, `detail_subject_id`)
);

-- Table structure for table `edinet_document`(EDINETに提出された書類)
-- DROP TABLE IF EXISTS `edinet_document`;
CREATE TABLE IF NOT EXISTS `edinet_document`
(
    `id`                     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `doc_id`                 CHAR(8)         NOT NULL COMMENT '書類ID',
    `edinet_code`            CHAR(6)                  DEFAULT NULL COMMENT '提出者EDINETコード',
    `sec_code`               CHAR(5)                  DEFAULT NULL COMMENT '提出者証券コード',
    `jcn`                    CHAR(13)                 DEFAULT NULL COMMENT '提出者法人番号',
    `filer_name`             VARCHAR(128)             DEFAULT NULL COMMENT '提出者名',
    `fund_code`              CHAR(6)                  DEFAULT NULL COMMENT 'ファンドコード',
    `ordinance_code`         CHAR(3)                  DEFAULT NULL COMMENT '府令コード',
    `form_code`              CHAR(6)                  DEFAULT NULL COMMENT '様式コード',
    `doc_type_code`          CHAR(3)                  DEFAULT NULL COMMENT '書類種別コード',
    `period_start`           CHAR(10)                 DEFAULT NULL COMMENT '期間（自）',
    `period_end`             CHAR(10)                 DEFAULT NULL COMMENT '期間（至）',
    `submit_date_time`       CHAR(16)                 DEFAULT NULL COMMENT '提出日時',
    `doc_description`        VARCHAR(147)             DEFAULT NULL COMMENT '提出書類概要',
    `issuer_edinet_code`     CHAR(6)                  DEFAULT NULL COMMENT '発行会社EDINETコード',
    `subject_edinet_code`    CHAR(6)                  DEFAULT NULL COMMENT '対象EDINETコード',
    `subsidiary_edinet_code` VARCHAR(69)              DEFAULT NULL COMMENT '小会社EDINETコード',
    `current_report_reason`  VARCHAR(1000)            DEFAULT NULL COMMENT '臨報提出事由',
    `parent_doc_id`          CHAR(8)                  DEFAULT NULL COMMENT '親書類管理番号',
    `ope_date_time`          CHAR(16)                 DEFAULT NULL COMMENT '操作日時',
    `withdrawal_status`      CHAR(1)                  DEFAULT NULL COMMENT '取下区分',
    `doc_info_edit_status`   CHAR(1)                  DEFAULT NULL COMMENT '書類情報修正区分',
    `disclosure_status`      CHAR(1)                  DEFAULT NULL COMMENT '開示不開示区分',
    `xbrl_flag`              CHAR(1)                  DEFAULT NULL COMMENT 'XBRL有無フラグ',
    `pdf_flag`               CHAR(1)                  DEFAULT NULL COMMENT 'PDF有無フラグ',
    `attach_doc_flag`        CHAR(1)                  DEFAULT NULL COMMENT '代替書面・添付文書有無フラグ',
    `english_doc_flag`       CHAR(1)                  DEFAULT NULL COMMENT '英文ファイル有無フラグ',
    `created_at`             DATETIME        NOT NULL DEFAULT CURRENT_TIME() COMMENT '登録日',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ed_doc_id` (`doc_id`)
);
CREATE INDEX IF NOT EXISTS `idx_doc_type_code` ON edinet_document (`doc_type_code`);

-- Table structure for table `document`(書類ステータス)
-- DROP TABLE IF EXISTS `document`;
CREATE TABLE IF NOT EXISTS `document`
(
    `id`                             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `document_id`                    CHAR(8)         NOT NULL COMMENT '書類ID',
    `document_type_code`             CHAR(3)                  DEFAULT NULL COMMENT '書類種別コード',
    `edinet_code`                    CHAR(6)                  DEFAULT NULL COMMENT 'EDINETコード',
    `document_period`                DATE                     DEFAULT NULL COMMENT '対象期間',
    `submit_date`                    DATE            NOT NULL COMMENT '提出日',
    `downloaded`                     CHAR(1)         NOT NULL DEFAULT '0' COMMENT 'ダウンロードステータス' CHECK (`downloaded` IN ('0', '1', '9')),
    `decoded`                        CHAR(1)         NOT NULL DEFAULT '0' COMMENT 'ファイル解凍ステータス' CHECK (`decoded` IN ('0', '1', '9')),
    `scraped_number_of_shares`       CHAR(1)         NOT NULL DEFAULT '0' COMMENT 'スクレイピング（株式総数）ステータス' CHECK (`scraped_number_of_shares` IN ('0', '1', '5', '9')),
    `number_of_shares_document_path` VARCHAR(256)             DEFAULT NULL COMMENT 'ドキュメントファイル（株式総数）パス',
    `scraped_bs`                     CHAR(1)         NOT NULL DEFAULT '0' COMMENT 'スクレイピング（貸借対照表）ステータス' CHECK (`scraped_bs` IN ('0', '1', '5', '9')),
    `bs_document_path`               VARCHAR(256)             DEFAULT NULL COMMENT 'ドキュメントファイル（貸借対照表）パス',
    `scraped_pl`                     CHAR(1)         NOT NULL DEFAULT '0' COMMENT 'スクレイピング（損益計算書）ステータス' CHECK (`scraped_pl` IN ('0', '1', '5', '9')),
    `pl_document_path`               VARCHAR(256)             DEFAULT NULL COMMENT 'ドキュメントファイル（損益計算書）パス',
    `scraped_cf`                     CHAR(1)         NOT NULL DEFAULT '0' COMMENT 'スクレイピング（キャッシュ・フロー計算書）ステータス' CHECK (`scraped_cf` IN ('0', '1', '5', '9')),
    `cf_document_path`               VARCHAR(256)             DEFAULT NULL COMMENT 'ドキュメントファイル（キャッシュ・フロー計算書）パス',
    `removed`                        CHAR(1)         NOT NULL DEFAULT '0' COMMENT '除外フラグ' CHECK (`removed` IN ('0', '1')),
    `created_at`                     DATETIME        NOT NULL DEFAULT CURRENT_TIME() COMMENT '登録日',
    `updated_at`                     DATETIME        NOT NULL DEFAULT CURRENT_TIME() COMMENT '更新日',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_document_document_id` (`document_id`),
    CONSTRAINT `fk_document_edinet_code` FOREIGN KEY (`edinet_code`) REFERENCES `company` (`edinet_code`)
);
CREATE INDEX IF NOT EXISTS `idx_document_type_code` ON document (`document_type_code`);

-- Table structure for table `financial_statement`(財務諸表)
-- DROP TABLE IF EXISTS `financial_statement`;
CREATE TABLE IF NOT EXISTS `financial_statement`
(
    `id`                     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `company_code`           CHAR(5)                  DEFAULT NULL COMMENT '企業コード',
    `edinet_code`            CHAR(6)         NOT NULL COMMENT 'EDINETコード',
    `financial_statement_id` VARCHAR(10)     NOT NULL COMMENT '財務諸表ID',
    `subject_id`             VARCHAR(10)     NOT NULL COMMENT '科目ID',
    `period_start`           DATE            NOT NULL COMMENT '開始日',
    `period_end`             DATE            NOT NULL COMMENT '終了日',
    `value`                  BIGINT(20)               DEFAULT NULL COMMENT '値',
    `document_type_code`     CHAR(3)         NOT NULL COMMENT '書類種別コード',
    `quarter_type`           CHAR(1)                  DEFAULT NULL COMMENT '四半期種別' CHECK (`quarter_type` IN ('1', '2', '3', '4')),
    `submit_date`            DATE            NOT NULL COMMENT '提出日',
    `document_id`            CHAR(8)         NOT NULL COMMENT '書類ID',
    `created_type`           CHAR(1)         NOT NULL COMMENT '登録方法' CHECK (`created_type` IN ('0', '1')),
    `created_at`             DATETIME        NOT NULL DEFAULT CURRENT_TIME() COMMENT '登録日',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_fs` (`edinet_code`, `financial_statement_id`, `subject_id`, `period_end`, `document_type_code`,
                        `submit_date`),
    CONSTRAINT `fk_fs_edinet_code` FOREIGN KEY (`edinet_code`) REFERENCES `company` (`edinet_code`),
    CONSTRAINT `fk_fs_document_id` FOREIGN KEY (`document_id`) REFERENCES `document` (`document_id`)
);

-- Table structure for table `analysis_result`(企業価値)
-- DROP TABLE IF EXISTS `analysis_result`;
CREATE TABLE IF NOT EXISTS `analysis_result`
(
    `id`                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `company_code`       CHAR(5)         NOT NULL COMMENT '企業コード',
    `document_period`    DATE            NOT NULL COMMENT '期間',
    `corporate_value`    FLOAT           NOT NULL COMMENT '企業価値',
    `bps`                FLOAT                    DEFAULT NULL COMMENT '1株当たり純資産',
    `eps`                FLOAT                    DEFAULT NULL COMMENT '1株当たり当期純利益',
    `roe`                FLOAT                    DEFAULT NULL COMMENT '自己資本利益率',
    `roa`                FLOAT                    DEFAULT NULL COMMENT '総資産利益率',
    `document_type_code` CHAR(3)         NOT NULL COMMENT '書類種別コード',
    `quarter_type`       CHAR(1)                  DEFAULT NULL COMMENT '四半期種別' CHECK (`quarter_type` IN ('1', '2', '3', '4')),
    `submit_date`        DATE            NOT NULL COMMENT '提出日',
    `document_id`        CHAR(8)         NOT NULL COMMENT '書類ID',
    `created_at`         DATETIME        NOT NULL DEFAULT CURRENT_TIME() COMMENT '登録日',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ar` (`company_code`, `document_period`, `document_type_code`, `submit_date`),
    CONSTRAINT `fk_ar_document_id` FOREIGN KEY (`document_id`) REFERENCES `document` (`document_id`)
);

-- Table structure for table `stock_price`(株価)
-- DROP TABLE IF EXISTS `stock_price`;
CREATE TABLE IF NOT EXISTS `stock_price`
(
    `id`                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `company_code`          CHAR(5)         NOT NULL COMMENT '企業コード',
    `target_date`           DATE            NOT NULL COMMENT '対象日付',
    `stock_price`           FLOAT                    DEFAULT NULL COMMENT '終値',
    `opening_price`         FLOAT                    DEFAULT NULL COMMENT '始値',
    `high_price`            FLOAT                    DEFAULT NULL COMMENT '高値',
    `low_price`             FLOAT                    DEFAULT NULL COMMENT '安値',
    `volume`                INT(11)                  DEFAULT NULL COMMENT '出来高',
    `per`                   VARCHAR(10)              DEFAULT NULL COMMENT '予想PER',
    `pbr`                   VARCHAR(10)              DEFAULT NULL COMMENT '実績PBR',
    `roe`                   VARCHAR(10)              DEFAULT NULL COMMENT '予想ROE',
    `number_of_shares`      VARCHAR(50)              DEFAULT NULL COMMENT '普通株式数',
    `market_capitalization` VARCHAR(50)              DEFAULT NULL COMMENT '時価総額',
    `dividend_yield`        VARCHAR(10)              DEFAULT NULL COMMENT '予想配当利回り',
    `shareholder_benefit`   VARCHAR(100)             DEFAULT NULL COMMENT '株式優待',
    `source_of`             CHAR(1)                  DEFAULT NULL COMMENT '取得元',
    `created_at`            DATETIME        NOT NULL DEFAULT CURRENT_TIME() COMMENT '登録日',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sp` (`company_code`, `target_date`, `source_of`)
);

-- Table structure for table `minkabu`(みんかぶ)
-- DROP TABLE IF EXISTS `minkabu`;
CREATE TABLE IF NOT EXISTS `minkabu`
(
    `id`                         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `company_code`               CHAR(5)         NOT NULL COMMENT '企業コード',
    `target_date`                DATE            NOT NULL COMMENT '対象日付',
    `stock_price`                FLOAT                    DEFAULT NULL COMMENT '現在株価',
    `goals_stock`                FLOAT                    DEFAULT NULL COMMENT '予想株価',
    `theoretical_stock`          FLOAT                    DEFAULT NULL COMMENT '理論株価',
    `individual_investors_stock` FLOAT                    DEFAULT NULL COMMENT '個人投資家の予想株価',
    `securities_analyst_stock`   FLOAT                    DEFAULT NULL COMMENT '証券アナリストの予想株価',
    `created_at`                 DATETIME        NOT NULL DEFAULT CURRENT_TIME() COMMENT '登録日',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_m` (`company_code`, `target_date`)
);

-- Table structure for table `investment_indicator`(投資指標)
-- DROP TABLE IF EXISTS `investment_indicator`;
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

-- Table structure for table `valuation`(企業価値評価)
-- DROP TABLE IF EXISTS `valuation`;
CREATE TABLE IF NOT EXISTS `valuation`
(
    `id`                          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `company_code`                CHAR(5)         NOT NULL COMMENT '企業コード',
    `submit_date`                 DATE            NOT NULL COMMENT '提出日',
    `target_date`                 DATE            NOT NULL COMMENT '対象日付',
    `stock_price_id`              BIGINT UNSIGNED          DEFAULT NULL COMMENT '株価ID',
    `stock_price`                 FLOAT           NOT NULL COMMENT '株価終値',
    `goals_stock`                 FLOAT                    DEFAULT NULL COMMENT '予想株価',
    `dividend_yield`              FLOAT                    DEFAULT NULL COMMENT '予想配当利回り',
    `investment_indicator_id`     BIGINT UNSIGNED          DEFAULT NULL COMMENT '投資指標ID',
    `graham_index`                FLOAT                    DEFAULT NULL COMMENT 'グレアム指数',
    `day_since_submit_date`       INT(11)         NOT NULL COMMENT '提出日からの日数',
    `difference_from_submit_date` FLOAT           NOT NULL COMMENT '提出日との株価の差',
    `submit_date_ratio`           FLOAT           NOT NULL COMMENT '提出日との株価比率',
    `discount_value`              FLOAT           NOT NULL COMMENT '割安値',
    `discount_rate`               FLOAT           NOT NULL COMMENT '割安度',
    `corporate_value`             FLOAT           NOT NULL COMMENT '企業価値',
    `valuation_id_of_submit_date` BIGINT UNSIGNED          DEFAULT NULL COMMENT '提出日の企業価値評価ID',
    `stock_price_of_submit_date`  FLOAT           NOT NULL COMMENT '提出日の株価終値',
    `graham_index_of_submit_date` FLOAT                    DEFAULT NULL COMMENT '提出日のグレアム指数',
    `analysis_result_id`          BIGINT UNSIGNED          DEFAULT NULL COMMENT '企業価値ID',
    `document_id`                 CHAR(8)         NOT NULL COMMENT '書類ID',
    `created_at`                  DATETIME        NOT NULL DEFAULT CURRENT_TIME() COMMENT '登録日',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_v` (`company_code`, `submit_date`, `target_date`),
    CONSTRAINT `fk_v_stock_price` FOREIGN KEY (`stock_price_id`) REFERENCES `stock_price` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_v_investment_indicator` FOREIGN KEY (`investment_indicator_id`) REFERENCES `investment_indicator` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_v_analysis_result` FOREIGN KEY (`analysis_result_id`) REFERENCES `analysis_result` (`id`),
    CONSTRAINT `fk_v_id` FOREIGN KEY (`valuation_id_of_submit_date`) REFERENCES `valuation` (`id`)
);

-- Table structure for table `valuation`(企業価値評価)
-- DROP TABLE IF EXISTS `valuation`;
-- CREATE TABLE IF NOT EXISTS `valuation`
-- (
--     `id`                          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
--     `company_code`                CHAR(5)         NOT NULL COMMENT '企業コード',
--     `submit_date`                 DATE            NOT NULL COMMENT '提出日',
--     `target_date`                 DATE            NOT NULL COMMENT '対象日付',
--     `stock_price_id`              BIGINT UNSIGNED          DEFAULT NULL COMMENT '株価ID',
--     `stock_price`                 FLOAT           NOT NULL COMMENT '株価終値',
--     `dividend_yield`              FLOAT                    DEFAULT NULL COMMENT '予想配当利回り',
--     `investment_indicator_id`     BIGINT UNSIGNED          DEFAULT NULL COMMENT '投資指標ID',
--     `graham_index`                FLOAT                    DEFAULT NULL COMMENT 'グレアム指数',
--     `day_since_submit_date`       INT(11)         NOT NULL COMMENT '提出日からの日数',
--     `difference_from_submit_date` FLOAT           NOT NULL COMMENT '提出日との株価の差',
--     `submit_date_ratio`           FLOAT           NOT NULL COMMENT '提出日との株価比率',
--     `discount_value`              FLOAT           NOT NULL COMMENT '割安値',
--     `discount_rate`               FLOAT           NOT NULL COMMENT '割安度',
--     `valuation_id_of_submit_date` BIGINT UNSIGNED          DEFAULT NULL COMMENT '提出日の企業価値評価ID',
--     `analysis_result_id`          BIGINT UNSIGNED NOT NULL COMMENT '企業価値ID',
--     `created_at`                  DATETIME        NOT NULL DEFAULT CURRENT_TIME() COMMENT '登録日',
--     PRIMARY KEY (`id`),
--     UNIQUE KEY `uk_v` (`company_code`, `submit_date`, `target_date`),
--     CONSTRAINT `fk_v_stock_price` FOREIGN KEY (`stock_price_id`) REFERENCES `stock_price` (`id`) ON DELETE SET NULL,
--     CONSTRAINT `fk_v_investment_indicator` FOREIGN KEY (`investment_indicator_id`) REFERENCES `investment_indicator` (`id`) ON DELETE SET NULL,
--     CONSTRAINT `fk_v_analysis_result` FOREIGN KEY (`analysis_result_id`) REFERENCES `analysis_result` (`id`),
--     CONSTRAINT `fk_v_id` FOREIGN KEY (`valuation_id_of_submit_date`) REFERENCES `valuation` (`id`)
-- );

-- Table structure for table `corporate_view`(企業一覧)
-- DROP TABLE IF EXISTS `corporate_view`;
CREATE TABLE IF NOT EXISTS `corporate_view`
(
    `code`                           CHAR(4)      NOT NULL COMMENT '企業コード',
    `name`                           VARCHAR(100) NOT NULL COMMENT '企業名',
    `submit_date`                    DATE                  DEFAULT NULL COMMENT '提出日',
    `latest_document_type_code`      CHAR(3)      NOT NULL COMMENT '最新書類種別コード',
    `latest_corporate_value`         FLOAT                 DEFAULT NULL COMMENT '最新企業価値',
    `three_average_corporate_value`  FLOAT                 DEFAULT NULL COMMENT '3年平均企業価値',
    `three_standard_deviation`       FLOAT                 DEFAULT NULL COMMENT '3年標準偏差',
    `three_coefficient_of_variation` FLOAT                 DEFAULT NULL COMMENT '3年変動係数',
    `five_average_corporate_value`   FLOAT                 DEFAULT NULL COMMENT '5年平均企業価値',
    `five_standard_deviation`        FLOAT                 DEFAULT NULL COMMENT '5年標準偏差',
    `five_coefficient_of_variation`  FLOAT                 DEFAULT NULL COMMENT '5年変動係数',
    `ten_average_corporate_value`    FLOAT                 DEFAULT NULL COMMENT '10年平均企業価値',
    `ten_standard_deviation`         FLOAT                 DEFAULT NULL COMMENT '10年標準偏差',
    `ten_coefficient_of_variation`   FLOAT                 DEFAULT NULL COMMENT '10年変動係数',
    `all_average_corporate_value`    FLOAT                 DEFAULT NULL COMMENT '全平均企業価値',
    `all_standard_deviation`         FLOAT                 DEFAULT NULL COMMENT '全標準偏差',
    `all_coefficient_of_variation`   FLOAT                 DEFAULT NULL COMMENT '全変動係数',
    `average_stock_price`            FLOAT                 DEFAULT NULL COMMENT '提出日株価平均',
    `import_date`                    DATE                  DEFAULT NULL COMMENT '株価取得日',
    `latest_stock_price`             FLOAT                 DEFAULT NULL COMMENT '最新株価',
    `three_discount_value`           FLOAT                 DEFAULT NULL COMMENT '3年割安値',
    `three_discount_rate`            FLOAT                 DEFAULT NULL COMMENT '3年割安度',
    `five_discount_value`            FLOAT                 DEFAULT NULL COMMENT '5年割安値',
    `five_discount_rate`             FLOAT                 DEFAULT NULL COMMENT '5年割安度',
    `ten_discount_value`             FLOAT                 DEFAULT NULL COMMENT '10年割安値',
    `ten_discount_rate`              FLOAT                 DEFAULT NULL COMMENT '10年割安度',
    `all_discount_value`             FLOAT                 DEFAULT NULL COMMENT '全割安値',
    `all_discount_rate`              FLOAT                 DEFAULT NULL COMMENT '全割安度',
    `count_year`                     INT(11)               DEFAULT NULL COMMENT '対象年カウント',
    `forecast_stock`                 FLOAT                 DEFAULT NULL COMMENT '株価予想',
    `price_corporate_value_ratio`    FLOAT                 DEFAULT NULL COMMENT '株価企業価値率',
    `per`                            FLOAT                 DEFAULT NULL COMMENT '株価収益率',
    `pbr`                            FLOAT                 DEFAULT NULL COMMENT '株価純資産倍率',
    `graham_index`                   FLOAT                 DEFAULT NULL COMMENT 'グレアム指数',
    `bps`                            FLOAT                 DEFAULT NULL COMMENT '1株当たり純資産',
    `eps`                            FLOAT                 DEFAULT NULL COMMENT '1株当たり当期純利益',
    `roe`                            FLOAT                 DEFAULT NULL COMMENT '自己資本利益率',
    `roa`                            FLOAT                 DEFAULT NULL COMMENT '総資産利益率',
    `created_at`                     DATETIME     NOT NULL DEFAULT CURRENT_TIME() COMMENT '登録日',
    `updated_at`                     DATETIME     NOT NULL DEFAULT CURRENT_TIME() COMMENT '更新日',
    PRIMARY KEY (`code`, `latest_document_type_code`),
    UNIQUE KEY `uk_cv` (`code`, `latest_document_type_code`)
);

-- Table structure for table `edinet_list_view`(EDINET処理一覧)
-- DROP TABLE IF EXISTS `edinet_list_view`;
CREATE TABLE IF NOT EXISTS edinet_list_view
(
    `submit_date`       DATE     NOT NULL COMMENT '提出日',
    `count_all`         INT(11)  NOT NULL COMMENT '総件数',
    `count_target`      INT(11)  NOT NULL COMMENT '処理対象件数',
    `count_scraped`     INT(11)  NOT NULL COMMENT '処理済件数',
    `count_analyzed`    INT(11)  NOT NULL COMMENT '分析済件数',
    `cant_scraped_id`   VARCHAR(1000)     DEFAULT NULL COMMENT '未分析ID',
    `not_analyzed_id`   VARCHAR(1000)     DEFAULT NULL COMMENT '処理確認ID',
    `count_not_scraped` INT(11)  NOT NULL COMMENT '未処理件数',
    `count_not_target`  INT(11)  NOT NULL COMMENT '対象外件数',
    `created_at`        DATETIME NOT NULL DEFAULT CURRENT_TIME() COMMENT '登録日',
    `updated_at`        DATETIME NOT NULL DEFAULT CURRENT_TIME() COMMENT '更新日',
    PRIMARY KEY (`submit_date`),
    UNIQUE KEY `uk_elv_submit_date` (`submit_date`)
);
