select *
from document
where document_type_code in /* documentTypeCode */('120', '130', '140', '150')
  and (document_period is null or document_period = '1970-01-01')
  and submit_date = /* submitDate */'2021-09-25'
