-- 業種
create TABLE IF NOT EXISTS industry
(
    id         INT AUTO_INCREMENT,
    name       VARCHAR(100) NOT NULL COMMENT '業種名',
    created_at DATETIME     NOT NULL COMMENT '登録日',
    PRIMARY KEY (id),
    UNIQUE KEY (name)
);

-- 企業
create TABLE IF NOT EXISTS company
(
    code            CHAR(5) COMMENT '企業コード',
    company_name    VARCHAR(100) NOT NULL COMMENT '企業名',
    industry_id     INT(10)      NOT NULL COMMENT '業種ID' REFERENCES industry (id),
    edinet_code     CHAR(6)      NOT NULL COMMENT 'EDINETコード',
    list_categories CHAR(1) COMMENT '上場区分' CHECK (list_categories IN ('0', '1', '9')),
    consolidated    CHAR(1) COMMENT '連結の有無' CHECK (consolidated IN ('0', '1', '9')),
    capital_stock   INT COMMENT '資本金',
    settlement_date VARCHAR(6) COMMENT '提出日',
    created_at      DATETIME     NOT NULL COMMENT '登録日',
    updated_at      DATETIME     NOT NULL COMMENT '更新日',
    PRIMARY KEY (edinet_code),
    UNIQUE KEY (code)
);

-- スクレイピングキーワード
create TABLE IF NOT EXISTS scraping_keyword
(
    id                     INT AUTO_INCREMENT,
    financial_statement_id VARCHAR(10)  NOT NULL COMMENT '財務諸表ID',
    keyword                VARCHAR(256) NOT NULL COMMENT 'キーワード',
    remarks                VARCHAR(100) COMMENT '備考',
    created_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登録日',
    PRIMARY KEY (id),
    UNIQUE KEY (keyword)
);

-- 貸借対照表
create TABLE IF NOT EXISTS bs_subject
(
    id                 INT AUTO_INCREMENT,
    outline_subject_id VARCHAR(10) COMMENT '大科目ID',
    detail_subject_id  VARCHAR(10) COMMENT '小科目ID',
    name               VARCHAR(100) NOT NULL COMMENT '科目名',
    PRIMARY KEY (id),
    UNIQUE KEY (outline_subject_id, detail_subject_id)
);

-- 損益計算書
create TABLE IF NOT EXISTS pl_subject
(
    id                 INT AUTO_INCREMENT,
    outline_subject_id VARCHAR(10) COMMENT '大科目ID',
    detail_subject_id  VARCHAR(10) COMMENT '小科目ID',
    name               VARCHAR(100) NOT NULL COMMENT '科目名',
    PRIMARY KEY (id),
    UNIQUE KEY (outline_subject_id, detail_subject_id)
);

-- EDINETに提出された書類
create TABLE IF NOT EXISTS edinet_document
(
    id                     INT AUTO_INCREMENT,
    doc_id                 CHAR(8)  NOT NULL COMMENT '書類ID',
    edinet_code            CHAR(6) COMMENT '提出者EDINETコード',
    sec_code               CHAR(5) COMMENT '提出者証券コード',
    jcn                    CHAR(13) COMMENT '提出者法人番号',
    filer_name             VARCHAR(128) COMMENT '提出者名',
    fund_code              CHAR(6) COMMENT 'ファンドコード',
    ordinance_code         CHAR(3) COMMENT '府令コード',
    form_code              CHAR(6) COMMENT '様式コード',
    doc_type_code          CHAR(3) COMMENT '書類種別コード',
    period_start           CHAR(10) COMMENT '期間（自）',
    period_end             CHAR(10) COMMENT '期間（至）',
    submit_date_time       CHAR(16) COMMENT '提出日時',
    doc_description        VARCHAR(147) COMMENT '提出書類概要',
    issuer_edinet_code     CHAR(6) COMMENT '発行会社EDINETコード',
    subject_edinet_code    CHAR(6) COMMENT '対象EDINETコード',
    subsidiary_edinet_code VARCHAR(69) COMMENT '小会社EDINETコード',
    current_report_reason  VARCHAR(1000) COMMENT '臨報提出事由',
    parent_doc_id          CHAR(8) COMMENT '親書類管理番号',
    ope_date_time          CHAR(16) COMMENT '操作日時',
    withdrawal_status      CHAR(1) COMMENT '取下区分',
    doc_info_edit_status   CHAR(1) COMMENT '書類情報修正区分',
    disclosure_status      CHAR(1) COMMENT '開示不開示区分',
    xbrl_flag              CHAR(1) COMMENT 'XBRL有無フラグ',
    pdf_flag               CHAR(1) COMMENT 'PDF有無フラグ',
    attach_doc_flag        CHAR(1) COMMENT '代替書面・添付文書有無フラグ',
    english_doc_flag       CHAR(1) COMMENT '英文ファイル有無フラグ',
    created_at             DATETIME NOT NULL COMMENT '登録日',
    PRIMARY KEY (id),
    UNIQUE KEY (doc_id)
);

