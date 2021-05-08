package github.com.ioridazo.fundanalyzer.domain.value;

import github.com.ioridazo.fundanalyzer.domain.entity.master.CompanyEntity;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Consolidated;
import github.com.ioridazo.fundanalyzer.domain.entity.master.ListCategories;
import lombok.Value;

import java.util.Optional;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
public class Company {

    // 証券コード
    private final String code;

    // 銘柄名
    private final String companyName;

    // 業種ID
    private final Integer industryId;

    // 業種名
    private final String industryName;

    // EDINETコード
    private final String edinetCode;

    // 上場区分
    private final ListCategories listCategories;

    // 連結の有無
    private final Consolidated consolidated;

    // 資本金
    private final Integer capitalStock;

    // 決算日
    private final String settlementDate;

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
                entity.getSettlementDate()
        );
    }

    public Optional<String> getCode() {
        return Optional.ofNullable(code);
    }
}
