package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.client.slack.SlackClient;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.FinancialStatementEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.AnalysisResultSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CompanySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.DocumentSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.FinancialStatementSpecification;
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
                            () -> assertEquals("code", actual.getCompany().getCode(), "company.code"),
                            () -> assertEquals("name", actual.getCompany().getName(), "company.name"),
                            () -> assertEquals("edinetCode", actual.getCompany().getEdinetCode())
                    ),
                    () -> assertAll(
                            () -> assertEquals("code", actual.getCorporate().getCode(), "corporate.code"),
                            () -> assertEquals("name", actual.getCorporate().getName(), "corporate.name"),
                            () -> assertEquals(BigDecimal.TEN, actual.getCorporate().getLatestCorporateValue()),
                            () -> assertEquals(BigDecimal.TEN, actual.getCorporate().getAllAverageCorporateValue()),
                            () -> assertEquals(BigDecimal.TEN, actual.getCorporate().getAllStandardDeviation()),
                            () -> assertEquals(BigDecimal.TEN, actual.getCorporate().getAllDiscountValue()),
                            () -> assertEquals(BigDecimal.TEN, actual.getCorporate().getAllDiscountRate())
                    ),
                    () -> assertAll(
                            () -> assertEquals(LocalDate.parse("2021-01-01"), actual.getAnalysisResultList().get(0).getDocumentPeriod()),
                            () -> assertEquals(BigDecimal.TEN, actual.getAnalysisResultList().get(0).getCorporateValue()),
                            () -> assertEquals("120", actual.getAnalysisResultList().get(0).getDocumentTypeCode()),
                            () -> assertNull(actual.getAnalysisResultList().get(0).getQuarterType())
                    ),
                    () -> assertAll(
                            () -> assertAll(
                                    () -> assertEquals(LocalDate.parse("2021-01-01"), actual.getFinancialStatement().get(0).getKey().getPeriodStart()),
                                    () -> assertEquals(LocalDate.parse("2021-12-31"), actual.getFinancialStatement().get(0).getKey().getPeriodEnd()),
                                    () -> assertEquals("有価証券報告書", actual.getFinancialStatement().get(0).getKey().getDocumentTypeName()),
                                    () -> assertEquals(LocalDate.parse("2021-05-15"), actual.getFinancialStatement().get(0).getKey().getSubmitDate())
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

        @DisplayName("viewCorporateDetail : backward と forward の値を確認する")
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
                    null
            )).when(viewCorporateInteractor).viewCorporateDetail(inputData);
            doReturn(List.of(
                    defaultCorporateViewModel("code-1"),
                    defaultCorporateViewModel("code"),
                    defaultCorporateViewModel("code+1")
            )).when(viewCorporateInteractor).viewMain();

            var actual = viewCorporateInteractor.viewCorporateDetail(inputData, Target.MAIN);

            assertEquals("code-1", actual.getBackwardCode());
            assertEquals("code+1", actual.getForwardCode());
        }

        @DisplayName("viewCorporateDetail : backward が null になることを確認する")
        @Test
        void target_backward_is_null() {
            doReturn(CorporateDetailViewModel.of(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            )).when(viewCorporateInteractor).viewCorporateDetail(inputData);
            doReturn(List.of(
                    defaultCorporateViewModel("code"),
                    defaultCorporateViewModel("code+1")
            )).when(viewCorporateInteractor).viewMain();

            var actual = viewCorporateInteractor.viewCorporateDetail(inputData, Target.MAIN);

            assertNull(actual.getBackwardCode());
            assertEquals("code+1", actual.getForwardCode());
        }

        @DisplayName("viewCorporateDetail : forward が null になることを確認する")
        @Test
        void target_forward_is_null() {
            doReturn(CorporateDetailViewModel.of(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            )).when(viewCorporateInteractor).viewCorporateDetail(inputData);
            doReturn(List.of(
                    defaultCorporateViewModel("code-1"),
                    defaultCorporateViewModel("code")
            )).when(viewCorporateInteractor).viewMain();

            var actual = viewCorporateInteractor.viewCorporateDetail(inputData, Target.MAIN);

            assertEquals("code-1", actual.getBackwardCode());
            assertNull(actual.getForwardCode());
        }

        @DisplayName("viewCorporateDetail : backward と forward が null になることを確認する")
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
                    null
            )).when(viewCorporateInteractor).viewCorporateDetail(inputData);
            doReturn(List.of(
                    defaultCorporateViewModel("code")
            )).when(viewCorporateInteractor).viewMain();

            var actual = viewCorporateInteractor.viewCorporateDetail(inputData, Target.MAIN);

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
            when(viewSpecification.generateCorporateView(eq(company), any())).thenReturn(corporateViewModel);

            assertDoesNotThrow(() -> viewCorporateInteractor.updateView());
            verify(viewSpecification, times(1)).upsert(corporateViewModel);
            verify(slackClient, times(1)).sendMessage(any());
        }

        @DisplayName("updateView : ビューを更新する")
        @Test
        void inputData() {
            when(documentSpecification.targetList(inputData)).thenReturn(List.of(document));
            when(companySpecification.findCompanyByEdinetCode("edinetCode")).thenReturn(Optional.of(company));
            when(viewSpecification.generateCorporateView(eq(company), any())).thenReturn(corporateViewModel);

            assertDoesNotThrow(() -> viewCorporateInteractor.updateView(inputData));
            verify(viewSpecification, times(1)).upsert(corporateViewModel);
            verify(slackClient, times(0)).sendMessage(any());
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
            var list = List.of(CorporateViewModel.of(
                    null,
                    null,
                    null,
                    null,
                    true,
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
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            ));

            assertEquals(0, viewCorporateInteractor.filter(list).size());
        }

        @DisplayName("filter : 300日以前の提出日のみを表示する")
        @Test
        void configCorporateSize() {
            var list = List.of(CorporateViewModel.of(
                    null,
                    null,
                    LocalDate.parse("2020-07-04"),
                    null,
                    true,
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
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            ));

            assertEquals(0, viewCorporateInteractor.filter(list).size());
        }

        @DisplayName("filter : 割安度が120%以上を表示する")
        @Test
        void configDiscountRate() {
            var list = List.of(CorporateViewModel.of(
                    null,
                    null,
                    LocalDate.parse("2021-07-04"),
                    null,
                    true,
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
                    null,
                    null,
                    null,
                    BigDecimal.valueOf(119),
                    null,
                    null
            ));

            assertEquals(0, viewCorporateInteractor.filter(list).size());
        }

        @DisplayName("filter : 標準偏差が外れ値となっていたら除外する")
        @Test
        void configOutlierOfStandardDeviation() {
            var list = List.of(CorporateViewModel.of(
                    null,
                    null,
                    LocalDate.parse("2021-07-04"),
                    null,
                    true,
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
                    BigDecimal.valueOf(10000),
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
                    null,
                    null,
                    BigDecimal.valueOf(120),
                    null,
                    null
            ));

            assertEquals(0, viewCorporateInteractor.filter(list).size());
        }

        @DisplayName("filter : 最新企業価値がマイナスの場合は除外する")
        @Test
        void corporateValue_isMinus() {
            var list = List.of(CorporateViewModel.of(
                    null,
                    null,
                    LocalDate.parse("2021-07-04"),
                    null,
                    true,
                    BigDecimal.valueOf(-1),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    BigDecimal.valueOf(100),
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
                    null,
                    null,
                    BigDecimal.valueOf(120),
                    null,
                    null
            ));

            assertEquals(0, viewCorporateInteractor.filter(list).size());
        }

        @DisplayName("filter : 最新企業価値が平均より低い場合は除外する")
        @Test
        void corporateValue_isAboveAverage() {
            var list = List.of(CorporateViewModel.of(
                    null,
                    null,
                    LocalDate.parse("2021-07-04"),
                    null,
                    true,
                    BigDecimal.valueOf(1000),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    BigDecimal.valueOf(10000),
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(0.5),
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
                    BigDecimal.valueOf(120),
                    null,
                    null
            ));

            assertEquals(0, viewCorporateInteractor.filter(list).size());
        }

        @DisplayName("filter : 変動係数が0.6未満であること")
        @Test
        void configCoefficientOfVariation1() {
            var list = List.of(CorporateViewModel.of(
                    null,
                    null,
                    LocalDate.parse("2021-07-04"),
                    null,
                    true,
                    BigDecimal.valueOf(100),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    BigDecimal.valueOf(10),
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(0.5),
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
                    BigDecimal.valueOf(120),
                    null,
                    null
            ));

            assertEquals(1, viewCorporateInteractor.filter(list).size());
        }

        @DisplayName("filter : 変動係数が0.6以上でも最新企業価値が高ければOK")
        @Test
        void configCoefficientOfVariation2() {
            var list = List.of(CorporateViewModel.of(
                    null,
                    null,
                    LocalDate.parse("2021-07-04"),
                    null,
                    true,
                    BigDecimal.valueOf(10000),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    BigDecimal.valueOf(5000),
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(0.7),
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
                    BigDecimal.valueOf(120),
                    null,
                    null
            ));

            assertEquals(1, viewCorporateInteractor.filter(list).size());
        }

        @DisplayName("filter : 株価予想が存在する場合、最新株価より高ければOK")
        @Test
        void forecastStock1() {
            var list = List.of(CorporateViewModel.of(
                    null,
                    null,
                    LocalDate.parse("2021-07-04"),
                    null,
                    true,
                    BigDecimal.valueOf(10000),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    BigDecimal.valueOf(5000),
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(0.7),
                    null,
                    null,
                    BigDecimal.valueOf(100),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    BigDecimal.valueOf(120),
                    null,
                    BigDecimal.valueOf(200)
            ));

            assertEquals(1, viewCorporateInteractor.filter(list).size());
        }

        @DisplayName("filter : 株価予想が存在する場合、株価予想と最新株価との差が100以上であればOK")
        @Test
        void forecastStock2() {
            var list = List.of(CorporateViewModel.of(
                    null,
                    null,
                    LocalDate.parse("2021-07-04"),
                    null,
                    true,
                    BigDecimal.valueOf(10000),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    BigDecimal.valueOf(5000),
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(0.7),
                    null,
                    null,
                    BigDecimal.valueOf(10),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    BigDecimal.valueOf(120),
                    null,
                    BigDecimal.valueOf(100)
            ));

            assertEquals(0, viewCorporateInteractor.filter(list).size());
        }

        @DisplayName("filter : 表示に必要な最低限の情報があるもののみ表示する")
        @Test
        void present() {
            var list = List.of(CorporateViewModel.of(
                    null,
                    null,
                    LocalDate.parse("2021-07-04"),
                    null,
                    true,
                    BigDecimal.valueOf(10000),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    BigDecimal.valueOf(10),
                    BigDecimal.valueOf(100),
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
                    null,
                    BigDecimal.valueOf(120),
                    null,
                    null
            ));

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
        return CorporateViewModel.of(
                "code",
                "name",
                null,
                null,
                true,
                BigDecimal.TEN,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                BigDecimal.TEN,
                BigDecimal.TEN,
                BigDecimal.TEN,
                BigDecimal.TEN,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                BigDecimal.TEN,
                BigDecimal.TEN,
                null,
                null
        );
    }

    private CorporateViewModel defaultCorporateViewModel(String code) {
        return CorporateViewModel.of(
                code,
                "name",
                null,
                null,
                true,
                BigDecimal.TEN,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                BigDecimal.TEN,
                BigDecimal.TEN,
                BigDecimal.TEN,
                BigDecimal.TEN,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                BigDecimal.TEN,
                BigDecimal.TEN,
                null,
                null
        );
    }
}