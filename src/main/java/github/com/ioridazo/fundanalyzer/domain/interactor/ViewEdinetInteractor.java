package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CompanySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.DocumentSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.FinancialStatementSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.ViewSpecification;
import github.com.ioridazo.fundanalyzer.domain.usecase.ViewEdinetUseCase;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import github.com.ioridazo.fundanalyzer.client.slack.SlackClient;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.view.model.edinet.EdinetListViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.edinet.detail.DocumentViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.edinet.detail.EdinetDetailViewModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ViewEdinetInteractor implements ViewEdinetUseCase {

    private final CompanySpecification companySpecification;
    private final DocumentSpecification documentSpecification;
    private final FinancialStatementSpecification financialStatementSpecification;
    private final ViewSpecification viewSpecification;
    private final SlackClient slackClient;

    @Value("${app.config.view.edinet-list.size}")
    int edinetListSize;

    public ViewEdinetInteractor(
            final CompanySpecification companySpecification,
            final DocumentSpecification documentSpecification,
            final FinancialStatementSpecification financialStatementSpecification,
            final ViewSpecification viewSpecification,
            final SlackClient slackClient) {
        this.companySpecification = companySpecification;
        this.documentSpecification = documentSpecification;
        this.financialStatementSpecification = financialStatementSpecification;
        this.viewSpecification = viewSpecification;
        this.slackClient = slackClient;
    }

    LocalDate nowLocalDate() {
        return LocalDate.now();
    }

    /**
     * メインビューを取得する
     *
     * @return EDINETリストビュー
     */
    @Override
    public List<EdinetListViewModel> viewMain() {
        return viewSpecification.findAllEdinetListView().stream()
                .filter(viewModel -> !viewModel.isAllDone())
                .collect(Collectors.toList());
    }

    /**
     * オールビューを取得する
     *
     * @return EDINETリストビュー
     */
    @Override
    public List<EdinetListViewModel> viewAll() {
        return viewSpecification.findAllEdinetListView();
    }

    /**
     * EDINETリスト詳細ビューを取得する
     *
     * @param inputData 提出日
     * @return EDINETリスト詳細ビュー
     */
    @Override
    public EdinetDetailViewModel viewEdinetDetail(final DateInputData inputData) {
        final List<DocumentViewModel> documentView = documentSpecification.targetList(inputData).stream()
                .filter(document -> !documentSpecification.allStatusDone(document))
                .map(document -> DocumentViewModel.of(
                        companySpecification.findCompanyByEdinetCode(document.getEdinetCode()).orElseThrow(FundanalyzerRuntimeException::new),
                        document,
                        financialStatementSpecification.getFinanceValue(document)
                ))
                .collect(Collectors.toList());

        return EdinetDetailViewModel.of(
                viewSpecification.findEdinetListView(inputData),
                documentView
        );
    }

    /**
     * すべてのビューを更新する
     */
    @Override
    public void updateView() {
        final List<EdinetListViewModel> viewModelList = documentSpecification.documentList().stream()
                .map(Document::getSubmitDate)
                .filter(submitDate -> submitDate.isAfter(nowLocalDate().minusDays(edinetListSize)))
                .map(DateInputData::of)
                .map(viewSpecification::generateEdinetListView)
                .collect(Collectors.toList());

        viewModelList.parallelStream().forEach(viewSpecification::upsert);

        slackClient.sendMessage("g.c.i.f.domain.service.ViewService.display.update.complete.edinet.list");

        FundanalyzerLogClient.logService(
                "処理状況アップデートが正常に終了しました。",
                Category.VIEW,
                Process.UPDATE
        );
    }

    /**
     * ビューを更新する
     *
     * @param inputData 提出日
     */
    @Override
    public void updateView(final DateInputData inputData) {
        viewSpecification.upsert(viewSpecification.generateEdinetListView(inputData));

        FundanalyzerLogClient.logService(
                MessageFormat.format("処理状況アップデートが正常に終了しました。対象提出日:{0}", inputData.getDate()),
                Category.VIEW,
                Process.UPDATE
        );
    }
}
