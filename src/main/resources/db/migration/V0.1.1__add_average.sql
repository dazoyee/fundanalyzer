ALTER TABLE IF EXISTS `corporate_view` CHANGE COLUMN `average_corporate_value` `all_average_corporate_value` FLOAT DEFAULT NULL COMMENT '全平均企業価値';
ALTER TABLE IF EXISTS `corporate_view` CHANGE COLUMN `standard_deviation` `all_standard_deviation` FLOAT DEFAULT NULL COMMENT '全標準偏差';
ALTER TABLE IF EXISTS `corporate_view` CHANGE COLUMN `coefficient_of_variation` `all_coefficient_of_variation` FLOAT DEFAULT NULL COMMENT '全変動係数';

ALTER TABLE IF EXISTS `corporate_view` ADD COLUMN `three_average_corporate_value` FLOAT DEFAULT NULL COMMENT '3年平均企業価値' AFTER `latest_corporate_value`;
ALTER TABLE IF EXISTS `corporate_view` ADD COLUMN `three_standard_deviation` FLOAT DEFAULT NULL COMMENT '3年標準偏差' AFTER `three_average_corporate_value`;
ALTER TABLE IF EXISTS `corporate_view` ADD COLUMN `three_coefficient_of_variation` FLOAT DEFAULT NULL COMMENT '3年変動係数' AFTER `three_standard_deviation`;
ALTER TABLE IF EXISTS `corporate_view` ADD COLUMN `five_average_corporate_value` FLOAT DEFAULT NULL COMMENT '5年平均企業価値' AFTER `three_coefficient_of_variation`;
ALTER TABLE IF EXISTS `corporate_view` ADD COLUMN `five_standard_deviation` FLOAT DEFAULT NULL COMMENT '5年標準偏差' AFTER `five_average_corporate_value`;
ALTER TABLE IF EXISTS `corporate_view` ADD COLUMN `five_coefficient_of_variation` FLOAT DEFAULT NULL COMMENT '5年変動係数' AFTER `five_standard_deviation`;
ALTER TABLE IF EXISTS `corporate_view` ADD COLUMN `ten_average_corporate_value` FLOAT DEFAULT NULL COMMENT '10年平均企業価値' AFTER `five_coefficient_of_variation`;
ALTER TABLE IF EXISTS `corporate_view` ADD COLUMN `ten_standard_deviation` FLOAT DEFAULT NULL COMMENT '10年標準偏差' AFTER `ten_average_corporate_value`;
ALTER TABLE IF EXISTS `corporate_view` ADD COLUMN `ten_coefficient_of_variation` FLOAT DEFAULT NULL COMMENT '10年変動係数' AFTER `ten_standard_deviation`;

ALTER TABLE IF EXISTS `corporate_view` CHANGE COLUMN `discount_value` `all_discount_value` FLOAT DEFAULT NULL COMMENT '全割安値';
ALTER TABLE IF EXISTS `corporate_view` CHANGE COLUMN `discount_rate` `all_discount_rate` FLOAT DEFAULT NULL COMMENT '全割安度';

ALTER TABLE IF EXISTS `corporate_view` ADD COLUMN `three_discount_value` FLOAT DEFAULT NULL COMMENT '3年割安値' AFTER `latest_stock_price`;
ALTER TABLE IF EXISTS `corporate_view` ADD COLUMN `three_discount_rate` FLOAT DEFAULT NULL COMMENT '3年割安度' AFTER `three_discount_value`;
ALTER TABLE IF EXISTS `corporate_view` ADD COLUMN `five_discount_value` FLOAT DEFAULT NULL COMMENT '5年割安値' AFTER `three_discount_rate`;
ALTER TABLE IF EXISTS `corporate_view` ADD COLUMN `five_discount_rate` FLOAT DEFAULT NULL COMMENT '5年割安度' AFTER `five_discount_value`;
ALTER TABLE IF EXISTS `corporate_view` ADD COLUMN `ten_discount_value` FLOAT DEFAULT NULL COMMENT '10年割安値' AFTER `five_discount_rate`;
ALTER TABLE IF EXISTS `corporate_view` ADD COLUMN `ten_discount_rate` FLOAT DEFAULT NULL COMMENT '10年割安度' AFTER `ten_discount_value`;
