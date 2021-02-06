package github.com.ioridazo.fundanalyzer.web.scheduler;

import github.com.ioridazo.fundanalyzer.domain.service.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doNothing;

class CompanySchedulerTest {

    private DocumentService documentService;

    private CompanyScheduler scheduler;

    @BeforeEach
    void setUp() {
        this.documentService = Mockito.mock(DocumentService.class);

        this.scheduler = new CompanyScheduler(documentService);
    }

    @Test
    void company_ok() {
        doNothing().when(documentService).downloadCompanyInfo();
        assertDoesNotThrow(() -> scheduler.company());
    }
}