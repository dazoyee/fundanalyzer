package github.com.ioridazo.fundanalyzer.domain.usecase;

import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import org.springframework.cloud.sleuth.annotation.NewSpan;

public interface NoticeUseCase {

    /**
     * Slack通知
     *
     * @param inputData 提出日
     */
    @NewSpan
    void noticeSlack(DateInputData inputData);
}
