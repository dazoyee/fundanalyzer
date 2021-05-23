package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.client.csv.CsvCommander;
import github.com.ioridazo.fundanalyzer.domain.specification.CompanySpecification;
import github.com.ioridazo.fundanalyzer.domain.specification.IndustrySpecification;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRestClientException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import github.com.ioridazo.fundanalyzer.client.file.FileOperator;
import github.com.ioridazo.fundanalyzer.client.selenium.SeleniumClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompanyInteractorTest {

    private CompanySpecification companySpecification;
    private FileOperator fileOperator;
    private SeleniumClient seleniumClient;

    private CompanyInteractor companyInteractor;

    @BeforeEach
    void setUp() {
        companySpecification = Mockito.mock(CompanySpecification.class);
        fileOperator = Mockito.mock(FileOperator.class);
        seleniumClient = Mockito.mock(SeleniumClient.class);

        companyInteractor = Mockito.spy(new CompanyInteractor(
                Mockito.mock(IndustrySpecification.class),
                companySpecification,
                fileOperator,
                Mockito.mock(CsvCommander.class),
                seleniumClient
        ));
        companyInteractor.pathCompany = "pathCompany";
        companyInteractor.pathCompanyZip = "pathCompanyZip";
    }

    @Nested
    class getUpdateDate {

        @DisplayName("getUpdateDate : 企業情報の更新日時を取得する")
        @Test
        void present() {
            when(companySpecification.findLastUpdateDateTime()).thenReturn(Optional.of("time"));

            assertEquals("time", companyInteractor.getUpdateDate());
        }

        @DisplayName("getUpdateDate : 更新日時がないときはnullとなる")
        @Test
        void empty() {
            when(companySpecification.findLastUpdateDateTime()).thenReturn(Optional.empty());

            assertEquals("null", companyInteractor.getUpdateDate());
        }
    }

    @Nested
    class importCompanyInfo {

        @DisplayName("importCompanyInfo : 企業情報ファイルを取得して登録する")
        @Test
        void ok() throws IOException {
            when(seleniumClient.edinetCodeList(any())).thenReturn("fileName");
            doNothing().when(companyInteractor).saveCompanyInfo();

            assertDoesNotThrow(() -> companyInteractor.importCompanyInfo());
            verify(seleniumClient, times(1)).edinetCodeList(any());
            verify(fileOperator, times(1)).decodeZipFile(any(), any());
            verify(companyInteractor, times(1)).saveCompanyInfo();
        }

        @DisplayName("importCompanyInfo : Selenium処理中にエラーが発生したときの挙動を確認する")
        @Test
        void fundanalyzerRestClientException() {
            when(seleniumClient.edinetCodeList(any())).thenThrow(FundanalyzerRestClientException.class);
            assertThrows(FundanalyzerRuntimeException.class, () -> companyInteractor.importCompanyInfo());
        }

        @DisplayName("importCompanyInfo : zipファイル解凍処理中にエラーが発生したときの挙動を確認する")
        @Test
        void iOException() throws IOException {
            when(seleniumClient.edinetCodeList(any())).thenReturn("fileName");
            doThrow(IOException.class).when(fileOperator).decodeZipFile(any(), any());
            assertThrows(FundanalyzerRuntimeException.class, () -> companyInteractor.importCompanyInfo());
        }
    }
}