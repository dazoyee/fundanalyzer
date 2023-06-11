package github.com.ioridazo.fundanalyzer.domain.domain.entity.master;

import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

@Entity(immutable = true)
@Table(name = "pl_subject")
public record PlSubjectEntity(

        @Id
        String id,

        String outlineSubjectId,

        String detailSubjectId,

        String name
) {
}
