create TABLE company(
  code CHAR(4) UNIQUE NOT NULL,
  name VARCHAR(100) UNIQUE NOT NULL,
  PRIMARY KEY(code)
);

create TABLE financial_statement(
  id INT AUTO_INCREMENT,
  name VARCHAR(20) UNIQUE NOT NULL,
  PRIMARY KEY(id)
);

create TABLE subject(
  id INT AUTO_INCREMENT,
  name VARCHAR(20) UNIQUE NOT NULL,
  PRIMARY KEY(id)
);

create TABLE zaim(
  id INT AUTO_INCREMENT,
  company_id VARCHAR(10) NOT NULL REFERENCES company(id),
  financial_statement_id VARCHAR(10) NOT NULL REFERENCES financial_statement(id),
  subject_id VARCHAR(10) NOT NULL REFERENCES subject(id),
  period DATE NOT NULL,
  value INT NOT NULL,
  PRIMARY KEY(id)
);
