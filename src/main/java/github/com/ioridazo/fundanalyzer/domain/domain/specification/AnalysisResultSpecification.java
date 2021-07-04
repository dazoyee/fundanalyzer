package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction.AnalysisResultDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seasar.doma.jdbc.UniqueConstraintException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedRuntimeException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class AnalysisResultSpecification {

    private static final Logger log = LogManager.getLogger(AnalysisResultSpecification.class);
    private static final int SECOND_DECIMAL_PLACE = 2;
    private static final int THIRD_DECIMAL_PLACE = 3;

    private final AnalysisResultDao analysisResultDao;
    private final CompanySpecification companySpecification;

    @Value("${app.config.view.document-type-code}")
    List<String> targetTypeCodes;

    public AnalysisResultSpecification(
            final AnalysisResultDao analysisResultDao,
            final CompanySpecification companySpecification) {
        this.analysisResultDao = analysisResultDao;
        this.companySpecification = companySpecification;
    }

    LocalDateTime nowLocalDateTime() {
        return LocalDateTime.now();
    }

    /**
     * 企業価値を登録する
     *
     * @param document ドキュメント
     * @param value    企業価値
     */
    public void insert(final Document document, final BigDecimal value) {
        final String companyCode = companySpecification.findCompanyByEdinetCode(document.getEdinetCode())
                .flatMap(Company::getCode)
                .orElseThrow(FundanalyzerNotExistException::new);
        try {
            analysisResultDao.insert(AnalysisResultEntity.of(
                    companyCode,
                    document.getDocumentPeriod().orElseThrow(FundanalyzerNotExistException::new),
                    value,
                    document.getDocumentTypeCode(),
                    document.getQuarterType(),
                    document.getSubmitDate(),
                    document.getDocumentId(),
                    nowLocalDateTime()
            ));
        } catch (NestedRuntimeException e) {
            if (e.contains(UniqueConstraintException.class)) {
                log.debug(
                        "一意制約違反のため、データベースへの登録をスキップします。" +
                                "\tテーブル名:{}\t会社コード:{}\t期間:{}\t書類種別コード:{}\t提出日:{}",
                        "analysis_result",
                        companyCode,
                        document.getDocumentPeriod(),
                        document.getDocumentTypeCode().toValue(),
                        document.getSubmitDate()
                );
            } else {
                throw new FundanalyzerRuntimeException("想定外のエラーが発生しました。", e);
            }
        }
    }

    /**
     * 最新の企業価値を取得する
     *
     * @param company 企業情報
     * @return 最新の企業価値
     */
    public Optional<BigDecimal> latestCorporateValue(final Company company) {
        return analysisTargetList(company, targetTypeCodes).stream()
                // latest
                .max(Comparator.comparing(AnalysisResultEntity::getDocumentPeriod)
                        .thenComparing(AnalysisResultEntity::getSubmitDate))
                // corporate value
                .map(AnalysisResultEntity::getCorporateValue)
                // scale
                .map(bigDecimal -> bigDecimal.setScale(SECOND_DECIMAL_PLACE, RoundingMode.HALF_UP));
    }

    /**
     * 平均の企業価値を取得する
     *
     * @param company 企業情報
     * @return 平均の企業価値
     */
    public Optional<BigDecimal> averageCorporateValue(final Company company) {
        final List<AnalysisResultEntity> targetList = analysisTargetList(company, targetTypeCodes);
        if (targetList.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(targetList.stream()
                    .map(AnalysisResultEntity::getCorporateValue)
                    // sum
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    // average
                    .divide(BigDecimal.valueOf(targetList.size()), SECOND_DECIMAL_PLACE, RoundingMode.HALF_UP));
        }
    }

    /**
     * 企業価値の標準偏差を取得する
     *
     * @param company               企業情報
     * @param averageCorporateValue 平均の企業価値
     * @return 標準偏差
     */
    public Optional<BigDecimal> standardDeviation(final Company company, final BigDecimal averageCorporateValue) {
        final List<AnalysisResultEntity> targetList = analysisTargetList(company, targetTypeCodes);
        if (Objects.isNull(averageCorporateValue) || targetList.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(targetList.stream()
                    .map(AnalysisResultEntity::getCorporateValue)
                    // (value - average) ^2
                    .map(value -> value.subtract(averageCorporateValue).pow(2))
                    // sum
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    // average
                    .divide(BigDecimal.valueOf(targetList.size()), THIRD_DECIMAL_PLACE, RoundingMode.HALF_UP)
                    // sqrt
                    .sqrt(new MathContext(5, RoundingMode.HALF_UP)));
        }
    }

    /**
     * 変動係数を取得する
     *
     * @param standardDeviation     標準偏差
     * @param averageCorporateValue 平均の企業価値
     * @return 変動係数
     */
    public Optional<BigDecimal> coefficientOfVariation(
            final BigDecimal standardDeviation, final BigDecimal averageCorporateValue) {
        if (Objects.isNull(standardDeviation) || Objects.isNull(averageCorporateValue)) {
            return Optional.empty();
        } else {
            return Optional.of(standardDeviation.divide(averageCorporateValue, THIRD_DECIMAL_PLACE, RoundingMode.HALF_UP));
        }
    }

    /**
     * 分析年数を取得する
     *
     * @param company 企業情報
     * @return 分析年数
     */
    public BigDecimal countYear(final Company company) {
        return BigDecimal.valueOf(analysisTargetList(company, targetTypeCodes).size());
    }

    /**
     * 分析処理対象となる企業価値リストを取得する
     *
     * @param company          企業情報
     * @param documentTypeCode 書類種別コード
     * @return 企業価値リスト
     */
    public List<AnalysisResultEntity> analysisTargetList(final Company company, final List<String> documentTypeCode) {
        final String code = company.getCode().orElseThrow(FundanalyzerNotExistException::new);
        return analysisResultDao.selectByCompanyCodeAndType(code, documentTypeCode).stream()
                .map(AnalysisResultEntity::getDocumentPeriod)
                // null のときはEPOCHとなるため、除外する
                .filter(period -> !LocalDate.EPOCH.isEqual(period))
                .distinct()
                .map(documentPeriod -> latestAnalysisResult(company.getCode().get(), documentPeriod))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    /**
     * 表示対象とする企業価値リストを取得する
     *
     * @param company          企業情報
     * @param documentTypeCode 書類種別コード
     * @return 企業価値リスト
     */
    public List<AnalysisResultEntity> displayTargetList(final Company company, final List<String> documentTypeCode) {
        final String code = company.getCode().orElseThrow(FundanalyzerNotExistException::new);
        return analysisResultDao.selectByCompanyCodeAndType(code, documentTypeCode);
    }

    /**
     * 分析済みかどうか
     *
     * @param document ドキュメント
     * @return boolean
     */
    public boolean isAnalyzed(final Document document) {
        final Optional<LocalDate> documentPeriod = document.getDocumentPeriod();
        if (documentPeriod.isEmpty()) {
            return false;
        }

        final Optional<String> companyCode = companySpecification.findCompanyByEdinetCode(document.getEdinetCode()).flatMap(Company::getCode);
        if (companyCode.isEmpty()) {
            return false;
        }

        return analysisResultDao.selectByUniqueKey(
                companyCode.get(),
                documentPeriod.get(),
                document.getDocumentTypeCode().toValue(),
                document.getSubmitDate()
        ).isPresent();
    }

    /**
     * 最新の企業価値を取得する
     *
     * @param code           企業コード
     * @param documentPeriod 期間
     * @return 最新の企業価値
     */
    private Optional<AnalysisResultEntity> latestAnalysisResult(final String code, final LocalDate documentPeriod) {
        return analysisResultDao.selectByCodeAndPeriod(code, documentPeriod).stream()
                .max(Comparator.comparing(AnalysisResultEntity::getSubmitDate));
    }
}
