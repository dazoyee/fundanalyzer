-- 貸借対照表科目に、銀行法・保険業法の業法様式で使われる部単位の合計ラベルを追加する。
-- 科目照合は bs_subject.name との完全一致（SubjectSpecification.findBsSubject）のため、
-- 未登録の表記はスクレイピング時に黙って読み飛ばされ、financial_statement に値が入らない。
-- detail_subject_id は、マスタが手動運用されてきた環境に存在する既存行との衝突を避けるため、
-- 大きめの値から採番する。
INSERT INTO `bs_subject` (`outline_subject_id`, `detail_subject_id`, `name`)
VALUES ('7', '11', '資産の部合計'),
       ('10', '11', '負債の部合計'),
       ('14', '11', '純資産の部合計');
