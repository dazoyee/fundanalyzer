package github.com.ioridazo.fundanalyzer.client.jsoup.result;

import java.util.List;
import java.util.Map;

public record StockPriceResultBean(

        String targetDate,

        String openingPrice,

        String highPrice,

        String lowPrice,

        String closingPrice,

        String volume,

        String closingPriceAdjustment) {

    public static StockPriceResultBean ofKabuoji3(final Map<String, Integer> thOrder, final List<String> tdList) {
        return new StockPriceResultBean(
                tdList.get(thOrder.get("日付")),
                tdList.get(thOrder.get("始値")),
                tdList.get(thOrder.get("高値")),
                tdList.get(thOrder.get("安値")),
                tdList.get(thOrder.get("終値")),
                tdList.get(thOrder.get("出来高")),
                tdList.get(thOrder.get("終値調整"))
        );
    }

    public static StockPriceResultBean ofYahooFinance(final Map<String, Integer> thOrder, final List<String> tdList) {
        return new StockPriceResultBean(
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
