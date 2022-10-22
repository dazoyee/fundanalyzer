package github.com.ioridazo.fundanalyzer.domain.domain.entity.master;

import lombok.Value;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.GeneratedValue;
import org.seasar.doma.GenerationType;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.time.LocalDateTime;
import java.util.Optional;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
@Entity(immutable = true)
@Table(name = "scraping_keyword")
public class ScrapingKeywordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private final Integer id;

    private final String financialStatementId;

    private final String keyword;

    private final Integer priority;

    private final String remarks;

    @Column(updatable = false)
    private final LocalDateTime createdAt;

    public Optional<Integer> getPriority() {
        return Optional.ofNullable(priority);
    }

    public Optional<String> getRemarks() {
        return Optional.ofNullable(remarks);
    }
}
