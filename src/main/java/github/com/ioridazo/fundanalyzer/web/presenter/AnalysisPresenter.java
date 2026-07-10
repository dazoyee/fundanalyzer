package github.com.ioridazo.fundanalyzer.web.presenter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.domain.usecase.ViewCorporateUseCase.SummaryChartData;
import github.com.ioridazo.fundanalyzer.domain.value.Horizon;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.view.model.analysis.BacktestResult;
import github.com.ioridazo.fundanalyzer.web.view.model.analysis.BacktestResult.BacktestScatterPoint;
import github.com.ioridazo.fundanalyzer.web.view.model.analysis.BacktestResult.HorizonResult;
import github.com.ioridazo.fundanalyzer.web.view.model.analysis.DistributionResult;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.AnalysisResultViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.StockPriceViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.CompanyValuationViewModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 分析ダッシュボード画面を返す Presenter。
 */
@Controller
public class AnalysisPresenter {

    private static final Logger log = LoggerFactory.getLogger(AnalysisPresenter.class);

    private static final String ANALYSIS_V2 = "analysis-v2";
    private static final String ANALYSIS_CHART_FRAGMENT = "fragments/analysis-chart :: chart";
    private static final String ANALYSIS_BACKTEST_FRAGMENT = "fragments/analysis-backtest :: backtest";
    private static final String ANALYSIS_DISTRIBUTION_FRAGMENT = "fragments/analysis-distribution :: distribution";

    @Value("${app.config.view.document-type-code}")
    private List<String> targetTypeCodes;

    @Value("${app.config.analysis.ranking-size}")
    private int rankingSize;

    private final ViewService viewService;
    private final ObjectMapper objectMapper;

    public AnalysisPresenter(
            final ViewService viewService,
            final ObjectMapper objectMapper) {
        this.viewService = viewService;
        this.objectMapper = objectMapper;
    }

