select * from edinet_document
where
  doc_type_code = /* docTypeCode */'120'
and
  period_end like /* @prefix(dayOfYear) */'2020%'
