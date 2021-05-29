select *
from financial_statement
where edinet_code = /* edinetCode */'E00000'
  and period_end = /* periodEnd */'2020-12-31'
  and document_type_code = /* documentTypeCode */'120'
  and submit_date = /* submitDate */'2021-05-05'
