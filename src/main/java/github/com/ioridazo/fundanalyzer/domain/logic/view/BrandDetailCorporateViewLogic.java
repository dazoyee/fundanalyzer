package github.com.ioridazo.fundanalyzer.domain.logic.view;

import github.com.ioridazo.fundanalyzer.domain.dao.master.BsSubjectDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.IndustryDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.PlSubjectDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.FinancialStatementDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.StockPriceDao;
import github.com.ioridazo.fundanalyzer.domain.entity.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.FinancialStatement;
import github.com.ioridazo.fundanalyzer.domain.logic.view.bean.BrandDetailCorporateViewBean;
import github.com.ioridazo.fundanalyzer.domain.logic.view.bean.BrandDetailViewBean;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class BrandDetailCorporateViewLogic {

    private final IndustryDao industryDao;
    private final CompanyDao companyDao;
    private final BsSubjectDao bsSubjectDao;
    private final PlSubjectDao plSubjectDao;
    private final FinancialStatementDao financialStatementDao;
    private final StockPriceDao stockPriceDao;

    public BrandDetailCorporateViewLogic(
            final IndustryDao industryDao,
            final CompanyDao companyDao,
            final BsSubjectDao bsSubjectDao,
            final PlSubjectDao plSubjectDao,
            final FinancialStatementDao financialStatementDao,
            final StockPriceDao stockPriceDao) {
        this.industryDao = industryDao;
        this.companyDao = companyDao;
        this.bsSubjectDao = bsSubjectDao;
        this.plSubjectDao = plSubjectDao;
        this.financialStatementDao = financialStatementDao;
        this.stockPriceDao = stockPriceDao;
    }

    /**
     * 銘柄詳細に表示する企業情報を取得する
     *
     * @param code 会社コード
     * @return 銘柄詳細に表示する企業情報
     */
    @NewSpan("BrandDetailCorporateViewLogic.brandDetailCompanyViewOf")
    public BrandDetailCorporateViewBean brandDetailCompanyViewOf(final String code) {
        final var company = companyDao.selectByCode(code).orElseThrow();
        final var industry = industryDao.selectById(company.getIndustryId()).getName();
        final var stockPrices = stockPriceDao.selectByCode(code);

        return BrandDetailCorporateViewBean.of(company, industry, stockPrices);
    }

    /**
     * 銘柄詳細に表示する財務諸表を取得する
     *
     * @param code 会社コード
     * @return 銘柄詳細に表示する財務諸表
     */
    @NewSpan("BrandDetailCorporateViewLogic.brandDetailFinancialStatement")
    public List<BrandDetailViewBean.BrandDetailFinancialStatement> brandDetailFinancialStatement(final String code) {
        final var financialStatementList = financialStatementDao.selectByCode(code);
        return financialStatementList.stream()
                .map(FinancialStatement::getPeriodStart)
                .distinct()
                .map(periodStart -> {
                    final var targetList = financialStatementList.stream()
                            .filter(fi -> periodStart.equals(fi.getPeriodStart()))
                            .collect(Collectors.toList());
                    return new BrandDetailViewBean.BrandDetailFinancialStatement(
                            // periodStart
                            periodStart,
                            // periodEnd
                            targetList.stream()
                                    .map(FinancialStatement::getPeriodEnd)
                                    .findAny()
                                    .orElseThrow(),
                            // bs(List)
                            targetList.stream()
                                    .filter(fi -> FinancialStatementEnum.BALANCE_SHEET.toValue().equals(fi.getFinancialStatementId()))
                                    .map(fi -> new BrandDetailViewBean.BrandDetailFinancialStatement.BrandDetailFinancialStatementValue(
                                            bsSubjectDao.selectById(fi.getSubjectId()).getName(), fi.getValue().orElse(null)
                                    )).collect(Collectors.toList()),
                            // pl(List)
                            targetList.stream()
                                    .filter(fi -> FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT.toValue().equals(fi.getFinancialStatementId()))
                                    .map(fi -> new BrandDetailViewBean
                                            .BrandDetailFinancialStatement
                                            .BrandDetailFinancialStatementValue(
                                            plSubjectDao.selectById(fi.getSubjectId()).getName(),
                                            fi.getValue().orElse(null)
                                    )).collect(Collectors.toList())
                    );
                }).collect(Collectors.toList());
    }
}
