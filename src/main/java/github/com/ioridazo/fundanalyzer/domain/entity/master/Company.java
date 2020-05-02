package github.com.ioridazo.fundanalyzer.domain.entity.master;

import lombok.Value;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.time.LocalDateTime;

@Value
@Entity(immutable = true)
@Table(name = "company")
public class Company {

    // 証券コード
    @Id
    String code;

    // 銘柄名
    String companyName;

    // 業種
    String industryId;

    // EDINETコード
    String edinetCode;

    // 上場区分
    ListCategories listCategories;

    // 連結の有無
    Consolidated consolidated;

    // 資本金
    Integer capitalStock;

    // 決算日
    String settlementDate;

    // 登録日
    @Column(updatable = false)
    LocalDateTime insertDate;

    // 更新日
    LocalDateTime updateDate;
}
