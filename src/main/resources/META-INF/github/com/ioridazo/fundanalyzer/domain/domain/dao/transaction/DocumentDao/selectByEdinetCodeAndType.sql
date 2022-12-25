select *
from document
where edinet_code = /* edinetCode */'E00000'
  and document_type_code in /* documentTypeCode */('120', '130')
