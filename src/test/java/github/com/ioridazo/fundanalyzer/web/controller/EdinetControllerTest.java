package github.com.ioridazo.fundanalyzer.web.controller;

import github.com.ioridazo.fundanalyzer.domain.service.AnalysisService;
import github.com.ioridazo.fundanalyzer.domain.service.EdinetService;
import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.domain.value.Result;
import github.com.ioridazo.fundanalyzer.web.model.BetweenDateInputData;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.model.FinancialStatementInputData;
import github.com.ioridazo.fundanalyzer.web.model.IdInputData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.time.LocalDate;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class EdinetControllerTest {

    private AnalysisService analysisService;
    private EdinetService edinetService;
    private ViewService viewService;

    private EdinetController controller;

    @BeforeEach
    void setUp() {
        analysisService = Mockito.mock(AnalysisService.class);
        edinetService = Mockito.mock(EdinetService.class);
        viewService = Mockito.mock(ViewService.class);

        controller = new EdinetController(analysisService, edinetService, viewService);
    }

    @DisplayName("updateCompany : 会社情報を更新する")
    @Test
    void updateCompany() {
        var actual = UriComponentsBuilder.fromUriString(controller.updateCompany()).build();

        assertEquals("/fundanalyzer/v1/edinet/list", actual.getPath());
        assertEquals("表示アップデート処理を要求しました。", UriUtils.decode(Objects.requireNonNull(actual.getQueryParams().getFirst("message")), "UTF-8"));
        Mockito.verify(edinetService, Mockito.times(1)).updateCompany();
    }

    @DisplayName("saveEdinet : EDINETから提出書類一覧を取得する")
    @Test
    void saveEdinet() {
        assertEquals("redirect:/fundanalyzer/v1/edinet/list", controller.saveEdinet("2021-05-29", "2021-05-29"));
        Mockito.verify(edinetService, Mockito.times(1))
                .saveEdinetList(BetweenDateInputData.of(LocalDate.parse("2021-05-29"), LocalDate.parse("2021-05-29")));
    }

    @DisplayName("updateEdinetList : EDINETリストをアップデートする")
    @Test
    void updateEdinetList() {
        assertEquals("redirect:/fundanalyzer/v1/edinet/list", controller.updateEdinetList("2021-05-29"));
        Mockito.verify(viewService, Mockito.times(1)).updateEdinetListView(DateInputData.of(LocalDate.parse("2021-05-29")));
    }

    @DisplayName("registerFinancialStatementValue : 財務諸表の値を登録する")
    @Test
    void registerFinancialStatementValue() {
        var inputData = FinancialStatementInputData.of(
                "edinetCode",
                "documentId",
                "1",
                "1",
                1000
        );
        when(analysisService.registerFinancialStatementValue(inputData)).thenReturn(Result.OK);
        assertEquals(
                "redirect:/fundanalyzer/v1/edinet/list/detail?submitDate=2021-08-22",
                controller.registerFinancialStatementValue("2021-08-22", inputData)
        );
    }

    @DisplayName("removeDocument : 対象書類IDを処理対象外にする")
    @Test
    void removeDocument() {
        var actual = UriComponentsBuilder.fromUriString(controller.removeDocument("2021-05-29", "test1234")).build();

        assertEquals("/fundanalyzer/v1/edinet/list/detail", actual.getPath());
        assertEquals("2021-05-29", UriUtils.decode(Objects.requireNonNull(actual.getQueryParams().getFirst("submitDate")), "UTF-8"));
        Mockito.verify(edinetService, Mockito.times(1)).removeDocument(IdInputData.of("test1234"));
    }
}