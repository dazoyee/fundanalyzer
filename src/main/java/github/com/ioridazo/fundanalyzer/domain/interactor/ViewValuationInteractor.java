package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Logged;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.client.slack.SlackClient;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CompanySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.ValuationSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.ViewSpecification;
import github.com.ioridazo.fundanalyzer.domain.usecase.ViewValuationUseCase;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.CompanyValuationViewModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ViewValuationInteractor implements ViewValuationUseCase {

    private static final Logger log = LogManager.getLogger(ViewValuationInteractor.class);

    private final CompanySpecification companySpecification;
    private final ValuationSpecification valuationSpecification;
    private final ViewSpecification viewSpecification;
    private final SlackClient slackClient;

    @Value("${app.slack.update-view.enabled:true}")
    boolean updateViewEnabled;
    @Value("${app.config.view.analysis.ranking-exclude-days}")
    int rankingExcludeDays;

    public ViewValuationInteractor(
            final CompanySpecification companySpecification,
            final ValuationSpecification valuationSpecification,
            final ViewSpecification viewSpecification,
            final SlackClient slackClient) {
        this.companySpecification = companySpecification;
        this.valuationSpecification = valuationSpecification;
        this.viewSpecification = viewSpecification;
        this.slackClient = slackClient;
    }

    /**
     * 企業ごとの評価結果ビュー
     *
     * @param inputData 企業コード
     * @return 評価結果ビュー
     */
    @Override
    public List<CompanyValuationViewModel> viewValuation(final CodeInputData inputData) {
        return viewSpecification.generateCompanyValuationViewsBatch(
                        valuationSpecification.findAllValuationEntities(inputData.getCode5()))
                .stream()
                .sorted(Comparator.comparing(CompanyValuationViewModel::targetDate).reversed())
                .toList();
    }

    /**
     * オールビューを取得する
     *
     * @return 評価結果ビュー
     */
    @Override
    public List<CompanyValuationViewModel> viewAllValuation() {
        final Set<String> targetCodes = companySpecification.targetCode4Set();
        final LocalDate thresholdDate = nowLocalDate().minusDays(rankingExcludeDays);
        return viewSpecification.findAllCompanyValuationView().stream()
                // 提出日は除外
                .filter(cvvm -> cvvm.daySinceSubmitDate() != 0L)
                .filter(cvvm -> targetCodes.contains(cvvm.code()))
                .filter(cvvm -> cvvm.targetDate() != null)
                .filter(cvvm -> !cvvm.targetDate().isBefore(thresholdDate))
                .toList();
    }

    LocalDate nowLocalDate() {
        return LocalDate.now();
    }

    /** 業種内zスコアを算出する最小社数（これ未満の業種は算出対象外）。 */
    static final int MIN_INDUSTRY_SIZE = 3;
    /** zスコアの小数桁数。 */
    private static final int Z_SCORE_SCALE = 2;

    /**
     * グレアム指数の業種内zスコアを企業コード（4桁）別に算出する。
     *
     * @return 企業コード（4桁）→ 業種内zスコア
     */
    @Override
    public Map<String, BigDecimal> findGrahamIndustryZScore() {
        return computeGrahamIndustryZScore(viewAllValuation(), companySpecification.industryIdByCode4());
    }

    /**
     * グレアム指数の業種内zスコアを算出する（純粋関数）。
     *
     * <p>平均・標準偏差は double で計算する（表示用の相対指標であり最終的に
     * {@code setScale(2, HALF_UP)} で丸めるため精度上問題ない）。標準偏差は母集団標準偏差。
     *
     * @param valuations     会社評価ビュー一覧
     * @param industryByCode 企業コード（4桁）→ 業種ID
     * @return 企業コード（4桁）→ 業種内zスコア（算出可能な社のみ）
     */
    static Map<String, BigDecimal> computeGrahamIndustryZScore(
            final List<CompanyValuationViewModel> valuations, final Map<String, Integer> industryByCode) {
        final Map<Integer, List<CompanyValuationViewModel>> byIndustry = valuations.stream()
                .filter(cvvm -> cvvm.grahamIndex() != null)
                .filter(cvvm -> industryByCode.get(cvvm.code()) != null)
                .collect(Collectors.groupingBy(cvvm -> industryByCode.get(cvvm.code())));

        final Map<String, BigDecimal> result = new HashMap<>();
        byIndustry.values().forEach(group -> {
            if (group.size() < MIN_INDUSTRY_SIZE) {
                return;
            }
            final double mean = group.stream()
                    .mapToDouble(cvvm -> cvvm.grahamIndex().doubleValue())
                    .average()
                    .orElse(0);
            final double standardDeviation = Math.sqrt(group.stream()
                    .mapToDouble(cvvm -> Math.pow(cvvm.grahamIndex().doubleValue() - mean, 2))
                    .average()
                    .orElse(0));
            if (standardDeviation == 0) {
                return;
            }
            group.forEach(cvvm -> result.put(
                    cvvm.code(),
                    BigDecimal.valueOf((cvvm.grahamIndex().doubleValue() - mean) / standardDeviation)
                            .setScale(Z_SCORE_SCALE, RoundingMode.HALF_UP)));
        });
        return result;
    }

    @Override
    @Logged(category = Category.VIEW, process = Process.UPDATE, message = "評価アップデートが正常に終了しました。")
    public void updateView() {
        companySpecification.inquiryAllTargetCompanies().stream()
                .map(Company::code)
                .map(valuationSpecification::findLatestValuation)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(viewSpecification::generateCompanyValuationView)
                .forEach(viewSpecification::upsert);

        if (updateViewEnabled) {
            slackClient.sendMessage("g.c.i.f.domain.service.ViewService.display.update.complete.valuation");
        }
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
