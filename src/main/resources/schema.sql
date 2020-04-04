--企業
create TABLE company(
  code CHAR(4) UNIQUE NOT NULL,
  name VARCHAR(100) UNIQUE NOT NULL,
  PRIMARY KEY(code)
);

--財務諸表
create TABLE financial_statement(
  id INT AUTO_INCREMENT,
  name VARCHAR(20) UNIQUE NOT NULL,
  PRIMARY KEY(id)
);

--大項目一覧（貸借対照表）
create TABLE balance_sheet_subject(
  id INT AUTO_INCREMENT,
  subject VARCHAR(20) UNIQUE NOT NULL,
  PRIMARY KEY(id)
);

--大項目一覧（損益計算書）
create TABLE profit_and_less_statement_subject(
  id INT AUTO_INCREMENT,
  subject VARCHAR(20) UNIQUE NOT NULL,
  PRIMARY KEY(id)
);

--大項目一覧（キャッシュ・フロー計算書）
create TABLE cash_flow_statement_subject(
  id INT AUTO_INCREMENT,
  subject VARCHAR(20) UNIQUE NOT NULL,
  PRIMARY KEY(id)
);

--小項目一覧（貸借対照表）
create TABLE balance_sheet_detail(
  id INT AUTO_INCREMENT,
  subject_id VARCHAR(10) NOT NULL REFERENCES balance_sheet_subject(id),
  name VARCHAR(50) NOT NULL,
  PRIMARY KEY(id)
--  UNIQUE KEY(subject_id, name)
);

--小項目一覧（損益計算書）
create TABLE profit_and_less_statement_detail(
  id INT AUTO_INCREMENT,
  subject_id VARCHAR(10) NOT NULL REFERENCES profit_and_less_statement_subject(id),
  name VARCHAR(50) NOT NULL,
  PRIMARY KEY(id),
  UNIQUE KEY(subject_id, name)
);

--小項目一覧（キャッシュ・フロー計算書）
create TABLE cash_flow_statement_detail(
  id INT AUTO_INCREMENT,
  subject_id VARCHAR(10) NOT NULL REFERENCES cash_flow_statement_subject(id),
  name VARCHAR(50) NOT NULL,
  PRIMARY KEY(id),
  UNIQUE KEY(subject_id, name)
);

--
create TABLE edinet_document(
  doc_id VARCHAR(8) NOT NULL,
  edinet_code VARCHAR(6),
  sec_code VARCHAR(5),
  jcn VARCHAR(13),
  filer_name VARCHAR(128),
  fund_code VARCHAR(6),
  ordinance_code VARCHAR(3),
  form_code VARCHAR(6),
  doc_type_code VARCHAR(3),
  period_start VARCHAR(10),
  period_end VARCHAR(10),
  submit_date_time VARCHAR(16),
  doc_description VARCHAR(147),
  issuer_edinet_code VARCHAR(6),
  subject_edinet_code VARCHAR(6),
  subsidiary_edinet_code VARCHAR(69),
  current_report_reason VARCHAR(1000),
  parent_doc_id VARCHAR(8),
  ope_date_time VARCHAR(16),
  withdrawal_status VARCHAR(1),
  doc_info_edit_status VARCHAR(1),
  disclosure_status VARCHAR(1),
  xbrl_flag VARCHAR(1),
  pdf_flag VARCHAR(1),
  attach_doc_flag VARCHAR(1),
  english_doc_flag VARCHAR(1),
  insert_date DATETIME NOT NULL,
  PRIMARY KEY(doc_id)
);

--貸借対照表
create TABLE balance_sheet(
  id INT AUTO_INCREMENT,
  company_id VARCHAR(10) NOT NULL REFERENCES company(code),
  financial_statement_id VARCHAR(10) NOT NULL REFERENCES financial_statement(id),
  detail_id VARCHAR(10) NOT NULL REFERENCES balance_sheet_detail(id),
  period DATE NOT NULL,
  value INT NOT NULL,
  PRIMARY KEY(id)
);

--損益計算書
create TABLE profit_and_less_statement(
  id INT AUTO_INCREMENT,
  company_id VARCHAR(10) NOT NULL REFERENCES company(code),
  financial_statement_id VARCHAR(10) NOT NULL REFERENCES financial_statement(id),
  detail_id VARCHAR(10) NOT NULL REFERENCES profit_and_less_statement_detail(id),
  period DATE NOT NULL,
  value INT NOT NULL,
  PRIMARY KEY(id)
);

--キャッシュ・フロー計算書
create TABLE cash_flow_statement(
  id INT AUTO_INCREMENT,
  company_id VARCHAR(10) NOT NULL REFERENCES company(code),
  financial_statement_id VARCHAR(10) NOT NULL REFERENCES financial_statement(id),
  detail_id VARCHAR(10) NOT NULL REFERENCES cash_flow_statement_detail(id),
  period DATE NOT NULL,
  value INT NOT NULL,
  PRIMARY KEY(id)
);
