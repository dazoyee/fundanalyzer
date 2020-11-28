package github.com.ioridazo.fundanalyzer.domain.scraping.jsoup;

import github.com.ioridazo.fundanalyzer.domain.scraping.jsoup.bean.Kabuoji3ResultBean;
import github.com.ioridazo.fundanalyzer.domain.scraping.jsoup.bean.NikkeiResultBean;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerFileException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
public class StockScraping {

    public NikkeiResultBean nikkei(final String code) {
        final var url = UriComponentsBuilder
                .newInstance()
                .scheme("https").host("www.nikkei.com")
                .path("/nkd/company/")
                .queryParam("scode", code.substring(0, 4))
                .toUriString();
        try {
            final var document = Jsoup.connect(url).get();
            return new NikkeiResultBean(
                    document.select(".m-stockPriceElm dd").first().text(),
                    document.select(".m-stockInfo_date").first().text(),
                    document.select(".m-stockInfo_detail_left li").stream()
                            .filter(e -> e.text().contains("始値")).map(Element::text).findAny().orElse(null),
                    document.select(".m-stockInfo_detail_left li").stream()
                            .filter(e -> e.text().contains("高値")).map(Element::text).findAny().orElse(null),
                    document.select(".m-stockInfo_detail_left li").stream()
                            .filter(e -> e.text().contains("安値")).map(Element::text).findAny().orElse(null),
                    document.select(".m-stockInfo_detail_right li").stream()
                            .filter(e -> e.text().contains("売買高")).map(Element::text).findAny().orElse(null),
                    document.select(".m-stockInfo_detail_right li").stream()
                            .filter(e -> e.text().contains("PER")).map(Element::text).findAny().orElse(null),
                    document.select(".m-stockInfo_detail_left li").stream()
                            .filter(e -> e.text().contains("PBR")).map(Element::text).findAny().orElse(null),
                    document.select(".m-stockInfo_detail_left li").stream()
                            .filter(e -> e.text().contains("ROE")).map(Element::text).findAny().orElse(null),
                    document.select(".m-stockInfo_detail_left li").stream()
                            .filter(e -> e.text().contains("普通株式数")).map(Element::text).findAny().orElse(null),
                    document.select(".m-stockInfo_detail_left li").stream()
                            .filter(e -> e.text().contains("時価総額")).map(Element::text).findAny().orElse(null),
                    document.select(".m-stockInfo_detail_left li").stream()
                            .filter(e -> e.text().contains("株式益回り")).map(Element::text).findAny().orElse(null),
                    document.select(".m-stockInfo_detail_left li").stream()
                            .filter(e -> e.text().contains("株主優待")).map(Element::text).findAny().orElse(null)
            );
        } catch (SocketTimeoutException e) {
            log.info("日経との通信でタイムアウトエラーが発生しました。\t企業コード:{}\tURL:{}", code, url);
            throw new FundanalyzerRuntimeException();
        } catch (Throwable t) {
            log.warn("株価の過程でエラーが発生しました。次のURLを確認してください。" +
                    "\t企業コード:{}\tURL:{}\tmessage:{}", code, url, t.getMessage()
            );
            throw new FundanalyzerRuntimeException();
        }
    }

    public List<Kabuoji3ResultBean> kabuoji3(final String code) {
        final var resultBeanList = new ArrayList<Kabuoji3ResultBean>();
        final var url = UriComponentsBuilder
                .newInstance()
                .scheme("https").host("kabuoji3.com")
                .path("/stock/{code}/")
                .buildAndExpand(code.substring(0, 4))
                .toUriString();

        try {
            final var document = Jsoup.connect(url).get();
            final var thOrder = readThOrder(document.select(".table_wrap table").select("tr").select("th"));

            document.select(".table_wrap table")
                    .select("tr")
                    .forEach(tr -> {
                        final var tdList = tr.select("td").stream()
                                .map(Element::text)
                                .collect(Collectors.toList());
                        if (tdList.size() == 7) {
                            resultBeanList.add(Kabuoji3ResultBean.of(thOrder, tdList));
                        }
                    });
            return resultBeanList;
        } catch (SocketTimeoutException e) {
            log.info("kabuoji3との通信でタイムアウトエラーが発生しました。\t企業コード:{}\tURL:{}", code, url);
            throw new FundanalyzerRuntimeException();
        } catch (Throwable t) {
            log.warn("株価の過程でエラーが発生しました。次のURLを確認してください。" +
                    "\t企業コード:{}\tURL:{}\tmessage:{}", code, url, t.getMessage()
            );
            throw new FundanalyzerRuntimeException();
        }
    }

    Elements elementsByKeyMatch(final File file, final KeyMatch keyMatch) {
        try {
            return Jsoup.parse(file, "UTF-8")
                    .getElementsByAttributeValue(keyMatch.getKey(), keyMatch.getMatch());
        } catch (IOException e) {
            log.error("ファイル形式に問題があり、読み取りに失敗しました。\t対象ファイルパス:\"{}\"", file.getPath());
            throw new FundanalyzerFileException("ファイルの認識に失敗しました。スタックトレースから詳細を確認してください。", e);
        }
    }

    Elements elementsContainingText(final File file, final String keyword) {
        try {
            return Jsoup.parse(file, "UTF-8").getElementsContainingText(keyword);
        } catch (IOException e) {
            log.error("ファイル形式に問題があり、読み取りに失敗しました。\t対象ファイルパス:\"{}\"", file.getPath());
            throw new FundanalyzerRuntimeException("ファイルの認識に失敗しました。スタックトレースから詳細を確認してください。", e);
        }
    }

    /**
     * 対象のフォルダからキーワードを含むファイルを見つける
     *
     * @param keyword    キーワード
     * @param targetFile 対象のフォルダ
     * @return キーワードを含むファイルのリスト
     */
    @SuppressWarnings("SameParameterValue")
    private List<File> findFilesByTitleKeywordContaining(final String keyword, final File targetFile) {
        final var targetFileList = List.of(Objects.requireNonNullElse(targetFile.listFiles(), File.listRoots()));

        return targetFileList.stream()
                .filter(file -> file.getName().contains(keyword))
                .collect(Collectors.toList());
    }

    private Map<String, Integer> readThOrder(final Elements elements) {
        final var thList = elements.stream()
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
            throw new FundanalyzerRuntimeException();
        }
    }

    @Value(staticConstructor = "of")
    static class KeyMatch {
        String key;
        String match;
    }
}
