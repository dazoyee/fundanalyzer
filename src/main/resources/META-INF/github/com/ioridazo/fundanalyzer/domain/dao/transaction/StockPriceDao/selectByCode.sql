select * from stock_price
where
  company_code = /* code */'00000'
order by target_date desc
limit 1
