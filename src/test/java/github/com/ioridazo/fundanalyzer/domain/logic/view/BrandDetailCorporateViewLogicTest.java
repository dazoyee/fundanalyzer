package github.com.ioridazo.fundanalyzer.domain.logic.view;

import github.com.ioridazo.fundanalyzer.domain.dao.master.BsSubjectDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.IndustryDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.PlSubjectDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.FinancialStatementDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.StockPriceDao;
import github.com.ioridazo.fundanalyzer.domain.entity.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.master.BsSubjectEntity;
import github.com.ioridazo.fundanalyzer.domain.entity.master.CompanyEntity;
import github.com.ioridazo.fundanalyzer.domain.entity.master.IndustryEntity;
import github.com.ioridazo.fundanalyzer.domain.entity.master.PlSubjectEntity;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.FinancialStatementEntity;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.StockPriceEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class BrandDetailCorporateViewLogicTest {

    private IndustryDao industryDao;
    private CompanyDao companyDao;
    private BsSubjectDao bsSubjectDao;
    private PlSubjectDao plSubjectDao;
    private FinancialStatementDao financialStatementDao;
    private StockPriceDao stockPriceDao;

    private BrandDetailCorporateViewLogic logic;

    @BeforeEach
    void setUp() {
        industryDao = Mockito.mock(IndustryDao.class);
        companyDao = Mockito.mock(CompanyDao.class);
        bsSubjectDao = Mockito.mock(BsSubjectDao.class);
        plSubjectDao = Mockito.mock(PlSubjectDao.class);
        financialStatementDao = Mockito.mock(FinancialStatementDao.class);
        stockPriceDao = Mockito.mock(StockPriceDao.class);

        logic = new BrandDetailCorporateViewLogic(
                industryDao,
                companyDao,
                bsSubjectDao,
                plSubjectDao,
                financialStatementDao,
                stockPriceDao
        );
    }

    @DisplayName("brandDetailCompanyViewOf : 銘柄詳細に表示する企業情報を取得する")
    @Test
    void brandDetailCompanyViewOf_ok() {
        var code = "9999";

        when(companyDao.selectByCode(code)).thenReturn(Optional.of(new CompanyEntity(
                "code",
                "name",
                1,
                "edinetCode",
                "",
                "",
                10000,
                "settlementDate",
                null,
                null
        )));
        when(industryDao.selectById(1)).thenReturn(new IndustryEntity(1, "industry", null));
        when(stockPriceDao.selectByCode(code)).thenReturn(List.of(
                new StockPriceEntity(
                        1,
                        "code",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "per",
                        "pbr",
                        "roe",
                        "numberOfShares",
                        "marketCapitalization",
                        "dividendYield",
                        "shareholderBenefit",
                        null,
                        null
                )
        ));

        var actual = logic.brandDetailCompanyViewOf(code);

        assertAll("BrandDetailCorporateViewBean",
                () -> assertEquals("code", actual.getCode()),
                () -> assertEquals("name", actual.getName()),
                () -> assertEquals("industry", actual.getIndustry()),
                () -> assertEquals("edinetCode", actual.getEdinetCode()),
                () -> assertEquals(10000, actual.getCapitalStock()),
                () -> assertEquals("settlementDate", actual.getSettlementDate()),
                () -> assertEquals("per", actual.getPer()),
                () -> assertEquals("pbr", actual.getPbr()),
                () -> assertEquals("roe", actual.getRoe()),
                () -> assertEquals("numberOfShares", actual.getNumberOfShares()),
                () -> assertEquals("marketCapitalization", actual.getMarketCapitalization()),
                () -> assertEquals("dividendYield", actual.getDividendYield()),
                () -> assertEquals("shareholderBenefit", actual.getShareholderBenefit())
        );
    }

    @DisplayName("brandDetailFinancialStatement : 銘柄詳細に表示する財務諸表を取得する")
    @Test
    void brandDetailFinancialStatement_ok() {
        var code = "9999";

        when(financialStatementDao.selectByCode(code)).thenReturn(List.of(
                new FinancialStatementEntity(
                        1,
                        "9999",
                        null,
                        FinancialStatementEnum.BALANCE_SHEET.toValue(),
                        "1",
                        LocalDate.parse("2020-01-01"),
                        LocalDate.parse("2020-12-31"),
                        1000L,
                        "120",
                        LocalDate.parse("2021-03-20"),
                        null,
                        null
                ),
                new FinancialStatementEntity(
                        2,
                        "9999",
                        null,
                        FinancialStatementEnum.BALANCE_SHEET.toValue(),
                        "2",
                        LocalDate.parse("2020-01-01"),
                        LocalDate.parse("2020-12-31"),
                        2000L,
                        "120",
                        LocalDate.parse("2021-03-20"),
                        null,
                        null
                ),
                new FinancialStatementEntity(
                        3,
                        "9999",
                        null,
                        FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT.toValue(),
                        "1",
                        LocalDate.parse("2020-01-01"),
                        LocalDate.parse("2020-12-31"),
                        3000L,
                        "120",
                        LocalDate.parse("2021-03-20"),
                        null,
                        null
                ),
                new FinancialStatementEntity(
                        4,
                        "9999",
                        null,
                        FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT.toValue(),
                        "1",
                        LocalDate.parse("2019-01-01"),
                        LocalDate.parse("2019-12-31"),
                        4000L,
                        "120",
                        LocalDate.parse("2020-03-20"),
                        null,
                        null
                ),
                new FinancialStatementEntity(
                        5,
                        "9999",
                        null,
                        FinancialStatementEnum.BALANCE_SHEET.toValue(),
                        "1",
                        LocalDate.parse("2020-01-01"),
                        LocalDate.parse("2020-12-31"),
                        1500L,
                        "130",
                        LocalDate.parse("2021-04-20"),
                        null,
                        null
                )
        ));
        when(bsSubjectDao.selectById("1")).thenReturn(new BsSubjectEntity("1", "1", "1", "subject1"));
        when(bsSubjectDao.selectById("2")).thenReturn(new BsSubjectEntity("2", "1", "2", "subject2"));
        when(plSubjectDao.selectById("1")).thenReturn(new PlSubjectEntity("1", "1", "1", "subject1"));

        var actual = logic.brandDetailFinancialStatement(code);

        assertAll("financialStatement",
                () -> assertAll("BrandDetailFinancialStatement",
                        () -> assertEquals("2020-01-01", actual.get(0).getPeriodStart().toString()),
                        () -> assertEquals("2020-12-31", actual.get(0).getPeriodEnd().toString()),
                        () -> assertEquals("有価証券報告書", actual.get(0).getDocumentTypeName()),
                        () -> assertAll("bs",
                                () -> assertEquals("subject1", actual.get(0).getBs().get(0).getSubject()),
                                () -> assertEquals(1000L, actual.get(0).getBs().get(0).getValue()),
                                () -> assertEquals("subject2", actual.get(0).getBs().get(1).getSubject()),
                                () -> assertEquals(2000L, actual.get(0).getBs().get(1).getValue())
                        ),
                        () -> assertAll("pl",
                                () -> assertEquals("subject1", actual.get(0).getPl().get(0).getSubject()),
                                () -> assertEquals(3000L, actual.get(0).getPl().get(0).getValue())
                        )
                ),
                () -> assertAll("BrandDetailFinancialStatement",
                        () -> assertEquals("2020-01-01", actual.get(1).getPeriodStart().toString()),
                        () -> assertEquals("2020-12-31", actual.get(1).getPeriodEnd().toString()),
                        () -> assertEquals("訂正有価証券報告書", actual.get(1).getDocumentTypeName()),
                        () -> assertAll("pl",
                                () -> assertEquals("subject1", actual.get(1).getBs().get(0).getSubject()),
                                () -> assertEquals(1500L, actual.get(1).getBs().get(0).getValue())
                        )
                ),
                () -> assertAll("BrandDetailFinancialStatement",
                        () -> assertEquals("2019-01-01", actual.get(2).getPeriodStart().toString()),
                        () -> assertEquals("2019-12-31", actual.get(2).getPeriodEnd().toString()),
                        () -> assertEquals("有価証券報告書", actual.get(2).getDocumentTypeName()),
                        () -> assertAll("bs",
                                () -> assertEquals("subject1", actual.get(2).getPl().get(0).getSubject()),
                                () -> assertEquals(4000L, actual.get(2).getPl().get(0).getValue())
                        )
                )
        );
    }
}