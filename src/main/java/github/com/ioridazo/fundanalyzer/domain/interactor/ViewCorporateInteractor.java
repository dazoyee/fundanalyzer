package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.client.slack.SlackClient;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.DocumentTypeCode;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.FinancialStatementEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.AnalysisResultSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CompanySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CorporateActionSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CorporateActionSpecification.CorporateAction;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.DocumentSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.FinancialStatementSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.InvestmentIndicatorSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.StockSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.ViewSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.StockPriceEntity;
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
    private final CorporateActionSpecification corporateActionSpecification;

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
            final SlackClient slackClient,
            final CorporateActionSpecification corporateActionSpecification) {
        this.analyzeInteractor = analyzeInteractor;
        this.companySpecification = companySpecification;
        this.documentSpecification = documentSpecification;
        this.financialStatementSpecification = financialStatementSpecification;
        this.analysisResultSpecification = analysisResultSpecification;
        this.stockSpecification = stockSpecification;
        this.investmentIndicatorSpecification = investmentIndicatorSpecification;
        this.viewSpecification = viewSpecification;
        this.slackClient = slackClient;
        this.corporateActionSpecification = corporateActionSpecification;
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
                        .comparing(CorporateViewModel::getSubmitDate)
                        .thenComparing(CorporateViewModel::getCode).reversed())
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
                        .comparing(CorporateViewModel::getSubmitDate)
                        .thenComparing(CorporateViewModel::getCode).reversed())
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
                        .comparing(CorporateViewModel::getSubmitDate)
                        .thenComparing(CorporateViewModel::getCode).reversed())
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
                .map(Company::code)
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
                        .comparing(CorporateViewModel::getSubmitDate)
                        .thenComparing(CorporateViewModel::getCode).reversed())
                .toList();
    }

    /**
     * 企業情報詳細ビューを取得する。target 未指定経路では viewAll() ベースで前後コードを設定する。
     *
     * @param inputData 企業コード
     * @return 企業情報詳細ビュー
     */
    @Override
    public CorporateDetailViewModel viewCorporateDetail(final CodeInputData inputData) {
        final CorporateDetailViewModel raw = viewCorporateDetailRaw(inputData);
        final List<String> codeList = viewAll().stream().map(CorporateViewModel::getCode).toList();
        return applyAdjacentCodes(raw, codeList, inputData.getCode());
    }

    /**
     * 前後コードを設定しない素の企業情報詳細ビューを生成する。前後コード計算の上位メソッドから呼び出される。
     *
     * @param inputData 企業コード
     * @return 前後コード null の企業情報詳細ビュー
     */
    protected CorporateDetailViewModel viewCorporateDetailRaw(final CodeInputData inputData) {
        final Company company = companySpecification.findCompanyByCode(inputData.getCode5())
                .orElseThrow(() -> new FundanalyzerNotExistException("企業コード"));
        final Stock stock = stockSpecification.findStock(company);

        final List<AnalysisResultViewModel> analysisResultList = analysisResultSpecification.displayTargetList(company, targetTypeCodes).stream()
                .map(AnalysisResultViewModel::of)
                .sorted(Comparator.comparing(AnalysisResultViewModel::documentPeriod)
                        .thenComparing(AnalysisResultViewModel::submitDate)
                        .reversed())
                .toList();

        final List<IndicatorViewModel> indicatorList = investmentIndicatorSpecification.findIndicatorValueList(company.code()).stream()
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
                            key.submitDate(),
                            key,
                            financialStatementSpecification.parseBsSubjectValue(valueList),
                            financialStatementSpecification.parsePlSubjectValue(valueList)
                    );
                })
                .sorted(Comparator.comparing(FinancialStatementViewModel::getSubmitDate).reversed())
                .toList();
        final List<LocalDate> splitDates = corporateActionSpecification.findActions(company.code()).stream()
                .filter(CorporateAction::confirmed)
                .map(CorporateAction::effectiveDate)
                .sorted()
                .toList();
        final Optional<LocalDate> basisDate = documentSpecification.findLatestDocument(company)
                .map(Document::getSubmitDate);

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
                        .sorted(Comparator.comparing(MinkabuViewModel::targetDate).reversed())
                        .toList(),
                stock.getStockPriceEntityList().stream()
                        .map(entity -> toAdjustedViewModel(entity, company.code(), basisDate))
                        .sorted(Comparator.comparing(StockPriceViewModel::targetDate).reversed())
                        .toList(),
                splitDates
        );
    }

    private StockPriceViewModel toAdjustedViewModel(
            final StockPriceEntity entity,
            final String companyCode,
            final Optional<LocalDate> basisDate) {
        if (basisDate.isEmpty()) {
            return StockPriceViewModel.of(entity);
        }
        final LocalDate basis = basisDate.orElseThrow();
        return new StockPriceViewModel(
                entity.getTargetDate(),
                corporateActionSpecification.adjustToBasis(
                        BigDecimal.valueOf(entity.getStockPrice()),
                        companyCode,
                        entity.getTargetDate(),
                        basis,
                        true
                ).doubleValue(),
                entity.getOpeningPrice()
                        .map(value -> corporateActionSpecification.adjustToBasis(
                                BigDecimal.valueOf(value),
                                companyCode,
                                entity.getTargetDate(),
                                basis,
                                true
                        ).doubleValue())
                        .orElse(null),
                entity.getHighPrice()
                        .map(value -> corporateActionSpecification.adjustToBasis(
                                BigDecimal.valueOf(value),
                                companyCode,
                                entity.getTargetDate(),
                                basis,
                                true
                        ).doubleValue())
                        .orElse(null),
                entity.getLowPrice()
                        .map(value -> corporateActionSpecification.adjustToBasis(
                                BigDecimal.valueOf(value),
                                companyCode,
                                entity.getTargetDate(),
                                basis,
                                true
                        ).doubleValue())
                        .orElse(null)
        );
    }

    @Override
    public CorporateDetailViewModel viewCorporateDetail(final CodeInputData inputData, final Target target) {
        final CorporateDetailViewModel raw = viewCorporateDetailRaw(inputData);
        final List<String> codeList = switch (target) {
            case MAIN -> viewMain().stream().map(CorporateViewModel::getCode).toList();
            case QUART -> viewQuart().stream().map(CorporateViewModel::getCode).toList();
            case ALL -> viewAll().stream().map(CorporateViewModel::getCode).toList();
            default -> List.of();
        };
        return applyAdjacentCodes(raw, codeList, inputData.getCode());
    }

    /**
     * 提出日順 (新→古) でソート済みの codeList から、指定コードに対する前後コードを ViewModel に設定する。
     * 前 = より古い提出日 (= リスト末尾方向 / index + 1) / 次 = より新しい提出日 (= リスト先頭方向 / index - 1)。
     *
     * @param base     前後コード設定前の ViewModel
     * @param codeList 提出日順でソート済みの企業コードリスト
     * @param code     対象企業コード
     * @return 前後コードを設定した ViewModel。codeList が空または対象コード未含有の場合は base をそのまま返す
     */
    private static CorporateDetailViewModel applyAdjacentCodes(
            final CorporateDetailViewModel base, final List<String> codeList, final String code) {
        if (codeList.isEmpty()) {
            return base;
        }
        final int index = codeList.indexOf(code);
        if (index < 0) {
            return base;
        }
        final String backwardCode = (index + 1 < codeList.size()) ? codeList.get(index + 1) : null;
        final String forwardCode = (index > 0) ? codeList.get(index - 1) : null;
        return CorporateDetailViewModel.of(base, backwardCode, forwardCode);
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
                .filter(cvm -> Objects.nonNull(cvm.getDiscountRateToDisplay()))
                .filter(cvm -> Objects.nonNull(cvm.getStandardDeviationToDisplay()))
                .filter(cvm -> Objects.nonNull(cvm.getLatestCorporateValue()))
                // 表示する提出日は一定期間のみ
                .filter(cvm -> cvm.getSubmitDate().isAfter(nowLocalDate().minusDays(configCorporateSize)))
                // 割安度が170%(外部設定値)以上を表示
                .filter(cvm -> cvm.getDiscountRateToDisplay().compareTo(configDiscountRate) >= 0)
                // 標準偏差が外れ値となっていたら除外
                .filter(cvm -> cvm.getStandardDeviationToDisplay().compareTo(configOutlierOfStandardDeviation) < 0)
                // 最新企業価値がマイナスの場合は除外
                .filter(cvm -> cvm.getLatestCorporateValue().compareTo(BigDecimal.ZERO) > 0)
                // 最新企業価値が平均（x1.1倍）より低い場合は除外
                .filter(cvm -> cvm.getLatestCorporateValue().compareTo(cvm.getAverageCorporateValueToDisplay().multiply(BigDecimal.valueOf(1.1))) > 0)
                // 変動係数
                .filter(cvm -> {
                    if (Objects.isNull(cvm.getCoefficientOfVariationToDisplay())) {
                        // 変動係数が存在しない
                        return true;
                    } else {
                        // 変動係数が0.6未満であること
                        if (cvm.getCoefficientOfVariationToDisplay().compareTo(configCoefficientOfVariation) < 1) {
                            return true;
                        } else {
                            // 変動係数が0.6以上でも最新企業価値が高ければOK
                            return cvm.getLatestCorporateValue().compareTo(cvm.getAverageCorporateValueToDisplay()) > -1;
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
                        analysisResultSpecification.findLatestAnalysisResult(company.code()).map(AnalysisResult::of).orElse(AnalysisResult.of()),
                        analyzeInteractor.calculateCorporateValue(company),
                        investmentIndicatorSpecification.findIndicatorValue(company.code()).orElse(IndicatorValue.of())
                )));
            } catch (final FundanalyzerNotExistException e) {
                log.warn(FundanalyzerLogClient.toInteractorLogObject(
                        MessageFormat.format(
                                "条件を満たさないため、次の企業のビューを更新しませんでした。\t企業コード:{0}",
                                company.code()
                        ),
                        companySpecification.findCompanyByCode(company.code()).map(Company::edinetCode).orElse("null"),
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
