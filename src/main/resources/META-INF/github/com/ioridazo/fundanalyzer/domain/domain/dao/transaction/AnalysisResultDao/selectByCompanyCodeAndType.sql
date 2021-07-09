select *
from analysis_result
where company_code = /* code */'00000'
  and document_type_code in /* documentTypeCode */('120', '130')