-- 書類ステータス
create TABLE IF NOT EXISTS document
(
    id                             INT AUTO_INCREMENT,
    document_id                    CHAR(8)  NOT NULL COMMENT '書類ID',
    document_type_code             CHAR(3) COMMENT '書類種別コード',
    edinet_code                    CHAR(6) COMMENT 'EDINETコード' REFERENCES company (edinet_code),
    document_period                DATE COMMENT '対象期間',
    submit_date                    DATE     NOT NULL COMMENT '提出日',
    downloaded                     CHAR(1)  NOT NULL DEFAULT '0' COMMENT 'ダウンロードステータス' CHECK (downloaded IN ('0', '1', '9')),
    decoded                        CHAR(1)  NOT NULL DEFAULT '0' COMMENT 'ファイル解凍ステータス' CHECK (decoded IN ('0', '1', '9')),
    scraped_number_of_shares       CHAR(1)  NOT NULL DEFAULT '0' COMMENT 'スクレイピング（株式総数）ステータス' CHECK (scraped_number_of_shares IN ('0', '1', '5', '9')),
    number_of_shares_document_path VARCHAR(256) COMMENT 'ドキュメントファイル（株式総数）パス',
    scraped_bs                     CHAR(1)  NOT NULL DEFAULT '0' COMMENT 'スクレイピング（貸借対照表）ステータス' CHECK (scraped_bs IN ('0', '1', '5', '9')),
    bs_document_path               VARCHAR(256) COMMENT 'ドキュメントファイル（貸借対照表）パス',
    scraped_pl                     CHAR(1)  NOT NULL DEFAULT '0' COMMENT 'スクレイピング（損益計算書）ステータス' CHECK (scraped_pl IN ('0', '1', '5', '9')),
    pl_document_path               VARCHAR(256) COMMENT 'ドキュメントファイル（損益計算書）パス',
    scraped_cf                     CHAR(1)  NOT NULL DEFAULT '0' COMMENT 'スクレイピング（キャッシュ・フロー計算書）ステータス' CHECK (scraped_cf IN ('0', '1', '5', '9')),
    cf_document_path               VARCHAR(256) COMMENT 'ドキュメントファイル（キャッシュ・フロー計算書）パス',
    removed                        CHAR(1)  NOT NULL DEFAULT '0' COMMENT '除外フラグ' CHECK (removed IN ('0', '1')),
    created_at                     DATETIME NOT NULL COMMENT '登録日',
    updated_at                     DATETIME NOT NULL COMMENT '更新日',
    PRIMARY KEY (id),
    UNIQUE KEY (document_id)
);

-- 財務諸表
create TABLE IF NOT EXISTS financial_statement
(
    id                     INT AUTO_INCREMENT,
    company_code           CHAR(5) COMMENT '企業コード',
    edinet_code            CHAR(6)     NOT NULL COMMENT 'EDINETコード' REFERENCES company (edinet_code),
    financial_statement_id VARCHAR(10) NOT NULL COMMENT '財務諸表ID',
    subject_id             VARCHAR(10) NOT NULL COMMENT '科目ID',
    period_start           DATE        NOT NULL COMMENT '開始日',
    period_end             DATE        NOT NULL COMMENT '終了日',
    value                  BIGINT COMMENT '値',
    document_type_code     CHAR(3)     NOT NULL COMMENT '書類種別コード',
    submit_date            DATE        NOT NULL COMMENT '提出日',
    document_id            CHAR(8)     NOT NULL COMMENT '書類ID' REFERENCES document (document_id),
    created_at             DATETIME    NOT NULL COMMENT '登録日',
    PRIMARY KEY (id),
    UNIQUE KEY (edinet_code, financial_statement_id, subject_id, period_end, document_type_code, submit_date)
);

-- 企業価値
create TABLE IF NOT EXISTS analysis_result
(
    id                 INT AUTO_INCREMENT,
    company_code       CHAR(5)  NOT NULL COMMENT '企業コード',
    document_period    DATE     NOT NULL COMMENT '期間',
    corporate_value    FLOAT    NOT NULL COMMENT '企業価値',
    document_type_code CHAR(3)  NOT NULL COMMENT '書類種別コード',
    submit_date        DATE     NOT NULL COMMENT '提出日',
    document_id        CHAR(8)  NOT NULL COMMENT '書類ID' REFERENCES document (document_id),
    created_at         DATETIME NOT NULL COMMENT '登録日',
    PRIMARY KEY (id),
    UNIQUE KEY (company_code, document_period, document_type_code, submit_date)
);

