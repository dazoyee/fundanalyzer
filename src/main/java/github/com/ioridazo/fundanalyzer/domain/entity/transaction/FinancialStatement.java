package github.com.ioridazo.fundanalyzer.domain.entity.transaction;

import github.com.ioridazo.fundanalyzer.domain.entity.DocumentTypeCode;
import lombok.Value;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.GeneratedValue;
import org.seasar.doma.GenerationType;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
@Entity(immutable = true)
@Table(name = "financial_statement")
public class FinancialStatement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private final Integer id;

    private final String companyCode;

    private final String edinetCode;

    private final String financialStatementId;

    private final String subjectId;

    private final LocalDate periodStart;

    private final LocalDate periodEnd;

    private final Long value;

    private final String documentTypeCode;

    private final LocalDate submitDate;

    private final String documentId;

    @Column(updatable = false)
    private final LocalDateTime createdAt;

    public static FinancialStatement of(
            final String companyCode,
            final String edinetCode,
            final String financialStatementId,
            final String subjectId,
            final LocalDate periodStart,
            final LocalDate periodEnd,
            final Long value,
            final DocumentTypeCode documentTypeCode,
            final LocalDate submitDate,
            final String documentId,
            final LocalDateTime createdAt) {
        return new FinancialStatement(
                null,
                companyCode,
                edinetCode,
                financialStatementId,
                subjectId,
                periodStart,
                periodEnd,
                value,
                documentTypeCode.toValue(),
                submitDate,
                documentId,
                createdAt
        );
    }

    public Optional<String> getCompanyCode() {
        return Optional.ofNullable(companyCode);
    }

    public Optional<Long> getValue() {
        return Optional.ofNullable(value);
    }
}
