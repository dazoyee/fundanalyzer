package github.com.ioridazo.fundanalyzer.domain.scraping.jsoup.bean;

import lombok.Value;

import java.util.List;
import java.util.Map;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
public class Kabuoji3ResultBean {

    private final String targetDate;

    private final String openingPrice;

    private final String highPrice;

    private final String lowPrice;

    private final String closingPrice;

    private final String volume;

    private final String closingPriceAdjustment;

    public static Kabuoji3ResultBean ofJsoup(final Map<String, Integer> thOrder, final List<String> tdList) {
        return new Kabuoji3ResultBean(
                tdList.get(thOrder.get("日付")),
                tdList.get(thOrder.get("始値")),
                tdList.get(thOrder.get("高値")),
                tdList.get(thOrder.get("安値")),
                tdList.get(thOrder.get("終値")),
                tdList.get(thOrder.get("出来高")),
                tdList.get(thOrder.get("終値調整"))
        );
    }
}
