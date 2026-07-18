select *
from stock_price
where company_code = /* code */'7203'
  and target_date >= /* fromDate */'2026-01-01'
  and target_date <= /* toDate */'2026-02-15'
order by target_date asc
limit 1
