package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction.ValuationDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.StockPriceEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.ValuationEntity;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.ValuationViewModel;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class ValuationSpecification {

    private static final int SECOND_DECIMAL_PLACE = 2;

    private final ValuationDao valuationDao;
    private final CompanySpecification companySpecification;
    private final StockSpecification stockSpecification;

    public ValuationSpecification(
            final ValuationDao valuationDao,
            final CompanySpecification companySpecification,
            final StockSpecification stockSpecification) {
        this.valuationDao = valuationDao;
        this.companySpecification = companySpecification;
        this.stockSpecification = stockSpecification;
    }

    LocalDateTime nowLocalDateTime() {
        return LocalDateTime.now();
    }

    /**
     * 最新の評価結果を取得する
     *
     * @param companyCode 企業コード
     * @param submitDate  提出日
     * @return 最新の評価結果
     */
    public Optional<ValuationEntity> findLatestValuation(final String companyCode, final LocalDate submitDate) {
        return valuationDao.selectByCodeAndSubmitDate(companyCode, submitDate).stream()
                .max(Comparator.comparing(ValuationEntity::getTargetDate));
    }

    /**
     * 評価結果を取得する
     *
     * @return 評価結果リスト
     */
    public List<ValuationViewModel> findAllValuationView() {
        final ArrayList<ValuationViewModel> viewList = new ArrayList<>();
        companySpecification.allTargetCompanies().forEach(company -> {
            final String code = company.getCode().orElseThrow(() -> new FundanalyzerNotExistException("企業コード"));
            valuationDao.selectByCode(code).stream()
                    .max(Comparator.comparing(ValuationEntity::getTargetDate))
                    .ifPresent(valuationEntity -> viewList.add(ValuationViewModel.of(valuationEntity, company)));
        });
        return viewList;
    }

    /**
     * 評価結果を登録する
     *
     * @param stock          株価
     * @param analysisResult 分析結果
     */
    public void insert(final StockPriceEntity stock, final AnalysisResultEntity analysisResult) {
        valuationDao.insert(evaluate(stock, analysisResult));
    }

    /**
     * 株価を評価する
     *
     * @param stock          株価
     * @param analysisResult 分析結果
     * @return 評価結果
     */
    ValuationEntity evaluate(final StockPriceEntity stock, final AnalysisResultEntity analysisResult) {
        final String code = stock.getCompanyCode();
        final LocalDate targetDate = stock.getTargetDate();
        final BigDecimal stockPrice = BigDecimal.valueOf(stock.getStockPrice());
        final BigDecimal averageStockPrice = stockSpecification.getAverageStockPriceOfLatestSubmitDate(code)
                .orElseThrow(() -> new FundanalyzerNotExistException("提出日株価平均"));

        final LocalDate submitDate = analysisResult.getSubmitDate();
        final BigDecimal corporateValue = analysisResult.getCorporateValue();

        return ValuationEntity.of(
                code,
                targetDate,
                stockPrice,
                stockSpecification.findForecastStock(code, targetDate).map(BigDecimal::valueOf).orElse(null),
                ChronoUnit.DAYS.between(submitDate, targetDate),
                stockPrice.subtract(averageStockPrice),
                stockPrice.divide(averageStockPrice, SECOND_DECIMAL_PLACE, RoundingMode.HALF_UP),
                corporateValue.subtract(stockPrice),
                corporateValue.divide(stockPrice, SECOND_DECIMAL_PLACE, RoundingMode.HALF_UP),
                submitDate,
                corporateValue,
                averageStockPrice,
                analysisResult.getDocumentId(),
                nowLocalDateTime()
        );
    }
}
