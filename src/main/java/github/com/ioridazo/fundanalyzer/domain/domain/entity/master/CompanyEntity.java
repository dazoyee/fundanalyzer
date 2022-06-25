package github.com.ioridazo.fundanalyzer.domain.domain.entity.master;

import github.com.ioridazo.fundanalyzer.client.csv.bean.EdinetCsvResultBean;
import lombok.Value;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
@Entity(immutable = true)
@Table(name = "company")
public class CompanyEntity {

    // 証券コード
    private final String code;

    // 銘柄名
    private final String companyName;

    // 業種
    private final Integer industryId;

    // EDINETコード
    @Id
    private final String edinetCode;

    // 上場区分
    private final String listCategories;

    // 連結の有無
    private final String consolidated;

    // 資本金
    private final Integer capitalStock;

    // 決算日
    private final String settlementDate;

    // お気に入り
    private final String favorite;

    // 登録日
    @Column(updatable = false)
    private final LocalDateTime createdAt;

    // 更新日
    private final LocalDateTime updatedAt;

    public Optional<String> getCode() {
        return Optional.ofNullable(code);
    }

    public static CompanyEntity ofNull() {
        return new CompanyEntity(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public static CompanyEntity of(
            final List<IndustryEntity> industryEntityList,
            final EdinetCsvResultBean resultBean,
            final LocalDateTime createdAt) {
        return new CompanyEntity(
                resultBean.getSecuritiesCode().isBlank() ? null : resultBean.getSecuritiesCode(),
                resultBean.getSubmitterName(),
                mapToIndustryId(industryEntityList, resultBean.getIndustry()),
                resultBean.getEdinetCode(),
                ListCategories.fromName(resultBean.getListCategories()).toValue(),
                Consolidated.fromName(resultBean.getConsolidated()).toValue(),
                resultBean.getCapitalStock(),
                resultBean.getSettlementDate().isBlank() ? null : resultBean.getSettlementDate(),
                "0",
                createdAt,
                createdAt
        );
    }

    public static CompanyEntity of(
            final Integer industryId,
            final EdinetCsvResultBean resultBean,
            final LocalDateTime createdAt) {
        return new CompanyEntity(
                resultBean.getSecuritiesCode().isBlank() ? null : resultBean.getSecuritiesCode(),
                resultBean.getSubmitterName(),
                industryId,
                resultBean.getEdinetCode(),
                ListCategories.fromName(resultBean.getListCategories()).toValue(),
                Consolidated.fromName(resultBean.getConsolidated()).toValue(),
                resultBean.getCapitalStock(),
                resultBean.getSettlementDate().isBlank() ? null : resultBean.getSettlementDate(),
                "0",
                createdAt,
                createdAt
        );
    }

    private static final int OTHER_INDUSTRY_ID = 40;

    public static CompanyEntity ofSqlForeignKey(
            final String edinetCode,
            final String companyName,
            final LocalDateTime createdAt) {
        return new CompanyEntity(
                null,
                companyName,
                OTHER_INDUSTRY_ID,
                edinetCode,
                null,
                null,
                null,
                null,
                "0",
                createdAt,
                createdAt
        );
    }

    private static Integer mapToIndustryId(final List<IndustryEntity> industryEntityList, final String industryName) {
        return industryEntityList.stream()
                .filter(industry -> industryName.equals(industry.getName()))
                .map(IndustryEntity::getId)
                .findAny()
                .orElseThrow();
    }
}
