package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.client.slack.SlackClient;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
import org.mockito.ArgumentCaptor;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerBadDataException;
import github.com.ioridazo.fundanalyzer.domain.value.FinanceValue;
import github.com.ioridazo.fundanalyzer.domain.value.AnalysisResult;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.FinancialStatementEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.MinkabuEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.StockPriceEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.AnalysisResultSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CompanySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CorporateActionSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.DocumentSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.FinancialStatementSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.StockSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.ViewSpecification;
import github.com.ioridazo.fundanalyzer.domain.service.InvestmentIndicatorReconciliationService;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.domain.value.IndicatorValue;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
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
    private InvestmentIndicatorReconciliationService investmentIndicatorReconciliationService;
    private ViewSpecification viewSpecification;
    private SlackClient slackClient;
    private CorporateActionSpecification corporateActionSpecification;

    private ViewCorporateInteractor viewCorporateInteractor;

    @BeforeEach
    void setUp() {
        companySpecification = Mockito.mock(CompanySpecification.class);
        documentSpecification = Mockito.mock(DocumentSpecification.class);
        financialStatementSpecification = Mockito.mock(FinancialStatementSpecification.class);
        analysisResultSpecification = Mockito.mock(AnalysisResultSpecification.class);
        stockSpecification = Mockito.mock(StockSpecification.class);
        investmentIndicatorReconciliationService = Mockito.mock(InvestmentIndicatorReconciliationService.class);
        viewSpecification = Mockito.mock(ViewSpecification.class);
        slackClient = Mockito.mock(SlackClient.class);
        corporateActionSpecification = Mockito.mock(CorporateActionSpecification.class);
        when(corporateActionSpecification.adjustToBasis(any(), any(), any(), any(), eq(true)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(corporateActionSpecification.findActions(any())).thenReturn(List.of());
        lenient().when(investmentIndicatorReconciliationService.reconcile(any(), any(), any())).thenReturn(List.of());
        lenient().when(investmentIndicatorReconciliationService.reconcilePrecomputed(any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        viewCorporateInteractor = Mockito.spy(new ViewCorporateInteractor(
                Mockito.mock(AnalyzeInteractor.class),
                companySpecification,
                documentSpecification,
                financialStatementSpecification,
                analysisResultSpecification,
                stockSpecification,
                investmentIndicatorReconciliationService,
                viewSpecification,
                slackClient,
                corporateActionSpecification
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
    @DisplayName("viewFavorite / viewStar メソッド")
    class ViewFavoriteAndStar {

        @DisplayName("viewFavorite : 4文字プレフィックス一致の企業のみ返す")
        @Test
        void viewFavorite_filtersByCodePrefix() {
            when(companySpecification.findFavoriteCompanies()).thenReturn(List.of(
                    new Company("11112", null, null, null, null, null, null, null, null, true, false, true),
                    new Company("3333", null, null, null, null, null, null, null, null, true, false, true)
            ));
            when(viewSpecification.findAllCorporateView()).thenReturn(List.of(
                    corporateViewWithSubmitDate("1111", LocalDate.parse("2026-03-25")),
                    corporateViewWithSubmitDate("2222", LocalDate.parse("2026-03-24")),
                    corporateViewWithSubmitDate("3333", LocalDate.parse("2026-03-23"))
            ));

            final List<CorporateViewModel> actual = viewCorporateInteractor.viewFavorite();

            assertEquals(List.of("1111", "3333"), actual.stream().map(CorporateViewModel::getCode).toList());
        }

        @DisplayName("viewStar : 4文字プレフィックス一致の企業のみ返す")
        @Test
        void viewStar_filtersByCodePrefix() {
            when(companySpecification.findStarCompanies()).thenReturn(List.of(
                    new Company("11112", null, null, null, null, null, null, null, null, false, true, true),
                    new Company("3333", null, null, null, null, null, null, null, null, false, true, true)
            ));
            when(viewSpecification.findAllCorporateView()).thenReturn(List.of(
                    corporateViewWithSubmitDate("1111", LocalDate.parse("2026-03-25")),
                    corporateViewWithSubmitDate("2222", LocalDate.parse("2026-03-24")),
                    corporateViewWithSubmitDate("3333", LocalDate.parse("2026-03-23"))
            ));

            final List<CorporateViewModel> actual = viewCorporateInteractor.viewStar();

            assertEquals(List.of("1111", "3333"), actual.stream().map(CorporateViewModel::getCode).toList());
        }
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
            when(documentSpecification.findLatestDocument(company)).thenReturn(Optional.of(defaultDocument()));
        }

        @DisplayName("viewCorporateDetail : 企業情報詳細ビューを取得する")
        @Test
        void of() {
            AnalysisResultEntity analysisResultEntity = new AnalysisResultEntity(
                    null,
                    null,
                    LocalDate.parse("2021-01-01"),
                    BigDecimal.TEN,
                    null,
                    "120",
                    null,
                    null,
                    null,
                    null
            );
            FinancialStatementEntity bsEntity = new FinancialStatementEntity(
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
            FinancialStatementEntity plEntity = new FinancialStatementEntity(
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
            when(financialStatementSpecification.parseBsSubjectValue(List.of(bsEntity, plEntity)))
                    .thenReturn(List.of(FinancialStatementValueViewModel.of("bs", 100L)));
            when(financialStatementSpecification.parsePlSubjectValue(List.of(bsEntity, plEntity)))
                    .thenReturn(List.of(FinancialStatementValueViewModel.of("pl", 100L)));

            CorporateDetailViewModel actual = viewCorporateInteractor.viewCorporateDetail(inputData);

            assertAll(
                    () -> assertAll(
                            () -> assertEquals("code", actual.getCompany().code(), "company.code"),
                            () -> assertEquals("name", actual.getCompany().name(), "company.name"),
                            () -> assertEquals("edinetCode", actual.getCompany().edinetCode())
                    ),
                    () -> assertAll(
                            () -> assertEquals("code", actual.getCorporate().getCode(), "corporate.code"),
                            () -> assertEquals("name", actual.getCorporate().getName(), "corporate.name"),
                            () -> assertEquals(0, BigDecimal.TEN.compareTo(actual.getCorporate().getLatestCorporateValue())),
                            () -> assertEquals(0, BigDecimal.TEN.compareTo(actual.getCorporate().getAverageCorporateValueToDisplay())),
                            () -> assertEquals(0, BigDecimal.TEN.compareTo(actual.getCorporate().getStandardDeviationToDisplay())),
                            () -> assertEquals(0, BigDecimal.TEN.compareTo(actual.getCorporate().getDiscountValueToDisplay())),
                            () -> assertEquals(0, BigDecimal.TEN.compareTo(actual.getCorporate().getDiscountRateToDisplay()))
                    ),
                    () -> assertAll(
                            () -> assertEquals(LocalDate.parse("2021-01-01"), actual.getAnalysisResultList().get(0).documentPeriod()),
                            () -> assertEquals(0, BigDecimal.TEN.compareTo(actual.getAnalysisResultList().get(0).corporateValue())),
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
            // N+1解消: 全件を1回取得しメモリ内グルーピングするため findByKeyPerCompany は呼ばれない
            verify(financialStatementSpecification, times(1)).findByCompany(company);
            verify(financialStatementSpecification, never()).findByKeyPerCompany(any(), any());
        }

        @DisplayName("viewCorporateDetail : 確定した株式分割日のみ splitDates に入る")
        @Test
        void includeConfirmedSplitDates() {
            when(analysisResultSpecification.displayTargetList(company, targetTypeCodes)).thenReturn(List.of());
            when(financialStatementSpecification.findByCompany(company)).thenReturn(List.of());
            when(corporateActionSpecification.findActions(company.code())).thenReturn(List.of(
                    new CorporateActionSpecification.CorporateAction(
                            LocalDate.parse("2024-04-01"),
                            BigDecimal.valueOf(2),
                            true
                    ),
                    new CorporateActionSpecification.CorporateAction(
                            LocalDate.parse("2024-03-01"),
                            BigDecimal.valueOf(3),
                            false
                    ),
                    new CorporateActionSpecification.CorporateAction(
                            LocalDate.parse("2024-05-01"),
                            BigDecimal.valueOf(5),
                            true
                    )
            ));

            CorporateDetailViewModel actual = viewCorporateInteractor.viewCorporateDetail(inputData);

            assertAll(
                    () -> assertEquals(2, actual.splitDates().size()),
                    () -> assertEquals(LocalDate.parse("2024-04-01"), actual.splitDates().get(0)),
                    () -> assertEquals(LocalDate.parse("2024-05-01"), actual.splitDates().get(1))
            );
        }

        @DisplayName("viewCorporateDetail : 有報基準かつ確定アクションのみで株価OHLCを補正する")
        @Test
        void adjustStockPriceToLatestDocumentBasis() {
            StockPriceEntity stockPriceEntity = new StockPriceEntity(
                    null,
                    "code",
                    LocalDate.parse("2024-06-10"),
                    100.0d,
                    90.0d,
                    110.0d,
                    80.0d,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    LocalDateTime.parse("2024-06-10T00:00:00"),
                    LocalDateTime.parse("2024-06-10T00:00:00")
            );
            Stock adjustedStock = Stock.of(
                    null,
                    null,
                    null,
                    null,
                    null,
                    List.of(stockPriceEntity),
                    List.<MinkabuEntity>of()
            );
            Document basisDocument = new Document(
                    null,
                    null,
                    null,
                    "edinetCode",
                    null,
                    LocalDate.parse("2024-06-20"),
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

            when(stockSpecification.findStock(company)).thenReturn(adjustedStock);
            when(documentSpecification.findLatestDocument(company)).thenReturn(Optional.of(basisDocument));
            // N+1解消後は事前取得した actions を adjustToBasisWithActions に渡す。actions が非空のとき補正が行われる
            when(corporateActionSpecification.findActions(company.code())).thenReturn(List.of(
                    new CorporateActionSpecification.CorporateAction(
                            LocalDate.parse("2024-06-15"),
                            BigDecimal.valueOf(2),
                            true
                    )
            ));
            when(corporateActionSpecification.adjustToBasisWithActions(any(), any(), any(), any(), eq(true)))
                    .thenAnswer(invocation -> ((BigDecimal) invocation.getArgument(0)).multiply(BigDecimal.valueOf(2)));

            CorporateDetailViewModel actual = viewCorporateInteractor.viewCorporateDetail(inputData);

            assertAll(
                    () -> assertEquals(1, actual.getStockPriceList().size()),
                    () -> assertEquals(200.0d, actual.getStockPriceList().get(0).stockPrice(), 0.0001d),
                    () -> assertEquals(180.0d, actual.getStockPriceList().get(0).openingPrice(), 0.0001d),
                    () -> assertEquals(220.0d, actual.getStockPriceList().get(0).highPrice(), 0.0001d),
                    () -> assertEquals(160.0d, actual.getStockPriceList().get(0).lowPrice(), 0.0001d)
            );
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

            CorporateDetailViewModel actual = viewCorporateInteractor.viewCorporateDetail(inputData, Target.MAIN);

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

            CorporateDetailViewModel actual = viewCorporateInteractor.viewCorporateDetail(inputData, Target.MAIN);

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

            CorporateDetailViewModel actual = viewCorporateInteractor.viewCorporateDetail(inputData, Target.MAIN);

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

            CorporateDetailViewModel actual = viewCorporateInteractor.viewCorporateDetail(inputData, Target.ALL);

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

            CorporateDetailViewModel actual = viewCorporateInteractor.viewCorporateDetail(inputData, Target.MAIN);

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

            CorporateDetailViewModel actual = viewCorporateInteractor.viewCorporateDetail(inputData);

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

            CorporateDetailViewModel actual = viewCorporateInteractor.viewCorporateDetail(inputData);

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

            CorporateDetailViewModel actual = viewCorporateInteractor.viewCorporateDetail(inputData);

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

        @DisplayName("updateView : BPS/EPS/ROE/ROA は永続列ではなく財務諸表値から都度計算して渡す")
        @Test
        void all_computesIndicatorsFromFinanceValue() {
            var entity = analysisResultEntity();
            var analysisDocument = analysisDocument();
            var financeValue = FinanceValue.of(
                    null, null, 1200L, null, null, 0L, 800L, null, 150L, 10L);

            when(companySpecification.inquiryAllTargetCompanies()).thenReturn(List.of(company));
            when(documentSpecification.findLatestDocument(company)).thenReturn(Optional.of(document));
            when(analysisResultSpecification.findLatestAnalysisResult("code")).thenReturn(Optional.of(entity));
            when(documentSpecification.findDocument("documentId")).thenReturn(analysisDocument);
            when(financialStatementSpecification.getFinanceValue(analysisDocument)).thenReturn(financeValue);
            when(viewSpecification.generateCorporateView(eq(company), eq(document), any(), any(), any())).thenReturn(corporateViewModel);

            assertDoesNotThrow(() -> viewCorporateInteractor.updateView());

            final ArgumentCaptor<AnalysisResult> captor = ArgumentCaptor.forClass(AnalysisResult.class);
            verify(viewSpecification, times(1)).generateCorporateView(eq(company), eq(document), captor.capture(), any(), any());
            final AnalysisResult actual = captor.getValue();
            // 永続列（bps=1）ではなく都度計算値（800/10=80）が渡ること
            assertEquals(0, BigDecimal.valueOf(80).compareTo(actual.getBps().orElseThrow()));
            assertEquals(0, BigDecimal.valueOf(15).compareTo(actual.getEps().orElseThrow()));
            // 係数依存値は永続値を凍結したまま
            assertEquals(BigDecimal.valueOf(999), actual.getCorporateValue());
            assertEquals(BigDecimal.valueOf(555), actual.getRimValue().orElseThrow());
        }

        @DisplayName("updateView : 同一の最新分析結果に対する書類・財務諸表値の解決は1回のみ（二重計算しない）")
        @Test
        void all_resolvesDocumentAndFinanceValueOnlyOnce() {
            var entity = analysisResultEntity();
            var analysisDocument = analysisDocument();
            var financeValue = FinanceValue.of(
                    null, null, 1200L, null, null, 0L, 800L, null, 150L, 10L);
            var stockPriceEntity = new StockPriceEntity(
                    null,
                    "code",
                    LocalDate.parse("2024-07-01"),
                    100.0d,
                    null, null, null, null, null, null, null, null, null, null, null, null,
                    LocalDateTime.parse("2024-07-01T00:00:00"),
                    LocalDateTime.parse("2024-07-01T00:00:00")
            );
            var expectedIndicatorValue = IndicatorValue.of();

            when(companySpecification.inquiryAllTargetCompanies()).thenReturn(List.of(company));
            when(documentSpecification.findLatestDocument(company)).thenReturn(Optional.of(document));
            when(analysisResultSpecification.findLatestAnalysisResult("code")).thenReturn(Optional.of(entity));
            when(documentSpecification.findDocument("documentId")).thenReturn(analysisDocument);
            when(financialStatementSpecification.getFinanceValue(analysisDocument)).thenReturn(financeValue);
            when(stockSpecification.findLatestStock("code")).thenReturn(Optional.of(stockPriceEntity));
            when(investmentIndicatorReconciliationService
                    .reconcilePrecomputed(eq("code"), eq(stockPriceEntity), eq(entity), any(AnalysisResult.class)))
                    .thenReturn(Optional.of(expectedIndicatorValue));
            when(viewSpecification.generateCorporateView(eq(company), eq(document), any(), any(), any())).thenReturn(corporateViewModel);

            assertDoesNotThrow(() -> viewCorporateInteractor.updateView());

            // documentSpecification.findDocument / financialStatementSpecification.getFinanceValue は
            // resolveAnalysisResult 側の1回のみで、resolveIndicatorValue 側からは再実行されないこと
            verify(documentSpecification, times(1)).findDocument("documentId");
            verify(financialStatementSpecification, times(1)).getFinanceValue(analysisDocument);
            // 一覧突合版の reconcile ではなく、事前計算済み版の reconcilePrecomputed が使われること
            verify(investmentIndicatorReconciliationService, never()).reconcile(any(), any(), any());

            final ArgumentCaptor<AnalysisResult> analysisResultCaptor = ArgumentCaptor.forClass(AnalysisResult.class);
            verify(viewSpecification, times(1))
                    .generateCorporateView(eq(company), eq(document), analysisResultCaptor.capture(), any(), eq(expectedIndicatorValue));
            final ArgumentCaptor<AnalysisResult> precomputedCaptor = ArgumentCaptor.forClass(AnalysisResult.class);
            verify(investmentIndicatorReconciliationService, times(1))
                    .reconcilePrecomputed(eq("code"), eq(stockPriceEntity), eq(entity), precomputedCaptor.capture());
            // resolveAnalysisResult で解決した同一の AnalysisResult インスタンスが resolveIndicatorValue にも渡されること
            assertSame(analysisResultCaptor.getValue(), precomputedCaptor.getValue());
        }

        @DisplayName("updateView : 都度計算で不正データ例外が発生した企業はスキップし他社の更新は継続する")
        @Test
        void all_continuesWhenFinanceValueThrowsBadData() {
            var badCompany = company;
            var okCompany = new Company(
                    "code2", "name2", null, null, "edinetCode2",
                    null, null, null, null, false, false, true);
            var entity = analysisResultEntity();
            var analysisDocument = analysisDocument();
            var okViewModel = defaultCorporateViewModel("code2");

            when(companySpecification.inquiryAllTargetCompanies()).thenReturn(List.of(badCompany, okCompany));
            when(documentSpecification.findLatestDocument(badCompany)).thenReturn(Optional.of(document));
            when(documentSpecification.findLatestDocument(okCompany)).thenReturn(Optional.of(document));
            when(analysisResultSpecification.findLatestAnalysisResult("code")).thenReturn(Optional.of(entity));
            when(analysisResultSpecification.findLatestAnalysisResult("code2")).thenReturn(Optional.empty());
            when(documentSpecification.findDocument("documentId")).thenReturn(analysisDocument);
            when(financialStatementSpecification.getFinanceValue(analysisDocument))
                    .thenThrow(new FundanalyzerBadDataException("bad data", new RuntimeException()));
            when(viewSpecification.generateCorporateView(eq(okCompany), eq(document), any(), any(), any())).thenReturn(okViewModel);

            assertDoesNotThrow(() -> viewCorporateInteractor.updateView());

            verify(viewSpecification, times(0)).generateCorporateView(eq(badCompany), eq(document), any(), any(), any());
            verify(viewSpecification, times(1)).upsert(okViewModel);
        }

        private AnalysisResultEntity analysisResultEntity() {
            return new AnalysisResultEntity(
                    1,
                    "code",
                    LocalDate.parse("2024-03-31"),
                    BigDecimal.valueOf(999),
                    BigDecimal.valueOf(555),
                    "120",
                    null,
                    LocalDate.parse("2024-06-30"),
                    "documentId",
                    null
            );
        }

        private Document analysisDocument() {
            return new Document(
                    "documentId",
                    null,
                    null,
                    "edinetCode",
                    null,
                    LocalDate.parse("2024-06-30"),
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