-- 株価
create TABLE IF NOT EXISTS stock_price
(
    id                    INT AUTO_INCREMENT,
    company_code          CHAR(5)  NOT NULL COMMENT '企業コード',
    target_date           DATE     NOT NULL COMMENT '対象日付',
    stock_price           FLOAT COMMENT '終値',
    opening_price         FLOAT COMMENT '始値',
    high_price            FLOAT COMMENT '高値',
    low_price             FLOAT COMMENT '安値',
    volume                INT COMMENT '出来高',
    per                   VARCHAR(10) COMMENT '予想PER',
    pbr                   VARCHAR(10) COMMENT '実績PBR',
    roe                   VARCHAR(10) COMMENT '予想ROE',
    number_of_shares      VARCHAR(50) COMMENT '普通株式数',
    market_capitalization VARCHAR(50) COMMENT '時価総額',
    dividend_yield        VARCHAR(10) COMMENT '予想配当利回り',
    shareholder_benefit   VARCHAR(100) COMMENT '株式優待',
    source_of             CHAR(1) COMMENT '取得元',
    created_at            DATETIME NOT NULL COMMENT '登録日',
    PRIMARY KEY (id),
    UNIQUE KEY (company_code, target_date, source_of)
);

-- みんかぶ
create TABLE IF NOT EXISTS minkabu
(
    id                         INT AUTO_INCREMENT,
    company_code               CHAR(5)  NOT NULL COMMENT '企業コード',
    target_date                DATE     NOT NULL COMMENT '対象日付',
    stock_price                FLOAT COMMENT '現在株価',
    goals_stock                FLOAT COMMENT '予想株価',
    theoretical_stock          FLOAT COMMENT '理論株価',
    individual_investors_stock FLOAT COMMENT '個人投資家の予想株価',
    securities_analyst_stock   FLOAT COMMENT '証券アナリストの予想株価',
    created_at                 DATETIME NOT NULL COMMENT '登録日',
    PRIMARY KEY (id),
    UNIQUE KEY (company_code, target_date)
);

-- 企業一覧
create TABLE IF NOT EXISTS corporate_view
(
    code                     CHAR(4) UNIQUE COMMENT '企業コード',
    name                     VARCHAR(100) NOT NULL COMMENT '企業名',
    submit_date              DATE COMMENT '提出日',
    latest_corporate_value   FLOAT COMMENT '最新企業価値',
    average_corporate_value  FLOAT COMMENT '平均企業価値',
    standard_deviation       FLOAT COMMENT '標準偏差',
    coefficient_of_variation FLOAT COMMENT '変動係数',
    average_stock_price      FLOAT COMMENT '提出日株価平均',
    import_date              DATE COMMENT '株価取得日',
    latest_stock_price       FLOAT COMMENT '最新株価',
    discount_value           FLOAT COMMENT '割安値',
    discount_rate            FLOAT COMMENT '割安度',
    count_year               INT COMMENT '対象年カウント',
    forecast_stock           FLOAT COMMENT '株価予想',
    created_at               DATETIME     NOT NULL COMMENT '登録日',
    updated_at               DATETIME     NOT NULL COMMENT '更新日',
    PRIMARY KEY (code),
    UNIQUE KEY (code)
);

-- EDINET処理一覧
create TABLE IF NOT EXISTS edinet_list_view
(
    submit_date       DATE     NOT NULL COMMENT '提出日',
    count_all         INT      NOT NULL COMMENT '総件数',
    count_target      INT      NOT NULL COMMENT '処理対象件数',
    count_scraped     INT      NOT NULL COMMENT '処理済件数',
    count_analyzed    INT      NOT NULL COMMENT '分析済件数',
    cant_scraped_code VARCHAR(100) COMMENT '未分析コード',
    not_analyzed_code VARCHAR(100) COMMENT '処理確認コード',
    count_not_scraped INT      NOT NULL COMMENT '未処理件数',
    count_not_target  INT      NOT NULL COMMENT '対象外件数',
    created_at        DATETIME NOT NULL COMMENT '登録日',
    updated_at        DATETIME NOT NULL COMMENT '更新日',
    PRIMARY KEY (submit_date),
    UNIQUE KEY (submit_date)
);