    /**
     * 分析ダッシュボード画面を表示する。
     *
     * @param model model
     * @return analysis-v2 テンプレート名
     */
    @GetMapping("/v3/analysis")
    public String analysisView(final Model model) {
        final List<CompanyValuationViewModel> ranking = viewService.getAllValuationView().stream()
                .sorted(Comparator.comparing(
                        CompanyValuationViewModel::discountRate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(rankingSize)
                .toList();
        model.addAttribute("ranking", ranking);
        return ANALYSIS_V2;
    }

    /**
     * 旧 valuation 画面の URL を分析ダッシュボードへリダイレクトする。
     *
     * @return /v3/analysis へのリダイレクト
     */
    @GetMapping("/v3/valuation")
    public String legacyValuationRedirect() {
        return "redirect:/v3/analysis";
    }

    /**
     * 個別銘柄の分析チャート fragment を返す。
     *
     * @param code  会社コード（4〜5桁の数値）
     * @param model model
     * @return fragments/analysis-chart :: chart
     */
    @GetMapping("/v3/analysis/{code:[0-9]{4,5}}/chart")
    public String analysisChart(
            @PathVariable final String code,
            final Model model) {
        model.addAttribute("chartId", "analysisChart-" + code);
        try {
            final SummaryChartData summaryChartData = viewService.getSummaryChartView(CodeInputData.of(code));
            final List<AnalysisResultViewModel> analysis = resolveLatestPerPeriod(summaryChartData.analysisResults());
            final List<StockPriceViewModel> allStockPrices = summaryChartData.stockPrices().stream()
                    .sorted(Comparator.comparing(StockPriceViewModel::targetDate))
                    .toList();
            populateChartModel(model, analysis, allStockPrices);

            final List<CompanyValuationViewModel> valuationView = viewService.getValuationView(CodeInputData.of(code)).stream()
                    .sorted(Comparator.comparing(CompanyValuationViewModel::targetDate))
                    .toList();
            populateTrendChartModel(model, valuationView);
        } catch (FundanalyzerNotExistException e) {
            log.warn("analysisChart: 企業コードが存在しない: code={}", code);
            populateEmptyChartModel(model);
        } catch (JsonProcessingException e) {
            log.error("analysisChart: チャートデータのJSON変換に失敗: code={}", code, e);
            populateEmptyChartModel(model);
        }
        return ANALYSIS_CHART_FRAGMENT;
    }

    /**
     * バックテスト fragment を返す。
     *
     * @param horizonLabel 表示対象期間ラベル
     * @param model        model
     * @return fragments/analysis-backtest :: backtest
     */
    @GetMapping("/v3/analysis/backtest")
    public String analysisBacktest(
            @RequestParam(name = "horizon", required = false) final String horizonLabel,
            final Model model) {
        final BacktestResult result = viewService.getBacktestView();
        final Horizon selected = resolveSelectedHorizon(horizonLabel);
        final HorizonResult selectedResult = result.horizons().stream()
                .filter(horizonResult -> horizonResult.horizon() == selected)
                .findFirst()
                .orElse(null);
        final String scatterJson = buildScatterJson(selectedResult);
        model.addAttribute("backtest", result);
        model.addAttribute("selectedResult", selectedResult);
        model.addAttribute("selectedHorizon", selected);
        model.addAttribute("horizons", Horizon.values());
        model.addAttribute("scatterJson", scatterJson);
        return ANALYSIS_BACKTEST_FRAGMENT;
    }

    /**
     * 分布 fragment を返す。
     *
     * @param model model
     * @return fragments/analysis-distribution :: distribution
     */
    @GetMapping("/v3/analysis/distribution")
    public String analysisDistribution(final Model model) {
        final DistributionResult result = viewService.getDistributionView();
        final List<DistributionResult.HistogramBin> discountHistogram = result.discountHistogram() != null
                ? result.discountHistogram()
                : List.of();
        final List<DistributionResult.HistogramBin> grahamHistogram = result.grahamHistogram() != null
                ? result.grahamHistogram()
                : List.of();
        String discountLabelsJson = "[]";
        String discountCountsJson = "[]";
        String grahamLabelsJson = "[]";
        String grahamCountsJson = "[]";
        try {
            final List<String> discountLabels = discountHistogram.stream()
                    .map(DistributionResult.HistogramBin::label)
                    .toList();
            final List<Long> discountCounts = discountHistogram.stream()
                    .map(DistributionResult.HistogramBin::count)
                    .toList();
            final List<String> grahamLabels = grahamHistogram.stream()
                    .map(DistributionResult.HistogramBin::label)
                    .toList();
            final List<Long> grahamCounts = grahamHistogram.stream()
                    .map(DistributionResult.HistogramBin::count)
                    .toList();
            discountLabelsJson = objectMapper.writeValueAsString(discountLabels);
            discountCountsJson = objectMapper.writeValueAsString(discountCounts);
            grahamLabelsJson = objectMapper.writeValueAsString(grahamLabels);
            grahamCountsJson = objectMapper.writeValueAsString(grahamCounts);
        } catch (JsonProcessingException e) {
            log.error("analysisDistribution: チャートデータのJSON変換に失敗", e);
        }
        model.addAttribute("distribution", result);
        model.addAttribute("discountLabelsJson", discountLabelsJson);
        model.addAttribute("discountCountsJson", discountCountsJson);
        model.addAttribute("grahamLabelsJson", grahamLabelsJson);
        model.addAttribute("grahamCountsJson", grahamCountsJson);
        return ANALYSIS_DISTRIBUTION_FRAGMENT;
    }

    /**
     * 対象書類タイプのうち、期ごとに最新提出日のレコードを1件抽出し期順に返す。
     *
     * @param list 分析結果リスト
     * @return 期ごとの代表分析結果リスト（期昇順）
     */
    private List<AnalysisResultViewModel> resolveLatestPerPeriod(final List<AnalysisResultViewModel> list) {
        return list.stream()
                .filter(vm -> targetTypeCodes.stream().anyMatch(t -> vm.documentTypeCode().equals(t)))
                .map(AnalysisResultViewModel::documentPeriod)
                .distinct()
                .map(dp -> list.stream()
                        .filter(vm -> targetTypeCodes.stream().anyMatch(t -> vm.documentTypeCode().equals(t)))
                        .filter(vm -> dp.equals(vm.documentPeriod()))
                        .max(Comparator.comparing(AnalysisResultViewModel::submitDate)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(Comparator.comparing(AnalysisResultViewModel::documentPeriod))
                .toList();
    }

    /**
     * summary chart 描画用の model 属性（labelsJson / cvJson / stJson）を設定する。
     *
     * @param model          model
     * @param analysis       期ごとの代表分析結果リスト
     * @param allStockPrices 株価リスト（日付昇順）
     */
    private void populateChartModel(
            final Model model,
            final List<AnalysisResultViewModel> analysis,
            final List<StockPriceViewModel> allStockPrices) throws JsonProcessingException {
        final List<String> labelList = analysis.stream()
                .map(vm -> vm.documentPeriod().toString())
                .toList();
        final List<Object> cvList = analysis.stream()
                .map(AnalysisResultViewModel::corporateValue)
                .map(v -> (Object) v)
                .toList();
        final List<Object> stList = analysis.stream()
                .map(vm -> allStockPrices.stream()
                        .filter(sp -> !sp.targetDate().isAfter(vm.submitDate()))
                        .max(Comparator.comparing(StockPriceViewModel::targetDate))
                .map(StockPriceViewModel::stockPrice)
                .map(v -> (Object) v)
                .orElse(null))
                .toList();
        model.addAttribute("labelsJson", objectMapper.writeValueAsString(labelList));
        model.addAttribute("cvJson", objectMapper.writeValueAsString(cvList));
        model.addAttribute("stJson", objectMapper.writeValueAsString(stList));
    }

    private void populateTrendChartModel(
            final Model model,
            final List<CompanyValuationViewModel> valuationView) throws JsonProcessingException {
        final List<String> trendLabels = valuationView.stream()
                .map(vm -> vm.targetDate().toString())
                .toList();
        final List<Object> discList = valuationView.stream()
                .map(CompanyValuationViewModel::discountRate)
                .map(v -> (Object) v)
                .toList();
        final List<Object> grahamList = valuationView.stream()
                .map(CompanyValuationViewModel::grahamIndex)
                .map(v -> (Object) v)
                .toList();
        final List<Object> ratioList = valuationView.stream()
                .map(CompanyValuationViewModel::submitDateRatio)
                .map(v -> (Object) v)
                .toList();
        model.addAttribute("trendLabelsJson", objectMapper.writeValueAsString(trendLabels));
        model.addAttribute("discJson", objectMapper.writeValueAsString(discList));
        model.addAttribute("grahamJson", objectMapper.writeValueAsString(grahamList));
        model.addAttribute("ratioJson", objectMapper.writeValueAsString(ratioList));
    }

    private void populateEmptyChartModel(final Model model) {
        model.addAttribute("labelsJson", "[]");
        model.addAttribute("cvJson", "[]");
        model.addAttribute("stJson", "[]");
        model.addAttribute("trendLabelsJson", "[]");
        model.addAttribute("discJson", "[]");
        model.addAttribute("grahamJson", "[]");
        model.addAttribute("ratioJson", "[]");
    }

    private Horizon resolveSelectedHorizon(final String horizonLabel) {
        if (horizonLabel == null) {
            return Horizon.M6;
        }
        try {
            return Horizon.fromLabel(horizonLabel);
        } catch (IllegalArgumentException e) {
            return Horizon.M6;
        }
    }

    private String buildScatterJson(final HorizonResult selectedResult) {
        if (selectedResult == null) {
            return "[]";
        }
        final List<Map<String, Double>> points = new ArrayList<>();
        for (BacktestScatterPoint point : selectedResult.scatter()) {
            points.add(Map.of(
                    "x", point.discountRate() * 100,
                    "y", point.returnRate() * 100));
        }
        try {
            return objectMapper.writeValueAsString(points);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
