-- =========================================================================
-- v3 画面検証用テストデータ (dev プロファイル限定)
-- 詳細: docs/notes/T20260501-v3-screen-test-data-seed.md
--
-- 投入対象:
--   - /v3/index 4 タブ動作確認用 (メイン / 四半期 / すべて / お気に入り)
--   - /v3/corporate?code=9001 詳細描画確認用 (Chart.js 14 個)
--
-- ViewCorporateInteractor.filter() を通過する条件 (configCorporateSize=300日,
-- configDiscountRate=120, configOutlierOfStandardDeviation=10000,
-- configCoefficientOfVariation=0.5, configDiffForecastStock=100):
--   1. submit_date は now-300 日以降
--   2. all_discount_rate >= 120 (5年値が null なら all_* を表示)
--   3. all_standard_deviation < 10000
--   4. latest_corporate_value > 0 かつ all_average_corporate_value * 1.1
--   5. all_coefficient_of_variation < 0.5
--   6. forecast_stock > latest_stock_price * 1.1 かつ差分 >= 100
--
-- 本番影響:
--   - application-prod.yml の spring.flyway.locations は classpath:db/migration のみ
--   - 本ファイルは classpath:/db/dataset 配下のため prod では走行しない
-- =========================================================================

-- -------------------------------------------------------------------------
-- company (5 社)
--   9001 = メイン (有報) フィルタ通過
--   9002 = 四半期 only (corporate_view.latest_document_type_code='140')
--   9003 = メイン+四半期両方表示 (corporate_view 2 行: '120' と '140')
--   9004 = お気に入り (favorite=1, 有報)
--   9005 = removed (company.removed=1, ただし corporate_view は表示される)
-- -------------------------------------------------------------------------
INSERT INTO `company` (`code`, `company_name`, `industry_id`, `edinet_code`, `list_categories`, `consolidated`, `capital_stock`, `settlement_date`, `favorite`, `removed`)
VALUES ('90010', 'テスト株式会社A', 12, 'E90001', '1', '1', 100000000, '03-31', '0', '0'),
       ('90020', 'テスト株式会社B', 13, 'E90002', '1', '1', 200000000, '03-31', '0', '0'),
       ('90030', 'テスト株式会社C', 20, 'E90003', '1', '1', 300000000, '03-31', '0', '0'),
       ('90040', 'テスト株式会社D', 24, 'E90004', '1', '1', 400000000, '03-31', '1', '0'),
       ('90050', 'テスト株式会社E', 14, 'E90005', '1', '1', 500000000, '03-31', '0', '1');

-- -------------------------------------------------------------------------
-- edinet_document (8 件 / document と 1:1 対応)
--   /v3/corporate 詳細画面で edinetDocumentDao.selectByDocId が必須
-- -------------------------------------------------------------------------
INSERT INTO `edinet_document` (`doc_id`, `edinet_code`, `sec_code`, `filer_name`, `doc_type_code`, `period_start`, `period_end`, `submit_date_time`, `doc_description`, `xbrl_flag`)
VALUES ('S0000001', 'E90001', '90010', 'テスト株式会社A', '120', '2024-04-01', '2025-03-31', '2026-03-25 09:00', '有価証券報告書', '1'),
       ('S0000002', 'E90001', '90010', 'テスト株式会社A', '120', '2023-04-01', '2024-03-31', '2025-06-26 09:00', '有価証券報告書', '1'),
       ('S0000003', 'E90002', '90020', 'テスト株式会社B', '140', '2025-07-01', '2025-09-30', '2026-02-14 09:00', '四半期報告書', '1'),
       ('S0000004', 'E90002', '90020', 'テスト株式会社B', '140', '2025-04-01', '2025-06-30', '2025-08-13 09:00', '四半期報告書', '1'),
       ('S0000005', 'E90003', '90030', 'テスト株式会社C', '120', '2024-04-01', '2025-03-31', '2026-03-27 09:00', '有価証券報告書', '1'),
       ('S0000006', 'E90003', '90030', 'テスト株式会社C', '140', '2025-07-01', '2025-09-30', '2026-02-13 09:00', '四半期報告書', '1'),
       ('S0000007', 'E90004', '90040', 'テスト株式会社D', '120', '2024-04-01', '2025-03-31', '2026-03-25 09:00', '有価証券報告書', '1'),
       ('S0000008', 'E90005', '90050', 'テスト株式会社E', '120', '2024-04-01', '2025-03-31', '2026-03-25 09:00', '有価証券報告書', '1');

