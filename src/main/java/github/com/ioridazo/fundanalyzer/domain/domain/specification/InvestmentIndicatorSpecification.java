package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction.InvestmentIndicatorDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.InvestmentIndicatorEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.StockPriceEntity;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.IndicatorValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seasar.doma.jdbc.UniqueConstraintException;
import org.springframework.core.NestedRuntimeException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.SQLIntegrityConstraintViolationException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class InvestmentIndicatorSpecification {

    private static final Logger log = LogManager.getLogger(InvestmentIndicatorSpecification.class);

    private final InvestmentIndicatorDao investmentIndicatorDao;
    private final CompanySpecification companySpecification;

    public InvestmentIndicatorSpecification(
            final InvestmentIndicatorDao investmentIndicatorDao,
            final CompanySpecification companySpecification) {
        this.investmentIndicatorDao = investmentIndicatorDao;
        this.companySpecification = companySpecification;
    }

    LocalDateTime nowLocalDateTime() {
        return LocalDateTime.now();
    }

    /**
     * 最新の投資指標を取得する
     *
     * @param companyCode 企業コード
     * @return 投資指標
     */
    public Optional<IndicatorValue> findIndicatorValue(final String companyCode) {
        return investmentIndicatorDao.selectByCode(companyCode).stream()
                // latest
                .max(Comparator.comparing(InvestmentIndicatorEntity::getTargetDate))
                .map(IndicatorValue::of);
    }

    /**
     * 投資指標を取得する
     *
     * @param companyCode 企業コード
     * @param targetDate  対象日
     * @return 投資指標エンティティ
     */
    public Optional<InvestmentIndicatorEntity> findEntity(final String companyCode, final LocalDate targetDate) {
        return investmentIndicatorDao.selectByCodeAndTargetDate(companyCode, targetDate);
    }

    /**
     * 投資指標を取得する
     *
     * @param companyCode 企業コード
     * @return 投資指標
     */
    public List<IndicatorValue> findIndicatorValueList(final String companyCode) {
        return investmentIndicatorDao.selectByCode(companyCode).stream()
                .map(IndicatorValue::of)
                .toList();
    }

    /**
     * 分析結果に紐づく投資指標を取得する
     *
     * @param analysisResultId 分析結果ID
     * @return 投資指標
     */
    public List<IndicatorValue> findIndicatorValueList(final Integer analysisResultId) {
        return investmentIndicatorDao.selectByAnalysisResultId(analysisResultId).stream()
                .map(IndicatorValue::of)
                .toList();
    }

    /**
     * 投資指標を登録する
     *
     * @param analysisResultEntity 企業価値エンティティ
     * @param stockPriceEntity     株価エンティティ
     */
    public void insert(
            final AnalysisResultEntity analysisResultEntity, final StockPriceEntity stockPriceEntity) {
        final IndicatorValue indicatorValue = new IndicatorValue(BigDecimal.valueOf(stockPriceEntity.getStockPrice()), analysisResultEntity);
        try {
            investmentIndicatorDao.insert(InvestmentIndicatorEntity.of(
                    stockPriceEntity.getId(),
                    analysisResultEntity.getId(),
                    analysisResultEntity.getCompanyCode(),
                    stockPriceEntity.getTargetDate(),
                    indicatorValue.getPriceCorporateValueRatio(),
                    indicatorValue.getPer().orElse(null),
                    indicatorValue.getPbr().orElse(null),
                    indicatorValue.getGrahamIndex().orElse(null),
                    analysisResultEntity.getDocumentId(),
                    nowLocalDateTime()
            ));
        } catch (final NestedRuntimeException e) {
            if (e.contains(UniqueConstraintException.class)) {
                log.debug(FundanalyzerLogClient.toSpecificationLogObject(
                        MessageFormat.format(
                                "一意制約違反のため、データベースへの登録をスキップします。" +
                                "\tテーブル名:{0}\t企業コード:{1}\t対象日付:{2}",
                                "investment_indicator",
                                analysisResultEntity.getCompanyCode(),
                                analysisResultEntity.getSubmitDate()
                        ),
                        companySpecification.findCompanyByCode(analysisResultEntity.getCompanyCode()).map(Company::edinetCode).orElse("null"),
                        Category.ANALYSIS,
                        Process.ANALYSIS
                ), e);
            } else if (e.contains(SQLIntegrityConstraintViolationException.class)) {
                log.debug(FundanalyzerLogClient.toSpecificationLogObject(
                        MessageFormat.format(
                                "整合性制約 (外部キー、主キー、または一意キー) 違反のため、データベースへの登録をスキップします。" +
                                "\tテーブル名:{0}\t企業コード:{1}\t対象日付:{2}",
                                "investment_indicator",
                                analysisResultEntity.getCompanyCode(),
                                analysisResultEntity.getSubmitDate()
                        ),
                        companySpecification.findCompanyByCode(analysisResultEntity.getCompanyCode()).map(Company::edinetCode).orElse("null"),
                        Category.ANALYSIS,
                        Process.ANALYSIS
                ), e);
            } else {
                throw e;
            }
        }
    }
}
