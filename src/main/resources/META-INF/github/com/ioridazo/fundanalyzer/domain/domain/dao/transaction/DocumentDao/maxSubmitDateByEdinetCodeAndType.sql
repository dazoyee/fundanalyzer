select *
from document as d1
         inner join (
    select max(submit_date) as submit_date
    from document as d2
    where edinet_code = /* edinetCode */'E00000'
      and document_type_code in /* documentTypeCode */('120', '130')
) as d2
where edinet_code = /* edinetCode */'E00000'
  and document_type_code in /* documentTypeCode */('120', '130')
  and d2.submit_date = d1.submit_date limit 1
