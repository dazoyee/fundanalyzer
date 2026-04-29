package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.client.csv.CsvCommander;
import github.com.ioridazo.fundanalyzer.client.file.FileOperator;
import github.com.ioridazo.fundanalyzer.client.jsoup.JsoupClient;
import github.com.ioridazo.fundanalyzer.client.selenium.SeleniumClient;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CompanySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.IndustrySpecification;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerFileException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRestClientException;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
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
    private JsoupClient jsoupClient;

    private CompanyInteractor companyInteractor;

    @BeforeEach
    void setUp() {
        companySpecification = Mockito.mock(CompanySpecification.class);
        fileOperator = Mockito.mock(FileOperator.class);
        seleniumClient = Mockito.mock(SeleniumClient.class);
        jsoupClient = Mockito.mock(JsoupClient.class);

        companyInteractor = Mockito.spy(new CompanyInteractor(
                Mockito.mock(IndustrySpecification.class),
                companySpecification,
                fileOperator,
                Mockito.mock(CsvCommander.class),
                seleniumClient,
                jsoupClient
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
            doNothing().when(companyInteractor).saveCompanyInfo();

            assertDoesNotThrow(() -> companyInteractor.importCompanyInfo());
            verify(seleniumClient, times(1)).edinetCodeList(any());
            verify(companyInteractor, times(1)).saveCompanyInfo();
        }

        @DisplayName("importCompanyInfo : zipファイル解凍処理中にエラーが発生したときの挙動を確認する")
        @Test
        void iOException() throws IOException {
            when(seleniumClient.edinetCodeList(any())).thenReturn("fileName");
            doThrow(IOException.class).when(fileOperator).decodeZipFile(any(), any());
            assertThrows(FundanalyzerFileException.class, () -> companyInteractor.importCompanyInfo());
        }
    }

    @Nested
    class updateRemovedCompany {

        @DisplayName("updateRemovedCompany : 除外フラグを有効にする")
        @Test
        void removed() {
            var inputData = CodeInputData.of("99999");
            when(companySpecification.findCompanyByCode("99999")).thenReturn(Optional.of(company()));
            assertDoesNotThrow(() -> companyInteractor.updateRemovedCompany(inputData));
            verify(companySpecification, times(1)).updateRemoved(any());
        }

        private Company company() {
            return new Company(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    false,
                    true
            );
        }
    }

    @Nested
    @DisplayName("updateFavoriteCompany メソッド")
    class UpdateFavoriteCompany {

        @DisplayName("updateFavoriteCompany : お気に入り登録に成功したら true を返す")
        @Test
        void registered() {
            final CodeInputData input = CodeInputData.of("1234");
            final Company company = new Company(
                    "1234", null, null, null, null, null, null, null, null, false, true);
            when(companySpecification.findCompanyByCode("1234")).thenReturn(Optional.of(company));
            when(companySpecification.updateFavorite(company)).thenReturn(true);

            assertEquals(true, companyInteractor.updateFavoriteCompany(input));
        }

        @DisplayName("updateFavoriteCompany : お気に入り解除なら false を返す")
        @Test
        void unregistered() {
            final CodeInputData input = CodeInputData.of("1234");
            final Company company = new Company(
                    "1234", null, null, null, null, null, null, null, null, true, true);
            when(companySpecification.findCompanyByCode("1234")).thenReturn(Optional.of(company));
            when(companySpecification.updateFavorite(company)).thenReturn(false);

            assertEquals(false, companyInteractor.updateFavoriteCompany(input));
        }

        @DisplayName("updateFavoriteCompany : 企業が存在しないときは FundanalyzerNotExistException")
        @Test
        void absent() {
            final CodeInputData input = CodeInputData.of("9999");
            when(companySpecification.findCompanyByCode("9999")).thenReturn(Optional.empty());

            assertThrows(
                    github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException.class,
                    () -> companyInteractor.updateFavoriteCompany(input));
        }
    }

    @Nested
    @DisplayName("isLived メソッド")
    class IsLived {

        @DisplayName("isLived : JsoupClient.isLivedCompanyFromMinkabu の結果を返す")
        @Test
        void delegates() {
            final CodeInputData input = CodeInputData.of("1234");
            when(jsoupClient.isLivedCompanyFromMinkabu("1234")).thenReturn(true);

            assertEquals(true, companyInteractor.isLived(input));
        }

        @DisplayName("isLived : 上場廃止の場合は false を返す")
        @Test
        void delisted() {
            final CodeInputData input = CodeInputData.of("9999");
            when(jsoupClient.isLivedCompanyFromMinkabu("9999")).thenReturn(false);

            assertEquals(false, companyInteractor.isLived(input));
        }
    }

    @Nested
    @DisplayName("updateRemovedCompany 例外パス")
    class UpdateRemovedCompanyAbsent {

        @DisplayName("updateRemovedCompany : 企業が存在しないときは FundanalyzerNotExistException")
        @Test
        void absent() {
            final CodeInputData input = CodeInputData.of("9999");
            when(companySpecification.findCompanyByCode("9999")).thenReturn(Optional.empty());

            assertThrows(
                    github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException.class,
                    () -> companyInteractor.updateRemovedCompany(input));
        }
    }
}
