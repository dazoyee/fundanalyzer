package github.com.ioridazo.fundanalyzer.domain.util;

import github.com.ioridazo.fundanalyzer.domain.entity.master.Company;

import java.util.List;
import java.util.Optional;

public class Converter {

    private Converter() {
    }

    /**
     * companyCode -> edinetCode
     *
     * @param companyCode 会社コード
     * @param companyAll  会社一覧
     * @return edinetCode
     */
    public static Optional<String> toEdinetCode(final String companyCode, final List<Company> companyAll) {
        return companyAll.stream()
                .filter(company -> company.getCode().isPresent())
                .filter(company -> companyCode.equals(company.getCode().get()))
                .map(Company::getEdinetCode)
                .findAny();
    }

    /**
     * edinetCode -> companyCode
     *
     * @param edinetCode EDINETコード
     * @param companyAll 会社一覧
     * @return companyCode
     */
    public static Optional<String> toCompanyCode(final String edinetCode, final List<Company> companyAll) {
        return companyAll.stream()
                .filter(company -> company.getCode().isPresent())
                .filter(company -> edinetCode.equals(company.getEdinetCode()))
                .map(Company::getCode)
                .map(Optional::get)
                .findAny();
    }
}
