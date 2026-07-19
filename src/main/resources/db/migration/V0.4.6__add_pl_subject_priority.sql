ALTER TABLE IF EXISTS `pl_subject`
    ADD COLUMN `priority` INT(2) DEFAULT NULL COMMENT '優先度' AFTER `detail_subject_id`;

UPDATE `pl_subject`
SET `priority` = 1
WHERE `outline_subject_id` = '11'
  AND `detail_subject_id` IN ('7', '8', '9', '10', '11');

UPDATE `pl_subject`
SET `priority` = 2
WHERE `outline_subject_id` = '11'
  AND `detail_subject_id` IN ('1', '2', '3', '4', '5', '6');