-- -------------------------------------------------------------------------
-- document (8 件)
-- -------------------------------------------------------------------------
INSERT INTO `document` (`document_id`, `document_type_code`, `edinet_code`, `document_period`, `submit_date`, `downloaded`, `decoded`, `scraped_number_of_shares`, `scraped_bs`, `scraped_pl`, `scraped_cf`, `removed`)
VALUES ('S0000001', '120', 'E90001', '2025-03-31', '2026-03-25', '1', '1', '1', '1', '1', '1', '0'),
       ('S0000002', '120', 'E90001', '2024-03-31', '2025-06-26', '1', '1', '1', '1', '1', '1', '0'),
       ('S0000003', '140', 'E90002', '2025-09-30', '2026-02-14', '1', '1', '1', '1', '1', '1', '0'),
       ('S0000004', '140', 'E90002', '2025-06-30', '2025-08-13', '1', '1', '1', '1', '1', '1', '0'),
       ('S0000005', '120', 'E90003', '2025-03-31', '2026-03-27', '1', '1', '1', '1', '1', '1', '0'),
       ('S0000006', '140', 'E90003', '2025-09-30', '2026-02-13', '1', '1', '1', '1', '1', '1', '0'),
       ('S0000007', '120', 'E90004', '2025-03-31', '2026-03-25', '1', '1', '1', '1', '1', '1', '0'),
       ('S0000008', '120', 'E90005', '2025-03-31', '2026-03-25', '1', '1', '1', '0', '0', '0', '0');
-- ↑ S0000008 はスクレイピング未完了（手動 fix 対象）として残す

-- -------------------------------------------------------------------------
-- analysis_result (8 件)
-- -------------------------------------------------------------------------
INSERT INTO `analysis_result` (`company_code`, `document_period`, `corporate_value`, `document_type_code`, `submit_date`, `document_id`)
VALUES ('90010', '2025-03-31', 3000.0, '120', '2026-03-25', 'S0000001'),
       ('90010', '2024-03-31', 2900.0, '120', '2025-06-26', 'S0000002'),
       ('90020', '2025-09-30', 5000.0, '140', '2026-02-14', 'S0000003'),
       ('90020', '2025-06-30', 4900.0, '140', '2025-08-13', 'S0000004'),
       ('90030', '2025-03-31', 7000.0, '120', '2026-03-27', 'S0000005'),
       ('90030', '2025-09-30', 7100.0, '140', '2026-02-13', 'S0000006'),
       ('90040', '2025-03-31', 11000.0, '120', '2026-03-25', 'S0000007'),
       ('90050', '2025-03-31', 2500.0, '120', '2026-03-25', 'S0000008');

-- -------------------------------------------------------------------------
-- stock_price (12 件 / 9001 を中心に直近 365 日内の点を散らす)
-- -------------------------------------------------------------------------
INSERT INTO `stock_price` (`company_code`, `target_date`, `stock_price`, `opening_price`, `high_price`, `low_price`, `volume`, `per`, `pbr`, `roe`, `source_of`)
VALUES ('90010', '2026-04-30', 1300.0, 1290.0, 1310.0, 1285.0, 100000, '10.83', '1.63', '0.12', '1'),
       ('90010', '2026-03-31', 1280.0, 1275.0, 1295.0, 1270.0, 95000, '10.67', '1.60', '0.12', '1'),
       ('90010', '2026-01-31', 1250.0, 1245.0, 1260.0, 1240.0, 80000, '10.42', '1.56', '0.12', '1'),
       ('90010', '2025-11-30', 1220.0, 1215.0, 1230.0, 1210.0, 70000, '10.17', '1.53', '0.11', '1'),
       ('90010', '2025-09-30', 1200.0, 1195.0, 1210.0, 1190.0, 65000, '10.00', '1.50', '0.11', '1'),
       ('90010', '2025-07-31', 1180.0, 1175.0, 1190.0, 1170.0, 60000, '9.83', '1.48', '0.11', '1'),
       ('90020', '2026-04-30', 2200.0, 2195.0, 2215.0, 2185.0, 50000, '12.22', '2.00', '0.15', '1'),
       ('90020', '2026-01-31', 2150.0, 2145.0, 2165.0, 2140.0, 48000, '11.94', '1.99', '0.14', '1'),
       ('90030', '2026-04-30', 3500.0, 3490.0, 3520.0, 3480.0, 30000, '14.00', '2.33', '0.18', '1'),
       ('90030', '2025-12-31', 3400.0, 3395.0, 3415.0, 3390.0, 28000, '13.60', '2.27', '0.18', '1'),
       ('90040', '2026-04-30', 4800.0, 4790.0, 4820.0, 4780.0, 20000, '13.71', '2.40', '0.20', '1'),
       ('90050', '2026-04-30', 900.0, 895.0, 910.0, 890.0, 10000, '10.00', '1.50', '0.10', '1');

