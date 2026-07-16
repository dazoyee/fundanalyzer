-- analysis_result.corporate_value の一括再計算後に、係数依存の割引値・割引率を追随させる。
-- stock_price は評価時点の凍結値のため再取得せず、corporate_value のみ相関サブクエリで現行値を参照する。
-- ROUND(..., 2) は MySQL / H2 いずれも正の値に対して四捨五入（HALF_UP 相当）で、
-- ValuationSpecification.evaluate() の BigDecimal#divide(..., 2, RoundingMode.HALF_UP) と同じ丸め規則。
-- ただし格納先列は FLOAT のため、浮動小数点表現由来の僅かな誤差は許容される前提とする。
-- analysis_result_id はスキーマ上 NOT NULL（FK）であり、WHERE 句は将来のスキーマ変更に備えた防御。
update valuation
set discount_value = (select ar.corporate_value from analysis_result ar where ar.id = valuation.analysis_result_id) - stock_price,
    discount_rate   = round((select ar.corporate_value from analysis_result ar where ar.id = valuation.analysis_result_id) / stock_price, 2)
where analysis_result_id is not null
