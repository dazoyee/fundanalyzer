select
  *
from financial_statement
where
  edinet_code = /* edinetCode */'E00000'
and
  financial_statement_id = /* financialStatementId */'1'
and
  subject_id = /* subjectId */'10'
and
  to_date like /* @prefix(dayOfYear) */'2020%'
