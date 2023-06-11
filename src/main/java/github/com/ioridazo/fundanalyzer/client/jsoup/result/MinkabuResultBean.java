package github.com.ioridazo.fundanalyzer.client.jsoup.result;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Objects;

public record MinkabuResultBean(
        String stockPrice,
        String targetDate,
        ExpectedStockPrice expectedStockPrice
) {

    public record ExpectedStockPrice(
            String goals,
            String theoretical,
            String individualInvestors,
            String securitiesAnalyst
    ) {
    }

    public static MinkabuResultBean ofJsoup(final Document document) {
        return new MinkabuResultBean(
                document.select("div.stock_price").stream().map(Element::text).findFirst().orElse(null),
                document.select("div.md_stockBoard_stockTable").select("span.fsm").stream()
                        .map(Element::text)
                        .findFirst()
                        .map(s -> s.replace("(", "").replace(")", "")).orElse(null),
                new ExpectedStockPrice(
                        document.select("div.ly_content_wrapper").stream()
                                .map(element -> element.select("div.md_card").select("div.md_card_ti"))
                                .filter(element -> "目標株価".equals(element.select("div.ly_colsize_6").select("h2").text()))
                                .map(element -> element.select("div.ly_colsize_7_fix").select("div.ly_row").select("div.ly_colsize_8"))
                                .map(element -> element.select("span.fsxxxl"))
                                .map(Elements::text)
                                .filter(text -> !"---".equals(text))
                                .findFirst().orElse(null),
                        document.select("div.ly_content_wrapper").select("div.md_card").select("div.ly_row").select("div.ly_colsize_4").stream()
                                .filter(element -> "株価診断".equals(element.select("div.md_sub_index").select("h3").text()))
                                .map(element -> element.select("div.ly_colsize_7_fix").select("div.tar"))
                                .map(element -> element.select("span.fsxl"))
                                .map(Elements::text)
                                .filter(text -> !"---".equals(text))
                                .findFirst().orElse(null),
                        document.select("div.ly_content_wrapper").select("div.md_card").select("div.ly_row").select("div.ly_colsize_4").stream()
                                .filter(element -> "個人投資家の株価予想".equals(element.select("div.md_sub_index").select("h3").text()))
                                .map(element -> element.select("div.ly_colsize_7_fix").select("div.tar"))
                                .map(element -> element.select("span.fsxl"))
                                .map(Elements::text)
                                .filter(text -> !"---".equals(text))
                                .findFirst().orElse(null),
                        document.select("div.ly_content_wrapper").select("div.md_card").select("div.ly_row").select("div.ly_colsize_4").stream()
                                .filter(element -> "証券アナリストの予想".equals(element.select("div.md_sub_index").select("h3").text()))
                                .map(element -> element.select("div.ly_colsize_7_fix").select("div.tar"))
                                .map(element -> element.select("span.fsxl"))
                                .map(Elements::text)
                                .filter(text -> !"---".equals(text))
                                .findFirst().orElse(null)
                )
        );
    }

    public static boolean isLivedCompany(final Document document) {
        return document
                .select("div.md_stockBoard")
                .select("div.md_stockBoard_header")
                .select("div.notice-stock-status")
                .select("em.fsl")
                .stream()
                .map(Element::text)
                .findFirst()
                .map(v -> !Objects.equals("上場廃止", v))
                .orElse(true);
    }
}
