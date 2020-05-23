package github.com.ioridazo.fundanalyzer.domain.jsoup;

import github.com.ioridazo.fundanalyzer.domain.entity.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.jsoup.bean.FinancialTableResultBean;
import github.com.ioridazo.fundanalyzer.domain.jsoup.bean.PeriodResultBean;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerFileException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class HtmlScraping {

    public HtmlScraping() {
    }

    public PeriodResultBean scrapePeriod(final File filePath) {
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
        return new PeriodResultBean(periodString);
    }

    public Optional<File> findFile(final File filePath, final FinancialStatementEnum financialStatement)
            throws FundanalyzerFileException, FundanalyzerRuntimeException {
        List<File> filePathList = new ArrayList<>();

        // 対象のディレクトリから"honbun"ファイルを取得
        getFilesByTitleKeywordContaining("honbun", filePath).forEach(file -> {
            if (file.isFile()) {
                final var filePathName = new File(filePath + "/" + file.getName());
                if (elementsByKeyMatch(filePathName, new keyMatch("name", financialStatement.getKeyWord())).hasText()) {
                    // キーワードが存在したらファイルリストに加える
                    filePathList.add(filePathName);
                }
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
        System.out.println(file.getName());
        try {
            return Jsoup.parse(file, "UTF-8")
                    .getElementsByAttributeValueContaining(keyMatch.getKey(), keyMatch.getMatch());
        } catch (IOException e) {
            log.error("ファイル認識エラー\tfilePath:\"{}\"", file.getPath());
            throw new FundanalyzerRuntimeException("ファイルの認識に失敗しました。スタックトレースから詳細を確認してください。", e);
        }
    }

    @Value
    static class keyMatch {
        String key;
        String match;
    }
}
