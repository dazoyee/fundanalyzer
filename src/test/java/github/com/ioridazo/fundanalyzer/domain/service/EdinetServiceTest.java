package github.com.ioridazo.fundanalyzer.domain.service;

import github.com.ioridazo.fundanalyzer.domain.usecase.CompanyUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.DocumentUseCase;
import github.com.ioridazo.fundanalyzer.domain.value.Result;
import github.com.ioridazo.fundanalyzer.web.model.BetweenDateInputData;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.model.IdInputData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("EdinetService のテスト")
class EdinetServiceTest {

    private CompanyUseCase companyUseCase;
    private DocumentUseCase documentUseCase;
    private EdinetService service;

    @BeforeEach
    void setUp() {
        companyUseCase = mock(CompanyUseCase.class);
        documentUseCase = mock(DocumentUseCase.class);
        service = new EdinetService(companyUseCase, documentUseCase);
    }

    @Nested
    @DisplayName("updateCompany メソッド")
    class UpdateCompany {

        @DisplayName("updateCompany : CompanyUseCase.importCompanyInfo に委譲する")
        @Test
        void delegatesToCompanyUseCase() {
            service.updateCompany();
            verify(companyUseCase, times(1)).importCompanyInfo();
        }
    }

    @Nested
    @DisplayName("saveEdinetList メソッド")
    class SaveEdinetList {

        @DisplayName("saveEdinetList : 期間内の各日付を DateInputData として DocumentUseCase に渡す（両端含む）")
        @Test
        void invokesPerDateInRange() {
            final BetweenDateInputData input = BetweenDateInputData.of(
                    LocalDate.parse("2024-04-01"),
                    LocalDate.parse("2024-04-03"));

            service.saveEdinetList(input);

            verify(documentUseCase, times(3)).saveEdinetList(any(DateInputData.class));
        }

        @DisplayName("saveEdinetList : 開始日と終了日が同じ場合は 1 回呼ばれる")
        @Test
        void invokesOnceWhenSameDay() {
            final BetweenDateInputData input = BetweenDateInputData.of(
                    LocalDate.parse("2024-04-01"),
                    LocalDate.parse("2024-04-01"));

            service.saveEdinetList(input);

            verify(documentUseCase, times(1)).saveEdinetList(any(DateInputData.class));
        }
    }

    @Nested
    @DisplayName("updateAllDoneStatus メソッド")
    class UpdateAllDoneStatus {

        @DisplayName("updateAllDoneStatus : DocumentUseCase に委譲し結果をそのまま返す")
        @Test
        void delegatesAndReturns() {
            final IdInputData input = IdInputData.of("doc-1");
            when(documentUseCase.updateAllDoneStatus(input)).thenReturn(Result.OK);

            final Result actual = service.updateAllDoneStatus(input);

            assertEquals(Result.OK, actual);
            verify(documentUseCase, times(1)).updateAllDoneStatus(input);
        }
    }

    @Nested
    @DisplayName("removeDocument メソッド")
    class RemoveDocument {

        @DisplayName("removeDocument : DocumentUseCase.removeDocument に委譲する")
        @Test
        void delegates() {
            final IdInputData input = IdInputData.of("doc-1");

            service.removeDocument(input);

            verify(documentUseCase, times(1)).removeDocument(input);
        }
    }
}
