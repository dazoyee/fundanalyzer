package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.domain.domain.dao.view.CorporateViewDao;
import github.com.ioridazo.fundanalyzer.domain.domain.dao.view.EdinetListViewDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.DocumentTypeCode;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ViewSpecificationTest {

    private CorporateViewDao corporateViewDao;
    private EdinetListViewDao edinetListViewDao;
    private DocumentSpecification documentSpecification;
    private StockSpecification stockSpecification;

    private ViewSpecification viewSpecification;

    @BeforeEach
    void setUp() {
        corporateViewDao = Mockito.mock(CorporateViewDao.class);
        edinetListViewDao = Mockito.mock(EdinetListViewDao.class);
        documentSpecification = Mockito.mock(DocumentSpecification.class);
        stockSpecification = Mockito.mock(StockSpecification.class);

        viewSpecification = Mockito.spy(new ViewSpecification(
                corporateViewDao,
                edinetListViewDao,
                documentSpecification,
                stockSpecification
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
        Stock stock = defaultStock();
        AnalysisResult analysisResult = analysisResult();
        CorporateValue corporateValue = defaultCorporateValue();
        IndicatorValue indicatorValue = indicatorValue();

        @DisplayName("generateCorporateView : 割安値を算出する")
        @Test
        void calculateDiscountValue_isPresent() {
            var averageInfo = new AverageInfo();
            averageInfo.setYear(AverageInfo.Year.ALL);
            averageInfo.setAverageCorporateValue(BigDecimal.valueOf(2132.0512495));
            corporateValue.setAverageInfoList(List.of(averageInfo));

            when(stockSpecification.findStock(company)).thenReturn(stock);
            when(documentSpecification.latestDocument(company)).thenReturn(Optional.of(defaultDocument()));

            var actual = viewSpecification.generateCorporateView(company, analysisResult, corporateValue, indicatorValue);
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
            when(documentSpecification.latestDocument(company)).thenReturn(Optional.of(defaultDocument()));

            var actual = viewSpecification.generateCorporateView(company, analysisResult, corporateValue, indicatorValue);
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
            when(documentSpecification.latestDocument(company)).thenReturn(Optional.of(defaultDocument()));

            var actual = viewSpecification.generateCorporateView(company, analysisResult, corporateValue, indicatorValue);
            assertEquals(BigDecimal.valueOf(213.205), actual.getAllDiscountRate());
        }

        @DisplayName("generateCorporateView : 割安度を算出できないときはnull")
        @Test
        void calculateDiscountRate_isEmpty() {
            var averageInfo = new AverageInfo();
            averageInfo.setYear(AverageInfo.Year.ALL);
            averageInfo.setAverageCorporateValue(null);
            corporateValue.setAverageInfoList(List.of(averageInfo));

            when(documentSpecification.latestDocument(company)).thenReturn(Optional.of(defaultDocument()));
            when(stockSpecification.findStock(company)).thenReturn(stock);

            var actual = viewSpecification.generateCorporateView(company, analysisResult, corporateValue, indicatorValue);
            assertNull(actual.getAllDiscountRate());
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
}