package github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction;

import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException;
import lombok.Value;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.GeneratedValue;
import org.seasar.doma.GenerationType;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
@Entity(immutable = true)
@Table(name = "financial_statement")
public class FinancialStatementEntity {

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

    private final String quarterType;

    private final LocalDate submitDate;

    private final String documentId;

    private final String createdType;

    @Column(updatable = false)
    private final LocalDateTime createdAt;

    public static FinancialStatementEntity of(
            final String companyCode,
            final String edinetCode,
            final String financialStatementId,
            final String subjectId,
            final LocalDate periodStart,
            final LocalDate periodEnd,
            final Long value,
            final DocumentTypeCode documentTypeCode,
            final QuarterType quarterType,
            final LocalDate submitDate,
            final String documentId,
            final String createdType,
            final LocalDateTime createdAt) {
        if (DocumentTypeCode.DTC_140 == documentTypeCode && Objects.isNull(quarterType.toValue())) {
            throw new FundanalyzerNotExistException("四半期種別");
        }

        return new FinancialStatementEntity(
                null,
                companyCode,
                edinetCode,
                financialStatementId,
                subjectId,
                periodStart,
                periodEnd,
                value,
                documentTypeCode.toValue(),
                quarterType.toValue(),
                submitDate,
                documentId,
                createdType,
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
