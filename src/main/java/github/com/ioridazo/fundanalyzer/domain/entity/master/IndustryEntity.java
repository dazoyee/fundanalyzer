package github.com.ioridazo.fundanalyzer.domain.entity.master;

import lombok.Value;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.GeneratedValue;
import org.seasar.doma.GenerationType;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.time.LocalDateTime;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
@Entity(immutable = true)
@Table(name = "industry")
public class IndustryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private final Integer id;

    private final String name;

    @Column(updatable = false)
    private final LocalDateTime createdAt;

    public static IndustryEntity of(final String industryName, final LocalDateTime nowLocalDateTime) {
        return new IndustryEntity(
                null,
                industryName,
                nowLocalDateTime
        );
    }
}
