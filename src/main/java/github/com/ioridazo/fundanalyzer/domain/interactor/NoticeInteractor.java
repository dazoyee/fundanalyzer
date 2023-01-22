package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.client.slack.SlackClient;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.AnalysisResultSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.ViewSpecification;
import github.com.ioridazo.fundanalyzer.domain.usecase.NoticeUseCase;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.view.model.edinet.EdinetListViewModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class NoticeInteractor implements NoticeUseCase {

    private static final Logger log = LogManager.getLogger(NoticeInteractor.class);

    private final AnalysisResultSpecification analysisResultSpecification;
    private final ViewSpecification viewSpecification;
    private final SlackClient slackClient;

    @Value("${app.config.view.discount-rate}")
    BigDecimal configDiscountRate;
    @Value("${app.slack.analysis-result.enabled:true}")
    boolean analysisResultEnabled;

    public NoticeInteractor(
            final AnalysisResultSpecification analysisResultSpecification,
            final ViewSpecification viewSpecification,
            final SlackClient slackClient) {
        this.analysisResultSpecification = analysisResultSpecification;
        this.viewSpecification = viewSpecification;
        this.slackClient = slackClient;
    }

    /**
     * 分析結果をSlackに通知する
     *
     * @param inputData 提出日
     */
    @Override
    public void noticeSlack(final DateInputData inputData) {
        try {
            final List<AnalysisResultEntity> updatedList = analysisResultSpecification.findUpdatedList(inputData.getDate());
            final EdinetListViewModel edinetListView = viewSpecification.findEdinetListView(inputData);

            if (!updatedList.isEmpty() && analysisResultEnabled) {
                slackClient.sendMessage("github.com.ioridazo.fundanalyzer.domain.interactor.NoticeInteractor.noticeSlack",
                        inputData.getDate(), edinetListView.getCountTarget(), updatedList.size());
            }
        } catch (final Exception e) {
            log.error(FundanalyzerLogClient.toInteractorLogObject(
                    "想定外のエラーが発生しました。",
                    Category.NOTICE,
                    Process.SLACK
            ), e);
        }
    }
}
