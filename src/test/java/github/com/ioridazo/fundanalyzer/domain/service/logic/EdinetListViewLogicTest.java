package github.com.ioridazo.fundanalyzer.domain.service.logic;

import github.com.ioridazo.fundanalyzer.domain.dao.transaction.AnalysisResultDao;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Company;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class EdinetListViewLogicTest {

    private EdinetListViewLogic logic;

    @BeforeEach
    void setUp() {
        logic = Mockito.spy(new EdinetListViewLogic(Mockito.mock(AnalysisResultDao.class)));
    }

    @DisplayName("counter : 提出日ごとに処理状況をカウントする")
    @Test
    void counter_ok() {
        var submitDate = LocalDate.parse("2020-11-08");
        var countAll = Long.valueOf(1);
        var documentList = List.of(
                Document.builder()
                        .edinetCode("edinetCode")
                        .documentTypeCode("120")
                        .submitDate(LocalDate.parse("2020-11-07"))
                        .scrapedNumberOfShares("0")
                        .scrapedBs("0")
                        .scrapedPl("0")
                        .removed("0")
                        .build(),
                Document.builder()
                        .edinetCode("ec")
                        .documentTypeCode("120")
                        .submitDate(LocalDate.parse("2020-11-08"))
                        .scrapedNumberOfShares("0")
                        .scrapedBs("0")
                        .scrapedPl("0")
                        .removed("0")
                        .build()
        );
        var companyAllTargeted = List.of(new Company(
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
        ));
        var createdAt = LocalDateTime.of(2020, 10, 17, 18, 15);

        when(logic.nowLocalDateTime()).thenReturn(createdAt);

        var actual = logic.counter(submitDate, countAll, documentList, companyAllTargeted);

        assertAll("EdinetListViewBean",
                () -> assertEquals(LocalDate.parse("2020-11-08"), actual.getSubmitDate(), "submitDate"),
                () -> assertEquals(1L, actual.getCountAll(), "countAll"),
                () -> assertEquals(0L, actual.getCountTarget(), "countTarget"),
                () -> assertEquals(0L, actual.getCountScraped(), "countScraped"),
                () -> assertEquals(0L, actual.getCountAnalyzed(), "countAnalyzed"),
                () -> assertEquals("", actual.getNotAnalyzedCode(), "notAnalyzedCode"),
                () -> assertEquals("", actual.getCantScrapedCode(), "cantScrapedCode"),
                () -> assertEquals(0L, actual.getCountNotScraped(), "countNotScraped"),
                () -> assertEquals(1L, actual.getCountNotTarget(), "countNotTarget"),
                () -> assertEquals(LocalDateTime.of(2020, 10, 17, 18, 15), actual.getCreatedAt()),
                () -> assertEquals(LocalDateTime.of(2020, 10, 17, 18, 15), actual.getUpdatedAt())
        );
    }
}