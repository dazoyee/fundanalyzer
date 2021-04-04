select *
from document
where edinet_code = /* edinetCode */'E12345'
  and document_type_code = /* documentTypeCode */'120'
  and document_period like /* @prefix(yearOfPeriod) */'2020%'