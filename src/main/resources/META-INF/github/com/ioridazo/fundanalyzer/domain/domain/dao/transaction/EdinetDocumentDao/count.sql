select count(*)
from edinet_document
where submit_date_time like /* @prefix(submitDate) */'2021-09-20%'
