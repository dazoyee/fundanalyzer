package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.client.slack.SlackClient;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CompanySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.IndustrySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.ValuationSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.ViewSpecification;
import github.com.ioridazo.fundanalyzer.domain.usecase.ViewValuationUseCase;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.CompanyValuationViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.IndustryValuationViewModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;

@Component
public class ViewValuationInteractor implements ViewValuationUseCase {

    private static final Logger log = LogManager.getLogger(ViewValuationInteractor.class);

    private final IndustrySpecification industrySpecification;
    private final CompanySpecification companySpecification;
    private final ValuationSpecification valuationSpecification;
    private final ViewSpecification viewSpecification;
    private final SlackClient slackClient;

    @Value("${app.config.view.discount-rate}")
    BigDecimal configDiscountRate;
    @Value("${app.config.scraping.no-industry}")
    List<String> noTargetList;
    @Value("${app.slack.update-view.enabled:true}")
    boolean updateViewEnabled;

    public ViewValuationInteractor(
            final IndustrySpecification industrySpecification,
            final CompanySpecification companySpecification,
            final ValuationSpecification valuationSpecification,
            final ViewSpecification viewSpecification,
            final SlackClient slackClient) {
        this.industrySpecification = industrySpecification;
        this.companySpecification = companySpecification;
        this.valuationSpecification = valuationSpecification;
        this.viewSpecification = viewSpecification;
        this.slackClient = slackClient;
    }

    /**
     * メインビューを取得する
     *
     * @return 評価結果ビュー
     */
    @Override
    public List<CompanyValuationViewModel> viewValuation() {
        return viewAllValuation().stream()
                // 割安度が170%(外部設定値)以上を表示
                .filter(cvvm -> cvvm.discountRate().multiply(BigDecimal.valueOf(100)).compareTo(configDiscountRate) >= 0)
                // 割安度が明らかな誤りは除外
                .filter(cvvm -> cvvm.discountRate().compareTo(BigDecimal.valueOf(1000)) < 0)
                .toList();
    }

    /**
     * 企業ごとの評価結果ビュー
     *
     * @param inputData 企業コード
     * @return 評価結果ビュー
     */
    @Override
    public List<CompanyValuationViewModel> viewValuation(final CodeInputData inputData) {
        return valuationSpecification.findValuation(inputData.getCode()).stream()
                .map(viewSpecification::generateCompanyValuationView)
                .toList();
    }

    /**
     * オールビューを取得する
     *
     * @return 評価結果ビュー
     */
    @Override
    public List<CompanyValuationViewModel> viewAllValuation() {
        return viewSpecification.findAllCompanyValuationView();
    }

    /**
     * お気に入りビューを取得する
     *
     * @return 評価結果ビュー
     */
    @Override
    public List<CompanyValuationViewModel> viewFavoriteValuation() {
        final List<String> favoriteList = companySpecification.findFavoriteCompanies().stream()
                .map(Company::getCode)
                .toList();

        return viewAllValuation().stream()
                .filter(cvvm -> favoriteList.stream().anyMatch(favorite -> cvvm.code().equals(favorite.substring(0, 4))))
                .toList();
    }

    /**
     * 業種ビューを取得する
     *
     * @return 評価結果ビュー
     */
    @Override
    public List<IndustryValuationViewModel> viewIndustryValuation() {
        return industrySpecification.inquiryIndustryList().stream()
                .filter(entity -> industrySpecification.isTarget(entity.getId()))
                .map(entity -> viewSpecification.generateIndustryValuationView(
                        entity.getName(),
                        viewSpecification.findCompanyValuationViewList(entity.getId())
                ))
                .toList();
    }

    @Override
    public void updateView() {
        final long startTime = System.currentTimeMillis();
        companySpecification.inquiryAllTargetCompanies().stream()
                .map(Company::getCode)
                .map(valuationSpecification::findLatestValuation)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(viewSpecification::generateCompanyValuationView)
                .forEach(viewSpecification::upsert);

        if (updateViewEnabled) {
            slackClient.sendMessage("g.c.i.f.domain.service.ViewService.display.update.complete.valuation");
        }

        log.info(FundanalyzerLogClient.toInteractorLogObject(
                "評価アップデートが正常に終了しました。",
                Category.VIEW,
                Process.UPDATE,
                System.currentTimeMillis() - startTime
        ));
    }

    @Override
    public void updateView(final CodeInputData inputData) {
        final long startTime = System.currentTimeMillis();

        try {
            valuationSpecification.findLatestValuation(inputData.getCode())
                    .map(viewSpecification::generateCompanyValuationView)
                    .ifPresent(viewSpecification::upsert);

            log.info(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format("評価アップデートが正常に終了しました。企業コード:{0}", inputData.getCode()),
                    Category.VIEW,
                    Process.UPDATE,
                    System.currentTimeMillis() - startTime
            ));
        } catch (final Exception e) {
            log.error(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format(
                            "{0}の企業評価ビューに対して想定外のエラーが発生しました。",
                            inputData.getCode()
                    ),
                    Category.VIEW,
                    Process.UPDATE
            ), e);
        }
    }
}
