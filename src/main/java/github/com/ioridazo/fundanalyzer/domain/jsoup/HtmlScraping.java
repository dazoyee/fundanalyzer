package github.com.ioridazo.fundanalyzer.domain.jsoup;

import github.com.ioridazo.fundanalyzer.domain.jsoup.bean.FinancialTableResultBean;
import github.com.ioridazo.fundanalyzer.domain.jsoup.bean.Kabuoji3ResultBean;
import github.com.ioridazo.fundanalyzer.domain.jsoup.bean.NikkeiResultBean;
import github.com.ioridazo.fundanalyzer.domain.jsoup.bean.NumberOfSharesResultBean;
import github.com.ioridazo.fundanalyzer.domain.jsoup.bean.Unit;
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
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class HtmlScraping {

    public HtmlScraping() {
    }

    public Optional<File> findFile(final File filePath, final String keyword)
            throws FundanalyzerFileException, FundanalyzerRuntimeException {
        List<File> filePathList = new ArrayList<>();

        try {
            // 対象のディレクトリから"honbun"ファイルを取得
            getFilesByTitleKeywordContaining("honbun", filePath).forEach(file -> {
                if (file.isFile()) {
                    final var filePathName = new File(filePath + "/" + file.getName());
                    if (elementsByKeyMatch(filePathName, new keyMatch("name", keyword)).hasText()) {
                        // キーワードが存在したらファイルリストに加える
                        filePathList.add(filePathName);
                    }
                }
            });
        } catch (Throwable t) {
            log.error("想定外のエラーが発生しました。", t);
            throw new FundanalyzerFileException(t);
        }

        if (filePathList.size() > 1) {
            filePathList.forEach(file -> log.error("複数ファイルエラー\tキーワード：{}\t対象ファイル：{}", keyword, file));
            throw new FundanalyzerFileException("ファイルが複数検出されました。スタックトレースを参考に詳細を確認してください。");
        }
        return filePathList.stream().findAny();
    }

    public List<FinancialTableResultBean> scrapeFinancialStatement(
            final File file, final String keyWord) throws FundanalyzerFileException {
        try {
            var resultBeanList = new ArrayList<FinancialTableResultBean>();

            Unit unit;
            if (elementsByKeyMatch(file, new keyMatch("name", keyWord))
                    .select("table")
                    .stream()
                    .map(Element::text)
                    .anyMatch(s -> s.contains(Unit.THOUSANDS_OF_YEN.getName()))) {
                unit = Unit.THOUSANDS_OF_YEN;
            } else if (elementsByKeyMatch(file, new keyMatch("name", keyWord))
                    .select("table")
                    .stream()
                    .map(Element::text)
                    .anyMatch(s -> s.contains(Unit.MILLIONS_OF_YEN.getName()))) {
                unit = Unit.MILLIONS_OF_YEN;
            } else {
                throw new FundanalyzerRuntimeException("財務諸表の金額単位を識別できませんでした。");
            }

            // ファイルをスクレイピング
            for (Element tr : elementsByKeyMatch(file, new keyMatch("name", keyWord))
                    .select("table")
                    .select("tr")) {
                var tdList = new ArrayList<String>();
                tr.select("td").forEach(td -> {
                    if (!td.text().equals(" "))
                        tdList.add(td.text());
                });
//                System.out.println(tdList);
                // TODO 取得前に年度の確認
                // 各要素をbeanに詰める
                if (tdList.size() == 2) {
                    resultBeanList.add(new FinancialTableResultBean(tdList.get(0), null, tdList.get(1), unit));
                } else if (tdList.size() == 3) {
                    resultBeanList.add(new FinancialTableResultBean(tdList.get(0), tdList.get(1), tdList.get(2), unit));
                }
            }

            return resultBeanList;
        } catch (Throwable t) {
            log.error("想定外のエラーが発生しました。", t);
            throw new FundanalyzerFileException(t);
        }
    }

    public String findNumberOfShares(final File file, final String keyWord) throws FundanalyzerFileException {
        try {
            var resultBeanList = new ArrayList<NumberOfSharesResultBean>();

            // ファイルをスクレイピング
            for (Element tr : elementsByKeyMatch(file, new keyMatch("name", keyWord))
                    .select("table")
                    .select("tr")) {
                var tdList = new ArrayList<String>();
                tr.select("td").forEach(td -> tdList.add(td.text()));
//                System.out.println(tdList);
                // 各要素をbeanに詰める
                if (tdList.size() == 5) {
                    resultBeanList.add(new NumberOfSharesResultBean(
                            tdList.get(0), tdList.get(1), tdList.get(2), tdList.get(3), tdList.get(4)));
                } else {
                    log.info("株式総数取得のためのテーブル形式に合致しなかったため、取得範囲対象外の項目として処理を進めます。");
                }
            }
            // FIXME 当てずっぽうで値を取得しているので、正しく取得するか、バリデーションチェックを行う
            return resultBeanList.stream()
                    .map(NumberOfSharesResultBean::getFiscalYearEndNumber)
                    .collect(Collectors.toList())
                    .get(resultBeanList.size() - 1);
        } catch (Throwable t) {
            log.error("想定外のエラーが発生しました。", t);
            throw new FundanalyzerFileException(t);
        }
    }

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

    List<File> getFilesByTitleKeywordContaining(final String keyword, final File filePath) {
        return Stream.of(Objects.requireNonNull(filePath.listFiles(), filePath.getPath()))
                .filter(file -> file.getName().contains(keyword))
                .collect(Collectors.toList());
    }

    Elements elementsByKeyMatch(final File file, final keyMatch keyMatch) {
        try {
            return Jsoup.parse(file, "UTF-8")
                    .getElementsByAttributeValue(keyMatch.getKey(), keyMatch.getMatch());
        } catch (IOException e) {
            log.error("ファイル形式に問題があり、読み取りに失敗しました。\t対象ファイルパス:\"{}\"", file.getPath());
            throw new FundanalyzerRuntimeException("ファイルの認識に失敗しました。スタックトレースから詳細を確認してください。", e);
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

    @Value
    static class keyMatch {
        String key;
        String match;
    }
}