-- -------------------------------------------------------------------------
-- minkabu (6 件)
-- -------------------------------------------------------------------------
INSERT INTO `minkabu` (`company_code`, `target_date`, `stock_price`, `goals_stock`, `theoretical_stock`, `individual_investors_stock`, `securities_analyst_stock`)
VALUES ('90010', '2026-04-30', 1300.0, 1500.0, 1450.0, 1400.0, 1550.0),
       ('90010', '2026-01-31', 1250.0, 1480.0, 1430.0, 1380.0, 1530.0),
       ('90010', '2025-09-30', 1200.0, 1450.0, 1400.0, 1350.0, 1500.0),
       ('90020', '2026-04-30', 2200.0, 2500.0, 2400.0, 2350.0, 2550.0),
       ('90030', '2026-04-30', 3500.0, 3800.0, 3700.0, 3650.0, 3850.0),
       ('90040', '2026-04-30', 4800.0, 5200.0, 5100.0, 5050.0, 5250.0);

-- -------------------------------------------------------------------------
-- investment_indicator (6 件)
-- -------------------------------------------------------------------------
INSERT INTO `investment_indicator` (`stock_id`, `analysis_result_id`, `company_code`, `target_date`, `price_corporate_value_ratio`, `per`, `pbr`, `graham_index`, `document_id`)
SELECT sp.id, ar.id, '90010', sp.target_date, sp.stock_price / ar.corporate_value, 10.83, 1.63, 17.66, 'S0000001'
FROM `stock_price` sp, `analysis_result` ar
WHERE sp.company_code = '90010' AND sp.target_date = '2026-04-30' AND ar.document_id = 'S0000001'
LIMIT 1;

INSERT INTO `investment_indicator` (`stock_id`, `analysis_result_id`, `company_code`, `target_date`, `price_corporate_value_ratio`, `per`, `pbr`, `graham_index`, `document_id`)
SELECT sp.id, ar.id, '90010', sp.target_date, sp.stock_price / ar.corporate_value, 10.42, 1.56, 16.25, 'S0000001'
FROM `stock_price` sp, `analysis_result` ar
WHERE sp.company_code = '90010' AND sp.target_date = '2026-01-31' AND ar.document_id = 'S0000001'
LIMIT 1;

INSERT INTO `investment_indicator` (`stock_id`, `analysis_result_id`, `company_code`, `target_date`, `price_corporate_value_ratio`, `per`, `pbr`, `graham_index`, `document_id`)
SELECT sp.id, ar.id, '90010', sp.target_date, sp.stock_price / ar.corporate_value, 10.00, 1.50, 15.00, 'S0000001'
FROM `stock_price` sp, `analysis_result` ar
WHERE sp.company_code = '90010' AND sp.target_date = '2025-09-30' AND ar.document_id = 'S0000001'
LIMIT 1;

INSERT INTO `investment_indicator` (`stock_id`, `analysis_result_id`, `company_code`, `target_date`, `price_corporate_value_ratio`, `per`, `pbr`, `graham_index`, `document_id`)
SELECT sp.id, ar.id, '90020', sp.target_date, sp.stock_price / ar.corporate_value, 12.22, 2.00, 24.44, 'S0000003'
FROM `stock_price` sp, `analysis_result` ar
WHERE sp.company_code = '90020' AND sp.target_date = '2026-04-30' AND ar.document_id = 'S0000003'
LIMIT 1;

INSERT INTO `investment_indicator` (`stock_id`, `analysis_result_id`, `company_code`, `target_date`, `price_corporate_value_ratio`, `per`, `pbr`, `graham_index`, `document_id`)
SELECT sp.id, ar.id, '90030', sp.target_date, sp.stock_price / ar.corporate_value, 14.00, 2.33, 32.62, 'S0000005'
FROM `stock_price` sp, `analysis_result` ar
WHERE sp.company_code = '90030' AND sp.target_date = '2026-04-30' AND ar.document_id = 'S0000005'
LIMIT 1;

