package github.com.ioridazo.fundanalyzer.domain.service;

import github.com.ioridazo.fundanalyzer.domain.bean.BrandDetailCorporateViewBean;
import github.com.ioridazo.fundanalyzer.domain.bean.BrandDetailViewBean;
import github.com.ioridazo.fundanalyzer.domain.bean.CorporateViewBean;
import github.com.ioridazo.fundanalyzer.domain.bean.CorporateViewDao;
import github.com.ioridazo.fundanalyzer.domain.bean.EdinetDetailViewBean;
import github.com.ioridazo.fundanalyzer.domain.bean.EdinetListViewBean;
import github.com.ioridazo.fundanalyzer.domain.bean.EdinetListViewDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.IndustryDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.AnalysisResultDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.MinkabuDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.StockPriceDao;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Company;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Industry;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.AnalysisResult;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Document;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.StockPrice;
import github.com.ioridazo.fundanalyzer.domain.service.logic.BrandDetailCorporateViewLogic;
import github.com.ioridazo.fundanalyzer.domain.service.logic.CorporateViewLogic;
import github.com.ioridazo.fundanalyzer.domain.service.logic.EdinetDetailViewLogic;
import github.com.ioridazo.fundanalyzer.domain.service.logic.EdinetListViewLogic;
import github.com.ioridazo.fundanalyzer.proxy.slack.SlackProxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ViewServiceTest {

    private SlackProxy slackProxy;
    private CorporateViewLogic corporateViewLogic;
    private EdinetListViewLogic edinetListViewLogic;
    private BrandDetailCorporateViewLogic brandDetailCompanyViewLogic;
    private EdinetDetailViewLogic edinetDetailViewLogic;
    private IndustryDao industryDao;
    private CompanyDao companyDao;
    private DocumentDao documentDao;
    private AnalysisResultDao analysisResultDao;
    private StockPriceDao stockPriceDao;
    private MinkabuDao minkabuDao;
    private CorporateViewDao corporateViewDao;
    private EdinetListViewDao edinetListViewDao;

    private ViewService service;

    @BeforeEach
    void before() {
        slackProxy = Mockito.mock(SlackProxy.class);
        corporateViewLogic = Mockito.mock(CorporateViewLogic.class);
        edinetListViewLogic = Mockito.mock(EdinetListViewLogic.class);
        brandDetailCompanyViewLogic = Mockito.mock(BrandDetailCorporateViewLogic.class);
        edinetDetailViewLogic = Mockito.mock(EdinetDetailViewLogic.class);
        industryDao = Mockito.mock(IndustryDao.class);
        companyDao = Mockito.mock(CompanyDao.class);
        documentDao = Mockito.mock(DocumentDao.class);
        analysisResultDao = Mockito.mock(AnalysisResultDao.class);
        stockPriceDao = Mockito.mock(StockPriceDao.class);
        minkabuDao = Mockito.mock(MinkabuDao.class);
        corporateViewDao = Mockito.mock(CorporateViewDao.class);
        edinetListViewDao = Mockito.mock(EdinetListViewDao.class);

        service = Mockito.spy(new ViewService(
                slackProxy,
                corporateViewLogic,
                edinetListViewLogic,
                brandDetailCompanyViewLogic,
                edinetDetailViewLogic,
                industryDao,
                companyDao,
                documentDao,
                analysisResultDao,
                stockPriceDao,
                minkabuDao,
                corporateViewDao,
                edinetListViewDao
        ));
    }

    @Nested
    class corporateView {

        @DisplayName("updateCorporateView : 表示リストに格納する処理を確認する")
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
            var createdAt = LocalDateTime.of(2020, 10, 17, 18, 15);

            when(companyDao.selectAll()).thenReturn(List.of(company));
            when(industryDao.selectByName("銀行業")).thenReturn(new Industry(2, "銀行業", null));
            when(industryDao.selectByName("保険業")).thenReturn(new Industry(3, "保険業", null));
            when(corporateViewLogic.corporateViewOf(company)).thenReturn(new CorporateViewBean(
                    "code",
                    "会社名",
                    LocalDate.parse("2019-10-11"),
                    BigDecimal.valueOf(2100),
                    BigDecimal.valueOf(2000),
                    BigDecimal.valueOf(10),
                    BigDecimal.valueOf(0.1),
                    BigDecimal.valueOf(900),
                    LocalDate.parse("2020-10-11"),
                    BigDecimal.valueOf(1000),
                    BigDecimal.valueOf(1000),
                    BigDecimal.valueOf(200),
                    BigDecimal.valueOf(3),
                    BigDecimal.valueOf(2000),
                    createdAt,
                    createdAt
            ));

            assertDoesNotThrow(() -> service.updateCorporateView());

            verify(corporateViewDao, times(1)).insert(new CorporateViewBean(
                    "code",
                    "会社名",
                    LocalDate.parse("2019-10-11"),
                    BigDecimal.valueOf(2100),
                    BigDecimal.valueOf(2000),
                    BigDecimal.valueOf(10),
                    BigDecimal.valueOf(0.1),
                    BigDecimal.valueOf(900),
                    LocalDate.parse("2020-10-11"),
                    BigDecimal.valueOf(1000),
                    BigDecimal.valueOf(1000),
                    BigDecimal.valueOf(200),
                    BigDecimal.valueOf(3),
                    BigDecimal.valueOf(2000),
                    createdAt,
                    createdAt
            ));
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
    class notice {

        @DisplayName("notice : slack通知処理されることを確認する")
        @Test
        void notice_ok() {
            var documentTypeCode = "120";
            var submitDate = LocalDate.parse("2020-11-01");
            var documentList = List.of(Document.builder()
                    .edinetCode("ec")
                    .documentTypeCode("120")
                    .submitDate(LocalDate.parse("2020-11-01"))
                    .scrapedNumberOfShares("0")
                    .scrapedBs("0")
                    .scrapedPl("0")
                    .removed("0")
                    .build()
            );
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

            when(documentDao.selectByTypeAndSubmitDate(documentTypeCode, submitDate)).thenReturn(documentList);
            when(companyDao.selectAll()).thenReturn(List.of(company));
            when(industryDao.selectByName("銀行業")).thenReturn(new Industry(2, "銀行業", null));
            when(industryDao.selectByName("保険業")).thenReturn(new Industry(3, "保険業", null));
            when(edinetListViewLogic.counter(LocalDate.parse("2020-11-01"), 1L, documentList, List.of(company)))
                    .thenReturn(new EdinetListViewBean(
                            LocalDate.parse("2020-11-01"),
                            null,
                            0L,
                            0L,
                            0L,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null
                    ));

            assertDoesNotThrow(() -> service.notice(submitDate));

            verify(slackProxy, times(1)).sendMessage("g.c.i.f.domain.service.ViewService.processing.notice.info", submitDate, 0L);
            verify(slackProxy, times(0)).sendMessage(eq("g.c.i.f.domain.service.ViewService.processing.notice.warn"), any());
        }
    }

    @Nested
    class edinetListView {

        @DisplayName("updateEdinetList : 処理状況をアップデートする")
        @Test
        void updateEdinetList_ok() {
            var documentTypeCode = "120";
            var documentList = List.of(Document.builder()
                            .edinetCode("edinetCode")
                            .documentTypeCode("120")
                            .submitDate(LocalDate.parse("2020-10-10"))
                            .scrapedNumberOfShares("0")
                            .scrapedBs("0")
                            .scrapedPl("0")
                            .removed("0")
                            .build(),
                    Document.builder()
                            .edinetCode("ec")
                            .documentTypeCode("120")
                            .submitDate(LocalDate.parse("2020-10-11"))
                            .scrapedNumberOfShares("0")
                            .scrapedBs("0")
                            .scrapedPl("0")
                            .removed("0")
                            .build()
            );
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
            var createdAt = LocalDateTime.of(2020, 10, 17, 18, 15);

            when(documentDao.selectByDocumentTypeCode(documentTypeCode)).thenReturn(documentList);
            when(companyDao.selectAll()).thenReturn(List.of(company));
            when(industryDao.selectByName("銀行業")).thenReturn(new Industry(2, "銀行業", null));
            when(industryDao.selectByName("保険業")).thenReturn(new Industry(3, "保険業", null));
            when(edinetListViewLogic.counter(
                    LocalDate.parse("2020-10-10"),
                    1L,
                    documentList,
                    List.of(company))).thenReturn(new EdinetListViewBean(
                    LocalDate.parse("2020-10-10"),
                    1L,
                    1L,
                    0L,
                    0L,
                    "",
                    "",
                    1L,
                    0L,
                    createdAt,
                    createdAt
            ));
            when(edinetListViewLogic.counter(
                    LocalDate.parse("2020-10-11"),
                    1L,
                    documentList,
                    List.of(company))).thenReturn(new EdinetListViewBean(
                    LocalDate.parse("2020-10-11"),
                    1L,
                    0L,
                    0L,
                    0L,
                    "",
                    "",
                    0L,
                    1L,
                    createdAt,
                    createdAt
            ));

            assertDoesNotThrow(() -> service.updateEdinetListView("120"));

            verify(edinetListViewDao, times(1)).insert(new EdinetListViewBean(
                    LocalDate.parse("2020-10-11"),
                    1L,
                    0L,
                    0L,
                    0L,
                    "",
                    "",
                    0L,
                    1L,
                    createdAt,
                    createdAt
            ));
            verify(edinetListViewDao, times(1)).insert(new EdinetListViewBean(
                    LocalDate.parse("2020-10-10"),
                    1L,
                    1L,
                    0L,
                    0L,
                    "",
                    "",
                    1L,
                    0L,
                    createdAt,
                    createdAt
            ));
        }

        @DisplayName("updateEdinetList : 対象提出日の処理状況をアップデートする")
        @Test
        void updateEdinetListView_ok() {
            var documentTypeCode = "120";
            var submitDate = LocalDate.parse("2020-12-14");
            var documentList = List.of(Document.builder()
                    .edinetCode("edinetCode")
                    .documentTypeCode("120")
                    .submitDate(LocalDate.parse("2020-12-14"))
                    .scrapedNumberOfShares("0")
                    .scrapedBs("0")
                    .scrapedPl("0")
                    .removed("0")
                    .build()
            );
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

            var createdAt = LocalDateTime.of(2020, 10, 17, 18, 15);

            when(documentDao.selectByTypeAndSubmitDate(documentTypeCode, submitDate)).thenReturn(documentList);
            when(companyDao.selectAll()).thenReturn(List.of(company));
            when(industryDao.selectByName("銀行業")).thenReturn(new Industry(2, "銀行業", null));
            when(industryDao.selectByName("保険業")).thenReturn(new Industry(3, "保険業", null));
            when(edinetListViewLogic.counter(
                    LocalDate.parse("2020-12-14"),
                    1L,
                    documentList,
                    List.of(company))).thenReturn(new EdinetListViewBean(
                    LocalDate.parse("2020-12-14"),
                    1L,
                    0L,
                    0L,
                    0L,
                    "",
                    "",
                    0L,
                    1L,
                    createdAt,
                    createdAt
            ));

            assertDoesNotThrow(() -> service.updateEdinetListView(documentTypeCode, submitDate));

            verify(edinetListViewDao, times(1)).update(new EdinetListViewBean(
                    LocalDate.parse("2020-12-14"),
                    1L,
                    0L,
                    0L,
                    0L,
                    "",
                    "",
                    0L,
                    1L,
                    createdAt,
                    createdAt
            ));
        }
    }

    @Nested
    class brandDetailView {

        @DisplayName("brandDetailView : 企業ごとの銘柄詳細情報を取得する")
        @Test
        void brandDetailView_ok() {
            var code = "00000";

            when(brandDetailCompanyViewLogic.brandDetailCompanyViewOf(code)).thenReturn(new BrandDetailCorporateViewBean(
                    "code",
                    "name",
                    "industry",
                    "edinetCode",
                    10000,
                    "settlementDate",
                    "per",
                    "pbr",
                    "roe",
                    "numberOfShares",
                    "marketCapitalization",
                    "dividendYield",
                    "shareholderBenefit"
            ));
            when(corporateViewDao.selectByCode(code.substring(0, 4))).thenReturn(new CorporateViewBean(
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
                    BigDecimal.valueOf(1),
                    null,
                    null,
                    null,
                    null
            ));
            when(analysisResultDao.selectByCompanyCode(code)).thenReturn(List.of(new AnalysisResult(
                    null,
                    null,
                    null,
                    BigDecimal.valueOf(100),
                    null
            )));
            when(brandDetailCompanyViewLogic.brandDetailFinancialStatement(code)).thenReturn(List.of(new BrandDetailViewBean.BrandDetailFinancialStatement(
                    LocalDate.parse("2019-01-01"),
                    LocalDate.parse("2019-12-31"),
                    null,
                    List.of(new BrandDetailViewBean.BrandDetailFinancialStatement.BrandDetailFinancialStatementValue(
                            "subject1",
                            4000L
                    ))
            )));
            when(stockPriceDao.selectByCode(code)).thenReturn(List.of(new StockPrice(
                    null,
                    null,
                    null,
                    100.0,
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
            )));

            var actual = service.brandDetailView(code);

            assertAll("BrandDetailViewBean",
                    () -> assertAll("corporate",
                            () -> assertEquals("code", actual.getCorporate().getCode()),
                            () -> assertEquals("name", actual.getCorporate().getName()),
                            () -> assertEquals("industry", actual.getCorporate().getIndustry()),
                            () -> assertEquals("edinetCode", actual.getCorporate().getEdinetCode()),
                            () -> assertEquals(10000, actual.getCorporate().getCapitalStock()),
                            () -> assertEquals("settlementDate", actual.getCorporate().getSettlementDate()),
                            () -> assertEquals("per", actual.getCorporate().getPer()),
                            () -> assertEquals("pbr", actual.getCorporate().getPbr()),
                            () -> assertEquals("roe", actual.getCorporate().getRoe()),
                            () -> assertEquals("numberOfShares", actual.getCorporate().getNumberOfShares()),
                            () -> assertEquals("marketCapitalization", actual.getCorporate().getMarketCapitalization()),
                            () -> assertEquals("dividendYield", actual.getCorporate().getDividendYield()),
                            () -> assertEquals("shareholderBenefit", actual.getCorporate().getShareholderBenefit())
                    ),
                    () -> assertAll("corporateView",
                            () -> assertEquals(BigDecimal.valueOf(1), actual.getCorporateView().getDiscountRate())
                    ),
                    () -> assertAll("analysisResultList",
                            () -> assertEquals(BigDecimal.valueOf(100), actual.getAnalysisResultList().get(0).getCorporateValue())
                    ),
                    () -> assertAll("financialStatement",
                            () -> assertEquals("2019-01-01", actual.getFinancialStatement().get(0).getPeriodStart().toString()),
                            () -> assertEquals("2019-12-31", actual.getFinancialStatement().get(0).getPeriodEnd().toString()),
                            () -> assertAll("pl",
                                    () -> assertEquals("subject1", actual.getFinancialStatement().get(0).getPl().get(0).getSubject()),
                                    () -> assertEquals(4000L, actual.getFinancialStatement().get(0).getPl().get(0).getValue())
                            )
                    ),
                    () -> assertAll("stockPriceList",
                            () -> assertEquals(100.0, actual.getStockPriceList().get(0).getStockPrice())
                    )
            );
        }
    }

    @Nested
    class edinetDetailView {

        @DisplayName("edinetDetailView : 提出日ごとの処理詳細情報を取得する")
        @Test
        void edinetDetailView_ok() {
            var submitDate = LocalDate.parse("2021-01-10");
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
            var edinetDetailViewBean = new EdinetDetailViewBean(
                    new EdinetListViewBean(
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
                    ),
                    List.of()
            );

            when(companyDao.selectAll()).thenReturn(List.of(company));
            when(industryDao.selectByName("銀行業")).thenReturn(new Industry(2, "銀行業", null));
            when(industryDao.selectByName("保険業")).thenReturn(new Industry(3, "保険業", null));
            when(edinetDetailViewLogic.edinetDetailView("120", submitDate, List.of(company)))
                    .thenReturn(edinetDetailViewBean);

            var actual = service.edinetDetailView(submitDate);

            assertEquals(edinetDetailViewBean, actual);
        }
    }
}