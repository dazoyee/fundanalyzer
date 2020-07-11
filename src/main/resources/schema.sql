-- 業種
CREATE TABLE IF NOT EXISTS industry(
  id INT AUTO_INCREMENT,
  name VARCHAR(100) UNIQUE NOT NULL COMMENT '業種名',
  created_at DATETIME NOT NULL COMMENT '登録日',
  PRIMARY KEY(id)
);

-- 企業
CREATE TABLE IF NOT EXISTS company(
  code CHAR(5) UNIQUE COMMENT '企業コード',
  company_name VARCHAR(100) NOT NULL COMMENT '企業名',
  industry_id INT(10) NOT NULL COMMENT '業種ID' REFERENCES industry(id),
  edinet_code CHAR(6) NOT NULL COMMENT 'EDINETコード',
  list_categories CHAR(1) COMMENT '上場区分' CHECK(list_categories IN('0', '1', '9')),
  consolidated CHAR(1) COMMENT '連結の有無' CHECK(consolidated IN('0', '1', '9')),
  capital_stock INT COMMENT '資本金',
  settlement_date VARCHAR(6) COMMENT '提出日',
  created_at DATETIME NOT NULL COMMENT '登録日',
  updated_at DATETIME NOT NULL COMMENT '更新日',
  PRIMARY KEY(edinet_code)
);

-- スクレイピングキーワード
CREATE TABLE IF NOT EXISTS scraping_keyword(
  id INT AUTO_INCREMENT,
  financial_statement_id VARCHAR(10) NOT NULL COMMENT '財務諸表ID',
  keyword VARCHAR(256) UNIQUE NOT NULL COMMENT 'キーワード',
  remarks VARCHAR(100) COMMENT '備考',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登録日',
  PRIMARY KEY(id)
);

-- 貸借対照表
CREATE TABLE IF NOT EXISTS bs_subject(
  id INT AUTO_INCREMENT,
  outline_subject_id VARCHAR(10) COMMENT '大科目ID',
  detail_subject_id VARCHAR(10) COMMENT '小科目ID',
  name VARCHAR(100) NOT NULL COMMENT '科目名',
  PRIMARY KEY(id)
);

-- 損益計算書
CREATE TABLE IF NOT EXISTS pl_subject(
  id INT AUTO_INCREMENT,
  outline_subject_id VARCHAR(10) COMMENT '大科目ID',
  detail_subject_id VARCHAR(10) COMMENT '小科目ID',
  name VARCHAR(100) NOT NULL COMMENT '科目名',
  PRIMARY KEY(id)
);

-- キャッシュ・フロー計算書
/*
--CREATE TABLE IF NOT EXISTS cash_flow_statement_detail(
--  id INT AUTO_INCREMENT,
--  subject_id VARCHAR(10) NOT NULL REFERENCES cash_flow_statement_subject(id),
--  name VARCHAR(50) NOT NULL,
--  PRIMARY KEY(id),
--  UNIQUE KEY(subject_id, name)
--);
*/

-- EDINETに提出された書類
CREATE TABLE IF NOT EXISTS edinet_document(
  id INT AUTO_INCREMENT,
  doc_id CHAR(8) NOT NULL COMMENT '書類ID',
  edinet_code CHAR(6) COMMENT '提出者EDINETコード',
  sec_code CHAR(5) COMMENT '提出者証券コード',
  jcn CHAR(13) COMMENT '提出者法人番号',
  filer_name VARCHAR(128) COMMENT '提出者名',
  fund_code CHAR(6) COMMENT 'ファンドコード',
  ordinance_code CHAR(3) COMMENT '府令コード',
  form_code CHAR(6) COMMENT '様式コード',
  doc_type_code CHAR(3) COMMENT '書類種別コード',
  period_start CHAR(10) COMMENT '期間（自）',
  period_end CHAR(10) COMMENT '期間（至）',
  submit_date_time CHAR(16) COMMENT '提出日時',
  doc_description VARCHAR(147) COMMENT '提出書類概要',
  issuer_edinet_code CHAR(6) COMMENT '発行会社EDINETコード',
  subject_edinet_code CHAR(6) COMMENT '対象EDINETコード',
  subsidiary_edinet_code VARCHAR(69) COMMENT '小会社EDINETコード',
  current_report_reason VARCHAR(1000) COMMENT '臨報提出事由',
  parent_doc_id CHAR(8) COMMENT '親書類管理番号',
  ope_date_time CHAR(16) COMMENT '操作日時',
  withdrawal_status CHAR(1) COMMENT '取下区分',
  doc_info_edit_status CHAR(1) COMMENT '書類情報修正区分',
  disclosure_status CHAR(1) COMMENT '開示不開示区分',
  xbrl_flag CHAR(1) COMMENT 'XBRL有無フラグ',
  pdf_flag CHAR(1) COMMENT 'PDF有無フラグ',
  attach_doc_flag CHAR(1) COMMENT '代替書面・添付文書有無フラグ',
  english_doc_flag CHAR(1) COMMENT '英文ファイル有無フラグ',
  created_at DATETIME NOT NULL COMMENT '登録日',
  PRIMARY KEY(id)
);

