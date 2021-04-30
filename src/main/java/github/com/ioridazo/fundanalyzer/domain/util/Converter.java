package github.com.ioridazo.fundanalyzer.domain.util;

import github.com.ioridazo.fundanalyzer.domain.entity.master.CompanyEntity;

import java.util.List;
import java.util.Optional;

public final class Converter {

    private Converter() {
    }

    /**
     * companyCode -> edinetCode
     *
     * @param companyCode 会社コード
     * @param companyEntityAll  会社一覧
     * @return edinetCode
     */
    public static Optional<String> toEdinetCode(final String companyCode, final List<CompanyEntity> companyEntityAll) {
        return companyEntityAll.stream()
                .filter(company -> company.getCode().isPresent())
                .filter(company -> companyCode.equals(company.getCode().get()))
                .map(CompanyEntity::getEdinetCode)
                .findAny();
    }

    /**
     * edinetCode -> companyCode
     *
     * @param edinetCode EDINETコード
     * @param companyEntityAll 会社一覧
     * @return companyCode
     */
    public static Optional<String> toCompanyCode(final String edinetCode, final List<CompanyEntity> companyEntityAll) {
        return companyEntityAll.stream()
                .filter(company -> company.getCode().isPresent())
                .filter(company -> edinetCode.equals(company.getEdinetCode()))
                .map(CompanyEntity::getCode)
                .map(Optional::get)
                .findAny();
    }
}
