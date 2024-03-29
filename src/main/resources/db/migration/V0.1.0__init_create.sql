-- Table structure for table `industry`(業種)
CREATE TABLE IF NOT EXISTS `industry`
(
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `name`       VARCHAR(100)    NOT NULL COMMENT '業種名',
    `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIME() COMMENT '登録日',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_industry_name` (`name`)
    );

-- Table structure for table `company`(企業)
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
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIME() COMMENT '登録日',
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIME() COMMENT '更新日',
    PRIMARY KEY (`edinet_code`),
    UNIQUE KEY `uk_company_code` (`code`),
    CONSTRAINT `fk_industry_id` FOREIGN KEY (`industry_id`) REFERENCES `industry` (`id`)
    );

-- Table structure for table `scraping_keyword`(スクレイピングキーワード)
CREATE TABLE IF NOT EXISTS `scraping_keyword`
(
    `id`                     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `financial_statement_id` VARCHAR(10)     NOT NULL COMMENT '財務諸表ID',
    `keyword`                VARCHAR(256)    NOT NULL COMMENT 'キーワード',
    `remarks`                VARCHAR(100)             DEFAULT NULL COMMENT '備考',
    `created_at`             DATETIME        NOT NULL DEFAULT CURRENT_TIME() COMMENT '登録日',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_scraping_keyword` (`keyword`)
    );

-- Table structure for table `bs_subject`(貸借対照表)
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
CREATE TABLE IF NOT EXISTS `analysis_result`
(
    `id`                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `company_code`       CHAR(5)         NOT NULL COMMENT '企業コード',
    `document_period`    DATE            NOT NULL COMMENT '期間',
    `corporate_value`    FLOAT           NOT NULL COMMENT '企業価値',
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

-- Table structure for table `corporate_view`(企業一覧)
CREATE TABLE IF NOT EXISTS `corporate_view`
(
    `code`                      CHAR(4)      NOT NULL COMMENT '企業コード',
    `name`                      VARCHAR(100) NOT NULL COMMENT '企業名',
    `submit_date`               DATE                  DEFAULT NULL COMMENT '提出日',
    `latest_document_type_code` CHAR(3)               DEFAULT NULL COMMENT '最新書類種別コード',
    `latest_corporate_value`    FLOAT                 DEFAULT NULL COMMENT '最新企業価値',
    `average_corporate_value`   FLOAT                 DEFAULT NULL COMMENT '平均企業価値',
    `standard_deviation`        FLOAT                 DEFAULT NULL COMMENT '標準偏差',
    `coefficient_of_variation`  FLOAT                 DEFAULT NULL COMMENT '変動係数',
    `average_stock_price`       FLOAT                 DEFAULT NULL COMMENT '提出日株価平均',
    `import_date`               DATE                  DEFAULT NULL COMMENT '株価取得日',
    `latest_stock_price`        FLOAT                 DEFAULT NULL COMMENT '最新株価',
    `discount_value`            FLOAT                 DEFAULT NULL COMMENT '割安値',
    `discount_rate`             FLOAT                 DEFAULT NULL COMMENT '割安度',
    `count_year`                INT(11)               DEFAULT NULL COMMENT '対象年カウント',
    `forecast_stock`            FLOAT                 DEFAULT NULL COMMENT '株価予想',
    `created_at`                DATETIME     NOT NULL DEFAULT CURRENT_TIME() COMMENT '登録日',
    `updated_at`                DATETIME     NOT NULL DEFAULT CURRENT_TIME() COMMENT '更新日',
    PRIMARY KEY (`code`),
    UNIQUE KEY `uk_cv_company_code` (`code`)
    );

-- Table structure for table `edinet_list_view`(EDINET処理一覧)
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
