select *
from analysis_result
where document_type_code in /* documentTypeCodes */('120', '130')
  and (
    bps is null
    or eps is null
    or roe is null
    or roa is null
    or rim_value is null
  )
order by submit_date asc, company_code asc, document_id asc
