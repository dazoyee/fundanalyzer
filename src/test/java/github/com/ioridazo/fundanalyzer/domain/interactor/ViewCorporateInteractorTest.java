package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.client.slack.SlackClient;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.FinancialStatementEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.AnalysisResultSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CompanySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.DocumentSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.FinancialStatementSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.InvestmentIndicatorSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.StockSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.ViewSpecification;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.domain.value.Stock;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.presenter.Target;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.CorporateViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.CorporateDetailViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.FinancialStatementValueViewModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ViewCorporateInteractorTest {

    private static final List<String> targetTypeCodes = List.of("120", "130", "140", "150");

    private CompanySpecification companySpecification;
    private DocumentSpecification documentSpecification;
    private FinancialStatementSpecification financialStatementSpecification;
    private AnalysisResultSpecification analysisResultSpecification;
    private StockSpecification stockSpecification;
    private ViewSpecification viewSpecification;
    private SlackClient slackClient;

    private ViewCorporateInteractor viewCorporateInteractor;

    @BeforeEach
    void setUp() {
        companySpecification = Mockito.mock(CompanySpecification.class);
        documentSpecification = Mockito.mock(DocumentSpecification.class);
        financialStatementSpecification = Mockito.mock(FinancialStatementSpecification.class);
        analysisResultSpecification = Mockito.mock(AnalysisResultSpecification.class);
        stockSpecification = Mockito.mock(StockSpecification.class);
        viewSpecification = Mockito.mock(ViewSpecification.class);
        slackClient = Mockito.mock(SlackClient.class);

        viewCorporateInteractor = Mockito.spy(new ViewCorporateInteractor(
                Mockito.mock(AnalyzeInteractor.class),
                companySpecification,
                documentSpecification,
                financialStatementSpecification,
                analysisResultSpecification,
                stockSpecification,
                Mockito.mock(InvestmentIndicatorSpecification.class),
                viewSpecification,
                slackClient
        ));
        viewCorporateInteractor.configDiscountRate = BigDecimal.valueOf(120);
        viewCorporateInteractor.configOutlierOfStandardDeviation = BigDecimal.valueOf(10000);
        viewCorporateInteractor.configCoefficientOfVariation = BigDecimal.valueOf(0.6);
        viewCorporateInteractor.configDiffForecastStock = BigDecimal.valueOf(100);
        viewCorporateInteractor.configCorporateSize = 300;
        viewCorporateInteractor.targetTypeCodes = List.of("120", "130", "140", "150");
        viewCorporateInteractor.updateViewEnabled = true;
    }

    @Nested
    class viewCorporateDetail {

        CodeInputData inputData = CodeInputData.of("code");
        Company company = defaultCompany();
        Stock stock = defaultStock();
        CorporateViewModel corporateViewModel = defaultCorporateViewModel();

        @BeforeEach
        void setUp() {
            when(companySpecification.findCompanyByCode("code0")).thenReturn(Optional.of(company));
            when(stockSpecification.findStock(company)).thenReturn(stock);
            when(viewSpecification.findLatestCorporateView(inputData)).thenReturn(corporateViewModel);
        }

        @DisplayName("viewCorporateDetail : 企業情報詳細ビューを取得する")
        @Test
        void of() {
            var analysisResultEntity = new AnalysisResultEntity(
                    null,
                    null,
                    LocalDate.parse("2021-01-01"),
                    BigDecimal.TEN,
                    null,
                    null,
                    null,
                    null,
                    "120",
                    null,
                    null,
                    null,
                    null
            );
            var bsEntity = new FinancialStatementEntity(
                    null,
                    null,
                    null,
                    null,
                    null,
                    LocalDate.parse("2021-01-01"),
                    LocalDate.parse("2021-12-31"),
                    null,
                    "120",
                    "4",
                    LocalDate.parse("2021-05-15"),
                    null,
                    "0",
                    null
            );
            var plEntity = new FinancialStatementEntity(
                    null,
                    null,
                    null,
                    null,
                    null,
                    LocalDate.parse("2021-01-01"),
                    LocalDate.parse("2021-12-31"),
                    null,
                    "120",
                    "4",
                    LocalDate.parse("2021-05-15"),
                    null,
                    "0",
                    null
            );

            when(analysisResultSpecification.displayTargetList(company, targetTypeCodes)).thenReturn(List.of(analysisResultEntity));
            when(financialStatementSpecification.findByCompany(company)).thenReturn(List.of(bsEntity, plEntity));
            when(financialStatementSpecification.findByKeyPerCompany(eq(company), any())).thenReturn(List.of(bsEntity, plEntity));
            when(financialStatementSpecification.parseBsSubjectValue(List.of(bsEntity, plEntity)))
                    .thenReturn(List.of(FinancialStatementValueViewModel.of("bs", 100L)));
            when(financialStatementSpecification.parsePlSubjectValue(List.of(bsEntity, plEntity)))
                    .thenReturn(List.of(FinancialStatementValueViewModel.of("pl", 100L)));

            var actual = viewCorporateInteractor.viewCorporateDetail(inputData);

            assertAll(
                    () -> assertAll(
                            () -> assertEquals("code", actual.getCompany().code(), "company.code"),
                            () -> assertEquals("name", actual.getCompany().name(), "company.name"),
                            () -> assertEquals("edinetCode", actual.getCompany().edinetCode())
                    ),
                    () -> assertAll(
                            () -> assertEquals("code", actual.getCorporate().getCode(), "corporate.code"),
                            () -> assertEquals("name", actual.getCorporate().getName(), "corporate.name"),
                            () -> assertEquals(BigDecimal.TEN, actual.getCorporate().getLatestCorporateValue()),
                            () -> assertEquals(BigDecimal.TEN, actual.getCorporate().getAverageCorporateValueToDisplay()),
                            () -> assertEquals(BigDecimal.TEN, actual.getCorporate().getStandardDeviationToDisplay()),
                            () -> assertEquals(BigDecimal.TEN, actual.getCorporate().getDiscountValueToDisplay()),
                            () -> assertEquals(BigDecimal.TEN, actual.getCorporate().getDiscountRateToDisplay())
                    ),
                    () -> assertAll(
                            () -> assertEquals(LocalDate.parse("2021-01-01"), actual.getAnalysisResultList().get(0).documentPeriod()),
                            () -> assertEquals(BigDecimal.TEN, actual.getAnalysisResultList().get(0).corporateValue()),
                            () -> assertEquals("120", actual.getAnalysisResultList().get(0).documentTypeCode()),
                            () -> assertNull(actual.getAnalysisResultList().get(0).quarterType())
                    ),
                    () -> assertAll(
                            () -> assertAll(
                                    () -> assertEquals(LocalDate.parse("2021-01-01"), actual.getFinancialStatement().get(0).getKey().periodStart()),
                                    () -> assertEquals(LocalDate.parse("2021-12-31"), actual.getFinancialStatement().get(0).getKey().periodEnd()),
                                    () -> assertEquals("有価証券報告書", actual.getFinancialStatement().get(0).getKey().documentTypeName()),
                                    () -> assertEquals(LocalDate.parse("2021-05-15"), actual.getFinancialStatement().get(0).getKey().submitDate())
                            ),
                            () -> assertAll(
                                    () -> assertEquals("bs", actual.getFinancialStatement().get(0).getBs().get(0).getSubject()),
                                    () -> assertEquals(100L, actual.getFinancialStatement().get(0).getBs().get(0).getValue())
                            ),
                            () -> assertAll(
                                    () -> assertEquals("pl", actual.getFinancialStatement().get(0).getPl().get(0).getSubject()),
                                    () -> assertEquals(100L, actual.getFinancialStatement().get(0).getPl().get(0).getValue())
                            )
                    ),
                    () -> assertEquals(0, actual.getMinkabuList().size()),
                    () -> assertEquals(0, actual.getStockPriceList().size())
            );
            verify(financialStatementSpecification, times(1)).findByKeyPerCompany(eq(company), any());
        }

        @DisplayName("viewCorporateDetail : backward と forward の値を確認する (リスト = 提出日新→古、次=より新しい提出日)")
        @Test
        void target() {
            doReturn(CorporateDetailViewModel.of(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            )).when(viewCorporateInteractor).viewCorporateDetailRaw(inputData);
            doReturn(List.of(
                    defaultCorporateViewModel("new"),
                    defaultCorporateViewModel("code"),
                    defaultCorporateViewModel("old")
            )).when(viewCorporateInteractor).viewMain();

            var actual = viewCorporateInteractor.viewCorporateDetail(inputData, Target.MAIN);

            assertEquals("old", actual.getBackwardCode());
            assertEquals("new", actual.getForwardCode());
        }

        @DisplayName("viewCorporateDetail : リスト先頭 (= 最新) の場合 forward が null")
        @Test
        void target_forward_is_null_at_head_of_list() {
            doReturn(CorporateDetailViewModel.of(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            )).when(viewCorporateInteractor).viewCorporateDetailRaw(inputData);
            doReturn(List.of(
                    defaultCorporateViewModel("code"),
                    defaultCorporateViewModel("old")
            )).when(viewCorporateInteractor).viewMain();

            var actual = viewCorporateInteractor.viewCorporateDetail(inputData, Target.MAIN);

            assertEquals("old", actual.getBackwardCode());
            assertNull(actual.getForwardCode());
        }

        @DisplayName("viewCorporateDetail : リスト末尾 (= 最古) の場合 backward が null")
        @Test
        void target_backward_is_null_at_tail_of_list() {
            doReturn(CorporateDetailViewModel.of(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            )).when(viewCorporateInteractor).viewCorporateDetailRaw(inputData);
            doReturn(List.of(
                    defaultCorporateViewModel("new"),
                    defaultCorporateViewModel("code")
            )).when(viewCorporateInteractor).viewMain();

            var actual = viewCorporateInteractor.viewCorporateDetail(inputData, Target.MAIN);

            assertNull(actual.getBackwardCode());
            assertEquals("new", actual.getForwardCode());
        }

        @DisplayName("viewCorporateDetail : target=ALL のときも viewAll() の DESC+DESC tie-break で前後コードが算出される")
        @Test
        void target_all_useViewAllAndCodeDescTieBreak() {
            doReturn(CorporateDetailViewModel.of(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            )).when(viewCorporateInteractor).viewCorporateDetailRaw(inputData);
            // viewAll() は submitDate DESC + code DESC でソートされる前提
            doReturn(List.of(
                    corporateViewWithSubmitDate("9003", LocalDate.parse("2026-03-27")),
                    corporateViewWithSubmitDate("9005", LocalDate.parse("2026-03-25")),
                    corporateViewWithSubmitDate("9004", LocalDate.parse("2026-03-25")),
                    corporateViewWithSubmitDate("code", LocalDate.parse("2026-03-25")),
                    corporateViewWithSubmitDate("9002", LocalDate.parse("2026-02-14"))
            )).when(viewCorporateInteractor).viewAll();

            var actual = viewCorporateInteractor.viewCorporateDetail(inputData, Target.ALL);

            // code(=9001相当) の前 (= リスト次要素 = 9002 / より古い提出日) と 次 (= リスト前要素 = 9004 / 同提出日 code 大)
            assertEquals("9002", actual.getBackwardCode());
            assertEquals("9004", actual.getForwardCode());
        }

        @DisplayName("viewCorporateDetail : 単独要素の場合 backward と forward が null")
        @Test
        void target_backward_and_forward_is_null() {
            doReturn(CorporateDetailViewModel.of(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            )).when(viewCorporateInteractor).viewCorporateDetailRaw(inputData);
            doReturn(List.of(
                    defaultCorporateViewModel("code")
            )).when(viewCorporateInteractor).viewMain();

            var actual = viewCorporateInteractor.viewCorporateDetail(inputData, Target.MAIN);

            assertNull(actual.getBackwardCode());
            assertNull(actual.getForwardCode());
        }
    }

    @Nested
    @DisplayName("viewCorporateDetail (target なしオーバーロード) — viewAll() ベースで前後コードを設定")
    class viewCorporateDetailWithoutTarget {

        CodeInputData inputData = CodeInputData.of("code");

        @DisplayName("viewAll() リストの中央要素 → backward は古い側、forward は新しい側に設定される")
        @Test
        void noTarget_setsBackwardAndForwardFromViewAll() {
            doReturn(CorporateDetailViewModel.of(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            )).when(viewCorporateInteractor).viewCorporateDetailRaw(inputData);
            doReturn(List.of(
                    defaultCorporateViewModel("new"),
                    defaultCorporateViewModel("code"),
                    defaultCorporateViewModel("old")
            )).when(viewCorporateInteractor).viewAll();

            var actual = viewCorporateInteractor.viewCorporateDetail(inputData);

            assertEquals("old", actual.getBackwardCode());
            assertEquals("new", actual.getForwardCode());
        }

        @DisplayName("viewAll() が空の場合 → 前後コードはどちらも null")
        @Test
        void noTarget_emptyViewAll_returnsNullCodes() {
            doReturn(CorporateDetailViewModel.of(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            )).when(viewCorporateInteractor).viewCorporateDetailRaw(inputData);
            doReturn(List.<CorporateViewModel>of()).when(viewCorporateInteractor).viewAll();

            var actual = viewCorporateInteractor.viewCorporateDetail(inputData);

            assertNull(actual.getBackwardCode());
            assertNull(actual.getForwardCode());
        }

        @DisplayName("viewAll() に対象コードが含まれない場合 → 前後コードはどちらも null")
        @Test
        void noTarget_codeNotInList_returnsNullCodes() {
            doReturn(CorporateDetailViewModel.of(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            )).when(viewCorporateInteractor).viewCorporateDetailRaw(inputData);
            doReturn(List.of(
                    defaultCorporateViewModel("other-1"),
                    defaultCorporateViewModel("other-2")
            )).when(viewCorporateInteractor).viewAll();

            var actual = viewCorporateInteractor.viewCorporateDetail(inputData);

            assertNull(actual.getBackwardCode());
            assertNull(actual.getForwardCode());
        }
    }

    @Nested
    class updateView {

        Company company = defaultCompany();
        Document document = defaultDocument();
        CorporateViewModel corporateViewModel = defaultCorporateViewModel();
        DateInputData inputData = DateInputData.of(LocalDate.parse("2021-05-16"));

        @DisplayName("updateView : すべてのビューを更新する")
        @Test
        void all() {
            when(companySpecification.inquiryAllTargetCompanies()).thenReturn(List.of(company));
            when(documentSpecification.findLatestDocument(company)).thenReturn(Optional.of(document));
            when(viewSpecification.generateCorporateView(eq(company), eq(document), any(), any(), any())).thenReturn(corporateViewModel);

            assertDoesNotThrow(() -> viewCorporateInteractor.updateView());
            verify(viewSpecification, times(1)).upsert(corporateViewModel);
            verify(slackClient, times(1)).sendMessage(any());
        }

        @DisplayName("updateView : ビューを更新する")
        @Test
        void inputData() {
            when(documentSpecification.inquiryTargetDocuments(inputData)).thenReturn(List.of(document));
            when(companySpecification.findCompanyByEdinetCode("edinetCode")).thenReturn(Optional.of(company));
            when(documentSpecification.findLatestDocument(company)).thenReturn(Optional.of(document));
            when(viewSpecification.generateCorporateView(eq(company), eq(document), any(), any(), any())).thenReturn(corporateViewModel);

            assertDoesNotThrow(() -> viewCorporateInteractor.updateView(inputData));
            verify(viewSpecification, times(1)).upsert(corporateViewModel);
            verify(slackClient, times(0)).sendMessage(any());
        }

        @DisplayName("updateView : 書類が存在しないときはビューを更新しない")
        @Test
        void document_isEmpty() {
            when(documentSpecification.inquiryTargetDocuments(inputData)).thenReturn(List.of(document));
            when(companySpecification.findCompanyByEdinetCode("edinetCode")).thenReturn(Optional.of(company));
            when(documentSpecification.findLatestDocument(company)).thenReturn(Optional.empty());

            assertDoesNotThrow(() -> viewCorporateInteractor.updateView(inputData));
            verify(viewSpecification, times(0)).upsert(corporateViewModel);
        }
    }

    @Nested
    class filter {

        @BeforeEach
        void setUp() {
            doReturn(LocalDate.parse("2021-07-04")).when(viewCorporateInteractor).nowLocalDate();
        }

        @DisplayName("filter : 割安度が存在しないときは表示対象外とする")
        @Test
        void discountRate_isEmpty() {
            var list = List.of(new CorporateViewModel());
            assertEquals(0, viewCorporateInteractor.filter(list).size());
        }

        @DisplayName("filter : 300日以前の提出日のみを表示する")
        @Test
        void configCorporateSize() {
            var model = new CorporateViewModel();
            model.setSubmitDate(LocalDate.parse("2020-07-04"));
            var list = List.of(model);

            assertEquals(0, viewCorporateInteractor.filter(list).size());
        }

        @DisplayName("filter : 割安度が120%以上を表示する")
        @Test
        void configDiscountRate() {
            var model = new CorporateViewModel();
            model.setSubmitDate(LocalDate.parse("2020-07-04"));
            model.setDiscountRateToDisplay(BigDecimal.valueOf(119));
            var list = List.of(model);

            assertEquals(0, viewCorporateInteractor.filter(list).size());
        }

        @DisplayName("filter : 標準偏差が外れ値となっていたら除外する")
        @Test
        void configOutlierOfStandardDeviation() {
            var model = new CorporateViewModel();
            model.setSubmitDate(LocalDate.parse("2020-07-04"));
            model.setAverageCorporateValueToDisplay(BigDecimal.valueOf(10000));
            model.setDiscountRateToDisplay(BigDecimal.valueOf(120));
            var list = List.of(model);

            assertEquals(0, viewCorporateInteractor.filter(list).size());
        }

        @DisplayName("filter : 最新企業価値がマイナスの場合は除外する")
        @Test
        void corporateValue_isMinus() {
            var model = new CorporateViewModel();
            model.setSubmitDate(LocalDate.parse("2020-07-04"));
            model.setMainReport(true);
            model.setLatestCorporateValue(BigDecimal.valueOf(-1));
            model.setAverageCorporateValueToDisplay(BigDecimal.valueOf(100));
            model.setDiscountRateToDisplay(BigDecimal.valueOf(120));
            var list = List.of(model);

            assertEquals(0, viewCorporateInteractor.filter(list).size());
        }

        @DisplayName("filter : 最新企業価値が平均より低い場合は除外する")
        @Test
        void corporateValue_isAboveAverage() {
            var model = new CorporateViewModel();
            model.setSubmitDate(LocalDate.parse("2020-07-04"));
            model.setMainReport(true);
            model.setLatestCorporateValue(BigDecimal.valueOf(1000));
            model.setAverageCorporateValueToDisplay(BigDecimal.valueOf(10000));
            model.setStandardDeviationToDisplay(BigDecimal.valueOf(100));
            model.setCoefficientOfVariationToDisplay(BigDecimal.valueOf(0.5));
            model.setDiscountRateToDisplay(BigDecimal.valueOf(120));
            var list = List.of(model);

            assertEquals(0, viewCorporateInteractor.filter(list).size());
        }

        @DisplayName("filter : 変動係数が0.6未満であること")
        @Test
        void configCoefficientOfVariation1() {
            var model = new CorporateViewModel();
            model.setSubmitDate(LocalDate.parse("2021-07-04"));
            model.setMainReport(true);
            model.setLatestCorporateValue(BigDecimal.valueOf(100));
            model.setAverageCorporateValueToDisplay(BigDecimal.valueOf(10));
            model.setStandardDeviationToDisplay(BigDecimal.valueOf(100));
            model.setCoefficientOfVariationToDisplay(BigDecimal.valueOf(0.5));
            model.setDiscountRateToDisplay(BigDecimal.valueOf(120));
            var list = List.of(model);

            assertEquals(1, viewCorporateInteractor.filter(list).size());
        }

        @DisplayName("filter : 変動係数が0.6以上でも最新企業価値が高ければOK")
        @Test
        void configCoefficientOfVariation2() {
            var model = new CorporateViewModel();
            model.setSubmitDate(LocalDate.parse("2021-07-04"));
            model.setMainReport(true);
            model.setLatestCorporateValue(BigDecimal.valueOf(10000));
            model.setAverageCorporateValueToDisplay(BigDecimal.valueOf(5000));
            model.setStandardDeviationToDisplay(BigDecimal.valueOf(100));
            model.setCoefficientOfVariationToDisplay(BigDecimal.valueOf(0.7));
            model.setDiscountRateToDisplay(BigDecimal.valueOf(120));
            var list = List.of(model);

            assertEquals(1, viewCorporateInteractor.filter(list).size());
        }

        @DisplayName("filter : 株価予想が存在する場合、最新株価より高ければOK")
        @Test
        void forecastStock1() {
            var model = new CorporateViewModel();
            model.setSubmitDate(LocalDate.parse("2021-07-04"));
            model.setMainReport(true);
            model.setLatestCorporateValue(BigDecimal.valueOf(10000));
            model.setAverageCorporateValueToDisplay(BigDecimal.valueOf(5000));
            model.setStandardDeviationToDisplay(BigDecimal.valueOf(100));
            model.setCoefficientOfVariationToDisplay(BigDecimal.valueOf(0.7));
            model.setLatestStockPrice(BigDecimal.valueOf(100));
            model.setDiscountRateToDisplay(BigDecimal.valueOf(120));
            model.setForecastStock(BigDecimal.valueOf(200));
            var list = List.of(model);

            assertEquals(1, viewCorporateInteractor.filter(list).size());
        }

        @DisplayName("filter : 株価予想が存在する場合、株価予想と最新株価との差が100以上であればOK")
        @Test
        void forecastStock2() {
            var model = new CorporateViewModel();
            model.setSubmitDate(LocalDate.parse("2021-07-04"));
            model.setMainReport(true);
            model.setLatestCorporateValue(BigDecimal.valueOf(10000));
            model.setAverageCorporateValueToDisplay(BigDecimal.valueOf(5000));
            model.setStandardDeviationToDisplay(BigDecimal.valueOf(100));
            model.setCoefficientOfVariationToDisplay(BigDecimal.valueOf(0.7));
            model.setLatestStockPrice(BigDecimal.valueOf(10));
            model.setDiscountRateToDisplay(BigDecimal.valueOf(120));
            model.setForecastStock(BigDecimal.valueOf(100));
            var list = List.of(model);

            assertEquals(0, viewCorporateInteractor.filter(list).size());
        }

        @DisplayName("filter : 表示に必要な最低限の情報があるもののみ表示する")
        @Test
        void present() {
            var model = new CorporateViewModel();
            model.setSubmitDate(LocalDate.parse("2021-07-04"));
            model.setMainReport(true);
            model.setLatestCorporateValue(BigDecimal.valueOf(10000));
            model.setAverageCorporateValueToDisplay(BigDecimal.valueOf(10));
            model.setStandardDeviationToDisplay(BigDecimal.valueOf(100));
            model.setDiscountRateToDisplay(BigDecimal.valueOf(120));
            var list = List.of(model);

            assertEquals(1, viewCorporateInteractor.filter(list).size());
        }
    }

    private Company defaultCompany() {
        return new Company(
                "code",
                "name",
                null,
                null,
                "edinetCode",
                null,
                null,
                null,
                null,
                false,
                true
        );
    }

    private Document defaultDocument() {
        return new Document(
                null,
                null,
                null,
                "edinetCode",
                null,
                LocalDate.now(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false
        );
    }

    private Stock defaultStock() {
        return Stock.of(
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of()
        );
    }

    private CorporateViewModel defaultCorporateViewModel() {
        var model = new CorporateViewModel();
        model.setCode("code");
        model.setName("name");
        model.setLatestCorporateValue(BigDecimal.TEN);
        model.setAverageCorporateValueToDisplay(BigDecimal.TEN);
        model.setStandardDeviationToDisplay(BigDecimal.TEN);
        model.setCoefficientOfVariationToDisplay(BigDecimal.TEN);
        model.setDiscountValueToDisplay(BigDecimal.TEN);
        model.setDiscountRateToDisplay(BigDecimal.TEN);
        return model;
    }

    private CorporateViewModel defaultCorporateViewModel(String code) {
        var model = new CorporateViewModel();
        model.setCode(code);
        model.setName("name");
        model.setLatestCorporateValue(BigDecimal.TEN);
        model.setAverageCorporateValueToDisplay(BigDecimal.TEN);
        model.setStandardDeviationToDisplay(BigDecimal.TEN);
        model.setCoefficientOfVariationToDisplay(BigDecimal.TEN);
        model.setDiscountValueToDisplay(BigDecimal.TEN);
        model.setDiscountRateToDisplay(BigDecimal.TEN);
        return model;

    }

    private CorporateViewModel corporateViewWithSubmitDate(String code, LocalDate submitDate) {
        var model = defaultCorporateViewModel(code);
        model.setSubmitDate(submitDate);
        return model;
    }

    @Nested
    @DisplayName("viewAll() — 提出日 DESC + コード DESC でソート（viewMain と tie-break を統一）")
    class viewAllSort {

        @Test
        @DisplayName("複数提出日 × 同提出日複数コード → submitDate DESC, then code DESC でソートされる")
        void sortedBySubmitDateDescThenCodeDesc() {
            var c1 = corporateViewWithSubmitDate("9001", LocalDate.parse("2026-03-25"));
            var c2 = corporateViewWithSubmitDate("9004", LocalDate.parse("2026-03-25"));
            var c3 = corporateViewWithSubmitDate("9005", LocalDate.parse("2026-03-25"));
            var c4 = corporateViewWithSubmitDate("9003", LocalDate.parse("2026-03-27"));
            var c5 = corporateViewWithSubmitDate("9002", LocalDate.parse("2026-02-14"));
            doReturn(List.of(c1, c2, c3, c4, c5)).when(viewSpecification).findAllCorporateView();

            var actual = viewCorporateInteractor.viewAll().stream()
                    .map(CorporateViewModel::getCode)
                    .toList();

            assertEquals(List.of("9003", "9005", "9004", "9001", "9002"), actual);
        }
    }
}