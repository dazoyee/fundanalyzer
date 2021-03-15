package github.com.ioridazo.fundanalyzer.domain.util;

import github.com.ioridazo.fundanalyzer.domain.entity.master.Company;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Industry;

import java.util.List;
import java.util.stream.Collectors;

public final class Target {

    private Target() {
    }

    /**
     * 処理対象となる会社を抽出する
     * <p/>
     * 銀行業、保険業は対象外とする想定
     *
     * @param companyList          データベースに登録されている会社一覧
     * @param excludedIndustryList 対象外とする業種リスト
     * @return 処理対象の会社一覧
     */
    public static List<Company> allCompanies(
            final List<Company> companyList,
            final List<Industry> excludedIndustryList) {
        return companyList.stream()
                .filter(company -> company.getCode().isPresent())
                .filter(company -> excludedIndustryList.stream().noneMatch(industry -> company.getIndustryId().equals(industry.getId())))
                .collect(Collectors.toList());
    }

    /**
     * 処理対象となる会社から特定のEdinetコードが含まれているかどうかを返却する
     *
     * @param edinetCode           EDINETコード
     * @param companyList          データベースに登録されている会社一覧
     * @param excludedIndustryList 対象外とする業種リスト
     * @return boolean
     */
    public static boolean containsEdinetCode(
            final String edinetCode,
            final List<Company> companyList,
            final List<Industry> excludedIndustryList) {
        return allCompanies(companyList, excludedIndustryList).stream()
                .map(Company::getEdinetCode)
                .collect(Collectors.toList())
                .contains(edinetCode);
    }

}
