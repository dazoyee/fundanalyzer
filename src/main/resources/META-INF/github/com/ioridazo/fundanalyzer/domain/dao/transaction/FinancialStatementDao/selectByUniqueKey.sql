select
  *
from financial_statement
where
  company_code = /* companyCode */'00000'
and
  financial_statement_id = /* financialStatementId */'1'
and
  subject_id = /* subjectId */'10'
and
  to_date like /* @prefix(dayOfYear) */'2020%'
