package github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail;

import github.com.ioridazo.fundanalyzer.web.view.model.corporate.CorporateViewModel;

import java.time.LocalDate;
import java.util.List;

/**
 * 企業詳細ビュー
 *
 * @param company            企業情報
 * @param backwardCode       前へ遷移する企業コード
 * @param forwardCode        次へ遷移する企業コード
 * @param corporate          企業情報ビュー
 * @param analysisResultList 分析結果のリスト
 * @param indicatorList      投資指標のリスト
 * @param financialStatement 財務諸表のリスト
 * @param minkabuList        みんかぶ予想のリスト
 * @param stockPriceList     株価のリスト
 * @param splitDates         株式分割日一覧
 */
public record CorporateDetailViewModel(
        CompanyViewModel company,
        String backwardCode,
        String forwardCode,
        CorporateViewModel corporate,
        List<AnalysisResultViewModel> analysisResultList,
        List<IndicatorViewModel> indicatorList,
        List<FinancialStatementViewModel> financialStatement,
        List<MinkabuViewModel> minkabuList,
        List<StockPriceViewModel> stockPriceList,
        List<LocalDate> splitDates) {

    /**
     * 全フィールド指定の静的ファクトリ
     */
    public static CorporateDetailViewModel of(
            final CompanyViewModel company,
            final String backwardCode,
            final String forwardCode,
            final CorporateViewModel corporate,
            final List<AnalysisResultViewModel> analysisResultList,
            final List<IndicatorViewModel> indicatorList,
            final List<FinancialStatementViewModel> financialStatement,
            final List<MinkabuViewModel> minkabuList,
            final List<StockPriceViewModel> stockPriceList) {
        return of(
                company,
                backwardCode,
                forwardCode,
                corporate,
                analysisResultList,
                indicatorList,
                financialStatement,
                minkabuList,
                stockPriceList,
                List.of()
        );
    }

    /**
     * 全フィールド指定の静的ファクトリ。
     */
    public static CorporateDetailViewModel of(
            final CompanyViewModel company,
            final String backwardCode,
            final String forwardCode,
            final CorporateViewModel corporate,
            final List<AnalysisResultViewModel> analysisResultList,
            final List<IndicatorViewModel> indicatorList,
            final List<FinancialStatementViewModel> financialStatement,
            final List<MinkabuViewModel> minkabuList,
            final List<StockPriceViewModel> stockPriceList,
            final List<LocalDate> splitDates) {
        return new CorporateDetailViewModel(
                company,
                backwardCode,
                forwardCode,
                corporate,
                analysisResultList,
                indicatorList,
                financialStatement,
                minkabuList,
                stockPriceList,
                splitDates
        );
    }

    /**
     * 既存ビューに前後の企業コードを差し替えて生成する静的ファクトリ
     *
     * @param viewModel    既存ビュー
     * @param backwardCode 前へ遷移する企業コード
     * @param forwardCode  次へ遷移する企業コード
     * @return CorporateDetailViewModel
     */
    public static CorporateDetailViewModel of(
            final CorporateDetailViewModel viewModel, final String backwardCode, final String forwardCode) {
        return new CorporateDetailViewModel(
                viewModel.company(),
                backwardCode,
                forwardCode,
                viewModel.corporate(),
                viewModel.analysisResultList(),
                viewModel.indicatorList(),
                viewModel.financialStatement(),
                viewModel.minkabuList(),
                viewModel.stockPriceList(),
                viewModel.splitDates()
        );
    }

    public CompanyViewModel getCompany() {
        return company;
    }

    public String getBackwardCode() {
        return backwardCode;
    }

    public String getForwardCode() {
        return forwardCode;
    }

    public CorporateViewModel getCorporate() {
        return corporate;
    }

    public List<AnalysisResultViewModel> getAnalysisResultList() {
        return analysisResultList;
    }

    public List<IndicatorViewModel> getIndicatorList() {
        return indicatorList;
    }

    public List<FinancialStatementViewModel> getFinancialStatement() {
        return financialStatement;
    }

    public List<MinkabuViewModel> getMinkabuList() {
        return minkabuList;
    }

    public List<StockPriceViewModel> getStockPriceList() {
        return stockPriceList;
    }

    /**
     * 株式分割日一覧を返す。
     */
    public List<String> getSplitDates() {
        return splitDates.stream()
                .map(LocalDate::toString)
                .toList();
    }
}
