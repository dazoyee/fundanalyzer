--業種
create TABLE industry(
  id INT AUTO_INCREMENT,
  name VARCHAR(100) UNIQUE NOT NULL,
  PRIMARY KEY(id)
);

--企業
create TABLE company(
  code CHAR(5),
  company_name VARCHAR(100) NOT NULL,
  industry_id VARCHAR(10) NOT NULL REFERENCES industry(id),
  edinet_code CHAR(6) UNIQUE NOT NULL,
  list_categories CHAR(1),
  consolidated CHAR(1),
  capital_stock INT,
  settlement_date VARCHAR(6),
  insert_date DATETIME NOT NULL,
  update_date DATETIME NOT NULL,
  PRIMARY KEY(code)
);

--貸借対照表
create TABLE balance_sheet_subject(
  id INT AUTO_INCREMENT,
  outline_subject_id VARCHAR(10),
  detail_subject_id VARCHAR(10),
  name VARCHAR(50) NOT NULL,
  PRIMARY KEY(id)
--  UNIQUE KEY(subject_id, name)
);

--損益計算書
create TABLE profit_and_less_statement_subject(
  id INT AUTO_INCREMENT,
  outline_subject_id VARCHAR(10),
  detail_subject_id VARCHAR(10),
  name VARCHAR(50) NOT NULL,
  PRIMARY KEY(id)
--  UNIQUE KEY(subject_id, name)
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
  PRIMARY KEY(doc_id)
);

--書類ステータス
create TABLE document(
  doc_id CHAR(8) NOT NULL REFERENCES edinet_document(doc_id),
  doc_type_code CHAR(3),
  filer_name VARCHAR(128),
  submit_date DATE NOT NULL,
  downloaded CHAR(1) NOT NULL DEFAULT '0' CHECK(downloaded IN('0', '1', '9')),
  decoded CHAR(1) NOT NULL DEFAULT '0' CHECK(decoded IN('0', '1', '9')),
  scraped_number_of_shares CHAR(1) NOT NULL DEFAULT '0' CHECK(scraped_number_of_shares IN('0', '1', '5', '9')),
  scraped_balance_sheet CHAR(1) NOT NULL DEFAULT '0' CHECK(scraped_balance_sheet IN('0', '1', '9')),
  scraped_profit_and_less_statement CHAR(1) NOT NULL DEFAULT '0' CHECK(scraped_profit_and_less_statement IN('0', '1', '9')),
  scraped_cash_flow_statement CHAR(1) NOT NULL DEFAULT '0' CHECK(scraped_cash_flow_statement IN('0', '1', '9')),
  PRIMARY KEY(doc_id)
);

--財務諸表
create TABLE financial_statement(
  id INT AUTO_INCREMENT,
  company_code CHAR(5) NOT NULL REFERENCES company(code),
  financial_statement_id VARCHAR(10) NOT NULL,
  subject_id VARCHAR(10) NOT NULL,
  period_start DATE NOT NULL,
  period_end DATE NOT NULL,
  value BIGINT,
  number_of_shares VARCHAR,
--FIXME  UNIQUE KEY(company_code, financial_statement_id, subject_id, to_date),
  PRIMARY KEY(id)
);
