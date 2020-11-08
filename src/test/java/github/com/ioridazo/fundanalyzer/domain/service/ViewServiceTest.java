package github.com.ioridazo.fundanalyzer.domain.service;

import github.com.ioridazo.fundanalyzer.domain.bean.CorporateViewBean;
import github.com.ioridazo.fundanalyzer.domain.bean.CorporateViewDao;
import github.com.ioridazo.fundanalyzer.domain.bean.EdinetListViewBean;
import github.com.ioridazo.fundanalyzer.domain.bean.EdinetListViewDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.IndustryDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Company;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Industry;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Document;
import github.com.ioridazo.fundanalyzer.domain.service.logic.CorporateViewLogic;
import github.com.ioridazo.fundanalyzer.domain.service.logic.EdinetListViewLogic;
import github.com.ioridazo.fundanalyzer.slack.SlackProxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
    private IndustryDao industryDao;
    private CompanyDao companyDao;
    private DocumentDao documentDao;
    private CorporateViewDao corporateViewDao;
    private EdinetListViewDao edinetListViewDao;

    private ViewService service;

    @BeforeEach
    void before() {
        slackProxy = Mockito.mock(SlackProxy.class);
        corporateViewLogic = Mockito.mock(CorporateViewLogic.class);
        edinetListViewLogic = Mockito.mock(EdinetListViewLogic.class);
        industryDao = Mockito.mock(IndustryDao.class);
        companyDao = Mockito.mock(CompanyDao.class);
        documentDao = Mockito.mock(DocumentDao.class);
        corporateViewDao = Mockito.mock(CorporateViewDao.class);
        edinetListViewDao = Mockito.mock(EdinetListViewDao.class);

        service = Mockito.spy(new ViewService(
                slackProxy,
                corporateViewLogic,
                edinetListViewLogic,
                industryDao,
                companyDao,
                documentDao,
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
            var document1 = Document.builder()
                    .edinetCode("ec")
                    .documentTypeCode("120")
                    .submitDate(LocalDate.parse("2020-11-01"))
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

            when(documentDao.selectByTypeAndSubmitDate(documentTypeCode, submitDate)).thenReturn(List.of(document1));
            when(companyDao.selectAll()).thenReturn(List.of(company));
            when(industryDao.selectByName("銀行業")).thenReturn(new Industry(2, "銀行業", null));
            when(industryDao.selectByName("保険業")).thenReturn(new Industry(3, "保険業", null));

            assertDoesNotThrow(() -> service.notice(submitDate));

            verify(slackProxy, times(1)).sendMessage("g.c.i.f.domain.service.ViewService.processing.notice.info", submitDate, 0L);
            verify(slackProxy, times(0)).sendMessage(eq("g.c.i.f.domain.service.ViewService.processing.notice.warn"), any());
        }
    }

    @Nested
    class edinetListView {

        @DisplayName("updateEdinetList : 処理状況の表示ができることを確認する")
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
    }
}