package github.com.ioridazo.fundanalyzer.domain.entity.master;

import github.com.ioridazo.fundanalyzer.domain.csv.bean.EdinetCsvResultBean;
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
public class Company {

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

    // 登録日
    @Column(updatable = false)
    private final LocalDateTime createdAt;

    // 更新日
    private final LocalDateTime updatedAt;

    public Optional<String> getCode() {
        return Optional.ofNullable(code);
    }

    public static Company of(final List<Industry> industryList, final EdinetCsvResultBean resultBean) {
        return new Company(
                resultBean.getSecuritiesCode().isBlank() ? null : resultBean.getSecuritiesCode(),
                resultBean.getSubmitterName(),
                mapToIndustryId(industryList, resultBean.getIndustry()),
                resultBean.getEdinetCode(),
                ListCategories.fromName(resultBean.getListCategories()).toValue(),
                Consolidated.fromName(resultBean.getConsolidated()).toValue(),
                resultBean.getCapitalStock(),
                resultBean.getSettlementDate().isBlank() ? null : resultBean.getSettlementDate(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    public static Company ofSqlForeignKey(final String edinetCode, final String companyName, final LocalDateTime createdAt) {
        return new Company(
                null,
                companyName,
                40,
                edinetCode,
                null,
                null,
                null,
                null,
                createdAt,
                createdAt
        );
    }

    private static Integer mapToIndustryId(final List<Industry> industryList, final String industryName) {
        return industryList.stream()
                .filter(industry -> industryName.equals(industry.getName()))
                .map(Industry::getId)
                .findAny()
                .orElseThrow();
    }
}
