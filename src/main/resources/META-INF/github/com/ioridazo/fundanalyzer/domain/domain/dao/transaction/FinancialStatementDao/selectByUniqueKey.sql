select *
from financial_statement
where edinet_code = /* edinetCode */'E00000'
  and financial_statement_id = /* financialStatementId */'1'
  and subject_id = /* subjectId */'10'
  and period_end like /* @prefix(dayOfYear) */'2020%'
  and document_type_code = /* documentTypeCode */'120'
  and quarter_type = /* quarterType */'1'
  and submit_date = /* submitDate */'2021-01-01'
