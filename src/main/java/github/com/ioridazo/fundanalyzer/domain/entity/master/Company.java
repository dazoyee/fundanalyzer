package github.com.ioridazo.fundanalyzer.domain.entity.master;

import lombok.Value;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.time.LocalDateTime;
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
    private final String industryId;

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
}
