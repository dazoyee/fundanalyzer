package github.com.ioridazo.fundanalyzer.domain.value;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.CompanyEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.Consolidated;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.ListCategories;

import java.util.Objects;

/**
 * @param code           証券コード
 * @param companyName    銘柄名
 * @param industryId     業種ID
 * @param industryName   業種名
 * @param edinetCode     EDINETコード
 * @param listCategories 上場区分
 * @param consolidated   連結の有無
 * @param capitalStock   資本金
 * @param settlementDate 決算日
 */
public record Company(

        String code,

        String companyName,

        Integer industryId,

        String industryName,

        String edinetCode,

        ListCategories listCategories,

        Consolidated consolidated,

        Integer capitalStock,
        String settlementDate,

        boolean favorite,

        boolean lived
) {

    public static Company of(final CompanyEntity entity, final String industryName) {
        return new Company(
                entity.getCode().orElse(null),
                entity.getCompanyName(),
                entity.getIndustryId(),
                industryName,
                entity.getEdinetCode(),
                ListCategories.fromValue(entity.getListCategories()),
                Consolidated.fromValue(entity.getConsolidated()),
                entity.getCapitalStock(),
                entity.getSettlementDate(),
                Objects.equals(entity.getFavorite(), "1"),
                Objects.equals(entity.getRemoved(), "0")
        );
    }
}
