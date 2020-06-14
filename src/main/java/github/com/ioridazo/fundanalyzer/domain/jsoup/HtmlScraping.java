package github.com.ioridazo.fundanalyzer.domain.jsoup;

import github.com.ioridazo.fundanalyzer.domain.jsoup.bean.FinancialTableResultBean;
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

        if (filePathList.size() > 1) {
            filePathList.forEach(file -> log.error("複数ファイルエラー\tキーワード：{}\t対象ファイル：{}", keyword, file));
            throw new FundanalyzerFileException("ファイルが複数検出されました。スタックトレースを参考に詳細を確認してください。");
        }
        return filePathList.stream().findAny();
    }

    public List<FinancialTableResultBean> scrapeFinancialStatement(final File file, final String keyWord) {
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
            tr.select("td").forEach(td -> tdList.add(td.text()));
            System.out.println(tdList); // FIXME
            // 各要素をbeanに詰める
            resultBeanList.add(new FinancialTableResultBean(tdList.get(0), tdList.get(1), tdList.get(2), unit));
        }

        log.info("スクレイピング処理を正常に実施しました。\t対象ファイル:{}", file.getPath());

        return resultBeanList;
    }

    public String findNumberOfShares(final File filePath) {
        List<File> filePathList = new ArrayList<>();

        // 対象のディレクトリから"honbun"ファイルを取得
        getFilesByTitleKeywordContaining("honbun", filePath).forEach(file -> {
            if (file.isFile()) {
                final var filePathName = new File(filePath + "/" + file.getName());
                if (elementsContainingText(filePathName, "株式総数").hasText()) {
                    // キーワードが存在したらファイルリストに加える
                    filePathList.add(filePathName);
                }
            }
        });

        if (filePathList.size() > 1) {
            filePathList.forEach(file -> log.error("複数ファイルエラー\tキーワード：{}\t対象ファイル：{}", "株式総数", file));
            return "複数ファイルあり";
        } else if (filePathList.isEmpty()) {
            return "ファイルなし";
        } else {
            return filePathList.stream().findAny().get().getPath();
        }
    }

    List<File> getFilesByTitleKeywordContaining(final String keyword, final File filePath) {
        return Stream.of(Objects.requireNonNull(filePath.listFiles()))
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