INSERT INTO `investment_indicator` (`stock_id`, `analysis_result_id`, `company_code`, `target_date`, `price_corporate_value_ratio`, `per`, `pbr`, `graham_index`, `document_id`)
SELECT sp.id, ar.id, '90040', sp.target_date, sp.stock_price / ar.corporate_value, 13.71, 2.40, 32.90, 'S0000007'
FROM `stock_price` sp, `analysis_result` ar
WHERE sp.company_code = '90040' AND sp.target_date = '2026-04-30' AND ar.document_id = 'S0000007'
LIMIT 1;

-- -------------------------------------------------------------------------
-- financial_statement (8 件)
-- -------------------------------------------------------------------------
INSERT INTO `financial_statement` (`company_code`, `edinet_code`, `financial_statement_id`, `subject_id`, `period_start`, `period_end`, `value`, `document_type_code`, `submit_date`, `document_id`, `created_type`)
VALUES ('90010', 'E90001', '1', '1',  '2024-04-01', '2025-03-31', 800000000, '120', '2026-03-25', 'S0000001', '0'),
       ('90010', 'E90001', '1', '5',  '2024-04-01', '2025-03-31', 600000000, '120', '2026-03-25', 'S0000001', '0'),
       ('90010', 'E90001', '2', '1',  '2024-04-01', '2025-03-31', 1200000000,'120', '2026-03-25', 'S0000001', '0'),
       ('90010', 'E90001', '2', '11', '2024-04-01', '2025-03-31', 90000000,  '120', '2026-03-25', 'S0000001', '0'),
       ('90020', 'E90002', '1', '1',  '2025-07-01', '2025-09-30', 1500000000,'140', '2026-02-14', 'S0000003', '0'),
       ('90020', 'E90002', '1', '5',  '2025-07-01', '2025-09-30', 1100000000,'140', '2026-02-14', 'S0000003', '0'),
       ('90030', 'E90003', '2', '1',  '2024-04-01', '2025-03-31', 2500000000,'120', '2026-03-27', 'S0000005', '0'),
       ('90030', 'E90003', '2', '11', '2024-04-01', '2025-03-31', 200000000, '120', '2026-03-27', 'S0000005', '0');

-- -------------------------------------------------------------------------
-- edinet_list_view (4 件 / EDINET 一覧 + 詳細画面の主データソース)
-- -------------------------------------------------------------------------
INSERT INTO `edinet_list_view` (`submit_date`, `count_all`, `count_target`, `count_scraped`, `count_analyzed`, `cant_scraped_id`, `not_analyzed_id`, `count_not_scraped`, `count_not_target`)
VALUES ('2026-03-25', 3, 3, 2, 2, 'S0000008', 'S0000008', 1, 0),
       ('2026-03-27', 1, 1, 1, 1, NULL, NULL, 0, 0),
       ('2026-02-13', 1, 1, 1, 1, NULL, NULL, 0, 0),
       ('2026-02-14', 1, 1, 1, 1, NULL, NULL, 0, 0);

