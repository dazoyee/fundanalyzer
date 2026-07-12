-- Update only industry master operating_profit_weight values based on the
-- recommended plan B: TSE 33-industry PER * (1 - effective tax rate 30.62%).
-- No retroactive recalculation is performed for existing analysis_result rows.
-- current_liabilities_ratio (1.2) and cost_of_equity (0.08) remain unchanged.
-- Source: docs/plans/industry-coefficient-setup.md
-- Verification note: each UPDATE is expected to affect exactly 1 row by
-- industry.name exact match, including production-specific spellings.

-- 33 industries
UPDATE `industry` SET `operating_profit_weight` = 8.2600 WHERE `name` = '水産・農林業';
UPDATE `industry` SET `operating_profit_weight` = 7.9800 WHERE `name` = '鉱業';
UPDATE `industry` SET `operating_profit_weight` = 12.0000 WHERE `name` = '建設業';
UPDATE `industry` SET `operating_profit_weight` = 15.2600 WHERE `name` = '食料品';
UPDATE `industry` SET `operating_profit_weight` = 11.7300 WHERE `name` = '繊維製品';
UPDATE `industry` SET `operating_profit_weight` = 11.0300 WHERE `name` = 'パルプ・紙';
UPDATE `industry` SET `operating_profit_weight` = 15.7500 WHERE `name` = '化学';
UPDATE `industry` SET `operating_profit_weight` = 14.3600 WHERE `name` = '医薬品';
UPDATE `industry` SET `operating_profit_weight` = 9.5100 WHERE `name` = '石油・石炭製品';
UPDATE `industry` SET `operating_profit_weight` = 8.9500 WHERE `name` = 'ゴム製品';
UPDATE `industry` SET `operating_profit_weight` = 21.8500 WHERE `name` = 'ガラス・土石製品';
UPDATE `industry` SET `operating_profit_weight` = 7.9100 WHERE `name` = '鉄鋼';
UPDATE `industry` SET `operating_profit_weight` = 25.7400 WHERE `name` = '非鉄金属';
UPDATE `industry` SET `operating_profit_weight` = 10.6200 WHERE `name` = '金属製品';
UPDATE `industry` SET `operating_profit_weight` = 17.4800 WHERE `name` = '機械';
UPDATE `industry` SET `operating_profit_weight` = 24.0100 WHERE `name` = '電気機器';
UPDATE `industry` SET `operating_profit_weight` = 13.8100 WHERE `name` = '輸送用機器';
UPDATE `industry` SET `operating_profit_weight` = 19.4300 WHERE `name` = '精密機器';
UPDATE `industry` SET `operating_profit_weight` = 14.4300 WHERE `name` = 'その他製品';
UPDATE `industry` SET `operating_profit_weight` = 6.4500 WHERE `name` = '電気・ガス業';
UPDATE `industry` SET `operating_profit_weight` = 9.6400 WHERE `name` = '陸運業';
UPDATE `industry` SET `operating_profit_weight` = 4.1600 WHERE `name` = '海運業';
UPDATE `industry` SET `operating_profit_weight` = 7.0800 WHERE `name` = '空運業';
UPDATE `industry` SET `operating_profit_weight` = 13.0400 WHERE `name` = '倉庫・運輸関連';
UPDATE `industry` SET `operating_profit_weight` = 14.2200 WHERE `name` = '情報・通信業';
UPDATE `industry` SET `operating_profit_weight` = 9.9200 WHERE `name` = '卸売業';
UPDATE `industry` SET `operating_profit_weight` = 15.6100 WHERE `name` = '小売業';
UPDATE `industry` SET `operating_profit_weight` = 12.8400 WHERE `name` = '銀行業';
UPDATE `industry` SET `operating_profit_weight` = 9.7800 WHERE `name` = '証券、商品先物取引業';
UPDATE `industry` SET `operating_profit_weight` = 10.7500 WHERE `name` = '保険業';
UPDATE `industry` SET `operating_profit_weight` = 8.9500 WHERE `name` = 'その他金融業';
UPDATE `industry` SET `operating_profit_weight` = 8.8100 WHERE `name` = '不動産業';
UPDATE `industry` SET `operating_profit_weight` = 13.4600 WHERE `name` = 'サービス業';

-- 7 non-industry placeholders: keep default-equivalent value 10.0000 explicitly
UPDATE `industry` SET `operating_profit_weight` = 10.0000 WHERE `name` = '内国法人・組合（有価証券報告書等の提出義務者以外）';
UPDATE `industry` SET `operating_profit_weight` = 10.0000 WHERE `name` = '外国法人・組合';
UPDATE `industry` SET `operating_profit_weight` = 10.0000 WHERE `name` = '外国法人・組合（有価証券報告書等の提出義務者以外）';
UPDATE `industry` SET `operating_profit_weight` = 10.0000 WHERE `name` = '外国政府等';
UPDATE `industry` SET `operating_profit_weight` = 10.0000 WHERE `name` = '個人（組合発行者を除く）';
UPDATE `industry` SET `operating_profit_weight` = 10.0000 WHERE `name` = '個人（非居住者）（組合発行者を除く）';
UPDATE `industry` SET `operating_profit_weight` = 10.0000 WHERE `name` = 'その他（仮登録用）';
