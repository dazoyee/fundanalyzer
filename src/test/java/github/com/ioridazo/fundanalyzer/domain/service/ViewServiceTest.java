package github.com.ioridazo.fundanalyzer.domain.service;

import github.com.ioridazo.fundanalyzer.domain.bean.CorporateViewBean;
import github.com.ioridazo.fundanalyzer.domain.bean.CorporateViewDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.IndustryDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.AnalysisResultDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.StockPriceDao;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Company;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Industry;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.AnalysisResult;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Document;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.StockPrice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.util.Pair;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ViewServiceTest {

    private IndustryDao industryDao;
    private CompanyDao companyDao;
    private DocumentDao documentDao;
    private AnalysisResultDao analysisResultDao;
    private StockPriceDao stockPriceDao;
    private CorporateViewDao corporateViewDao;

    private ViewService service;

    @BeforeEach
    void before() {
        industryDao = Mockito.mock(IndustryDao.class);
        companyDao = Mockito.mock(CompanyDao.class);
        documentDao = Mockito.mock(DocumentDao.class);
        analysisResultDao = Mockito.mock(AnalysisResultDao.class);
        stockPriceDao = Mockito.mock(StockPriceDao.class);
        corporateViewDao = mock(CorporateViewDao.class);

        service = Mockito.spy(new ViewService(
                industryDao,
                companyDao,
                documentDao,
                analysisResultDao,
                stockPriceDao,
                corporateViewDao
        ));
    }

    @Nested
    class updateCorporateView {

        @DisplayName("viewCompany : 表示リストに格納する処理を確認する")
        @Test
        void updateCorporateView_ok() {
            var company = new Company(
                    "code",
                    "会社名",
                    1,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            var submitDate = LocalDate.parse("2019-10-11");
            var createdAt = LocalDateTime.of(2020, 10, 17, 18, 15);

            when(companyDao.selectAll()).thenReturn(List.of(company));
            when(industryDao.selectByName("銀行業")).thenReturn(new Industry(2, "銀行業", null));
            when(industryDao.selectByName("保険業")).thenReturn(new Industry(3, "保険業", null));
            doReturn(Optional.of(submitDate)).when(service).latestSubmitDate(company);
            doReturn(ViewService.CorporateValue.of(
                    BigDecimal.valueOf(2000), BigDecimal.valueOf(10), BigDecimal.valueOf(3)
            )).when(service).corporateValue(company);
            doReturn(ViewService.StockPriceValue.of(
                    BigDecimal.valueOf(900), LocalDate.parse("2020-10-11"), BigDecimal.valueOf(1000))
            ).when(service).stockPrice(company, submitDate);
            doReturn(Pair.of(Optional.of(BigDecimal.valueOf(1000)), Optional.of(BigDecimal.valueOf(200))))
                    .when(service).discountValue(any(), any());
            when(service.nowLocalDateTime()).thenReturn(createdAt);

            assertDoesNotThrow(() -> service.updateCorporateView());

            verify(corporateViewDao, times(1)).insert(new CorporateViewBean(
                    "code",
                    "会社名",
                    LocalDate.parse("2019-10-11"),
                    BigDecimal.valueOf(2000),
                    BigDecimal.valueOf(10),
                    BigDecimal.valueOf(900),
                    LocalDate.parse("2020-10-11"),
                    BigDecimal.valueOf(1000),
                    BigDecimal.valueOf(1000),
                    BigDecimal.valueOf(200),
                    BigDecimal.valueOf(3),
                    createdAt,
                    createdAt
            ));
        }

        @DisplayName("latestSubmitDate : 対象の書類が存在したときの処理を確認する")
        @Test
        void latestSubmitDate_ok() {
            var company = new Company(
                    "code",
                    "会社名",
                    1,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            var document = Document.builder()
                    .edinetCode("edinetCode")
                    .submitDate(LocalDate.parse("2020-10-11"))
                    .build();

            when(documentDao.selectByDocumentTypeCode("120")).thenReturn(List.of(document));

            assertEquals(LocalDate.parse("2020-10-11"), service.latestSubmitDate(company).orElseThrow());
        }

        @DisplayName("latestSubmitDate : 対象の書類が存在しないときの処理を確認する")
        @Test
        void latestSubmitDate_nothing() {
            var company = new Company(
                    "code",
                    "会社名",
                    1,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            var document = Document.builder()
                    .edinetCode("edinetCode2")
                    .submitDate(LocalDate.parse("2020-10-11"))
                    .build();

            when(documentDao.selectByDocumentTypeCode("120")).thenReturn(List.of(document));

            assertNull(service.latestSubmitDate(company).orElse(null));
        }

        @DisplayName("corporateValue : 企業価値等が算出されることを確認する")
        @Test
        void corporateValue_ok() {
            var company = new Company(
                    "code",
                    "会社名",
                    1,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            var analysisResult1 = new AnalysisResult(1, "code", null, BigDecimal.valueOf(1100), null);
            var analysisResult2 = new AnalysisResult(2, "code", null, BigDecimal.valueOf(900), null);

            when(analysisResultDao.selectByCompanyCode("code")).thenReturn(List.of(analysisResult1, analysisResult2));

            var actual = service.corporateValue(company);

            assertAll("CorporateValue",
                    () -> assertEquals(BigDecimal.valueOf(100000, 2), actual.getCorporateValue().orElseThrow()),
                    () -> assertEquals(BigDecimal.valueOf(100.0), actual.getStandardDeviation().orElseThrow()),
                    () -> assertEquals(BigDecimal.valueOf(2), actual.getCountYear().orElseThrow())
            );
        }

        @DisplayName("corporateValue : 企業価値に関する値が存在しないときの処理を確認する")
        @Test
        void corporateValue_nothing() {
            var company = new Company(
                    "code",
                    "会社名",
                    1,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            when(analysisResultDao.selectByCompanyCode("code")).thenReturn(List.of());

            var actual = service.corporateValue(company);

            assertAll("CorporateValue",
                    () -> assertNull(actual.getCorporateValue().orElse(null)),
                    () -> assertNull(actual.getStandardDeviation().orElse(null)),
                    () -> assertNull(actual.getCountYear().orElse(null))
            );
        }

        @DisplayName("stockPrice : 株価取得できることを確認する")
        @Test
        void stockPrice_ok() {
            var company = new Company(
                    "code",
                    "会社名",
                    1,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            var submitDate = LocalDate.parse("2020-10-08");
            var stockPriceOfSubmitDate = new StockPrice(
                    null,
                    null,
                    LocalDate.parse("2020-10-08"),
                    (double) 1000,
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
            );
            var latestStockPrice = new StockPrice(
                    null,
                    null,
                    LocalDate.parse("2020-10-09"),
                    (double) 1001,
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
            );

            when(stockPriceDao.selectByCode("code")).thenReturn(List.of(stockPriceOfSubmitDate, latestStockPrice));

            var actual = service.stockPrice(company, submitDate);

            assertAll("StockPriceValue",
                    () -> assertEquals(BigDecimal.valueOf(1000.0), actual.getStockPriceOfSubmitDate().orElseThrow()),
                    () -> assertEquals(LocalDate.parse("2020-10-09"), actual.getImportDate().orElseThrow()),
                    () -> assertEquals(BigDecimal.valueOf(1001.0), actual.getLatestStockPrice().orElseThrow())
            );
        }


        @DisplayName("stockPrice : 株価取得できないときの処理を確認する")
        @Test
        void stockPrice_nothing() {
            var company = new Company(
                    "code",
                    "会社名",
                    1,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            var submitDate = LocalDate.parse("2020-10-08");

            when(stockPriceDao.selectByCode("code")).thenReturn(List.of());

            var actual = service.stockPrice(company, submitDate);

            assertAll("StockPriceValue",
                    () -> assertNull(actual.getStockPriceOfSubmitDate().orElse(null)),
                    () -> assertNull(actual.getImportDate().orElse(null)),
                    () -> assertNull(actual.getLatestStockPrice().orElse(null))
            );
        }

        @DisplayName("discountValue : 割安値が算出されることを確認する")
        @Test
        void discountValue_ok() {
            var corporateValue = BigDecimal.valueOf(2132.0512495);
            var latestStockPrice = BigDecimal.valueOf(1000.0);

            var actual = service.discountValue(corporateValue, latestStockPrice);

            assertEquals(BigDecimal.valueOf(1132.05), actual.getFirst().orElseThrow());
            assertEquals(BigDecimal.valueOf(213.205), actual.getSecond().orElseThrow());
        }

        @DisplayName("discountValue : 割安値を算出しないときの処理を確認する")
        @Test
        void discountValue_nothing() {
            var corporateValue = nullable(BigDecimal.class);
            var latestStockPrice = nullable(BigDecimal.class);

            var actual = service.discountValue(corporateValue, latestStockPrice);

            assertNull(actual.getFirst().orElse(null));
            assertNull(actual.getSecond().orElse(null));
        }
    }

    @Nested
    class corporateValue {

        @DisplayName("corporateValue : 企業価値を算出するロジックを確認する")
        @Test
        void corporateValue_scale() {
            var company = new Company(
                    "code",
                    "会社名",
                    1,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            var analysisResult1 = new AnalysisResult(1, "code", null, BigDecimal.valueOf(500.250515), null);

            when(analysisResultDao.selectByCompanyCode("code")).thenReturn(List.of(analysisResult1));

            var actual = service.corporateValue(company);

            assertAll("CorporateValue",
                    () -> assertEquals(BigDecimal.valueOf(500.25), actual.getCorporateValue().orElseThrow()),
                    () -> assertEquals(BigDecimal.valueOf(0, 1), actual.getStandardDeviation().orElseThrow()),
                    () -> assertEquals(BigDecimal.valueOf(1), actual.getCountYear().orElseThrow())
            );
        }

        @DisplayName("corporateValue : 企業価値を算出するロジックを確認する")
        @Test
        void corporateValue_sd() {
            var company = new Company(
                    "code",
                    "会社名",
                    1,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            var analysisResult1 = new AnalysisResult(1, "code", null, BigDecimal.valueOf(500.250515), null);
            var analysisResult2 = new AnalysisResult(2, "code", null, BigDecimal.valueOf(418.02101), null);

            when(analysisResultDao.selectByCompanyCode("code")).thenReturn(List.of(analysisResult1, analysisResult2));

            var actual = service.corporateValue(company);

            assertAll("CorporateValue",
                    () -> assertEquals(BigDecimal.valueOf(459.14), actual.getCorporateValue().orElseThrow()),
                    () -> assertEquals(BigDecimal.valueOf(41.115), actual.getStandardDeviation().orElseThrow()),
                    () -> assertEquals(BigDecimal.valueOf(2), actual.getCountYear().orElseThrow())
            );
        }
    }

    @Nested
    class companyUpdated {

        @DisplayName("companyUpdated : 会社情報の更新日を取得することを確認する")
        @Test
        void companyUpdated_ok() {
            when(companyDao.selectAll()).thenReturn(List.of(new Company(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    LocalDateTime.of(2020, 10, 11, 15, 25)
            )));

            assertEquals("2020/10/11 15:25:00", service.companyUpdated());
        }

        @DisplayName("companyUpdated : 会社情報が存在しないときの処理を確認する")
        @Test
        void companyUpdated_nothing() {
            when(companyDao.selectAll()).thenReturn(List.of());
            assertEquals("null", service.companyUpdated());
        }
    }

    @Nested
    class edinetList {

        @DisplayName("edinetListAll : 処理状況の表示ができることを確認する")
        @Test
        void edinetListAll_ok() {
            var documentTypeCode = "120";
            final var document1 = Document.builder()
                    .edinetCode("edinetCode")
                    .documentTypeCode("120")
                    .submitDate(LocalDate.parse("2020-10-10"))
                    .scrapedNumberOfShares("0")
                    .scrapedBs("0")
                    .scrapedPl("0")
                    .removed("0")
                    .build();
            final var document2 = Document.builder()
                    .edinetCode("ec")
                    .documentTypeCode("120")
                    .submitDate(LocalDate.parse("2020-10-11"))
                    .scrapedNumberOfShares("0")
                    .scrapedBs("0")
                    .scrapedPl("0")
                    .removed("0")
                    .build();
            var company = new Company(
                    "code",
                    "会社名",
                    1,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            when(documentDao.selectByDocumentTypeCode(documentTypeCode)).thenReturn(List.of(document1, document2));
            when(companyDao.selectAll()).thenReturn(List.of(company));
            when(industryDao.selectByName("銀行業")).thenReturn(new Industry(2, "銀行業", null));
            when(industryDao.selectByName("保険業")).thenReturn(new Industry(3, "保険業", null));

            final var actual = service.edinetListAll(documentTypeCode);

            assertAll("EdinetListViewBean",
                    () -> assertAll(
                            () -> assertEquals(LocalDate.parse("2020-10-11"), actual.get(0).getSubmitDate()),
                            () -> assertEquals(Long.valueOf(1), actual.get(0).getCountAll()),
                            () -> assertEquals(Long.valueOf(0), actual.get(0).getCountTarget()),
                            () -> assertEquals(Long.valueOf(0), actual.get(0).getCountScraped()),
                            () -> assertEquals(Long.valueOf(0), actual.get(0).getCountAnalyzed()),
                            () -> assertEquals("", actual.get(0).getNotAnalyzedCode()),
                            () -> assertEquals("", actual.get(0).getCantScrapedCode()),
                            () -> assertEquals(Long.valueOf(0), actual.get(0).getCountNotScraped()),
                            () -> assertEquals(Long.valueOf(1), actual.get(0).getCountNotTarget())),
                    () -> assertAll(
                            () -> assertEquals(LocalDate.parse("2020-10-10"), actual.get(1).getSubmitDate()),
                            () -> assertEquals(Long.valueOf(1), actual.get(1).getCountAll()),
                            () -> assertEquals(Long.valueOf(1), actual.get(1).getCountTarget()),
                            () -> assertEquals(Long.valueOf(0), actual.get(1).getCountScraped()),
                            () -> assertEquals(Long.valueOf(0), actual.get(1).getCountAnalyzed()),
                            () -> assertEquals("", actual.get(1).getNotAnalyzedCode()),
                            () -> assertEquals("", actual.get(1).getCantScrapedCode()),
                            () -> assertEquals(Long.valueOf(1), actual.get(1).getCountNotScraped()),
                            () -> assertEquals(Long.valueOf(0), actual.get(1).getCountNotTarget()))
            );
        }
    }
}