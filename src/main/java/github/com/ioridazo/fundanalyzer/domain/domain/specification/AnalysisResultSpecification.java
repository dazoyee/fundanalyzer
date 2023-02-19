package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction.AnalysisResultDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
import github.com.ioridazo.fundanalyzer.domain.value.AnalysisResult;
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
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
     * 分析結果を取得する
     *
     * @param id ID
     * @return 分析結果
     */
    public Optional<AnalysisResultEntity> findAnalysisResult(final Integer id) {
        return analysisResultDao.selectById(id);
    }

    /**
     * 最新の分析結果を取得する
     *
     * @param companyCode 企業コード
     * @return 最新の分析結果
     */
    public Optional<AnalysisResultEntity> findLatestAnalysisResult(final String companyCode) {
        return analysisTargetList(companyCode, targetTypeCodes).stream()
                // latest
                .max(Comparator.comparing(AnalysisResultEntity::getDocumentPeriod)
                        .thenComparing(AnalysisResultEntity::getSubmitDate));
    }

    /**
     * 分析結果を取得する
     *
     * @param documentId ドキュメントID
     * @return 分析結果
     */
    public Optional<AnalysisResultEntity> findAnalysisResult(final String documentId) {
        return analysisResultDao.selectByDocumentId(documentId);
    }

    /**
     * 本日にテーブルに登録されたリストを取得する
     *
     * @param submitDate 提出日
     * @return 更新されたリスト
     */
    public List<AnalysisResultEntity> findUpdatedList(final LocalDate submitDate) {
        return analysisResultDao.selectBySubmitDateAndCreatedAt(submitDate, LocalDate.now());
    }

    /**
     * 企業価値を登録する
     *
     * @param document       ドキュメント
     * @param analysisResult 分析結果
     */
    public void insert(final Document document, final AnalysisResult analysisResult) {
        final Company company = companySpecification.findCompanyByEdinetCode(document.getEdinetCode()).orElseThrow(FundanalyzerNotExistException::new);
        try {
            analysisResultDao.insert(AnalysisResultEntity.of(
                    company.code(),
                    document.getDocumentPeriod().orElseThrow(() -> new FundanalyzerNotExistException("documentPeriod")),
                    analysisResult.getCorporateValue(),
                    analysisResult.getBps().orElse(null),
                    analysisResult.getEps().orElse(null),
                    analysisResult.getRoe().orElse(null),
                    analysisResult.getRoa().orElse(null),
                    document.getDocumentTypeCode(),
                    document.getQuarterType(),
                    document.getSubmitDate(),
                    document.getDocumentId(),
                    nowLocalDateTime()
            ));
        } catch (final NestedRuntimeException e) {
            if (e.contains(UniqueConstraintException.class)) {
                log.debug(FundanalyzerLogClient.toSpecificationLogObject(
                        MessageFormat.format(
                                "一意制約違反のため、データベースへの登録をスキップします。" +
                                        "\tテーブル名:{0}\t会社コード:{1}\t期間:{2}\t書類種別コード:{3}\t提出日:{4}",
                                "analysis_result",
                                company.code(),
                                document.getDocumentPeriod().map(LocalDate::toString).orElse("null"),
                                document.getDocumentTypeCode().toValue(),
                                document.getSubmitDate()
                        ),
                        document,
                        Category.ANALYSIS,
                        Process.ANALYSIS
                ));
            } else {
                throw new FundanalyzerRuntimeException("想定外のエラーが発生しました。", e);
            }
        }
    }

    /**
     * 指定年数平均の企業価値を取得する
     *
     * @param company 企業情報
     * @param year    指定年数
     * @return 平均の企業価値
     */
    public Optional<BigDecimal> yearAverageCorporateValue(final Company company, final Integer year) {
        final List<AnalysisResultEntity> targetList = analysisTargetList(company.code(), targetTypeCodes);
        if (targetList.isEmpty() || targetList.size() < year) {
            return Optional.empty();
        } else {

            return Optional.of(targetList.stream()
                    // sort
                    .sorted(Comparator.comparing(AnalysisResultEntity::getDocumentPeriod).reversed())
                    .toList()
                    // filter
                    .subList(0, year).stream()
                    .map(AnalysisResultEntity::getCorporateValue)
                    // sum
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    // average
                    .divide(BigDecimal.valueOf(year), SECOND_DECIMAL_PLACE, RoundingMode.HALF_UP));
        }
    }

    /**
     * 全年平均の企業価値を取得する
     *
     * @param company 企業情報
     * @return 平均の企業価値
     */
    public Optional<BigDecimal> allYearAverageCorporateValue(final Company company) {
        final List<AnalysisResultEntity> targetList = analysisTargetList(company.code(), targetTypeCodes);
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
        final List<AnalysisResultEntity> targetList = analysisTargetList(company.code(), targetTypeCodes);
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
        return BigDecimal.valueOf(analysisTargetList(company.code(), targetTypeCodes).size());
    }

    /**
     * 表示対象とする企業価値リストを取得する
     *
     * @param company          企業情報
     * @param documentTypeCode 書類種別コード
     * @return 企業価値リスト
     */
    public List<AnalysisResultEntity> displayTargetList(final Company company, final List<String> documentTypeCode) {
        return analysisResultDao.selectByCompanyCodeAndType(company.code(), documentTypeCode);
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

        final Optional<Company> company = companySpecification.findCompanyByEdinetCode(document.getEdinetCode());
        //noinspection OptionalIsPresent
        if (company.isEmpty()) {
            return false;
        }

        return analysisResultDao.selectByUniqueKey(
                company.get().code(),
                documentPeriod.get(),
                document.getDocumentTypeCode().toValue(),
                document.getSubmitDate()
        ).isPresent();
    }

    /**
     * 分析処理対象となる企業価値リストを取得する
     *
     * @param companyCode      企業コード
     * @param documentTypeCode 書類種別コード
     * @return 企業価値リスト
     */
    List<AnalysisResultEntity> analysisTargetList(final String companyCode, final List<String> documentTypeCode) {
        final List<AnalysisResultEntity> analysisTargetList = analysisResultDao.selectByCompanyCodeAndType(companyCode, documentTypeCode);

        return analysisTargetList.stream()
                .map(AnalysisResultEntity::getDocumentPeriod)
                // null のときはEPOCHとなるため、除外する
                .filter(period -> !LocalDate.EPOCH.isEqual(period))
                .distinct()
                // 最新の企業価値を取得する
                .map(dp -> analysisTargetList.stream()
                        .filter(e -> dp.equals(e.getDocumentPeriod()))
                        .max(Comparator.comparing(AnalysisResultEntity::getSubmitDate)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }
}
