package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.StockPriceEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.ValuationEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.AnalysisResultSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CompanySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.StockSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.ValuationSpecification;
import github.com.ioridazo.fundanalyzer.domain.usecase.ValuationUseCase;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Optional;

@Component
public class ValuationInteractor implements ValuationUseCase {

    private static final Logger log = LogManager.getLogger(ValuationInteractor.class);

    private final CompanySpecification companySpecification;
    private final AnalysisResultSpecification analysisResultSpecification;
    private final StockSpecification stockSpecification;
    private final ValuationSpecification valuationSpecification;

    public ValuationInteractor(
            final CompanySpecification companySpecification,
            final AnalysisResultSpecification analysisResultSpecification,
            final StockSpecification stockSpecification,
            final ValuationSpecification valuationSpecification) {
        this.companySpecification = companySpecification;
        this.analysisResultSpecification = analysisResultSpecification;
        this.stockSpecification = stockSpecification;
        this.valuationSpecification = valuationSpecification;
    }

    /**
     * 株価を評価する
     */
    @Override
    public int evaluate() {
        return (int) companySpecification.inquiryAllTargetCompanies().stream()
                .map(company -> CodeInputData.of(company.getCode()))
                .filter(this::evaluate)
                .count();
    }

    /**
     * 株価を評価する
     *
     * @param inputData 企業コード
     */
    @Override
    public boolean evaluate(final CodeInputData inputData) {
        final long startTime = System.currentTimeMillis();
        try {
            final String companyCode = inputData.getCode();
            final Optional<AnalysisResultEntity> latestAnalysisResult = analysisResultSpecification.findLatestAnalysisResult(companyCode);

            if (latestAnalysisResult.isPresent()) {
                final LocalDate targetDate =
                        valuationSpecification.findLatestValuation(companyCode, latestAnalysisResult.get().getSubmitDate())
                                // 最新の評価した対象日を取得
                                .map(ValuationEntity::getTargetDate)
                                .map(td -> LocalDate.of(
                                        td.getYear(),
                                        td.getMonth(),
                                        latestAnalysisResult.get().getSubmitDate().getDayOfMonth()
                                ))
                                // 過去の評価から1ヶ月後を対象日付とする
                                .map(td -> td.plusMonths(1))
                                // はじめての評価ならば提出日を対象日付とする
                                .orElseGet(() -> latestAnalysisResult.get().getSubmitDate());

                final Optional<StockPriceEntity> targetStock = findPresentStock(companyCode, targetDate);

                if (targetStock.isPresent()) {
                    valuationSpecification.insert(targetStock.get(), latestAnalysisResult.get());

                    log.info(FundanalyzerLogClient.toInteractorLogObject(
                            MessageFormat.format("株価を評価しました。\t企業コード:{0}", inputData.getCode()),
                            Category.STOCK,
                            Process.EVALUATE,
                            System.currentTimeMillis() - startTime
                    ));

                    return true;
                }
            }

            log.info(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format("評価対象の株価が存在しませんでした。\t企業コード:{0}", inputData.getCode()),
                    Category.STOCK,
                    Process.EVALUATE,
                    System.currentTimeMillis() - startTime
            ));

        } catch (final FundanalyzerNotExistException e) {
            log.info(FundanalyzerLogClient.toInteractorLogObject(
                    "評価できませんでした。",
                    Category.STOCK,
                    Process.EVALUATE
            ), e);
        }
        return false;
    }

    /**
     * 対象日付に近い株価を取得する
     *
     * @param companyCode 企業コード
     * @param targetDate  対象日付
     * @return 株価情報
     */
    Optional<StockPriceEntity> findPresentStock(final String companyCode, final LocalDate targetDate) {
        int i = 0;
        while (i < 5) {
            final Optional<StockPriceEntity> stock = stockSpecification.findStock(companyCode, targetDate.plusDays(i));
            if (stock.isPresent()) {
                return stock;
            }
            i++;
        }
        return Optional.empty();
    }
}
