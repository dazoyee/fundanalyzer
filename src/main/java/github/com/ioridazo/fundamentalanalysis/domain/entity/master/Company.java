package github.com.ioridazo.fundamentalanalysis.domain.entity.master;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Company {

    // 証券コード
    private String code;

    // 銘柄名
    private String companyName;

    // 業種
    private String industryId;

    // EDINETコード
    private String edinetCode;

    // 上場区分
    private ListCategories listCategories;

    // 連結の有無
    private Consolidated consolidated;

    // 資本金
    private Integer capitalStock;

    // 決算日
    private String settlementDate;

    // 登録日
    private LocalDateTime insertDate;

    // 更新日
    private LocalDateTime updateDate;
}
