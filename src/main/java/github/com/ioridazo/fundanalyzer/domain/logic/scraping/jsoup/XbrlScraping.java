package github.com.ioridazo.fundanalyzer.domain.logic.scraping.jsoup;

import github.com.ioridazo.fundanalyzer.domain.entity.master.ScrapingKeyword;
import github.com.ioridazo.fundanalyzer.domain.logic.scraping.jsoup.bean.FinancialTableResultBean;
import github.com.ioridazo.fundanalyzer.domain.logic.scraping.jsoup.bean.Unit;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerFileException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
@Component
public class XbrlScraping {

    private static final String TOTAL = "計";

    /**
     * 対象のフォルダ配下にあるファイルからキーワードに合致するものを返却する
     *
     * @param filePath        フォルダパス
     * @param scrapingKeyword キーワード
     * @return キーワードに合致するファイル
     */
    public Optional<File> findFile(final File filePath, final ScrapingKeyword scrapingKeyword) {
        // 対象のディレクトリから"honbun"ファイルを取得
        final var filePathList = findFilesByTitleKeywordContaining("honbun", filePath).stream()
                .filter(File::isFile)
                .map(file -> new File(filePath, file.getName()))
                // キーワードが存在するものを見つける
                .filter(filePathName -> elementsByKeyMatch(filePathName, KeyMatch.of("name", scrapingKeyword.getKeyword())).hasText())
                .collect(Collectors.toList());

        if (filePathList.size() == 1) {
            // ファイルが一つ見つかったとき
            return filePathList.stream().findFirst();
        } else if (filePathList.isEmpty()) {
            // ファイルがみつからなかったとき
            log.info("次のキーワードに合致するファイルは存在しませんでした。\t財務諸表名:{}\tキーワード:{}",
                    scrapingKeyword.getRemarks(), scrapingKeyword.getKeyword());
            return Optional.empty();
        } else {
            // ファイルが複数見つかったとき
            filePathList.forEach(file -> log.error("複数ファイルエラー\tキーワード：{}\t対象ファイル：{}", scrapingKeyword.getKeyword(), file));
            throw new FundanalyzerFileException("ファイルが複数検出されました。スタックトレースを参考に詳細を確認してください。");

        }
    }

    /**
     * ファイルからキーワードに合致する財務諸表テーブルの科目とその値をスクレイピングする
     *
     * @param targetFile 対象ファイル
     * @param keyWord    キーワード
     * @return スクレイピングした結果のリスト
     */
    public List<FinancialTableResultBean> scrapeFinancialStatement(final File targetFile, final String keyWord) {
        final var unit = unit(targetFile, keyWord);

        final var scrapingList = elementsByKeyMatch(targetFile, KeyMatch.of("name", keyWord))
                .select(Tag.TABLE.getName())
                .select(Tag.TR.getName()).stream()
                // tdの要素をリストにする
                .map(tr -> tr.select(Tag.TD.getName()).stream()
                        .map(Element::text)
                        // tdの中から" "（空）を取り除く
                        .filter(tdText -> !tdText.equals(" "))
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());

        // 年度の順序を確認する
        final var isMain = Optional.of(true)
                .map(aBoolean -> {
                    try {
                        return scrapingList.get(1).get(0).contains("前") || scrapingList.get(1).get(1).contains("当");
                    } catch (IndexOutOfBoundsException e) {
                        return true;
                    }
                })
                .get();

        return scrapingList.stream()
                .map(tdList -> FinancialTableResultBean.ofTdList(tdList, unit, isMain))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * ファイルから財務諸表の金額単位をスクレイピングする
     *
     * @param file    対象ファイル
     * @param keyWord キーワード
     * @return 単位（金額）
     */
    Unit unit(final File file, final String keyWord) {
        if (elementsByKeyMatch(file, KeyMatch.of("name", keyWord))
                .select(Tag.TABLE.getName())
                .stream()
                .map(Element::text)
                .anyMatch(s -> s.contains(Unit.THOUSANDS_OF_YEN.getName()))) {
            return Unit.THOUSANDS_OF_YEN;
        } else if (elementsByKeyMatch(file, KeyMatch.of("name", keyWord))
                .select(Tag.TABLE.getName())
                .stream()
                .map(Element::text)
                .anyMatch(s -> s.contains(Unit.MILLIONS_OF_YEN.getName()))) {
            return Unit.MILLIONS_OF_YEN;
        } else {
            throw new FundanalyzerFileException("財務諸表の金額単位を識別できませんでした。");
        }
    }

    /**
     * ファイルから株式総数を取得し、その値をスクレイピングする
     *
     * @param file    対象のファイル
     * @param keyWord キーワード
     * @return 株式総数
     */
    public String scrapeNumberOfShares(final File file, final String keyWord) {
        final var scrapingList = elementsByKeyMatch(file, KeyMatch.of("name", keyWord))
                .select(Tag.TABLE.getName())
                .select(Tag.TR.getName()).stream()
                // tdの要素をリストにする
                .map(tr -> tr.select(Tag.TD.getName()).stream()
                        .map(Element::text)
                        .collect(Collectors.toList())
                )
                .collect(Collectors.toList());

        if (scrapingList.isEmpty()) {
            throw new FundanalyzerFileException("株式総数取得のためのテーブルが存在しなかったため、株式総数取得に失敗しました。");
        }

        try {
            // "事業年度末現在発行数"を含む項目を探す
            final var key1 = scrapingList.stream()
                    // 対象行の取得
                    .filter(tdList -> tdList.stream().anyMatch(this::isFiscalYearEndNumber))
                    .findFirst().orElseThrow().stream()
                    // 対象行から"事業年度末現在発行数"を含むカラムを取得
                    .filter(this::isFiscalYearEndNumber)
                    .findFirst()
                    .orElseThrow();

            // "事業年度末現在発行数"を含む項目の列数
            final var indexOfKey1 = scrapingList.stream()
                    .filter(tdList -> tdList.stream().anyMatch(key1::equals))
                    .findFirst().orElseThrow()
                    .indexOf(key1);

            // "計"を含む項目を探す
            final var key2 = scrapingList.stream()
                    // 対象行の取得
                    .filter(tdList -> tdList.stream().anyMatch(td -> td.contains(TOTAL)))
                    .findFirst().orElseThrow().stream()
                    // 対象行から"計"を含むカラムを取得
                    .filter(td -> td.contains(TOTAL))
                    .findFirst().orElseThrow();

            // "計"を含む項目の列数
            final var indexOfKey2 = scrapingList.indexOf(scrapingList.stream()
                    .filter(strings -> strings.stream().anyMatch(key2::equals))
                    .findFirst().orElseThrow());

            return scrapingList.get(indexOfKey2).get(indexOfKey1);
        } catch (NoSuchElementException e) {
            throw new FundanalyzerFileException("株式総数取得のためのキーワードが存在しなかったため、株式総数取得に失敗しました。");
        }
    }

    private boolean isFiscalYearEndNumber(final String td) {
        return td.contains("事業") && td.contains("年度") && td.contains("末")
                && td.contains("現在") && td.contains("発行") && td.contains("数");
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

    @Value(staticConstructor = "of")
    static class KeyMatch {
        String key;
        String match;
    }
}
