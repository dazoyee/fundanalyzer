select *
from document
where edinet_code = /* edinetCode */'E12345'
  and document_type_code = /* documentTypeCode */'120'
  and submit_date = /* submitDate */'2021-01-01'
  and document_period like /* @prefix(yearOfPeriod) */'2020%'