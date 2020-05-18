package github.com.ioridazo.fundanalyzer.domain.jsoup;

import github.com.ioridazo.fundanalyzer.domain.entity.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.jsoup.bean.FinancialTableResultBean;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerFileException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.chrono.JapaneseChronology;
import java.time.chrono.JapaneseDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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

    public Map<String, String> scrapePeriod(final File filePath) {
        final var file = getFilesByTitleKeywordContaining("header", filePath)
                .stream().findAny().orElseThrow(FundanalyzerRuntimeException::new);
        final var fiscalYearCoverPage = elementsByKeyMatch(file, new keyMatch("name", "FiscalYearCoverPage"));
        final var accountingPeriodCoverPage = elementsByKeyMatch(file, new keyMatch("name", "AccountingPeriodCoverPage"));

        String periodString = null;
        if (fiscalYearCoverPage.hasText()) {
            periodString = fiscalYearCoverPage.text();
        } else if (accountingPeriodCoverPage.hasText()) {
            periodString = accountingPeriodCoverPage.text();
        }
        final var period = new Period(periodString);
        return Map.of("period", period.getPeriod(), "fromDate", period.getFromDate(), "toDate", period.getToDate());

    }

    public Optional<File> findFile(final File filePath, final FinancialStatementEnum financialStatement)
            throws FundanalyzerFileException, FundanalyzerRuntimeException {
        List<File> filePathList = new ArrayList<>();

        // 対象のディレクトリから"honbun"ファイルを取得
        getFilesByTitleKeywordContaining("honbun", filePath).forEach(file -> {
            final var filePathName = new File(filePath + "/" + file.getName());
            if (elementsByKeyMatch(filePathName, new keyMatch("name", financialStatement.getKeyWord())).hasText()) {
                // キーワードが存在したらファイルリストに加える
                filePathList.add(filePathName);
            }
        });

        if (filePathList.size() > 1) {
            filePathList.forEach(file -> log.error("複数ファイルエラー\tキーワード：{}\t対象ファイル：{}", financialStatement.getKeyWord(), file));
            throw new FundanalyzerFileException(financialStatement.getKeyWord() + "に関するファイルが複数検出されました。スタックトレースを参考に詳細を確認してください。");
        }
        log.info("ファイル正常応答\tキーワード：{}\t対象ファイル：{}", financialStatement.getKeyWord(), filePathList.stream().findAny().orElse(null));
        return filePathList.stream().findAny();
    }

    public List<FinancialTableResultBean> scrapeFinancialStatement(final File file, final String keyWord) {
        var resultBeanList = new ArrayList<FinancialTableResultBean>();
        // ファイルをスクレイピング
        elementsByKeyMatch(file, new keyMatch("name", keyWord))
                .select("table")
                .select("tr")
                .forEach(tr -> {
                    var tdList = new ArrayList<String>();
                    tr.select("td").forEach(td -> tdList.add(td.text()));
                    System.out.println(tdList); // FIXME
                    // 各要素をbeanに詰める
                    resultBeanList.add(new FinancialTableResultBean(tdList.get(0), tdList.get(1), tdList.get(2)));
                });
        return resultBeanList;
    }

    List<File> getFilesByTitleKeywordContaining(final String keyword, final File filePath) {
        return Stream.of(Objects.requireNonNull(filePath.listFiles()))
                .filter(file -> file.getName().contains(keyword))
                .collect(Collectors.toList());
    }

    Elements elementsByKeyMatch(final File file, final keyMatch keyMatch) {
        try {
            return Jsoup.parse(file, "UTF-8")
                    .getElementsByAttributeValueContaining(keyMatch.getKey(), keyMatch.getMatch());
        } catch (IOException e) {
            log.error("ファイル認識エラー\tfilePath:\"{}\"", file.getPath());
            throw new FundanalyzerRuntimeException("ファイルの認識に失敗しました。スタックトレースから詳細を確認してください。", e);
        }
    }

    @Value
    static class Period {
        String period;
        String fromDate;
        String toDate;

        public Period(String scraped) {
            this.period = Objects.requireNonNull(scraped)
                    .substring(scraped.indexOf("第"), scraped.indexOf("期") + 1);
            this.fromDate = parseLocalDateString(Objects.requireNonNull(scraped)
                    .substring(scraped.indexOf("自　") + 2, scraped.indexOf("日") + 1));
            this.toDate = parseLocalDateString(Objects.requireNonNull(scraped)
                    .substring(scraped.indexOf("至　") + 2, scraped.lastIndexOf("日") + 1));
        }

        String parseLocalDateString(String scrapedDate) {
            final var normalizeDate = Normalizer.normalize(scrapedDate, Normalizer.Form.NFKC);
            try {
                return LocalDate.from(JapaneseDate.from(
                        DateTimeFormatter
                                .ofPattern("Gy年M月d日")
                                .withChronology(JapaneseChronology.INSTANCE)
                                .parse(normalizeDate))).toString();
            } catch (DateTimeParseException e) {
                return LocalDate.parse(normalizeDate, DateTimeFormatter.ofPattern("y年M月d日")).toString();
            }
        }
    }

    @Value
    static class keyMatch {
        String key;
        String match;
    }
}
