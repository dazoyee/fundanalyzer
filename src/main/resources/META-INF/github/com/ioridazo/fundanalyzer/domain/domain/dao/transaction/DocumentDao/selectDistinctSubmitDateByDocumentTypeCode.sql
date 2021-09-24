select distinct submit_date
from document
where document_type_code in /* documentTypeCode */('120', '130', '140', '150')
