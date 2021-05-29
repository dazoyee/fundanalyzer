package github.com.ioridazo.fundanalyzer.domain.domain.jsoup.bean;

import lombok.Value;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
public class MinkabuResultBean {

    private final String stockPrice;

    private final String targetDate;

    private final ExpectedStockPrice expectedStockPrice;

    @Value
    public static class ExpectedStockPrice {
        private final String goals;
        private final String theoretical;
        private final String individualInvestors;
        private final String securitiesAnalyst;
    }

    public static MinkabuResultBean ofJsoup(final Document document) {
        return new MinkabuResultBean(
                document.select("div.stock_price").text(),
                document.select("div.md_stockBoard_stockTable").select("span.fsm").text()
                        .replace("(", "").replace(")", ""),
                new ExpectedStockPrice(
                        document.select("div.ly_content_wrapper").stream()
                                .filter(element -> "目標株価".equals(element.select("div.md_index").select("h2").text()))
                                .findFirst().orElse(new Element("null"))
                                .select("div.md_card")
                                .select("div.ly_row").select("div.md_card_ti").select("div.md_box")
                                .select("div.ly_col").select("div.md_box").select("div.ly_colsize_7_fix")
                                .select("div.ly_col").select("div.ly_colsize_8")
                                .select("span.fsxxxl")
                                .text(),
                        document.select("div.ly_content_wrapper").stream()
                                .filter(element -> "目標株価".equals(element.select("div.md_index").select("h2").text()))
                                .findFirst().orElse(new Element("null"))
                                .select("div.md_card")
                                .select("div.ly_row")
                                .select("div.ly_col").select("div.ly_colsize_4").stream()
                                .filter(element -> "株価診断".equals(element.select("div.md_sub_index").select("a").text()))
                                .findFirst().orElse(new Element("null"))
                                .select("div.ly_col").select("div.ly_colsize_7_fix")
                                .select("span.fsxl")
                                .text(),
                        document.select("div.ly_content_wrapper").stream()
                                .filter(element -> "目標株価".equals(element.select("div.md_index").select("h2").text()))
                                .findFirst().orElse(new Element("null"))
                                .select("div.md_card")
                                .select("div.ly_row")
                                .select("div.ly_col").select("div.ly_colsize_4").stream()
                                .filter(element -> "個人投資家の予想".equals(element.select("div.md_sub_index").select("a").text()))
                                .findFirst().orElse(new Element("null"))
                                .select("div.ly_col").select("div.ly_colsize_7_fix")
                                .select("span.fsxl")
                                .text(),
                        document.select("div.ly_content_wrapper").stream()
                                .filter(element -> "目標株価".equals(element.select("div.md_index").select("h2").text()))
                                .findFirst().orElse(new Element("null"))
                                .select("div.md_card")
                                .select("div.ly_row")
                                .select("div.ly_col").select("div.ly_colsize_4").stream()
                                .filter(element -> "証券アナリストの予想".equals(element.select("div.md_sub_index").select("a").text()))
                                .findFirst().orElse(new Element("null"))
                                .select("div.ly_col").select("div.ly_colsize_7_fix")
                                .select("span.fsxl")
                                .text()
                )
        );
    }
}
