package github.com.ioridazo.fundanalyzer.domain.logic.company;

import github.com.ioridazo.fundanalyzer.csv.CsvCommander;
import github.com.ioridazo.fundanalyzer.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.IndustryDao;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Company;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Industry;
import github.com.ioridazo.fundanalyzer.domain.logic.company.bean.EdinetCsvResultBean;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerFileException;
import github.com.ioridazo.fundanalyzer.file.FileOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompanyLogicTest {

    private FileOperator fileOperator;
    private CsvCommander csvCommander;
    private IndustryDao industryDao;
    private CompanyDao companyDao;

    private CompanyLogic companyLogic;

    @BeforeEach
    void setUp() {
        this.fileOperator = Mockito.mock(FileOperator.class);
        this.csvCommander = Mockito.mock(CsvCommander.class);
        this.industryDao = Mockito.mock(IndustryDao.class);
        this.companyDao = Mockito.mock(CompanyDao.class);

        companyLogic = Mockito.spy(new CompanyLogic(
                fileOperator,
                csvCommander,
                industryDao,
                companyDao
        ));
    }

    @Nested
    class readFile {

        @Test
        void readFile_ok() throws IOException {
            var fileName = "file.zip";
            var fileInputPath = "C:/test/input";
            var fileOutputPath = "C:/test/output";

            doNothing().when(fileOperator).decodeZipFile(any(), any());
            doReturn(List.of(new EdinetCsvResultBean())).when(companyLogic).readFile(fileOutputPath);

            assertDoesNotThrow(() -> companyLogic.readFile(fileName, fileInputPath, fileOutputPath));
        }

        @Test
        void readFile_exception() throws IOException {
            var fileName = "";
            var fileInputPath = "";
            var fileOutputPath = "";

            doThrow(IOException.class).when(fileOperator).decodeZipFile(any(), any());

            assertThrows(FundanalyzerFileException.class, () -> companyLogic.readFile(fileName, fileInputPath, fileOutputPath));
        }
    }

    @Nested
    class insertIndustry {

        @DisplayName("insertIndustry: industryが登録されていなかったら登録されることを確認する")
        @Test
        void insertIndustry_insert() {
            var edinetCsvResultBean = new EdinetCsvResultBean();
            edinetCsvResultBean.setIndustry("まだ登録されていない業種");
            var resultBeanList = List.of(edinetCsvResultBean);
            var createdAt = LocalDateTime.of(2020, 9, 12, 23, 54);
            var industryAlready = new Industry(1, "既に登録されている業種", createdAt);
            var industryInserted = new Industry(null, "まだ登録されていない業種", createdAt);

            when(industryDao.selectAll()).thenReturn(List.of(industryAlready));
            doReturn(createdAt).when(companyLogic).nowLocalDateTime();

            assertDoesNotThrow(() -> companyLogic.insertIndustry(resultBeanList));

            // insertされることを確認する
            verify(industryDao, times(1)).insert(industryInserted);
        }

        @DisplayName("insertIndustry: industryが登録されていたら登録されないことを確認する")
        @Test
        void insertIndustry_not_insert() {
            var edinetCsvResultBean = new EdinetCsvResultBean();
            edinetCsvResultBean.setIndustry("既に登録されている業種");
            var resultBeanList = List.of(edinetCsvResultBean);
            var createdAt = LocalDateTime.of(2020, 9, 12, 23, 54);
            var industryAlready = new Industry(1, "既に登録されている業種", createdAt);

            when(industryDao.selectAll()).thenReturn(List.of(industryAlready));
            doReturn(createdAt).when(companyLogic).nowLocalDateTime();

            assertDoesNotThrow(() -> companyLogic.insertIndustry(resultBeanList));

            // insertされないことを確認する
            verify(industryDao, times(0)).insert(any());
        }
    }

    @Nested
    class upsertCompany {

        @DisplayName("upsertCompany: companyが登録されていなかったら登録されることを確認する")
        @Test
        void upsertCompany_insert() {
            var edinetCsvResultBean = new EdinetCsvResultBean();
            edinetCsvResultBean.setSecuritiesCode("code");
            edinetCsvResultBean.setSubmitterName("まだ登録されていない会社");
            edinetCsvResultBean.setIndustry("industry");
            edinetCsvResultBean.setEdinetCode("edinetCodeInserted");
            edinetCsvResultBean.setListCategories("上場");
            edinetCsvResultBean.setConsolidated("有");
            edinetCsvResultBean.setCapitalStock(1);
            edinetCsvResultBean.setSettlementDate("date");
            var resultBeanList = List.of(edinetCsvResultBean);
            var createdAt = LocalDateTime.of(2020, 9, 19, 17, 39);
            var industryAlready = new Industry(1, "industry", createdAt);
            var companyAlready = new Company(
                    "code",
                    "既に登録されている会社",
                    1,
                    "edinetCodeAlready",
                    "1",
                    "1",
                    1,
                    "date",
                    null,
                    null
            );
            var companyInserted = new Company(
                    "code",
                    "まだ登録されていない会社",
                    1,
                    "edinetCodeInserted",
                    "1",
                    "1",
                    1,
                    "date",
                    createdAt,
                    createdAt
            );

            when(csvCommander.readCsv(any(), any(), eq(EdinetCsvResultBean.class))).thenReturn(resultBeanList);
            when(companyDao.selectAll()).thenReturn(List.of(companyAlready));
            when(industryDao.selectAll()).thenReturn(List.of(industryAlready));
            doReturn(createdAt).when(companyLogic).nowLocalDateTime();

            assertDoesNotThrow(() -> companyLogic.upsertCompany(resultBeanList));

            // insertされることを確認する
            verify(companyDao, times(1)).insert(companyInserted);
            verify(companyDao, times(0)).update(any());
        }

        @DisplayName("company: companyが登録されていたら登録されないことを確認する")
        @Test
        void upsertCompany_update() {
            var edinetCsvResultBean = new EdinetCsvResultBean();
            edinetCsvResultBean.setSecuritiesCode("code");
            edinetCsvResultBean.setSubmitterName("既に登録されている会社");
            edinetCsvResultBean.setIndustry("industry");
            edinetCsvResultBean.setEdinetCode("edinetCodeAlready");
            edinetCsvResultBean.setListCategories("上場");
            edinetCsvResultBean.setConsolidated("有");
            edinetCsvResultBean.setCapitalStock(1);
            edinetCsvResultBean.setSettlementDate("date");
            var resultBeanList = List.of(edinetCsvResultBean);
            var createdAt = LocalDateTime.of(2020, 9, 19, 17, 39);
            var industryAlready = new Industry(1, "industry", createdAt);
            var companyAlready = new Company(
                    "code",
                    "既に登録されている会社",
                    1,
                    "edinetCodeAlready",
                    null,
                    null,
                    null,
                    "date",
                    null,
                    null
            );
            when(companyDao.selectAll()).thenReturn(List.of(companyAlready));
            when(industryDao.selectAll()).thenReturn(List.of(industryAlready));
            doReturn(createdAt).when(companyLogic).nowLocalDateTime();

            assertDoesNotThrow(() -> companyLogic.upsertCompany(resultBeanList));

            // updateされることを確認する
            verify(companyDao, times(1)).update(new Company(
                    "code",
                    "既に登録されている会社",
                    1,
                    "edinetCodeAlready",
                    "1",
                    "1",
                    1,
                    "date",
                    createdAt,
                    createdAt
            ));
            verify(companyDao, times(0)).insert(any());
        }
    }
}