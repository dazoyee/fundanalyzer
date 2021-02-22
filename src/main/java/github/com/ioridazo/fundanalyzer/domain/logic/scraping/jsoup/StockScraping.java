package github.com.ioridazo.fundanalyzer.domain.logic.scraping.jsoup;

import github.com.ioridazo.fundanalyzer.domain.logic.scraping.jsoup.bean.Kabuoji3ResultBean;
import github.com.ioridazo.fundanalyzer.domain.logic.scraping.jsoup.bean.MinkabuResultBean;
import github.com.ioridazo.fundanalyzer.domain.logic.scraping.jsoup.bean.NikkeiResultBean;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
@Component
public class StockScraping {

    /**
     * 日経の会社コードによる株価情報を取得する
     *
     * @param code 会社コード
     * @return 株価情報
     */
    public NikkeiResultBean nikkei(final String code) {
        return NikkeiResultBean.ofJsoup(jsoup(
                "日経",
                code,
                UriComponentsBuilder
                        .newInstance()
                        .scheme("https").host("www.nikkei.com")
                        .path("/nkd/company/")
                        .queryParam("scode", code.substring(0, 4))
                        .toUriString()));
    }

    /**
     * kabuoji3の会社コードによる株価情報を取得する
     *
     * @param code 会社コード
     * @return 株価情報
     */
    public List<Kabuoji3ResultBean> kabuoji3(final String code) {
        final var url = UriComponentsBuilder
                .newInstance()
                .scheme("https").host("kabuoji3.com")
                .path("/stock/{code}/")
                .buildAndExpand(code.substring(0, 4))
                .toUriString();

        final var document = jsoup("kabuoji3", code, url);
        final var thOrder = readThOrder(document);

        return document.select(".table_wrap table").select("tr").stream()
                .map(tr -> tr.select("td").stream()
                        .map(Element::text)
                        .collect(Collectors.toList()))
                .filter(tdList -> tdList.size() == 7)
                .map(tdList -> Kabuoji3ResultBean.ofJsoup(thOrder, tdList))
                .collect(Collectors.toList());
    }

    /**
     * みんかぶの会社コードによる株価情報予想を取得する
     *
     * @param code 会社コード
     * @return 株価情報予想
     */
    public MinkabuResultBean minkabu(final String code) {
        return MinkabuResultBean.ofJsoup(jsoup(
                "みんかぶ",
                code,
                UriComponentsBuilder
                        .newInstance()
                        .scheme("https").host("minkabu.jp")
                        .path("/stock/{code}")
                        .buildAndExpand(code.substring(0, 4))
                        .toUriString()
        ));
    }

    /**
     * 対象URLのスクレイピングを実行する
     *
     * @param targetName 対象サイト名
     * @param code       会社コード
     * @param url        対象URL
     * @return スクレイピング結果
     */
    Document jsoup(final String targetName, final String code, final String url) {
        try {
            return Jsoup.connect(url).get();
        } catch (SocketTimeoutException e) {
            log.info("{}との通信でタイムアウトエラーが発生しました。\t企業コード:{}\tURL:{}", targetName, code, url);
            throw new FundanalyzerRuntimeException();
        } catch (IOException | RuntimeException e) {
            log.warn("株価の過程でエラーが発生しました。次のURLを確認してください。" +
                    "\t企業コード:{}\tURL:{}\tmessage:{}", code, url, e.getMessage(), e);
            throw new FundanalyzerRuntimeException();
        }
    }

    /**
     * kabuoji3のスクレイピング結果からタイトル行を識別する
     *
     * @param document スクレイピング結果
     * @return <ul><li>日付</li><li>始値</li><li>高値</li><li>安値</li><li>終値</li><li>出来高</li><li>終値調整</li></ul>
     */
    private Map<String, Integer> readThOrder(final Document document) {
        final var thList = document
                .select(".table_wrap table")
                .select("tr")
                .select("th").stream()
                .map(Element::text)
                .collect(Collectors.toList());
        try {
            return Map.of(
                    "日付", thList.indexOf("日付"),
                    "始値", thList.indexOf("始値"),
                    "高値", thList.indexOf("高値"),
                    "安値", thList.indexOf("安値"),
                    "終値", thList.indexOf("終値"),
                    "出来高", thList.indexOf("出来高"),
                    "終値調整", thList.indexOf("終値調整")
            );
        } catch (RuntimeException t) {
            log.warn("kabuoji3の表形式に問題が発生したため、読み取り出来ませんでした。\tth:{}", thList.toString());
            throw new FundanalyzerRuntimeException();
        }
    }

    @Value(staticConstructor = "of")
    static class KeyMatch {
        String key;
        String match;
    }
}
