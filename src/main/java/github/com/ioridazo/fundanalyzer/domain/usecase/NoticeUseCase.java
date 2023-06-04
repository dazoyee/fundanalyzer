package github.com.ioridazo.fundanalyzer.domain.usecase;

import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import io.micrometer.observation.annotation.Observed;

public interface NoticeUseCase {

    /**
     * Slack通知
     *
     * @param inputData 提出日
     */
    @Observed
    void noticeSlack(DateInputData inputData);
}
