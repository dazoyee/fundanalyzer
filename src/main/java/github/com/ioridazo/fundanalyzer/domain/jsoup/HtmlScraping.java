package github.com.ioridazo.fundanalyzer.domain.jsoup;

import github.com.ioridazo.fundanalyzer.domain.jsoup.bean.FinancialTableResultBean;
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
        } catch (Exception e) {
            log.error("想定外のエラーが発生しました。", e);
            throw new FundanalyzerFileException(e);
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
                System.out.println(tdList);
                // TODO 取得前に年度の確認
                // 各要素をbeanに詰める
                if (tdList.size() == 2) {
                    resultBeanList.add(new FinancialTableResultBean(tdList.get(0), null, tdList.get(1), unit));
                } else if (tdList.size() == 3) {
                    resultBeanList.add(new FinancialTableResultBean(tdList.get(0), tdList.get(1), tdList.get(2), unit));
                }
            }

            return resultBeanList;
        } catch (Exception e) {
            log.error("想定外のエラーが発生しました。", e);
            throw new FundanalyzerFileException(e);
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
                System.out.println(tdList);
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
        } catch (Exception e) {
            log.error("想定外のエラーが発生しました。", e);
            throw new FundanalyzerFileException(e);
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

    @Value
    static class keyMatch {
        String key;
        String match;
    }
}
