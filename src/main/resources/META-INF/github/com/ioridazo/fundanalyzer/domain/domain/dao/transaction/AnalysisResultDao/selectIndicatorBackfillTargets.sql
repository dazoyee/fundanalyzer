select *
from analysis_result
where document_type_code in /* documentTypeCodes */('120', '130')
  -- ダミー行（手動投入されたプレースホルダ document_id）は削除前でもバックフィル対象に含めない（P3↔P6 の順序ミス時の安全弁）
  and document_id <> 'XXXXXXXX'
  and (
    bps is null
    or eps is null
    or roe is null
    or roa is null
    or rim_value is null
  )
order by submit_date asc, company_code asc, document_id asc
