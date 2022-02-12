package github.com.ioridazo.fundanalyzer.client.jsoup;

import github.com.ioridazo.fundanalyzer.client.jsoup.result.Kabuoji3ResultBean;
import github.com.ioridazo.fundanalyzer.client.jsoup.result.MinkabuResultBean;
import github.com.ioridazo.fundanalyzer.client.jsoup.result.NikkeiResultBean;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerCircuitBreakerRecordException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerScrapingException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class JsoupClient {

    private static final Logger log = LogManager.getLogger(JsoupClient.class);

    private static final String CIRCUIT_BREAKER_NIKKEI = "nikkei";
    private static final String CIRCUIT_BREAKER_KABUOJI3 = "kabuoji3";
    private static final String CIRCUIT_BREAKER_MINKABU = "minkabu";

    private final RetryTemplate retryTemplate;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final String nikkeiBaseUri;
    private final String kabuoji3BaseUri;
    private final String minkabuBaseUri;

    public JsoupClient(
            @Qualifier("jsoup-retry") final RetryTemplate retryTemplate,
            final CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.config.rest-template.nikkei.base-uri}") final String nikkeiBaseUri,
            @Value("${app.config.rest-template.kabuoji3.base-uri}") final String kabuoji3BaseUri,
            @Value("${app.config.rest-template.minkabu.base-uri}") final String minkabuBaseUri) {
        this.retryTemplate = retryTemplate;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.nikkeiBaseUri = nikkeiBaseUri;
        this.kabuoji3BaseUri = kabuoji3BaseUri;
        this.minkabuBaseUri = minkabuBaseUri;
    }

    /**
     * 日経の会社コードによる株価情報を取得する
     *
     * @param code 会社コード
     * @return 株価情報
     */
    public NikkeiResultBean nikkei(final String code) {
        return NikkeiResultBean.ofJsoup(jsoup(
                CIRCUIT_BREAKER_NIKKEI,
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

        final var document = jsoup(CIRCUIT_BREAKER_KABUOJI3, code, url);
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
                CIRCUIT_BREAKER_MINKABU,
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
     * @param circuitBreakerName サーキットブレーカー名
     * @param code               会社コード
     * @param url                対象URL
     * @return スクレイピング結果
     */
    Document jsoup(final String circuitBreakerName, final String code, final String url) {
        return retryTemplate.execute(context -> {

            try {
                return circuitBreakerRegistry.circuitBreaker(circuitBreakerName)
                        .executeSupplier(() -> {
                            try {
                                return Jsoup.connect(url).get();
                            } catch (final SocketTimeoutException e) {
                                throw new FundanalyzerCircuitBreakerRecordException(MessageFormat.format(
                                        "{0}との通信でタイムアウトエラーが発生しました。\t企業コード:{1}\tURL:{2}",
                                        circuitBreakerName,
                                        code,
                                        url
                                ), e);
                            } catch (final HttpStatusException e) {
                                throw new FundanalyzerCircuitBreakerRecordException(MessageFormat.format(
                                        "{0}から200以外のHTTPステータスコードが返却されました。\tHTTPステータスコード:{1}\t企業コード:{2}\tURL:{3}",
                                        circuitBreakerName,
                                        e.getStatusCode(),
                                        code,
                                        url
                                ), e);
                            } catch (final IOException | RuntimeException e) {
                                throw new FundanalyzerCircuitBreakerRecordException(MessageFormat.format(
                                        "{0}との通信で想定外のエラーが発生しました。次のURLを確認してください。\t企業コード:{1}\tURL:{2}",
                                        circuitBreakerName,
                                        code,
                                        url
                                ), e);
                            }
                        });
            } catch (final CallNotPermittedException e) {
                throw new FundanalyzerScrapingException(CIRCUIT_BREAKER_NIKKEI + "との通信でサーキットブレーカーがオープンしました。");
            }
        });
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
        } catch (Throwable t) {
            log.warn("kabuoji3の表形式に問題が発生したため、読み取り出来ませんでした。\tth:{}", thList.toString());
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
