package github.com.ioridazo.fundanalyzer.domain.logic.view;

import github.com.ioridazo.fundanalyzer.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.entity.BsEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.PlEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Company;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Document;
import github.com.ioridazo.fundanalyzer.domain.logic.analysis.AnalysisLogic;
import github.com.ioridazo.fundanalyzer.domain.logic.view.bean.EdinetListViewBean;
import github.com.ioridazo.fundanalyzer.domain.logic.view.bean.EdinetListViewDao;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerCalculateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

class EdinetDetailViewLogicTest {

    private AnalysisLogic analysisLogic;
    private CompanyDao companyDao;
    private DocumentDao documentDao;
    private EdinetListViewDao edinetListViewDao;

    private EdinetDetailViewLogic logic;

    @BeforeEach
    void setUp() {
        analysisLogic = Mockito.mock(AnalysisLogic.class);
        companyDao = Mockito.mock(CompanyDao.class);
        documentDao = Mockito.mock(DocumentDao.class);
        edinetListViewDao = Mockito.mock(EdinetListViewDao.class);

        logic = new EdinetDetailViewLogic(
                analysisLogic, companyDao, documentDao, edinetListViewDao
        );
    }

    @Nested
    class edinetDetailView {

        @DisplayName("edinetDetailView : 対象提出日の未処理書類リストを取得する")
        @Test
        void edinetDetailView_ok() {
            var documentTypeCode = "120";
            var submitDate = LocalDate.parse("2020-12-14");
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
            var allTargetCompanies = List.of(company);
            var period = LocalDate.parse("2020-12-31");
            var document = Document.builder()
                    .documentId("documentId")
                    .edinetCode("edinetCode")
                    .period(period)
                    .scrapedBs("9")
                    .scrapedPl("1")
                    .scrapedNumberOfShares("1")
                    .removed("0")
                    .build();
            var documentList = List.of(document);
            var edinetListViewBean = new EdinetListViewBean(
                    LocalDate.parse("2020-12-14"),
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

            when(documentDao.selectByTypeAndSubmitDate(documentTypeCode, submitDate)).thenReturn(documentList);
            when(edinetListViewDao.selectBySubmitDate(submitDate)).thenReturn(edinetListViewBean);
            when(companyDao.selectByEdinetCode("edinetCode")).thenReturn(Optional.of(company));
            when(analysisLogic.bsValues(company, BsEnum.TOTAL_CURRENT_ASSETS, period)).thenReturn(1000L);
            when(analysisLogic.bsValues(company, BsEnum.TOTAL_INVESTMENTS_AND_OTHER_ASSETS, period)).thenReturn(2000L);
            when(analysisLogic.bsValues(company, BsEnum.TOTAL_CURRENT_LIABILITIES, period)).thenReturn(3000L);
            when(analysisLogic.bsValues(company, BsEnum.TOTAL_FIXED_LIABILITIES, period)).thenReturn(4000L);
            when(analysisLogic.plValues(company, PlEnum.OPERATING_PROFIT, period)).thenReturn(5000L);
            when(analysisLogic.nsValue(company, period)).thenReturn(6000L);

            var actual = logic.edinetDetailView(documentTypeCode, submitDate, allTargetCompanies);

            assertAll("EdinetDetailViewBean",
                    () -> assertEquals(edinetListViewBean, actual.getEdinetListView()),
                    () -> assertEquals(company, actual.getDocumentDetailList().get(0).getCompany()),
                    () -> assertEquals(document, actual.getDocumentDetailList().get(0).getDocument()),
                    () -> assertAll("ValuesForAnalysis",
                            () -> assertEquals(1000L, actual.getDocumentDetailList().get(0).getValues().getTotalCurrentAssets()),
                            () -> assertEquals(2000L, actual.getDocumentDetailList().get(0).getValues().getTotalInvestmentsAndOtherAssets()),
                            () -> assertEquals(3000L, actual.getDocumentDetailList().get(0).getValues().getTotalCurrentLiabilities()),
                            () -> assertEquals(4000L, actual.getDocumentDetailList().get(0).getValues().getTotalFixedLiabilities()),
                            () -> assertEquals(5000L, actual.getDocumentDetailList().get(0).getValues().getOperatingProfit()),
                            () -> assertEquals(6000L, actual.getDocumentDetailList().get(0).getValues().getNumberOfShares())
                    )
            );
        }

        @DisplayName("edinetDetailView : スクレイピング処理に失敗しているときはnullで返すようにする")
        @Test
        void edinetDetailView_value_is_null() {
            var documentTypeCode = "120";
            var submitDate = LocalDate.parse("2020-12-14");
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
            var allTargetCompanies = List.of(company);
            var period = LocalDate.parse("2020-12-31");
            var document = Document.builder()
                    .documentId("documentId")
                    .edinetCode("edinetCode")
                    .period(period)
                    .scrapedBs("9")
                    .scrapedPl("1")
                    .scrapedNumberOfShares("1")
                    .removed("0")
                    .build();
            var documentList = List.of(document);
            var edinetListViewBean = new EdinetListViewBean(
                    LocalDate.parse("2020-12-14"),
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

            when(documentDao.selectByTypeAndSubmitDate(documentTypeCode, submitDate)).thenReturn(documentList);
            when(edinetListViewDao.selectBySubmitDate(submitDate)).thenReturn(edinetListViewBean);
            when(companyDao.selectByEdinetCode("edinetCode")).thenReturn(Optional.of(company));
            when(analysisLogic.bsValues(company, BsEnum.TOTAL_CURRENT_ASSETS, period)).thenReturn(1000L);
            when(analysisLogic.bsValues(company, BsEnum.TOTAL_INVESTMENTS_AND_OTHER_ASSETS, period)).thenReturn(2000L);
            when(analysisLogic.bsValues(company, BsEnum.TOTAL_CURRENT_LIABILITIES, period)).thenReturn(3000L);
            when(analysisLogic.bsValues(company, BsEnum.TOTAL_FIXED_LIABILITIES, period)).thenReturn(4000L);
            when(analysisLogic.plValues(company, PlEnum.OPERATING_PROFIT, period)).thenThrow(FundanalyzerCalculateException.class);
            when(analysisLogic.nsValue(company, period)).thenThrow(FundanalyzerCalculateException.class);

            var actual = logic.edinetDetailView(documentTypeCode, submitDate, allTargetCompanies);

            assertAll("EdinetDetailViewBean",
                    () -> assertEquals(edinetListViewBean, actual.getEdinetListView()),
                    () -> assertEquals(document, actual.getDocumentDetailList().get(0).getDocument()),
                    () -> assertAll("ValuesForAnalysis",
                            () -> assertEquals(1000L, actual.getDocumentDetailList().get(0).getValues().getTotalCurrentAssets()),
                            () -> assertEquals(2000L, actual.getDocumentDetailList().get(0).getValues().getTotalInvestmentsAndOtherAssets()),
                            () -> assertEquals(3000L, actual.getDocumentDetailList().get(0).getValues().getTotalCurrentLiabilities()),
                            () -> assertEquals(4000L, actual.getDocumentDetailList().get(0).getValues().getTotalFixedLiabilities()),
                            () -> assertNull(actual.getDocumentDetailList().get(0).getValues().getOperatingProfit()),
                            () -> assertNull(actual.getDocumentDetailList().get(0).getValues().getNumberOfShares())
                    )
            );
        }
    }
}