-- -------------------------------------------------------------------------
-- corporate_view (6 件 / index 一覧の主データソース)
--   PRIMARY KEY (code, latest_document_type_code) のため複数行可
--   9003 は '120' と '140' の 2 行 (メイン+四半期両方表示テスト)
--
-- フィルタ通過条件 (configDiscountRate=120, all_average*1.1 < latest_corporate_value):
--   - latest_corporate_value: filter 通過用
--   - all_discount_rate >= 120
--   - all_average_corporate_value * 1.1 < latest_corporate_value
--   - all_coefficient_of_variation < 0.5
--   - forecast_stock / latest_stock_price > 1.1 かつ差 >= 100
-- -------------------------------------------------------------------------
INSERT INTO `corporate_view` (`code`, `name`, `submit_date`, `latest_document_type_code`, `latest_corporate_value`, `three_average_corporate_value`, `three_standard_deviation`, `three_coefficient_of_variation`, `all_average_corporate_value`, `all_standard_deviation`, `all_coefficient_of_variation`, `average_stock_price`, `import_date`, `latest_stock_price`, `three_discount_value`, `three_discount_rate`, `all_discount_value`, `all_discount_rate`, `count_year`, `forecast_stock`, `price_corporate_value_ratio`, `per`, `pbr`, `graham_index`, `bps`, `eps`, `roe`, `roa`)
VALUES ('9001', 'テスト株式会社A', '2026-03-25', '120', 3000.0, 2700.0, 50.0, 0.017, 2700.0, 50.0, 0.017, 1280.0, '2026-04-30', 1300.0, 1700.0, 130.8, 1700.0, 130.8, 2, 1500.0, 0.43, 10.83, 1.63, 17.66, 800.0,  120.0, 0.12, 0.08),
       ('9002', 'テスト株式会社B', '2026-02-14', '140', 5000.0, 4500.0, 50.0, 0.010, 4500.0, 50.0, 0.010, 2175.0, '2026-04-30', 2200.0, 2800.0, 127.3, 2800.0, 127.3, 1, 2500.0, 0.44, 12.22, 2.00, 24.44, 1100.0, 180.0, 0.15, 0.10),
       ('9003', 'テスト株式会社C', '2026-03-27', '120', 7000.0, 6300.0, 50.0, 0.007, 6300.0, 50.0, 0.007, 3450.0, '2026-04-30', 3500.0, 3500.0, 100.0, 3500.0, 100.0, 2, 4000.0, 0.50, 14.00, 2.33, 32.62, 1520.0, 260.0, 0.19, 0.12),
       ('9003', 'テスト株式会社C', '2026-02-13', '140', 7100.0, 6400.0, 50.0, 0.007, 6400.0, 50.0, 0.007, 3450.0, '2026-04-30', 3500.0, 3600.0, 102.9, 3600.0, 102.9, 1, 4000.0, 0.49, 14.00, 2.33, 32.62, 1520.0, 260.0, 0.19, 0.12),
       ('9004', 'テスト株式会社D', '2026-03-25', '120', 11000.0, 9900.0, 0.0, 0.000, 9900.0, 0.0, 0.000, 4800.0, '2026-04-30', 4800.0, 6200.0, 129.2, 6200.0, 129.2, 1, 5400.0, 0.44, 13.71, 2.40, 32.90, 2000.0, 350.0, 0.20, 0.15),
       ('9005', 'テスト株式会社E', '2026-03-25', '120', 2500.0, 2200.0, 0.0,  0.000, 2200.0, 0.0,  0.000, 900.0,  '2026-04-30', 900.0, 1600.0, 177.8, 1600.0, 177.8, 1, 1100.0, 0.36, 10.00, 1.50, 15.00, 600.0, 90.0, 0.10, 0.06);

-- -------------------------------------------------------------------------
-- valuation_view (5 件 / 分析・業種別表示の主データソース)
--   PK: code (CHAR(4))
--   差分パターン:
--     9001: +150 (上昇 GOOD・配当 2.5%)
--     9002: -100 (下落 BAD・配当 1.8%)
--     9003: 0    (横ばい・配当 3.2%)
--     9004: +250 (大幅上昇・お気に入り対象・配当 1.5%)
--     9005: -50  (小幅下落・配当 4.0%)
--
--   業種別タブ用に各社の company.industry_id に従って自動的に集約される:
--     9001: industry 12 情報・通信業
--     9002: industry 13 食料品
--     9003: industry 20 電気機器
--     9004: industry 24 医薬品
--     9005: industry 14 小売業
-- -------------------------------------------------------------------------
INSERT INTO `valuation_view` (`code`, `name`, `target_date`, `stock_price`, `graham_index`, `discount_value`, `discount_rate`, `submit_date`, `stock_price_of_submit_date`, `day_since_submit_date`, `difference_from_submit_date`, `submit_date_ratio`, `graham_index_of_submit_date`, `corporate_value`, `dividend_yield`)
VALUES ('9001', 'テスト株式会社A', '2026-04-30', 1450.0, 17.66, 1550.0, 106.9, '2026-03-25', 1300.0, 36, 150.0, 11.5, 16.10, 3000.0, 2.5),
       ('9002', 'テスト株式会社B', '2026-04-30', 2100.0, 22.50, 2900.0, 138.1, '2026-02-14', 2200.0, 75, -100.0, -4.5, 24.44, 5000.0, 1.8),
       ('9003', 'テスト株式会社C', '2026-04-30', 3500.0, 32.62, 3500.0, 100.0, '2026-03-27', 3500.0, 34, 0.0, 0.0, 32.00, 7000.0, 3.2),
       ('9004', 'テスト株式会社D', '2026-04-30', 5050.0, 30.00, 5950.0, 117.8, '2026-03-25', 4800.0, 36, 250.0, 5.2, 32.90, 11000.0, 1.5),
       ('9005', 'テスト株式会社E', '2026-04-30', 850.0, 14.50, 1650.0, 194.1, '2026-03-25', 900.0, 36, -50.0, -5.5, 15.00, 2500.0, 4.0);
