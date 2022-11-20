package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction.InvestmentIndicatorDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.InvestmentIndicatorEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.StockPriceEntity;
import github.com.ioridazo.fundanalyzer.domain.value.IndicatorValue;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seasar.doma.jdbc.UniqueConstraintException;
import org.springframework.core.NestedRuntimeException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.SQLIntegrityConstraintViolationException;
import java.text.MessageFormat;
import java.time.LocalDateTime;

@Component
public class InvestmentIndicatorSpecification {

    private static final Logger log = LogManager.getLogger(InvestmentIndicatorSpecification.class);

    private final InvestmentIndicatorDao investmentIndicatorDao;

    public InvestmentIndicatorSpecification(
            final InvestmentIndicatorDao investmentIndicatorDao) {
        this.investmentIndicatorDao = investmentIndicatorDao;
    }

    LocalDateTime nowLocalDateTime() {
        return LocalDateTime.now();
    }

    /**
     * 投資指標を登録する
     *
     * @param analysisResultEntity 企業価値エンティティ
     * @param stockPriceEntity     株価エンティティ
     */
    public void insert(
            final AnalysisResultEntity analysisResultEntity, final StockPriceEntity stockPriceEntity) {
        final BigDecimal stockPrice = stockPriceEntity.getStockPrice().map(BigDecimal::new).orElseThrow(FundanalyzerRuntimeException::new);
        final IndicatorValue indicatorValue = new IndicatorValue(stockPrice, analysisResultEntity);
        try {
            investmentIndicatorDao.insert(InvestmentIndicatorEntity.of(
                    stockPriceEntity.getId(),
                    analysisResultEntity.id(),
                    analysisResultEntity.companyCode(),
                    analysisResultEntity.submitDate(),
                    indicatorValue.getPriceCorporateValueRatio(),
                    indicatorValue.getPer(),
                    indicatorValue.getPbr(),
                    analysisResultEntity.documentId(),
                    nowLocalDateTime()
            ));
        } catch (final NestedRuntimeException e) {
            if (e.contains(UniqueConstraintException.class)) {
                log.warn(FundanalyzerLogClient.toSpecificationLogObject(
                        MessageFormat.format(
                                "一意制約違反のため、データベースへの登録をスキップします。" +
                                        "\t企業コード:{0}\t対象日付:{1}\t株価:{2}",
                                analysisResultEntity.companyCode(),
                                analysisResultEntity.submitDate(),
                                stockPrice
                        ),
                        Category.ANALYSIS,
                        Process.ANALYSIS
                ), e);
            } else if (e.contains(SQLIntegrityConstraintViolationException.class)) {
                log.warn(FundanalyzerLogClient.toSpecificationLogObject(
                        MessageFormat.format(
                                "整合性制約 (外部キー、主キー、または一意キー) 違反のため、データベースへの登録をスキップします。" +
                                        "\t企業コード:{0}\t対象日付:{1}\t株価:{2}",
                                analysisResultEntity.companyCode(),
                                analysisResultEntity.submitDate(),
                                stockPrice
                        ),
                        Category.ANALYSIS,
                        Process.ANALYSIS
                ), e);
            } else {
                throw e;
            }
        }
    }
}