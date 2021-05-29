package github.com.ioridazo.fundanalyzer.domain.domain.entity.master;

import lombok.Value;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.util.Optional;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
@Entity(immutable = true)
@Table(name = "bs_subject")
public final class BsSubjectEntity {

    @Id
    private final String id;

    private final String outlineSubjectId;

    private final String detailSubjectId;

    private final String name;

    public Optional<String> getDetailSubjectId() {
        return Optional.ofNullable(detailSubjectId);
    }
}
