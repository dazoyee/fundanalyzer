-- =========================================================================
-- 株式分割シナリオ検証用テストデータ (dev プロファイル限定)
-- 関連: docs/notes/T20260621-stock-price-split-adjustment.md（P1 動作確認）
--
-- 企業 91110（URL: /v3/corporate?code=9111）:
--   - 2025-01-31 に約1:5 の株式分割（確定アクション）を再現
--   - 有報の発行株式数: 1,000,000(2024期) → 5,000,000(2025期) の ×5 変化
--   - 株価: 2024-12-31 5,900 → 2025-01-31 1,180 の ÷5 クリフ
--   → CorporateActionSpecification が「確定分割（比率5・施行日2025-01-31）」を検知し、
--     チャート/明細/最新/平均/割安度/指標を有報基準で補正、チャートに分割マーカー（縦線）を描画する。
--
-- 本番影響なし（classpath:/db/dataset は prod の flyway.locations 対象外）。
-- =========================================================================

-- company
INSERT INTO `company` (`code`, `company_name`, `industry_id`, `edinet_code`, `list_categories`, `consolidated`, `capital_stock`, `settlement_date`, `favorite`, `removed`)
VALUES ('91110', 'テスト株式会社X（分割）', 12, 'E91110', '1', '1', 600000000, '03-31', '0', '0');

-- edinet_document（分割前 2024期 / 分割後 2025期）
INSERT INTO `edinet_document` (`doc_id`, `edinet_code`, `sec_code`, `filer_name`, `doc_type_code`, `period_start`, `period_end`, `submit_date_time`, `doc_description`, `xbrl_flag`)
VALUES ('S0000010', 'E91110', '91110', 'テスト株式会社X（分割）', '120', '2023-04-01', '2024-03-31', '2024-06-25 09:00', '有価証券報告書', '1'),
       ('S0000011', 'E91110', '91110', 'テスト株式会社X（分割）', '120', '2024-04-01', '2025-03-31', '2025-06-25 09:00', '有価証券報告書', '1');

-- document
INSERT INTO `document` (`document_id`, `document_type_code`, `edinet_code`, `document_period`, `submit_date`, `downloaded`, `decoded`, `scraped_number_of_shares`, `scraped_bs`, `scraped_pl`, `scraped_cf`, `removed`)
VALUES ('S0000010', '120', 'E91110', '2024-03-31', '2024-06-25', '1', '1', '1', '1', '1', '1', '0'),
       ('S0000011', '120', 'E91110', '2025-03-31', '2025-06-25', '1', '1', '1', '1', '1', '1', '0');

-- analysis_result（最新=分割後 S0000011。corporate_value は分割後株式数ベース）
INSERT INTO `analysis_result` (`company_code`, `document_period`, `corporate_value`, `bps`, `eps`, `roe`, `roa`, `document_type_code`, `submit_date`, `document_id`)
VALUES ('91110', '2024-03-31', 6000.0, 1500.0, 230.0, 0.15, 0.10, '120', '2024-06-25', 'S0000010'),
       ('91110', '2025-03-31', 1300.0, 320.0, 48.0, 0.15, 0.10, '120', '2025-06-25', 'S0000011');

-- financial_statement: 発行株式数（financial_statement_id='4', subject_id='0'）の ×5 変化
INSERT INTO `financial_statement` (`company_code`, `edinet_code`, `financial_statement_id`, `subject_id`, `period_start`, `period_end`, `value`, `document_type_code`, `submit_date`, `document_id`, `created_type`)
VALUES ('91110', 'E91110', '4', '0', '2023-04-01', '2024-03-31', 1000000, '120', '2024-06-25', 'S0000010', '0'),
       ('91110', 'E91110', '4', '0', '2024-04-01', '2025-03-31', 5000000, '120', '2025-06-25', 'S0000011', '0');

-- stock_price: 2024-12-31(5900) → 2025-01-31(1180) で ÷5 クリフ（その他は連続）
INSERT INTO `stock_price` (`company_code`, `target_date`, `stock_price`, `opening_price`, `high_price`, `low_price`, `volume`, `per`, `pbr`, `roe`, `source_of`)
VALUES ('91110', '2024-10-31', 6100.0, 6080.0, 6150.0, 6050.0, 40000, NULL, NULL, NULL, '1'),
       ('91110', '2024-11-29', 6000.0, 5980.0, 6050.0, 5950.0, 38000, NULL, NULL, NULL, '1'),
       ('91110', '2024-12-31', 5900.0, 5880.0, 5950.0, 5850.0, 36000, NULL, NULL, NULL, '1'),
       ('91110', '2025-01-31', 1180.0, 1175.0, 1190.0, 1170.0, 35000, NULL, NULL, NULL, '1'),
       ('91110', '2025-03-31', 1200.0, 1195.0, 1210.0, 1190.0, 34000, NULL, NULL, NULL, '1'),
       ('91110', '2025-06-30', 1220.0, 1215.0, 1230.0, 1210.0, 33000, NULL, NULL, NULL, '1'),
       ('91110', '2025-09-30', 1250.0, 1245.0, 1260.0, 1240.0, 32000, NULL, NULL, NULL, '1'),
       ('91110', '2025-12-31', 1280.0, 1275.0, 1290.0, 1270.0, 31000, NULL, NULL, NULL, '1'),
       ('91110', '2026-03-31', 1300.0, 1295.0, 1310.0, 1290.0, 30000, NULL, NULL, NULL, '1'),
       ('91110', '2026-04-30', 1320.0, 1315.0, 1330.0, 1310.0, 29000, NULL, NULL, NULL, '1');

-- corporate_view（一覧/詳細の表示用。最新=分割後基準）
INSERT INTO `corporate_view` (`code`, `name`, `submit_date`, `latest_document_type_code`, `latest_corporate_value`, `three_average_corporate_value`, `three_standard_deviation`, `three_coefficient_of_variation`, `all_average_corporate_value`, `all_standard_deviation`, `all_coefficient_of_variation`, `average_stock_price`, `import_date`, `latest_stock_price`, `three_discount_value`, `three_discount_rate`, `all_discount_value`, `all_discount_rate`, `count_year`, `forecast_stock`, `price_corporate_value_ratio`, `per`, `pbr`, `graham_index`, `bps`, `eps`, `roe`, `roa`)
VALUES ('9111', 'テスト株式会社X（分割）', '2025-06-25', '120', 1300.0, 1290.0, 10.0, 0.008, 1290.0, 10.0, 0.008, 1290.0, '2026-04-30', 1320.0, -20.0, 98.5, -20.0, 98.5, 2, 1450.0, 1.02, 27.5, 4.13, 113.6, 320.0, 48.0, 0.15, 0.10);

-- minkabu（予測株価チャート用・任意）
INSERT INTO `minkabu` (`company_code`, `target_date`, `stock_price`, `goals_stock`, `theoretical_stock`, `individual_investors_stock`, `securities_analyst_stock`)
VALUES ('91110', '2026-04-30', 1320.0, 1450.0, 1400.0, 1380.0, 1470.0);
