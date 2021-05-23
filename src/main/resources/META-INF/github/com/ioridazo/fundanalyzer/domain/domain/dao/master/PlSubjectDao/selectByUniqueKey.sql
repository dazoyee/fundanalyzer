select *
from pl_subject
where outline_subject_id = /* outlineSubjectId */'outlineSubjectId'
    /*%if detailSubjectId != null */
  and detail_subject_id = /* detailSubjectId */'detailSubjectId'
/*%end */