-- 書類ステータス
CREATE TABLE IF NOT EXISTS document(
  id INT AUTO_INCREMENT,
  document_id CHAR(8) NOT NULL COMMENT '書類ID',
  document_type_code CHAR(3) COMMENT '書類種別コード',
  edinet_code CHAR(6) COMMENT 'EDINETコード' REFERENCES company(edinet_code),
  submit_date DATE NOT NULL COMMENT '提出日',
  downloaded CHAR(1) NOT NULL DEFAULT '0' COMMENT 'ダウンロードステータス' CHECK(downloaded IN('0', '1', '9')),
  decoded CHAR(1) NOT NULL DEFAULT '0'  COMMENT 'ファイル解凍ステータス' CHECK(decoded IN('0', '1', '9')),
  scraped_number_of_shares CHAR(1) NOT NULL DEFAULT '0'  COMMENT 'スクレイピング（株式総数）ステータス' CHECK(scraped_number_of_shares IN('0', '1', '5', '9')),
  number_of_shares_document_path VARCHAR(256) COMMENT 'ドキュメントファイル（株式総数）パス',
  scraped_bs CHAR(1) NOT NULL DEFAULT '0' COMMENT 'スクレイピング（貸借対照表）ステータス' CHECK(scraped_bs IN('0', '1', '9')),
  bs_document_path VARCHAR(256) COMMENT 'ドキュメントファイル（貸借対照表）パス',
  scraped_pl CHAR(1) NOT NULL DEFAULT '0' COMMENT 'スクレイピング（損益計算書）ステータス' CHECK(scraped_pl IN('0', '1', '9')),
  pl_document_path VARCHAR(256) COMMENT 'ドキュメントファイル（損益計算書）パス',
  scraped_cf CHAR(1) NOT NULL DEFAULT '0' COMMENT 'スクレイピング（キャッシュ・フロー計算書）ステータス' CHECK(scraped_cf IN('0', '1', '9')),
  cf_document_path VARCHAR(256) COMMENT 'ドキュメントファイル（キャッシュ・フロー計算書）パス',
  created_at DATETIME NOT NULL COMMENT '登録日',
  updated_at DATETIME NOT NULL COMMENT '更新日',
  PRIMARY KEY(id)
);

-- 財務諸表
CREATE TABLE IF NOT EXISTS financial_statement(
  id INT AUTO_INCREMENT,
  company_code CHAR(5) COMMENT '企業コード' REFERENCES company(code),
  edinet_code CHAR(6) NOT NULL COMMENT 'EDINETコード' REFERENCES company(edinet_code),
  financial_statement_id VARCHAR(10) NOT NULL COMMENT '財務諸表ID',
  subject_id VARCHAR(10) NOT NULL COMMENT '科目ID',
  period_start DATE NOT NULL COMMENT '開始日',
  period_end DATE NOT NULL COMMENT '終了日',
  value BIGINT COMMENT '値',
  created_at DATETIME NOT NULL COMMENT '登録日',
  PRIMARY KEY(id),
  UNIQUE KEY(edinet_code, financial_statement_id, subject_id, period_end)
);

-- 企業価値
CREATE TABLE IF NOT EXISTS analysis_result(
  id INT AUTO_INCREMENT,
  company_code CHAR(5) NOT NULL COMMENT '企業コード' REFERENCES company(code),
  period DATE NOT NULL COMMENT '期間',
  corporate_value FLOAT NOT NULL COMMENT '企業価値',
  created_at DATETIME NOT NULL COMMENT '登録日',
  PRIMARY KEY(id),
  UNIQUE KEY(company_code, period)
);
