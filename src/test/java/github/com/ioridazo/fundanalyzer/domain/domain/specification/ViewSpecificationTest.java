package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.domain.domain.dao.view.CorporateViewDao;
import github.com.ioridazo.fundanalyzer.domain.domain.dao.view.EdinetListViewDao;
import github.com.ioridazo.fundanalyzer.domain.domain.dao.view.ValuationViewDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.DocumentTypeCode;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.StockPriceEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.ValuationEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.view.CorporateViewBean;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.view.EdinetListViewBean;
import github.com.ioridazo.fundanalyzer.domain.value.AnalysisResult;
import github.com.ioridazo.fundanalyzer.domain.value.AverageInfo;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.CorporateValue;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.domain.value.IndicatorValue;
import github.com.ioridazo.fundanalyzer.domain.value.Stock;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.CorporateViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.edinet.EdinetListViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.CompanyValuationViewModel;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ViewSpecificationTest {

    private CorporateViewDao corporateViewDao;
    private EdinetListViewDao edinetListViewDao;
    private CompanySpecification companySpecification;
    private AnalysisResultSpecification analysisResultSpecification;
    private StockSpecification stockSpecification;
    private ValuationSpecification valuationSpecification;

    private ViewSpecification viewSpecification;

    @BeforeEach
    void setUp() {
        corporateViewDao = Mockito.mock(CorporateViewDao.class);
        edinetListViewDao = Mockito.mock(EdinetListViewDao.class);
        companySpecification = Mockito.mock(CompanySpecification.class);
        analysisResultSpecification = Mockito.mock(AnalysisResultSpecification.class);
        stockSpecification = Mockito.mock(StockSpecification.class);
        valuationSpecification = mock(ValuationSpecification.class);

        viewSpecification = Mockito.spy(new ViewSpecification(
                corporateViewDao,
                edinetListViewDao,
                Mockito.mock(ValuationViewDao.class),
                companySpecification,
                Mockito.mock(DocumentSpecification.class),
                analysisResultSpecification,
                stockSpecification,
                Mockito.mock(InvestmentIndicatorSpecification.class),
                valuationSpecification
        ));
    }

    @Nested
    class upsert_CorporateViewModel {

        CorporateViewModel viewModel = new CorporateViewModel();

        @DisplayName("企業情報ビューを更新する")
        @Test
        void update() {
            when(corporateViewDao.selectByCodeAndType(any(), any())).thenReturn(Optional.of(defaultCorporateViewBean()));

            assertDoesNotThrow(() -> viewSpecification.upsert(viewModel));
            verify(corporateViewDao, times(1)).update(any());
            verify(corporateViewDao, times(0)).insert(any());
        }

        @DisplayName("企業情報ビューを登録する")
        @Test
        void insert() {
            assertDoesNotThrow(() -> viewSpecification.upsert(viewModel));
            verify(corporateViewDao, times(0)).update(any());
            verify(corporateViewDao, times(1)).insert(any());
        }
    }

    @Nested
    class upsert_EdinetListViewModel {

        EdinetListViewModel viewModel = EdinetListViewModel.of(
                null,
                1,
                1,
                1,
                1,
                null,
                null,
                1
        );

        @DisplayName("EDINETリストビューを更新する")
        @Test
        void update() {
            when(edinetListViewDao.selectBySubmitDate(any())).thenReturn(Optional.of(defaultEdinetListViewBean()));

            assertDoesNotThrow(() -> viewSpecification.upsert(viewModel));
            verify(edinetListViewDao, times(1)).update(any());
            verify(edinetListViewDao, times(0)).insert(any());
        }

        @DisplayName("EDINETリストビューを登録する")
        @Test
        void insert() {
            assertDoesNotThrow(() -> viewSpecification.upsert(viewModel));
            verify(edinetListViewDao, times(0)).update(any());
            verify(edinetListViewDao, times(1)).insert(any());
        }
    }

    @Nested
    class generateCorporateView {

        Company company = defaultCompany();
        Document document = defaultDocument();
        Stock stock = defaultStock();
        AnalysisResult analysisResult = analysisResult();
        CorporateValue corporateValue = defaultCorporateValue();
        IndicatorValue indicatorValue = indicatorValue();

        @DisplayName("generateCorporateView : 有価証券報告書フラグを算出する")
        @Test
        void isMainReport_isTrue() {
            when(stockSpecification.findStock(company)).thenReturn(stock);
            var actual = viewSpecification.generateCorporateView(company, document, analysisResult, corporateValue, indicatorValue);
            assertTrue(actual.isMainReport());
        }

        @DisplayName("generateCorporateView : 割安値を算出する")
        @Test
        void calculateDiscountValue_isPresent() {
            var averageInfo = new AverageInfo();
            averageInfo.setYear(AverageInfo.Year.ALL);
            averageInfo.setAverageCorporateValue(BigDecimal.valueOf(2132.0512495));
            corporateValue.setAverageInfoList(List.of(averageInfo));

            when(stockSpecification.findStock(company)).thenReturn(stock);

            var actual = viewSpecification.generateCorporateView(company, document, analysisResult, corporateValue, indicatorValue);
            assertEquals(BigDecimal.valueOf(1132.05), actual.getAllDiscountValue());
        }

        @DisplayName("generateCorporateView : 割安値を算出できないときはnull")
        @Test
        void calculateDiscountValue_isEmpty() {
            var averageInfo = new AverageInfo();
            averageInfo.setYear(AverageInfo.Year.ALL);
            averageInfo.setAverageCorporateValue(null);
            corporateValue.setAverageInfoList(List.of(averageInfo));

            when(stockSpecification.findStock(company)).thenReturn(stock);

            var actual = viewSpecification.generateCorporateView(company, document, analysisResult, corporateValue, indicatorValue);
            assertNull(actual.getAllDiscountValue());
        }

        @DisplayName("generateCorporateView : 割安度を算出する")
        @Test
        void calculateDiscountRate_isPresent() {
            var averageInfo = new AverageInfo();
            averageInfo.setYear(AverageInfo.Year.ALL);
            averageInfo.setAverageCorporateValue(BigDecimal.valueOf(2132.0512495));
            corporateValue.setAverageInfoList(List.of(averageInfo));

            when(stockSpecification.findStock(company)).thenReturn(stock);

            var actual = viewSpecification.generateCorporateView(company, document, analysisResult, corporateValue, indicatorValue);
            assertEquals(BigDecimal.valueOf(213.205), actual.getAllDiscountRate());
        }

        @DisplayName("generateCorporateView : 割安度を算出できないときはnull")
        @Test
        void calculateDiscountRate_isEmpty() {
            var averageInfo = new AverageInfo();
            averageInfo.setYear(AverageInfo.Year.ALL);
            averageInfo.setAverageCorporateValue(null);
            corporateValue.setAverageInfoList(List.of(averageInfo));

            when(stockSpecification.findStock(company)).thenReturn(stock);

            var actual = viewSpecification.generateCorporateView(company, document, analysisResult, corporateValue, indicatorValue);
            assertNull(actual.getAllDiscountRate());
        }
    }

    @DisplayName("generateCorporateView : 予想配当利回りを数値に変換できないとき")
    @Test
    void dividendYield() {
        var code = "code";
        var submitDate = LocalDate.parse("2022-01-29");
        var stockPriceEntity = new StockPriceEntity(
                null,
                code,
                submitDate,
                600.0,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "(4.01%)",
                null,
                null,
                null
        );
        var valuation = new ValuationEntity(
                null,
                code,
                submitDate,
                null,
                null,
                BigDecimal.valueOf(500),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                1,
                null
        );

        when(companySpecification.findCompanyByCode(code)).thenReturn(Optional.of(defaultCompany()));
        when(valuationSpecification.findValuationOfSubmitDate(code, submitDate)).thenReturn(Optional.of(valuation));
        when(stockSpecification.findEntityList(code)).thenReturn(List.of(stockPriceEntity));
        when(analysisResultSpecification.findAnalysisResult(1))
                .thenReturn(Optional.of(new AnalysisResultEntity(
                        null,
                        null,
                        null,
                        BigDecimal.TEN,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                )));


        var actual = viewSpecification.generateCompanyValuationView(valuation);
        assertNull(actual.dividendYield());
    }

    @DisplayName("generateIndustryValuationView : 業種による平均の評価結果を取得する")
    @Test
    void generateIndustryValuationView() {
        var actual = viewSpecification.generateIndustryValuationView(
                "name",
                List.of(
                        companyValuationViewModel(LocalDate.parse("2022-07-09"), BigDecimal.valueOf(100), BigDecimal.valueOf(1.1), null),
                        companyValuationViewModel(LocalDate.parse("2022-07-10"), BigDecimal.valueOf(-20), BigDecimal.valueOf(1.01), BigDecimal.valueOf(2.05))
                )
        );
        assertAll(
                () -> assertEquals("name", actual.getName()),
                () -> assertEquals(BigDecimal.valueOf(4000, 2), actual.getDifferenceFromSubmitDate()),
                () -> assertEquals(BigDecimal.valueOf(106, 2), actual.getSubmitDateRatio()),
                () -> assertEquals(BigDecimal.valueOf(205, 2), actual.getGrahamIndex()),
                () -> assertEquals(2, actual.getCount())
        );
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
                DocumentTypeCode.DTC_120,
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
                false
        );
    }

    private AnalysisResult analysisResult() {
        return new AnalysisResult(
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private CorporateValue defaultCorporateValue() {
        return CorporateValue.of();
    }

    private Stock defaultStock() {
        return Stock.of(
                null,
                null,
                null,
                BigDecimal.valueOf(1000.0),
                null,
                List.of(),
                List.of()
        );
    }

    private IndicatorValue indicatorValue() {
        return new IndicatorValue(
                null,
                null,
                null,
                null,
                null
        );
    }

    private CorporateViewBean defaultCorporateViewBean() {
        return CorporateViewBean.of();
    }

    private EdinetListViewBean defaultEdinetListViewBean() {
        return new EdinetListViewBean(
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
        );
    }

    private CompanyValuationViewModel companyValuationViewModel(
            LocalDate targetDate, BigDecimal differenceFromSubmitDate, BigDecimal submitDateRatio, BigDecimal grahamIndex) {
        return new CompanyValuationViewModel(
                "code",
                null,
                targetDate,
                null,
                grahamIndex,
                null,
                null,
                null,
                null,
                null,
                differenceFromSubmitDate,
                submitDateRatio,
                null,
                null,
                null
        );
    }
}