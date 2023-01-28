package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.domain.domain.specification.CompanySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.IndustrySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.ValuationSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.ViewSpecification;
import github.com.ioridazo.fundanalyzer.domain.usecase.ViewValuationUseCase;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.CompanyValuationViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.IndustryValuationViewModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class ViewValuationInteractor implements ViewValuationUseCase {

    private final ViewSpecification viewSpecification;
    private final IndustrySpecification industrySpecification;
    private final CompanySpecification companySpecification;
    private final ValuationSpecification valuationSpecification;

    @Value("${app.config.view.discount-rate}")
    BigDecimal configDiscountRate;
    @Value("${app.config.scraping.no-industry}")
    List<String> noTargetList;

    public ViewValuationInteractor(
            final ViewSpecification viewSpecification,
            final IndustrySpecification industrySpecification,
            final CompanySpecification companySpecification,
            final ValuationSpecification valuationSpecification) {
        this.viewSpecification = viewSpecification;
        this.industrySpecification = industrySpecification;
        this.companySpecification = companySpecification;
        this.valuationSpecification = valuationSpecification;
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
}
