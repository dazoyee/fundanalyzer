select
  company_code,
  max(target_date) as target_date
from
  stock_price
group by
  company_code
