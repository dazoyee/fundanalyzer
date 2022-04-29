package github.com.ioridazo.fundanalyzer.client.jsoup;

import github.com.ioridazo.fundanalyzer.client.jsoup.result.Kabuoji3ResultBean;
import github.com.ioridazo.fundanalyzer.client.jsoup.result.MinkabuResultBean;
import github.com.ioridazo.fundanalyzer.client.jsoup.result.NikkeiResultBean;
import github.com.ioridazo.fundanalyzer.client.jsoup.result.YahooFinanceResultBean;
import github.com.ioridazo.fundanalyzer.config.RestClientProperties;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerCircuitBreakerRecordException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerScrapingException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerShortCircuitException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class JsoupClient {

    private static final Logger log = LogManager.getLogger(JsoupClient.class);

    private static final String NIKKEI = "nikkei";
    private static final String KABUOJI3 = "kabuoji3";
    private static final String MINKABU = "minkabu";
    private static final String YAHOO_FINANCE = "yahoo-finance";

    private final RestClientProperties properties;
    private final RestTemplate restTemplate;
    private final RetryTemplate retryTemplate;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    int yahooPages = 13;

    public JsoupClient(
            final RestClientProperties properties,
            @Qualifier("rest-jsoup") final RestTemplate restTemplate,
            @Qualifier("retry-jsoup") final RetryTemplate retryTemplate,
            final CircuitBreakerRegistry circuitBreakerRegistry) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.retryTemplate = retryTemplate;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    LocalDate nowLocalDate() {
        return LocalDate.now();
    }

    /**
     * 日経の会社コードによる株価情報を取得する
     *
     * @param code 会社コード
     * @return 株価情報
     */
    public NikkeiResultBean nikkei(final String code) {
        return NikkeiResultBean.ofJsoup(getForHtml(
                NIKKEI,
                code,
                UriComponentsBuilder.fromUriString(properties.getRestClient().get(NIKKEI).getBaseUri())
                        .path("/nkd/company/")
                        .queryParam("scode", code.substring(0, 4))
                        .toUriString()
        ));
    }

    /**
     * kabuoji3の会社コードによる株価情報を取得する
     *
     * @param code 会社コード
     * @return 株価情報
     */
    public List<Kabuoji3ResultBean> kabuoji3(final String code) {
        final String url = UriComponentsBuilder.fromUriString(properties.getRestClient().get(KABUOJI3).getBaseUri())
                .path("/stock/{code}/")
                .buildAndExpand(code.substring(0, 4))
                .toUriString();

        final Document document = getForHtml(KABUOJI3, code, url);
        final Map<String, Integer> thOrder = readKabuoji3ThOrder(document);

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
        return MinkabuResultBean.ofJsoup(getForHtml(
                MINKABU,
                code,
                UriComponentsBuilder.fromUriString(properties.getRestClient().get(MINKABU).getBaseUri())
                        .path("/stock/{code}")
                        .buildAndExpand(code.substring(0, 4))
                        .toUriString()
        ));
    }

    /**
     * Yahoo!Financeの会社コードによる株価情報予想を取得する
     *
     * @param code 会社コード
     * @return 株価情報予想
     */
    public List<YahooFinanceResultBean> yahooFinance(final String code) {
        final ArrayList<YahooFinanceResultBean> yahooFinanceList = new ArrayList<>();

        int page = 1;
        while (page <= yahooPages) {     // 13ページまでを取得
            final String url = UriComponentsBuilder.fromUriString(properties.getRestClient().get(YAHOO_FINANCE).getBaseUri())
                    .path("/quote/{code}.T/history?from={fromDate}&to={toDate}&timeFrame=d&page={page}")
                    .buildAndExpand(
                            code.substring(0, 4),
                            nowLocalDate().minusYears(1).format(DateTimeFormatter.ofPattern("uuuuMMdd")),
                            nowLocalDate().format(DateTimeFormatter.ofPattern("uuuuMMdd")),
                            page
                    )
                    .toUriString();

            final Document document = getForHtml(YAHOO_FINANCE, code, url);
            final Map<String, Integer> thOrder = readYahooFinanceThOrder(document);

            document.select("table").select("tr").stream()
                    .map(tr -> {
                        final ArrayList<String> valueList = new ArrayList<>();
                        tr.select("th").stream().findFirst().ifPresent(element -> valueList.add(element.text()));
                        tr.select("td").stream()
                                .map(Element::text)
                                .forEach(valueList::add);
                        return valueList;
                    })
                    .filter(tdList -> tdList.size() == 7)
                    .map(tdList -> YahooFinanceResultBean.ofJsoup(thOrder, tdList))
                    .forEach(yahooFinanceList::add);

            page++;
        }

        return yahooFinanceList;
    }

    /**
     * 対象URLのスクレイピングを実行する
     *
     * @param circuitBreakerName サーキットブレーカー名
     * @param code               会社コード
     * @param url                対象URL
     * @return スクレイピング結果
     */
    Document getForHtml(final String circuitBreakerName, final String code, final String url) {
        // retry
        return retryTemplate.execute(context -> {

            try {
                // circuitBreaker
                return circuitBreakerRegistry.circuitBreaker(circuitBreakerName)
                        .executeSupplier(() -> {
                            try {
                                // scraping
                                return Jsoup.parse(Objects.requireNonNull(restTemplate.getForObject(url, String.class)));
                            } catch (final NullPointerException e) {
                                throw new FundanalyzerScrapingException(MessageFormat.format(
                                        "{0}からHTMLを取得できませんでした。\t企業コード:{1}\tURL:{2}",
                                        circuitBreakerName,
                                        code,
                                        url
                                ), e);
                            } catch (final RestClientResponseException e) {
                                throw new FundanalyzerCircuitBreakerRecordException(MessageFormat.format(
                                        "{0}から200以外のHTTPステータスコードが返却されました。\tHTTPステータスコード:{1}\t企業コード:{2}\tURL:{3}",
                                        circuitBreakerName,
                                        e.getRawStatusCode(),
                                        code,
                                        url
                                ), e);
                            } catch (final ResourceAccessException e) {
                                throw new FundanalyzerCircuitBreakerRecordException(MessageFormat.format(
                                        "{0}との通信でタイムアウトエラーが発生しました。\t企業コード:{1}\tURL:{2}",
                                        circuitBreakerName,
                                        code,
                                        url
                                ), e);
                            }
                        });
            } catch (final CallNotPermittedException e) {
                throw new FundanalyzerShortCircuitException(circuitBreakerName + "との通信でサーキットブレーカーがオープンしました。");
            }
        });
    }

    /**
     * kabuoji3のスクレイピング結果からタイトル行を識別する
     *
     * @param document スクレイピング結果
     * @return <ul><li>日付</li><li>始値</li><li>高値</li><li>安値</li><li>終値</li><li>出来高</li><li>終値調整</li></ul>
     */
    private Map<String, Integer> readKabuoji3ThOrder(final Document document) {
        final List<String> thList = document
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
        } catch (Throwable t) {
            log.warn("kabuoji3の表形式に問題が発生したため、読み取り出来ませんでした。\tth:{}", thList);
            throw new FundanalyzerScrapingException(t);
        }
    }

    /**
     * yahoo-financeのスクレイピング結果からタイトル行を識別する
     *
     * @param document スクレイピング結果
     * @return <ul><li>日付</li><li>始値</li><li>高値</li><li>安値</li><li>終値</li><li>出来高</li><li>調整後終値</li></ul>
     */
    private Map<String, Integer> readYahooFinanceThOrder(final Document document) {
        final List<String> thList = document
                .select("table")
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
                    "調整後終値", thList.indexOf("調整後終値*")
            );
        } catch (Throwable t) {
            log.warn("yahoo-financeの表形式に問題が発生したため、読み取り出来ませんでした。\tth:{}", thList);
            throw new FundanalyzerScrapingException(t);
        }
    }

    @SuppressWarnings("RedundantModifiersValueLombok")
    @lombok.Value(staticConstructor = "of")
    static class KeyMatch {
        private final String key;
        private final String match;
    }

    public static class RecordFailurePredicate implements Predicate<Throwable> {

        @Override
        public boolean test(final Throwable throwable) {
            return throwable instanceof FundanalyzerCircuitBreakerRecordException;
        }
    }
}
