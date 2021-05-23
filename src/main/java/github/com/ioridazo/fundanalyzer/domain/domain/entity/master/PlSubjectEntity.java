package github.com.ioridazo.fundanalyzer.domain.domain.entity.master;

import lombok.Value;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
@Entity(immutable = true)
@Table(name = "pl_subject")
public final class PlSubjectEntity {

    @Id
    private final String id;

    private final String outlineSubjectId;

    private final String detailSubjectId;

    private final String name;
}
