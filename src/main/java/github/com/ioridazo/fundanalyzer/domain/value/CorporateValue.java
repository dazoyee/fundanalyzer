package github.com.ioridazo.fundanalyzer.domain.value;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Data(staticConstructor = "of")
public class CorporateValue {

    // 最新企業価値
    private BigDecimal latestCorporateValue;
    // 平均企業価値
    private List<AverageInfo> averageInfoList;
    // 対象年カウント
    private BigDecimal countYear;

    public Optional<BigDecimal> getLatestCorporateValue() {
        return Optional.ofNullable(latestCorporateValue);
    }

    public List<AverageInfo> getAverageInfoList() {
        return Optional.ofNullable(averageInfoList).orElse(List.of());
    }

    public Optional<BigDecimal> getCountYear() {
        return Optional.ofNullable(countYear);
    }


}
