package github.com.ioridazo.fundanalyzer.domain.logic.scraping.jsoup.bean;

import lombok.Value;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value(staticConstructor = "of")
public class FinancialTableResultBean {

    private final String subject;

    private final String previousValue;

    private final String currentValue;

    private final Unit unit;

    public Optional<String> getSubject() {
        return Optional.ofNullable(subject);
    }

    public Optional<String> getPreviousValue() {
        return Optional.ofNullable(previousValue);
    }

    public static FinancialTableResultBean ofTdList(final List<String> tdList, final Unit unit, final boolean isMain) {
        if (tdList.size() == 2) {
            // 財務諸表に記載の項目が当年度のみの場合
            return FinancialTableResultBean.of(tdList.get(0), null, tdList.get(1), unit);
        } else if (tdList.size() == 3) {
            // 財務諸表に記載の項目が当年度と昨年度がある場合（main）
            if (isMain) {
                // テーブル項目の当年度が後に存在している場合（main）
                return FinancialTableResultBean.of(tdList.get(0), tdList.get(1), tdList.get(2), unit);
            } else {
                // テーブル項目の当年度が先に存在している場合
                return FinancialTableResultBean.of(tdList.get(0), tdList.get(2), tdList.get(1), unit);
            }
        } else {
            // 合致するものがない場合
            return null;
        }
    }
}
