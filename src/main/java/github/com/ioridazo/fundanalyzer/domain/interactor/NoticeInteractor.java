package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.domain.specification.ViewSpecification;
import github.com.ioridazo.fundanalyzer.domain.usecase.NoticeUseCase;
import github.com.ioridazo.fundanalyzer.proxy.slack.SlackProxy;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.view.model.edinet.EdinetListViewModel;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;

public class NoticeInteractor implements NoticeUseCase {

    private final ViewSpecification viewSpecification;
    private final SlackProxy slackProxy;

    @Value("${app.config.view.discount-rate}")
    BigDecimal configDiscountRate;

    public NoticeInteractor(final ViewSpecification viewSpecification, final SlackProxy slackProxy) {
        this.viewSpecification = viewSpecification;
        this.slackProxy = slackProxy;
    }

    /**
     * Slackに通知する
     *
     * @param inputData 提出日
     */
    @Override
    public void noticeSlack(final DateInputData inputData) {
        final EdinetListViewModel edinetListView = viewSpecification.findEdinetListView(inputData);

        if (edinetListView.isAllDone()) {
            // info message
            slackProxy.sendMessage("g.c.i.f.domain.service.ViewService.processing.notice.info",
                    edinetListView.getSubmitDate(), edinetListView.getCountTarget());
        } else {
            // warn message
            slackProxy.sendMessage("g.c.i.f.domain.service.ViewService.processing.notice.warn",
                    edinetListView.getSubmitDate(), edinetListView.getCountTarget(), edinetListView.getCountNotScraped());
        }

        viewSpecification.findAllCorporateView(inputData).stream()
                // 割安度が120%以上を表示
                .filter(cvm -> cvm.getDiscountRate().compareTo(configDiscountRate) >= 0)
                .forEach(cvm -> {
                    // 優良銘柄を通知する
                    slackProxy.sendMessage("g.c.i.f.domain.service.ViewService.processing.notice.submitDate",
                            cvm.getCode(), cvm.getName(), cvm.getDiscountRate());
                });
    }
}
