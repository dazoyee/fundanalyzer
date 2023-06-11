package github.com.ioridazo.fundanalyzer.domain.domain.entity.master;

import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.GeneratedValue;
import org.seasar.doma.GenerationType;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.time.LocalDateTime;

@Entity(immutable = true)
@Table(name = "industry")
public record IndustryEntity(

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        Integer id,

        String name,

        @Column(updatable = false)
        LocalDateTime createdAt
) {

    public static IndustryEntity of(final String industryName, final LocalDateTime nowLocalDateTime) {
        return new IndustryEntity(
                null,
                industryName,
                nowLocalDateTime
        );
    }
}
