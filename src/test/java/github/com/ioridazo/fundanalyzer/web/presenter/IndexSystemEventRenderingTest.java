package github.com.ioridazo.fundanalyzer.web.presenter;

import github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction.SystemEventDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.SystemEventEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.SystemEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 会社一覧のシステムイベント表示が Thymeleaf で描画できることを検証する統合テスト。
 * 集約表示は式が多く、テンプレートの記述誤りは実行時にしか現れないため画面全体を描画して確認する。
 */
@SpringBootTest(properties = {
        "management.server.port=",
        "management.endpoints.web.exposure.include=health"
})
@AutoConfigureMockMvc
@Transactional
@DisplayName("会社一覧のシステムイベント描画")
class IndexSystemEventRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SystemEventDao systemEventDao;

    @DisplayName("イベントがないときは異常なしのサマリを描画する")
    @Test
    void noEvent_rendersHealthySummary() throws Exception {
        mockMvc.perform(get("/v3/index").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("システムイベント")))
                .andExpect(content().string(containsString("異常なし")))
                .andExpect(content().string(not(containsString("最終発生"))));
    }

    @DisplayName("同一企業・同一書類の警告は1グループに集約して描画する")
    @Test
    void events_rendersGroupedSummary() throws Exception {
        // 表示は発生日時の期間で絞られるため、固定日時ではなく現在時刻を基準に登録する
        final LocalDateTime latest = LocalDateTime.now().minusHours(1).withNano(0);
        insertValidationWarning("10", latest);
        insertValidationWarning("25", latest.minusSeconds(1));

        mockMvc.perform(get("/v3/index").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("WARNING 2")))
                .andExpect(content().string(containsString(
                        "最終発生 " + latest.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))))
                // グループ行は企業コードと書類IDで 1 行になる
                .andExpect(content().string(containsString("S100YXHH")))
                // 明細は科目単位に要約される
                .andExpect(content().string(containsString("科目ID:10")))
                .andExpect(content().string(containsString("科目ID:25")))
                .andExpect(content().string(not(containsString("異常なし"))));
    }

    @DisplayName("表示期間より古い警告は描画しない")
    @Test
    void oldEvent_notRendered() throws Exception {
        insertValidationWarning("10", LocalDateTime.now().minusDays(30).withNano(0));

        mockMvc.perform(get("/v3/index").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("異常なし")))
                .andExpect(content().string(not(containsString("科目ID:10"))));
    }

    private void insertValidationWarning(final String subjectId, final LocalDateTime occurredAt) {
        systemEventDao.insert(SystemEventEntity.of(
                SystemEventType.WARNING,
                "FinancialStatementSpecification",
                "企業コード:27760 EDINET:E02960 財務諸表:損益計算書 科目ID:" + subjectId
                + " 書類ID:S100YXHH 当期末:2025-01-31 前回期末:2024-01-31"
                + " 前回値:1,430,000 今回値:21,879,000 比率:15.3",
                occurredAt
        ));
    }
}
