--業種
CREATE TABLE industry(
  id INT AUTO_INCREMENT,
  name VARCHAR(100) UNIQUE NOT NULL,
  created_at DATETIME NOT NULL,
  PRIMARY KEY(id)
);

--企業
CREATE TABLE company(
  code CHAR(5) UNIQUE,
  company_name VARCHAR(100) NOT NULL,
  industry_id VARCHAR(10) NOT NULL REFERENCES industry(id),
  edinet_code CHAR(6) NOT NULL,
  list_categories CHAR(1) CHECK(list_categories IN('0', '1', '9')),
  consolidated CHAR(1) CHECK(consolidated IN('0', '1', '9')),
  capital_stock INT,
  settlement_date VARCHAR(6),
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY(edinet_code)
);

--スクレイピングキーワード
CREATE TABLE scraping_keyword(
  id INT AUTO_INCREMENT,
  financial_statement_id VARCHAR(10) NOT NULL,
  keyword VARCHAR UNIQUE NOT NULL,
  remarks VARCHAR,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(id)
);

--貸借対照表
CREATE TABLE bs_subject(
  id INT AUTO_INCREMENT,
  outline_subject_id VARCHAR(10),
  detail_subject_id VARCHAR(10),
  name VARCHAR(50) NOT NULL,
  PRIMARY KEY(id)
);

--損益計算書
CREATE TABLE pl_subject(
  id INT AUTO_INCREMENT,
  outline_subject_id VARCHAR(10),
  detail_subject_id VARCHAR(10),
  name VARCHAR(50) NOT NULL,
  PRIMARY KEY(id)
);

--キャッシュ・フロー計算書
--create TABLE cash_flow_statement_detail(
--  id INT AUTO_INCREMENT,
--  subject_id VARCHAR(10) NOT NULL REFERENCES cash_flow_statement_subject(id),
--  name VARCHAR(50) NOT NULL,
--  PRIMARY KEY(id),
--  UNIQUE KEY(subject_id, name)
--);

--EDINETに提出された書類
CREATE TABLE edinet_document(
  id INT AUTO_INCREMENT,
  doc_id CHAR(8) NOT NULL,
  edinet_code CHAR(6),
  sec_code CHAR(5),
  jcn CHAR(13),
  filer_name VARCHAR(128),
  fund_code CHAR(6),
  ordinance_code CHAR(3),
  form_code CHAR(6),
  doc_type_code CHAR(3),
  period_start CHAR(10),
  period_end CHAR(10),
  submit_date_time CHAR(16),
  doc_description VARCHAR(147),
  issuer_edinet_code CHAR(6),
  subject_edinet_code CHAR(6),
  subsidiary_edinet_code VARCHAR(69),
  current_report_reason VARCHAR(1000),
  parent_doc_id CHAR(8),
  ope_date_time CHAR(16),
  withdrawal_status CHAR(1),
  doc_info_edit_status CHAR(1),
  disclosure_status CHAR(1),
  xbrl_flag CHAR(1),
  pdf_flag CHAR(1),
  attach_doc_flag CHAR(1),
  english_doc_flag CHAR(1),
  created_at DATETIME NOT NULL,
  PRIMARY KEY(id)
);

--書類ステータス
CREATE TABLE document(
  id INT AUTO_INCREMENT,
  document_id CHAR(8) NOT NULL,
  document_type_code CHAR(3),
  edinet_code CHAR(6) REFERENCES company(edinet_code),
  submit_date DATE NOT NULL,
  downloaded CHAR(1) NOT NULL DEFAULT '0' CHECK(downloaded IN('0', '1', '9')),
  decoded CHAR(1) NOT NULL DEFAULT '0' CHECK(decoded IN('0', '1', '9')),
  scraped_number_of_shares CHAR(1) NOT NULL DEFAULT '0' CHECK(scraped_number_of_shares IN('0', '1', '5', '9')),
  number_of_shares_document_path VARCHAR,
  scraped_bs CHAR(1) NOT NULL DEFAULT '0' CHECK(scraped_bs IN('0', '1', '9')),
  bs_document_path VARCHAR,
  scraped_pl CHAR(1) NOT NULL DEFAULT '0' CHECK(scraped_pl IN('0', '1', '9')),
  pl_document_path VARCHAR,
  scraped_cf CHAR(1) NOT NULL DEFAULT '0' CHECK(scraped_cf IN('0', '1', '9')),
  cf_document_path VARCHAR,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY(id)
);

--財務諸表
CREATE TABLE financial_statement(
  id INT AUTO_INCREMENT,
  company_code CHAR(5) REFERENCES company(code),
  edinet_code CHAR(6) NOT NULL REFERENCES company(edinet_code),
  financial_statement_id VARCHAR(10) NOT NULL,
  subject_id VARCHAR(10) NOT NULL,
  period_start DATE NOT NULL,
  period_end DATE NOT NULL,
  value BIGINT,
  created_at DATETIME NOT NULL,
  PRIMARY KEY(id),
  UNIQUE KEY(edinet_code, financial_statement_id, subject_id, period_end)
);

--企業価値
CREATE TABLE analysis_result(
  id INT AUTO_INCREMENT,
  company_code CHAR(5) NOT NULL REFERENCES company(code),
  period DATE NOT NULL,
  corporate_value FLOAT NOT NULL,
  created_at DATETIME NOT NULL,
  PRIMARY KEY(id),
  UNIQUE KEY(company_code, period)
);
