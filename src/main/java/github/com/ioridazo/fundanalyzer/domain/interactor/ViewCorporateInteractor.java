package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.client.slack.SlackClient;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.DocumentTypeCode;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.FinancialStatementEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.AnalysisResultSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CompanySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.DocumentSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.FinancialStatementSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.InvestmentIndicatorSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.StockSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.ViewSpecification;
import github.com.ioridazo.fundanalyzer.domain.usecase.ViewCorporateUseCase;
import github.com.ioridazo.fundanalyzer.domain.value.AnalysisResult;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.domain.value.IndicatorValue;
import github.com.ioridazo.fundanalyzer.domain.value.Stock;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.presenter.Target;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.CorporateViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.AnalysisResultViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.CompanyViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.CorporateDetailViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.FinancialStatementKeyViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.FinancialStatementViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.IndicatorViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.MinkabuViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.StockPriceViewModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class ViewCorporateInteractor implements ViewCorporateUseCase {

    private static final Logger log = LogManager.getLogger(ViewCorporateInteractor.class);

    private final AnalyzeInteractor analyzeInteractor;
    private final CompanySpecification companySpecification;
    private final DocumentSpecification documentSpecification;
    private final FinancialStatementSpecification financialStatementSpecification;
    private final AnalysisResultSpecification analysisResultSpecification;
    private final StockSpecification stockSpecification;
    private final InvestmentIndicatorSpecification investmentIndicatorSpecification;
    private final ViewSpecification viewSpecification;
    private final SlackClient slackClient;

    @Value("${app.config.view.discount-rate}")
    BigDecimal configDiscountRate;
    @Value("${app.config.view.outlier-of-standard-deviation}")
    BigDecimal configOutlierOfStandardDeviation;
    @Value("${app.config.view.coefficient-of-variation}")
    BigDecimal configCoefficientOfVariation;
    @Value("${app.config.view.diff-forecast-stock}")
    BigDecimal configDiffForecastStock;
    @Value("${app.config.view.corporate.size}")
    int configCorporateSize;
    @Value("${app.config.scraping.document-type-code}")
    List<String> targetTypeCodes;
    @Value("${app.slack.update-view.enabled:true}")
    boolean updateViewEnabled;

    public ViewCorporateInteractor(
            final AnalyzeInteractor analyzeInteractor,
            final CompanySpecification companySpecification,
            final DocumentSpecification documentSpecification,
            final FinancialStatementSpecification financialStatementSpecification,
            final AnalysisResultSpecification analysisResultSpecification,
            final StockSpecification stockSpecification,
            final InvestmentIndicatorSpecification investmentIndicatorSpecification,
            final ViewSpecification viewSpecification,
            final SlackClient slackClient) {
        this.analyzeInteractor = analyzeInteractor;
        this.companySpecification = companySpecification;
        this.documentSpecification = documentSpecification;
        this.financialStatementSpecification = financialStatementSpecification;
        this.analysisResultSpecification = analysisResultSpecification;
        this.stockSpecification = stockSpecification;
        this.investmentIndicatorSpecification = investmentIndicatorSpecification;
        this.viewSpecification = viewSpecification;
        this.slackClient = slackClient;
    }

    LocalDate nowLocalDate() {
        return LocalDate.now();
    }

    /**
     * メインビューを取得する
     *
     * @return 企業情報ビュー
     */
    @Override
    public List<CorporateViewModel> viewMain() {
        return filter(viewSpecification.findAllCorporateView()).stream()
                .filter(cvm -> Stream.of(
                                DocumentTypeCode.DTC_120,
                                DocumentTypeCode.DTC_130
                        )
                        .anyMatch(dtc -> DocumentTypeCode.fromValue(cvm.getLatestDocumentTypeCode()).equals(dtc)))
                .sorted(Comparator
                        .comparing(CorporateViewModel::getSubmitDate).reversed()
                        .thenComparing(CorporateViewModel::getCode))
                .toList();
    }

    @Override
    public List<CorporateViewModel> viewQuart() {
        return filter(viewSpecification.findAllCorporateView()).stream()
                .filter(cvm -> Stream.of(
                                DocumentTypeCode.DTC_140,
                                DocumentTypeCode.DTC_150
                        )
                        .anyMatch(dtc -> DocumentTypeCode.fromValue(cvm.getLatestDocumentTypeCode()).equals(dtc)))
                .sorted(Comparator
                        .comparing(CorporateViewModel::getSubmitDate).reversed()
                        .thenComparing(CorporateViewModel::getCode))
                .toList();
    }

    /**
     * オールビューを取得する
     *
     * @return 企業情報ビュー
     */
    @Override
    public List<CorporateViewModel> viewAll() {
        return viewSpecification.findAllCorporateView().stream()
                .sorted(Comparator
                        .comparing(CorporateViewModel::getSubmitDate).reversed()
                        .thenComparing(CorporateViewModel::getCode))
                .toList();
    }

    /**
     * お気に入りを取得する
     *
     * @return 企業情報ビュー
     */
    @Override
    public List<CorporateViewModel> viewFavorite() {
        final List<String> favoriteList = companySpecification.findFavoriteCompanies().stream()
                .map(Company::getCode)
                .toList();
        final List<CorporateViewModel> allCorporateView = viewSpecification.findAllCorporateView();
        return allCorporateView.stream()
                .map(CorporateViewModel::getCode)
                .distinct()
                .map(code -> allCorporateView.stream()
                        .filter(cvm -> code.equals(cvm.getCode()))
                        .max(Comparator.comparing(CorporateViewModel::getSubmitDate))
                        .orElseThrow()
                )
                .filter(cvm -> favoriteList.stream().anyMatch(favorite -> cvm.getCode().equals(favorite.substring(0, 4))))
                .sorted(Comparator
                        .comparing(CorporateViewModel::getSubmitDate).reversed()
                        .thenComparing(CorporateViewModel::getCode))
                .toList();
    }

    /**
     * 企業情報詳細ビューを取得する
     *
     * @param inputData 企業コード
     * @return 企業情報詳細ビュー
     */
    @Override
    public CorporateDetailViewModel viewCorporateDetail(final CodeInputData inputData) {
        final Company company = companySpecification.findCompanyByCode(inputData.getCode5())
                .orElseThrow(() -> {
                    throw new FundanalyzerNotExistException("企業コード");
                });
        final Stock stock = stockSpecification.findStock(company);

        final List<AnalysisResultViewModel> analysisResultList = analysisResultSpecification.displayTargetList(company, targetTypeCodes).stream()
                .map(AnalysisResultViewModel::of)
                .sorted(Comparator.comparing(AnalysisResultViewModel::getDocumentPeriod)
                        .thenComparing(AnalysisResultViewModel::getSubmitDate)
                        .reversed())
                .toList();

        final List<IndicatorViewModel> indicatorList = investmentIndicatorSpecification.findIndicatorValueList(company.getCode()).stream()
                .filter(indicatorValue -> indicatorValue.getGrahamIndex().isPresent())
                .map(IndicatorViewModel::of)
                .sorted(Comparator.comparing(IndicatorViewModel::targetDate).reversed())
                .toList();

        final List<FinancialStatementViewModel> fsList = financialStatementSpecification.findByCompany(company).stream()
                .map(FinancialStatementKeyViewModel::of)
                .distinct()
                .map(key -> {
                    final List<FinancialStatementEntity> valueList = financialStatementSpecification.findByKeyPerCompany(company, key);
                    return FinancialStatementViewModel.of(
                            key.getSubmitDate(),
                            key,
                            financialStatementSpecification.parseBsSubjectValue(valueList),
                            financialStatementSpecification.parsePlSubjectValue(valueList)
                    );
                })
                .sorted(Comparator.comparing(FinancialStatementViewModel::getSubmitDate).reversed())
                .toList();

        return CorporateDetailViewModel.of(
                CompanyViewModel.of(company, stock),
                null,
                null,
                viewSpecification.findLatestCorporateView(inputData),
                analysisResultList,
                indicatorList,
                fsList,
                stock.getMinkabuEntityList().stream()
                        .filter(minkabuEntity -> minkabuEntity.getGoalsStock().isPresent())
                        .map(MinkabuViewModel::of)
                        .sorted(Comparator.comparing(MinkabuViewModel::getTargetDate).reversed())
                        .toList(),
                stock.getStockPriceEntityList().stream()
                        .map(StockPriceViewModel::of)
                        .sorted(Comparator.comparing(StockPriceViewModel::getTargetDate).reversed())
                        .toList()
        );
    }

    @Override
    public CorporateDetailViewModel viewCorporateDetail(final CodeInputData inputData, final Target target) {
        final CorporateDetailViewModel viewModel = viewCorporateDetail(inputData);
        final List<String> codeList = switch (target) {
            case MAIN -> viewMain().stream().map(CorporateViewModel::getCode).toList();
            case QUART -> viewQuart().stream().map(CorporateViewModel::getCode).toList();
            case ALL -> viewAll().stream().map(CorporateViewModel::getCode).toList();
            default -> List.of();
        };

        if (codeList.isEmpty()) {
            return viewModel;
        } else {
            final int index = codeList.indexOf(inputData.getCode());
            String backwardCode = null;
            String forwardCode = null;
            if (index != 0) {
                backwardCode = codeList.get(index - 1);
            }
            if (index + 1 != codeList.size()) {
                forwardCode = codeList.get(index + 1);
            }

            return CorporateDetailViewModel.of(viewModel, backwardCode, forwardCode);
        }
    }

    /**
     * すべてのビューを更新する
     */
    @Override
    public void updateView() {
        final long startTime = System.currentTimeMillis();
        parallelUpdateView(companySpecification.inquiryAllTargetCompanies());

        if (updateViewEnabled) {
            slackClient.sendMessage("g.c.i.f.domain.service.ViewService.display.update.complete.corporate");
        }

        log.info(FundanalyzerLogClient.toInteractorLogObject(
                "表示アップデートが正常に終了しました。",
                Category.VIEW,
                Process.UPDATE,
                System.currentTimeMillis() - startTime
        ));
    }

    /**
     * ビューを更新する
     *
     * @param inputData 企業コード
     */
    @Override
    public void updateView(final DateInputData inputData) {
        final long startTime = System.currentTimeMillis();

        try {
            parallelUpdateView(
                    documentSpecification.inquiryTargetDocuments(inputData).stream()
                            .map(Document::getEdinetCode)
                            .map(companySpecification::findCompanyByEdinetCode)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .toList()
            );

            log.info(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format("表示アップデートが正常に終了しました。対象提出日:{0}", inputData.getDate()),
                    Category.VIEW,
                    Process.UPDATE,
                    System.currentTimeMillis() - startTime
            ));
        } catch (final Exception e) {
            log.error(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format(
                            "{0}付のビューに対して想定外のエラーが発生しました。",
                            inputData.getDate()
                    ),
                    Category.VIEW,
                    Process.UPDATE
            ), e);
        }
    }

    List<CorporateViewModel> filter(final List<CorporateViewModel> list) {
        return list.stream()
                // not null
                .filter(cvm -> Objects.nonNull(cvm.getAllDiscountRate()))
                .filter(cvm -> Objects.nonNull(cvm.getAllStandardDeviation()))
                .filter(cvm -> Objects.nonNull(cvm.getLatestCorporateValue()))
                // 表示する提出日は一定期間のみ
                .filter(cvm -> cvm.getSubmitDate().isAfter(nowLocalDate().minusDays(configCorporateSize)))
                // 割安度が170%(外部設定値)以上を表示
                .filter(cvm -> cvm.getAllDiscountRate().compareTo(configDiscountRate) >= 0)
                // 標準偏差が外れ値となっていたら除外
                .filter(cvm -> cvm.getAllStandardDeviation().compareTo(configOutlierOfStandardDeviation) < 0)
                // 最新企業価値がマイナスの場合は除外
                .filter(cvm -> cvm.getLatestCorporateValue().compareTo(BigDecimal.ZERO) > 0)
                // 最新企業価値が平均より低い場合は除外
                .filter(cvm -> cvm.getLatestCorporateValue().compareTo(cvm.getAllAverageCorporateValue()) > 0)
                // 変動係数
                .filter(cvm -> {
                    if (Objects.isNull(cvm.getAllCoefficientOfVariation())) {
                        // 変動係数が存在しない
                        return true;
                    } else {
                        // 変動係数が0.6未満であること
                        if (cvm.getAllCoefficientOfVariation().compareTo(configCoefficientOfVariation) < 1) {
                            return true;
                        } else {
                            // 変動係数が0.6以上でも最新企業価値が高ければOK
                            return cvm.getLatestCorporateValue().compareTo(cvm.getAllAverageCorporateValue()) > -1;
                        }
                    }
                })
                // 予想株価
                .filter(cvb -> {
                    if (Objects.nonNull(cvb.getForecastStock())) {
                        // 株価予想が存在する場合、最新株価より高ければOK
                        return (cvb.getForecastStock().divide(cvb.getLatestStockPrice(), 3, RoundingMode.HALF_UP).compareTo(BigDecimal.valueOf(1.1)) > 0)
                                && (cvb.getForecastStock().subtract(cvb.getLatestStockPrice()).compareTo(configDiffForecastStock) >= 0);
                    } else {
                        return true;
                    }
                })
                .toList();
    }

    private void parallelUpdateView(final List<Company> companyList) {
        final ArrayList<CorporateViewModel> viewList = new ArrayList<>();
        companyList.forEach(company -> {
            try {
                final Optional<Document> latestDocument = documentSpecification.findLatestDocument(company);

                latestDocument.ifPresent(document -> viewList.add(viewSpecification.generateCorporateView(
                        company,
                        document,
                        analysisResultSpecification.findLatestAnalysisResult(company.getCode()).map(AnalysisResult::of).orElse(AnalysisResult.of()),
                        analyzeInteractor.calculateCorporateValue(company),
                        investmentIndicatorSpecification.findIndicatorValue(company.getCode()).orElse(IndicatorValue.of())
                )));
            } catch (final FundanalyzerNotExistException e) {
                log.warn(FundanalyzerLogClient.toInteractorLogObject(
                        MessageFormat.format(
                                "条件を満たさないため、次の企業のビューを更新しませんでした。\t企業コード:{0}",
                                company.getCode()
                        ),
                        companySpecification.findCompanyByCode(company.getCode()).map(Company::getEdinetCode).orElse("null"),
                        Category.VIEW,
                        Process.UPDATE
                ), e);
            }
        });

        if (viewList.size() > 10) {
            viewList.parallelStream().forEach(viewSpecification::upsert);
        } else {
            viewList.forEach(viewSpecification::upsert);
        }
    }
}
