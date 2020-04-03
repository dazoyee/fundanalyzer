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
