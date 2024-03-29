package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.client.slack.SlackClient;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CompanySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.DocumentSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.FinancialStatementSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.ViewSpecification;
import github.com.ioridazo.fundanalyzer.domain.usecase.ViewEdinetUseCase;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.view.model.edinet.EdinetListViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.edinet.detail.DocumentViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.edinet.detail.EdinetDetailViewModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Component
public class ViewEdinetInteractor implements ViewEdinetUseCase {

    private static final Logger log = LogManager.getLogger(ViewEdinetInteractor.class);

    private final CompanySpecification companySpecification;
    private final DocumentSpecification documentSpecification;
    private final FinancialStatementSpecification financialStatementSpecification;
    private final ViewSpecification viewSpecification;
    private final SlackClient slackClient;

    @Value("${app.config.view.edinet-list.size}")
    int edinetListSize;
    @Value("${app.slack.update-view.enabled:true}")
    boolean updateViewEnabled;

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
                .sorted(Comparator.comparing(EdinetListViewModel::submitDate).reversed())
                .toList();
    }

    /**
     * オールビューを取得する
     *
     * @return EDINETリストビュー
     */
    @Override
    public List<EdinetListViewModel> viewAll() {
        return viewSpecification.findAllEdinetListView().stream()
                .sorted(Comparator.comparing(EdinetListViewModel::submitDate).reversed())
                .toList();
    }

    /**
     * EDINETリスト詳細ビューを取得する
     *
     * @param inputData 提出日
     * @return EDINETリスト詳細ビュー
     */
    @Override
    public EdinetDetailViewModel viewEdinetDetail(final DateInputData inputData) {
        final List<DocumentViewModel> documentView = documentSpecification.findTargetList(inputData).stream()
                .filter(document -> !documentSpecification.allStatusDone(document))
                .map(document -> DocumentViewModel.of(
                        companySpecification.findCompanyByEdinetCode(document.getEdinetCode()).orElseThrow(FundanalyzerRuntimeException::new),
                        document,
                        financialStatementSpecification.getFinanceValueViewModel(document)
                ))
                .toList();

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
        final long startTime = System.currentTimeMillis();
        final List<EdinetListViewModel> viewModelList = documentSpecification.submitDateList().stream()
                .filter(submitDate -> submitDate.isAfter(nowLocalDate().minusDays(edinetListSize)))
                .map(DateInputData::of)
                .map(viewSpecification::generateEdinetListView)
                .toList();

        viewModelList.parallelStream().forEach(viewSpecification::upsert);

        if (updateViewEnabled) {
            slackClient.sendMessage("g.c.i.f.domain.service.ViewService.display.update.complete.edinet.list");
        }

        log.info(FundanalyzerLogClient.toInteractorLogObject(
                "処理状況アップデートが正常に終了しました。",
                Category.VIEW,
                Process.UPDATE,
                System.currentTimeMillis() - startTime
        ));
    }

    /**
     * ビューを更新する
     *
     * @param inputData 提出日
     */
    @Override
    public void updateView(final DateInputData inputData) {
        final long startTime = System.currentTimeMillis();

        try {
            viewSpecification.upsert(viewSpecification.generateEdinetListView(inputData));

            log.info(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format("処理状況アップデートが正常に終了しました。対象提出日:{0}", inputData.getDate()),
                    Category.VIEW,
                    Process.UPDATE,
                    System.currentTimeMillis() - startTime
            ));
        } catch (final Exception e) {
            log.error(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format(
                            "{0}付のビューに対して想定外のエラーが発生しました。",
                            inputData.getDate()
                    ),
                    Category.VIEW,
                    Process.UPDATE
            ), e);
        }
    }
}
