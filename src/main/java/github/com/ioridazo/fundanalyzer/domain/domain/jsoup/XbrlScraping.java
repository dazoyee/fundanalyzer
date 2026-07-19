package github.com.ioridazo.fundanalyzer.domain.domain.jsoup;

import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.ScrapingKeywordEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.jsoup.bean.FinancialTableResultBean;
import github.com.ioridazo.fundanalyzer.domain.domain.jsoup.bean.Unit;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerFileException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerScrapingException;
import lombok.Value;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class XbrlScraping {

    private static final Logger log = LogManager.getLogger(XbrlScraping.class);
    private static final Pattern UNEXPECTED_SHARE_UNIT_PATTERN = Pattern.compile("(千|百|百万|億)株");

    private static final String TOTAL = "計";

    /**
     * 対象のフォルダ配下にあるファイルからキーワードに合致するものを返却する
     *
     * @param filePath              フォルダパス
     * @param scrapingKeywordEntity キーワード
     * @param document              ドキュメント
     * @return キーワードに合致するファイル
     */
    public Optional<File> findFile(
            final File filePath, final ScrapingKeywordEntity scrapingKeywordEntity, final Document document) {
        // 対象のディレクトリから"honbun"ファイルを取得
        final var filePathList = findFilesByTitleKeywordContaining("honbun", filePath).stream()
                .filter(File::isFile)
                .map(file -> new File(filePath, file.getName()))
                // キーワードが存在するものを見つける
                .filter(filePathName -> elementsByKeyMatch(filePathName, KeyMatch.of("name", scrapingKeywordEntity.getKeyword())).hasText())
                .toList();

        if (filePathList.size() == 1) {
            // ファイルが一つ見つかったとき
            log.info(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format(
                            "次のキーワードにてファイルを確認しました。\t財務諸表名:{0}\tキーワード:{1}",
                            scrapingKeywordEntity.getRemarks().orElse("null"),
                            scrapingKeywordEntity.getKeyword()
                    ),
                    document,
                    Category.SCRAPING,
                    Process.of(scrapingKeywordEntity)
            ));
            return filePathList.stream().findFirst();
        } else if (filePathList.isEmpty()) {
            // ファイルがみつからなかったとき
            log.debug(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format(
                            "次のキーワードに合致するファイルは存在しませんでした。\t財務諸表名:{0}\tキーワード:{1}",
                            scrapingKeywordEntity.getRemarks().orElse("null"),
                            scrapingKeywordEntity.getKeyword()
                    ),
                    document,
                    Category.SCRAPING,
                    Process.of(scrapingKeywordEntity)
            ));
            return Optional.empty();
        } else {
            // ファイルが複数見つかったとき
            filePathList.forEach(file -> log.error(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format(
                            "複数ファイルエラー\tキーワード：{0}\t対象ファイル：{1}",
                            scrapingKeywordEntity.getKeyword(),
                            file
                    ),
                    document,
                    Category.SCRAPING,
                    Process.of(scrapingKeywordEntity)
            )));
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

        // "売上原価明細書"を除外する
        final List<List<String>> ignoreList = getScrapingList(targetFile, "jpcrp_cor:DetailedScheduleOfCostOfSalesTextBlock").stream()
                // 年度項目は除外リストから除外
                .filter(list -> !(list.stream().anyMatch(s -> s.contains("前")) && list.stream().anyMatch(s -> s.contains("当"))))
                .filter(list -> list.stream().noneMatch(s -> s.contains("単位")))
                .toList();

        final List<List<String>> scrapingList = ignoreList.isEmpty() ?
                getScrapingList(targetFile, keyWord)
                :
                getScrapingList(targetFile, keyWord).stream()
                        .filter(sl -> ignoreList.stream().noneMatch(sl::equals))
                        .toList();

        // 年度以外の情報を取り除く
        scrapingList.get(1).removeIf(s -> s.contains("注記"));

        if (scrapingList.stream().allMatch(list -> list.size() <= 2)) {
            // 当期のみの場合
            return scrapingList.stream()
                    .map(tdList -> FinancialTableResultBean.ofTdList(tdList, unit))
                    .filter(Objects::nonNull)
                    .toList();
        } else if (scrapingList.stream().allMatch(list -> list.size() <= 4)) {
            // 前期と当期がある場合
            final boolean isMain = isMainOrderOfYear(scrapingList, targetFile);
            return scrapingList.stream()
                    .map(tdList -> FinancialTableResultBean.ofTdList(tdList, unit, isMain))
                    .filter(Objects::nonNull)
                    .toList();
        } else {
            throw new FundanalyzerScrapingException(
                    "定形外の財務諸表でした。詳細を確認してください。\nファイルパス:" + targetFile);
        }
    }

    private List<List<String>> getScrapingList(final File targetFile, final String keyWord) {
        return elementsByKeyMatch(targetFile, KeyMatch.of("name", keyWord))
                .select(Tag.TABLE.getName())
                .select(Tag.TR.getName()).stream()
                // tdの要素をリストにする
                .map(tr -> tr.select(Tag.TD.getName()).stream()
                        .map(Element::text)
                        // 不要なエレメントを削除
                        .filter(tdText -> Stream.of(" ", "", "円").noneMatch(tdText::equals))
                        .collect(Collectors.toList()))
                // 不要なエレメントを削除
                .filter(list -> 0 != list.size())
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
                .anyMatch(text -> Unit.THOUSANDS_OF_YEN.getName().stream().anyMatch(text::contains))) {
            return Unit.THOUSANDS_OF_YEN;
        } else if (elementsByKeyMatch(file, KeyMatch.of("name", keyWord))
                .select(Tag.TABLE.getName())
                .stream()
                .map(Element::text)
                .anyMatch(text -> Unit.MILLIONS_OF_YEN.getName().stream().anyMatch(text::contains))) {
            return Unit.MILLIONS_OF_YEN;
        } else {
            throw new FundanalyzerScrapingException("財務諸表の金額単位を識別できませんでした。");
        }
    }

    /**
     * 財務諸表の年度の順序を確認する
     *
     * @param scrapingList スクレイピング結果
     * @param targetFile   対象ファイル
     * @return 左から前年度→当年度の順なら true、逆なら false
     */
    private boolean isMainOrderOfYear(final List<List<String>> scrapingList, final File targetFile) {
        final List<String> headerRow = scrapingList.get(1);
        if (headerRow.size() == 2) {
            return compareYearOrder(headerRow.get(0), headerRow.get(1), targetFile);
        }
        if (headerRow.size() == 3) {
            return compareYearOrder(headerRow.get(0), headerRow.get(2), targetFile);
        }
        throw new FundanalyzerScrapingException(
                "年度に関して対象の財務諸表は定形外でした。詳細を確認してください。\nファイルパス:" + targetFile);
    }

    /**
     * 2 つのヘッダ文字列を比較し、左側が前年度（古い）なら true を返す
     *
     * @param left       比較元（左側のヘッダ文字列）
     * @param right      比較先（右側のヘッダ文字列）
     * @param targetFile エラー時に出力する対象ファイル
     * @return 左が前年度の順序なら true、逆なら false
     * @throws FundanalyzerScrapingException 順序を判定できないとき
     */
    private boolean compareYearOrder(final String left, final String right, final File targetFile) {
        if (left.contains("前") && right.contains("当")) {
            return true;
        }
        if (left.contains("当") && right.contains("前")) {
            return false;
        }

        try {
            if (left.contains("第") && left.contains("期")) {
                final int leftPeriod = parsePeriodNumber(left);
                final int rightPeriod = parsePeriodNumber(right);
                if (leftPeriod < rightPeriod) {
                    return true;
                }
                if (leftPeriod > rightPeriod) {
                    return false;
                }
            }
            if (left.contains("年度")) {
                final int leftYear = parseFiscalYear(left);
                final int rightYear = parseFiscalYear(right);
                if (leftYear < rightYear) {
                    return true;
                }
                if (leftYear > rightYear) {
                    return false;
                }
            }
        } catch (final NumberFormatException | StringIndexOutOfBoundsException e) {
            throw new FundanalyzerScrapingException(
                    "年度に関して対象の財務諸表は定形外でした。次の値を確認してください。" +
                    "\t'" + left + "' and '" + right + "'" +
                    "\nファイルパス:" + targetFile);
        }

        throw new FundanalyzerScrapingException(
                "年度に関して対象の財務諸表は定形外でした。詳細を確認してください。\nファイルパス:" + targetFile);
    }

    /**
     * '第' と '期' の間にある期数を整数として取り出す
     *
     * @param header ヘッダ文字列（例: "第10期" など）
     * @return 期数
     */
    private int parsePeriodNumber(final String header) {
        return Integer.parseInt(header.substring(
                header.indexOf("第") + 1,
                header.indexOf("期")
        ));
    }

    /**
     * '年度' の前にある年数を整数として取り出す
     *
     * @param header ヘッダ文字列（例: "2020年度" など）
     * @return 年数
     */
    private int parseFiscalYear(final String header) {
        return Integer.parseInt(header.substring(0, header.indexOf("年度")));
    }

    /**
     * ファイルから株式総数を取得し、その値をスクレイピングする
     *
     * @param file    対象のファイル
     * @param keyWord キーワード
     * @return 株式総数
     */
    public String scrapeNumberOfShares(final File file, final String keyWord) {
        final var tables = elementsByKeyMatch(file, KeyMatch.of("name", keyWord))
                .select(Tag.TABLE.getName());

        if (tables.isEmpty()) {
            throw new FundanalyzerScrapingException("株式総数取得のためのテーブルが存在しなかったため、株式総数取得に失敗しました。");
        }

        boolean foundHeaderInAnyTable = false;
        boolean foundTotalInAnyTable = false;

        for (final Element table : tables) {
            final var scrapingList = table.select(Tag.TR.getName()).stream()
                    .map(tr -> tr.select(Tag.TD.getName()).stream()
                            .map(Element::text)
                            .toList())
                    .filter(tdList -> !tdList.isEmpty())
                    .toList();

            if (scrapingList.isEmpty()) {
                continue;
            }

            final Optional<List<String>> headerRow = scrapingList.stream()
                    .filter(tdList -> tdList.stream().anyMatch(this::isShareCountHeaderKey))
                    .findFirst();
            final Optional<List<String>> totalRow = scrapingList.stream()
                    .filter(tdList -> tdList.stream().anyMatch(td -> td.contains(TOTAL) && !td.contains("会計")))
                    .findFirst();

            foundHeaderInAnyTable |= headerRow.isPresent();
            foundTotalInAnyTable |= totalRow.isPresent();

            if (headerRow.isEmpty() || totalRow.isEmpty()) {
                continue;
            }

            if (containsUnexpectedShareUnit(table)) {
                throw new FundanalyzerScrapingException("株式総数テーブル周辺に想定外の単位注記が存在したため、株式総数取得に失敗しました。");
            }

            try {
                final String key1 = findPreferredShareCountHeaderKey(headerRow.orElseThrow())
                        .orElseThrow();
                final int indexOfKey1 = headerRow.orElseThrow().indexOf(key1);

                return totalRow.orElseThrow().get(indexOfKey1);
            } catch (NoSuchElementException | IndexOutOfBoundsException e) {
                throw new FundanalyzerScrapingException("株式総数取得のためのキーワードが存在しなかったため、株式総数取得に失敗しました。");
            }
        }

        if (foundHeaderInAnyTable && foundTotalInAnyTable) {
            throw new FundanalyzerScrapingException("株式総数テーブルのヘッダ行と計行が同一テーブル内に存在しなかったため、株式総数取得に失敗しました。");
        }

        throw new FundanalyzerScrapingException("株式総数取得のためのキーワードが存在しなかったため、株式総数取得に失敗しました。");
    }

    private boolean containsUnexpectedShareUnit(final Element table) {
        return shareUnitTextsAround(table).stream()
                .anyMatch(text -> UNEXPECTED_SHARE_UNIT_PATTERN.matcher(text).find());
    }

    private List<String> shareUnitTextsAround(final Element table) {
        return List.of(
                table.select("caption").text(),
                table.parent() != null ? table.parent().ownText() : "",
                table.previousElementSibling() != null ? table.previousElementSibling().text() : "",
                table.nextElementSibling() != null ? table.nextElementSibling().text() : ""
        );
    }

    private Optional<String> findPreferredShareCountHeaderKey(final List<String> headers) {
        return headers.stream()
                .filter(this::isFilingDateTargetKey)
                .findFirst()
                .or(() -> headers.stream()
                        .filter(this::isTargetKey)
                        .findFirst());
    }

    private boolean isShareCountHeaderKey(final String td) {
        return isFilingDateTargetKey(td) || isTargetKey(td);
    }

    private boolean isFilingDateTargetKey(final String td) {
        return td.contains("提出日")
               && td.contains("現在")
               && td.contains("発行")
               && (td.contains("数") || td.contains("株"));
    }

    private boolean isTargetKey(final String td) {
        return (td.contains("事業") && td.contains("年度") && td.contains("末")
                && td.contains("現") && td.contains("在") && td.contains("発行"))
               ||
               (td.contains("当期") && td.contains("末")
                && td.contains("現在") && td.contains("発行") && td.contains("数"))
               ||
               (td.contains("連結会計年度") && td.contains("末")
                && td.contains("現在") && td.contains("発行") && td.contains("数"))
               ||
               (td.contains("四半期") && td.contains("末") && td.contains("発行") && td.contains("数"))
               ||
               (td.contains("四半期") && td.contains("末")
                && td.contains("現在") && td.contains("発行") && td.contains("株"))
                ;
    }

    Elements elementsByKeyMatch(final File file, final KeyMatch keyMatch) {
        try {
            return Jsoup.parse(file, "UTF-8")
                    .getElementsByAttributeValue(keyMatch.getKey(), keyMatch.getMatch());
        } catch (IOException e) {
            log.warn(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format(
                            "ファイル形式に問題があり、読み取りに失敗しました。\t対象ファイルパス:\"{0}\"",
                            file.getPath()
                    ),
                    Category.SCRAPING,
                    Process.SCRAPING
            ));
            throw new FundanalyzerFileException("ファイルの認識に失敗しました。スタックトレースから詳細を確認してください。", e);
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
                .toList();
    }

    @SuppressWarnings("RedundantModifiersValueLombok")
    @Value(staticConstructor = "of")
    static class KeyMatch {
        private final String key;
        private final String match;
    }
}
