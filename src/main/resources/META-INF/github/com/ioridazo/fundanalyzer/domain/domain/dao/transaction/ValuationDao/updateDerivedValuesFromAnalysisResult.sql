-- analysis_result.corporate_value の一括再計算後に、係数依存の割引値・割引率を追随させる。
-- stock_price は評価時点の凍結値のため再取得せず、corporate_value のみ相関サブクエリで現行値を参照する。
-- ROUND(..., 2) は MySQL / H2 いずれも正の値に対して四捨五入（HALF_UP 相当）となり、
-- ValuationSpecification.evaluate() の BigDecimal#divide(..., 2, RoundingMode.HALF_UP) と同じ丸め結果になる。
update valuation
set discount_value = (select ar.corporate_value from analysis_result ar where ar.id = valuation.analysis_result_id) - stock_price,
    discount_rate   = round((select ar.corporate_value from analysis_result ar where ar.id = valuation.analysis_result_id) / stock_price, 2)
where analysis_result_id is not null
