package github.com.ioridazo.fundanalyzer.client.jsoup.result;

import lombok.Value;

import java.util.List;
import java.util.Map;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value(staticConstructor = "of")
public class YahooFinanceResultBean {

    private final String targetDate;

    private final String openingPrice;

    private final String highPrice;

    private final String lowPrice;

    private final String closingPrice;

    private final String volume;

    private final String closingPriceAdjustment;

    public static YahooFinanceResultBean ofJsoup(final Map<String, Integer> thOrder, final List<String> tdList) {
        return new YahooFinanceResultBean(
                tdList.get(thOrder.get("日付")),
                tdList.get(thOrder.get("始値")),
                tdList.get(thOrder.get("高値")),
                tdList.get(thOrder.get("安値")),
                tdList.get(thOrder.get("終値")),
                tdList.get(thOrder.get("出来高")),
                tdList.get(thOrder.get("調整後終値"))
        );
    }
}
