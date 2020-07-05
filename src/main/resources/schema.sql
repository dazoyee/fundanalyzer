--業種
create TABLE industry(
  id INT AUTO_INCREMENT,
  name VARCHAR(100) UNIQUE NOT NULL,
  PRIMARY KEY(id)
);

--企業
create TABLE company(
  code CHAR(5) UNIQUE,
  company_name VARCHAR(100) NOT NULL,
  industry_id VARCHAR(10) NOT NULL REFERENCES industry(id),
  edinet_code CHAR(6) NOT NULL,
  list_categories CHAR(1) CHECK(list_categories IN('0', '1', '9')),
  consolidated CHAR(1) CHECK(consolidated IN('0', '1', '9')),
  capital_stock INT,
  settlement_date VARCHAR(6),
  insert_date DATETIME NOT NULL,
  update_date DATETIME NOT NULL,
  PRIMARY KEY(edinet_code)
);

--スクレイピングキーワード
create TABLE scraping_keyword(
  id INT AUTO_INCREMENT,
  financial_statement_id VARCHAR(10) NOT NULL,
  keyword VARCHAR UNIQUE NOT NULL,
  remarks VARCHAR,
  PRIMARY KEY(id)
);

--貸借対照表
create TABLE balance_sheet_subject(
  id INT AUTO_INCREMENT,
  outline_subject_id VARCHAR(10),
  detail_subject_id VARCHAR(10),
  name VARCHAR(50) NOT NULL,
  PRIMARY KEY(id)
);

--損益計算書
create TABLE profit_and_less_statement_subject(
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
create TABLE edinet_document(
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
  insert_date DATETIME NOT NULL,
  PRIMARY KEY(id)
);

--書類ステータス
create TABLE document(
  id INT AUTO_INCREMENT,
  doc_id CHAR(8) NOT NULL,
  doc_type_code CHAR(3),
  edinet_code CHAR(6) REFERENCES company(edinet_code),
  submit_date DATE NOT NULL,
  downloaded CHAR(1) NOT NULL DEFAULT '0' CHECK(downloaded IN('0', '1', '9')),
  decoded CHAR(1) NOT NULL DEFAULT '0' CHECK(decoded IN('0', '1', '9')),
  scraped_number_of_shares CHAR(1) NOT NULL DEFAULT '0' CHECK(scraped_number_of_shares IN('0', '1', '5', '9')),
  scraped_balance_sheet CHAR(1) NOT NULL DEFAULT '0' CHECK(scraped_balance_sheet IN('0', '1', '9')),
  scraped_profit_and_less_statement CHAR(1) NOT NULL DEFAULT '0' CHECK(scraped_profit_and_less_statement IN('0', '1', '9')),
  scraped_cash_flow_statement CHAR(1) NOT NULL DEFAULT '0' CHECK(scraped_cash_flow_statement IN('0', '1', '9')),
  PRIMARY KEY(id)
);

--財務諸表
create TABLE financial_statement(
  id INT AUTO_INCREMENT,
  company_code CHAR(5) REFERENCES company(code),
  edinet_code CHAR(6) NOT NULL REFERENCES company(edinet_code),
  financial_statement_id VARCHAR(10) NOT NULL,
  subject_id VARCHAR(10) NOT NULL,
  period_start DATE NOT NULL,
  period_end DATE NOT NULL,
  value BIGINT,
  UNIQUE KEY(edinet_code, financial_statement_id, subject_id, period_end),
  PRIMARY KEY(id)
);

--企業価値
create TABLE analysis_result(
  id INT AUTO_INCREMENT,
  company_code CHAR(5) NOT NULL REFERENCES company(code),
  corporate_value FLOAT NOT NULL,
  period DATE NOT NULL,
  insert_date DATETIME NOT NULL,
  UNIQUE KEY(company_code, period),
  PRIMARY KEY(id)
);
