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
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.ValuationCatchUpPreview;
import github.com.ioridazo.fundanalyzer.domain.value.ValuationCatchUpResult;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class ValuationInteractor implements ValuationUseCase {

    private static final Logger log = LogManager.getLogger(ValuationInteractor.class);

    private final CompanySpecification companySpecification;
    private final AnalysisResultSpecification analysisResultSpecification;
    private final StockSpecification stockSpecification;
    private final ValuationSpecification valuationSpecification;
    @Value("${app.config.valuation.forward-snap-limit-days:45}")
    int forwardSnapLimitDays = 45;
    @Value("${app.config.valuation.catch-up-max-iterations:60}")
    int catchUpMaxIterations = 60;

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
                .map(company -> CodeInputData.of(company.code()))
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
            final Optional<EvaluationTarget> evaluationTarget = resolveEvaluationTarget(inputData.getCode());

            if (evaluationTarget.isPresent() && evaluationTarget.get().stock().isPresent()) {
                valuationSpecification.insert(evaluationTarget.get().stock().get(), evaluationTarget.get().analysisResult());

                log.trace(FundanalyzerLogClient.toInteractorLogObject(
                        MessageFormat.format("株価を評価しました。\t企業コード:{0}", inputData.getCode()),
                        companySpecification.findCompanyByCode(inputData.getCode()).map(Company::edinetCode).orElse("null"),
                        Category.STOCK,
                        Process.EVALUATE,
                        System.currentTimeMillis() - startTime
                ));

                return true;
            }

            log.trace(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format("評価対象の株価が存在しませんでした。\t企業コード:{0}", inputData.getCode()),
                    companySpecification.findCompanyByCode(inputData.getCode()).map(Company::edinetCode).orElse("null"),
                    Category.STOCK,
                    Process.EVALUATE,
                    System.currentTimeMillis() - startTime
            ));

        } catch (final FundanalyzerNotExistException e) {
            log.info(FundanalyzerLogClient.toInteractorLogObject(
                    "情報が不足していたため、評価できませんでした。",
                    companySpecification.findCompanyByCode(inputData.getCode()).map(Company::edinetCode).orElse("null"),
                    Category.STOCK,
                    Process.EVALUATE
            ), e);
        } catch (final DateTimeException e) {
            log.warn(FundanalyzerLogClient.toInteractorLogObject(
                    "対象日時の生成に失敗したため、評価できませんでした。",
                    companySpecification.findCompanyByCode(inputData.getCode()).map(Company::edinetCode).orElse("null"),
                    Category.STOCK,
                    Process.EVALUATE
            ), e);
        }
        return false;
    }

    @Override
    public ValuationCatchUpPreview previewCatchUp() {
        final int targetCompanyCount = (int) companySpecification.inquiryAllTargetCompanies().stream()
                .map(Company::code)
                .map(this::resolveEvaluationTarget)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(this::isCatchUpTarget)
                .count();
        return new ValuationCatchUpPreview(targetCompanyCount);
    }

    @Override
    public ValuationCatchUpResult catchUp() {
        final List<EvaluationTarget> targetCompanies = companySpecification.inquiryAllTargetCompanies().stream()
                .map(Company::code)
                .map(this::resolveEvaluationTarget)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(this::isCatchUpTarget)
                .toList();
        int advancedCount = 0;
        int unresolvedCompanyCount = 0;

        for (final EvaluationTarget target : targetCompanies) {
            int count = 0;
            while (count < catchUpMaxIterations && evaluate(CodeInputData.of(target.companyCode()))) {
                advancedCount++;
                count++;
            }
            if (isUnresolved(target.companyCode())) {
                unresolvedCompanyCount++;
            }
        }
        return new ValuationCatchUpResult(targetCompanies.size(), advancedCount, unresolvedCompanyCount);
    }

    /**
     * 株価取得日に近似した評価日付を生成する
     *
     * @param targetDate 評価日付
     * @param submitDate 提出日
     * @return 調整後評価日付
     */
    LocalDate generateValuationDate(final LocalDate targetDate, final LocalDate submitDate) {
        // 提出日が 28, 29, 30, 31 の場合は注意
        if (Stream.of(28, 29, 30, 31).anyMatch(day -> submitDate.getDayOfMonth() == day)) {
            // 2月を考慮
            if (Month.FEBRUARY.equals(targetDate.getMonth())) {
                return LocalDate.of(
                        targetDate.getYear(),
                        targetDate.getMonth(),
                        28
                );
            }

            // 4月, 6月, 9月, 11月 を考慮
            if (31 == submitDate.getDayOfMonth()
                && Stream.of(Month.APRIL, Month.JUNE, Month.SEPTEMBER, Month.NOVEMBER)
                        .anyMatch(month -> month.equals(targetDate.getMonth()))) {
                return LocalDate.of(
                        targetDate.getYear(),
                        targetDate.getMonth(),
                        30
                );
            }
        }

        // デフォルト
        return LocalDate.of(
                targetDate.getYear(),
                targetDate.getMonth(),
                submitDate.getDayOfMonth()
        );
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
        // 月を跨がないように株価を取得することを理想としている
        while (i < 5) {
            if (Stream.of(27, 28, 29, 30, 31).anyMatch(day -> targetDate.getDayOfMonth() == day)) {
                final Optional<StockPriceEntity> stock = stockSpecification.findStock(companyCode, targetDate.minusDays(i));
                if (stock.map(StockPriceEntity::getStockPrice).isPresent()) {
                    return stock;
                }
            } else {
                final Optional<StockPriceEntity> stock = stockSpecification.findStock(companyCode, targetDate.plusDays(i));
                if (stock.map(StockPriceEntity::getStockPrice).isPresent()) {
                    return stock;
                }
            }
            i++;
        }
        return stockSpecification.findFirstStockFromDate(
                companyCode,
                targetDate,
                targetDate.plusDays(forwardSnapLimitDays));
    }

    private Optional<EvaluationTarget> resolveEvaluationTarget(final String companyCode) {
        return analysisResultSpecification.findLatestAnalysisResult(companyCode)
                .map(analysisResult -> {
                    final LocalDate targetDate = nextTargetDate(companyCode, analysisResult);
                    return new EvaluationTarget(
                            companyCode,
                            analysisResult,
                            targetDate,
                            findPresentStock(companyCode, targetDate)
                    );
                });
    }

    private LocalDate nextTargetDate(final String companyCode, final AnalysisResultEntity latestAnalysisResult) {
        return valuationSpecification.findLatestValuation(companyCode, latestAnalysisResult.getSubmitDate())
                .map(ValuationEntity::getTargetDate)
                .map(td -> generateValuationDate(td, latestAnalysisResult.getSubmitDate()))
                .map(td -> td.plusMonths(1))
                .orElseGet(latestAnalysisResult::getSubmitDate);
    }

    private boolean isCatchUpTarget(final EvaluationTarget target) {
        return latestStockDate(target.companyCode())
                .map(latestStockDate -> !latestStockDate.isBefore(target.targetDate()))
                .orElse(false);
    }

    private boolean isUnresolved(final String companyCode) {
        return resolveEvaluationTarget(companyCode)
                .filter(this::isCatchUpTarget)
                .filter(target -> target.stock().isEmpty())
                .isPresent();
    }

    private Optional<LocalDate> latestStockDate(final String companyCode) {
        return stockSpecification.findLatestStock(companyCode)
                .map(StockPriceEntity::getTargetDate);
    }

    private record EvaluationTarget(
            String companyCode,
            AnalysisResultEntity analysisResult,
            LocalDate targetDate,
            Optional<StockPriceEntity> stock) {
    }
}
