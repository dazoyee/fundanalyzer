select * from edinet_document
where
  edinet_code = /* edinetCode */'E12345'
and
  doc_type_code = /* docTypeCode */'120'
and
  submit_date_time like /* @prefix(dayOfYear) */'2020%'