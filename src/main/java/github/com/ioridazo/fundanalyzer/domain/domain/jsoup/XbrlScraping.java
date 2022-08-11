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
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class XbrlScraping {

    private static final Logger log = LogManager.getLogger(XbrlScraping.class);

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
                .collect(Collectors.toList());

        if (filePathList.size() == 1) {
            // ファイルが一つ見つかったとき
            log.info(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format(
                            "次のキーワードにてファイルを確認しました。\t財務諸表名:{0}\tキーワード:{1}",
                            scrapingKeywordEntity.getRemarks(),
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
                            scrapingKeywordEntity.getRemarks(),
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

        final List<List<String>> scrapingList = elementsByKeyMatch(targetFile, KeyMatch.of("name", keyWord))
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

        // 年度以外の情報を取り除く
        scrapingList.get(1).removeIf(s -> s.contains("注記"));

        if (scrapingList.stream().allMatch(list -> list.size() <= 2)) {
            // 当期のみの場合
            return scrapingList.stream()
                    .map(tdList -> FinancialTableResultBean.ofTdList(tdList, unit))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } else if (scrapingList.stream().allMatch(list -> list.size() <= 4)) {
            // 前期と当期がある場合
            final boolean isMain = isMainOrderOfYear(scrapingList, targetFile);
            return scrapingList.stream()
                    .map(tdList -> FinancialTableResultBean.ofTdList(tdList, unit, isMain))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } else {
            throw new FundanalyzerScrapingException(
                    "定形外の財務諸表でした。詳細を確認してください。\nファイルパス:" + targetFile);
        }
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
     * @return boolean
     */
    private boolean isMainOrderOfYear(final List<List<String>> scrapingList, final File targetFile) {
        if (scrapingList.get(1).size() == 2) {
            if (scrapingList.get(1).get(0).contains("前") && scrapingList.get(1).get(1).contains("当")) {
                return true;
            } else if (scrapingList.get(1).get(0).contains("当") && scrapingList.get(1).get(1).contains("前")) {
                return false;
            }

            try {
                if (scrapingList.get(1).get(0).contains("第") && scrapingList.get(1).get(0).contains("期")) {
                    if (
                        // '第' と '期' の間にある数値を比較する
                            Integer.parseInt(scrapingList.get(1).get(0).substring(
                                    scrapingList.get(1).get(0).indexOf("第") + 1,
                                    scrapingList.get(1).get(0).indexOf("期")
                            )) < Integer.parseInt(scrapingList.get(1).get(1).substring(
                                    scrapingList.get(1).get(1).indexOf("第") + 1,
                                    scrapingList.get(1).get(1).indexOf("期")
                            ))
                    ) {
                        return true;
                    } else if (
                        // '第' と '期' の間にある数値を比較する
                            Integer.parseInt(scrapingList.get(1).get(0).substring(
                                    scrapingList.get(1).get(0).indexOf("第") + 1,
                                    scrapingList.get(1).get(0).indexOf("期")
                            )) > Integer.parseInt(scrapingList.get(1).get(1).substring(
                                    scrapingList.get(1).get(1).indexOf("第") + 1,
                                    scrapingList.get(1).get(1).indexOf("期")
                            ))
                    ) {
                        return false;
                    }
                }

                if (scrapingList.get(1).get(0).contains("年度")) {
                    if (
                        // '年度' の数値を比較する
                            Integer.parseInt(
                                    scrapingList.get(1).get(0).substring(0, scrapingList.get(1).get(0).indexOf("年度"))
                            ) < Integer.parseInt(
                                    scrapingList.get(1).get(1).substring(0, scrapingList.get(1).get(1).indexOf("年度"))
                            )
                    ) {
                        return true;
                    } else if (
                        // '年度' の数値を比較する
                            Integer.parseInt(
                                    scrapingList.get(1).get(0).substring(0, scrapingList.get(1).get(0).indexOf("年度"))
                            ) > Integer.parseInt(
                                    scrapingList.get(1).get(1).substring(0, scrapingList.get(1).get(1).indexOf("年度"))
                            )
                    ) {
                        return false;
                    }
                }
            } catch (final NumberFormatException | StringIndexOutOfBoundsException e) {
                throw new FundanalyzerScrapingException(
                        "年度に関して対象の財務諸表は定形外でした。次の値を確認してください。" +
                                "\t'" + scrapingList.get(1).get(0) + "' and '" + scrapingList.get(1).get(1) + "'" +
                                "\nファイルパス:" + targetFile);
            }

        } else if (scrapingList.get(1).size() == 3) {
            if (scrapingList.get(1).get(0).contains("前") && scrapingList.get(1).get(2).contains("当")) {
                return true;
            } else if (scrapingList.get(1).get(0).contains("当") && scrapingList.get(1).get(2).contains("前")) {
                return false;
            }

            try {
                if (scrapingList.get(1).get(0).contains("第") && scrapingList.get(1).get(0).contains("期")) {
                    if (
                        // '第' と '期' の間にある数値を比較する
                            Integer.parseInt(scrapingList.get(1).get(0).substring(
                                    scrapingList.get(1).get(0).indexOf("第") + 1,
                                    scrapingList.get(1).get(0).indexOf("期")
                            )) < Integer.parseInt(scrapingList.get(1).get(2).substring(
                                    scrapingList.get(1).get(2).indexOf("第") + 1,
                                    scrapingList.get(1).get(2).indexOf("期")
                            ))
                    ) {
                        return true;
                    } else if (
                        // '第' と '期' の間にある数値を比較する
                            Integer.parseInt(scrapingList.get(1).get(0).substring(
                                    scrapingList.get(1).get(0).indexOf("第") + 1,
                                    scrapingList.get(1).get(0).indexOf("期")
                            )) > Integer.parseInt(scrapingList.get(1).get(2).substring(
                                    scrapingList.get(1).get(2).indexOf("第") + 1,
                                    scrapingList.get(1).get(2).indexOf("期")
                            ))
                    ) {
                        return false;
                    }
                }

                if (scrapingList.get(1).get(0).contains("年度")) {
                    if (
                        // '年度' の数値を比較する
                            Integer.parseInt(
                                    scrapingList.get(1).get(0).substring(0, scrapingList.get(1).get(0).indexOf("年度"))
                            ) < Integer.parseInt(
                                    scrapingList.get(1).get(2).substring(0, scrapingList.get(1).get(2).indexOf("年度"))
                            )
                    ) {
                        return true;
                    } else if (
                        // '年度' の数値を比較する
                            Integer.parseInt(
                                    scrapingList.get(1).get(0).substring(0, scrapingList.get(1).get(0).indexOf("年度"))
                            ) > Integer.parseInt(
                                    scrapingList.get(1).get(2).substring(0, scrapingList.get(1).get(2).indexOf("年度"))
                            )
                    ) {
                        return false;
                    }
                }
            } catch (final NumberFormatException | StringIndexOutOfBoundsException e) {
                throw new FundanalyzerScrapingException(
                        "年度に関して対象の財務諸表は定形外でした。次の値を確認してください。" +
                                "\t'" + scrapingList.get(1).get(0) + "' and '" + scrapingList.get(1).get(1) + "'" +
                                "\nファイルパス:" + targetFile);
            }
        }

        throw new FundanalyzerScrapingException(
                "年度に関して対象の財務諸表は定形外でした。詳細を確認してください。\nファイルパス:" + targetFile);
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
            throw new FundanalyzerScrapingException("株式総数取得のためのテーブルが存在しなかったため、株式総数取得に失敗しました。");
        }

        try {
            // "事業年度末現在発行数"を含む項目を探す
            final var key1 = scrapingList.stream()
                    // 対象行の取得
                    .filter(tdList -> tdList.stream().anyMatch(this::isTargetKey))
                    .findFirst().orElseThrow().stream()
                    // 対象行から"事業年度末現在発行数"を含むカラムを取得
                    .filter(this::isTargetKey)
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
                    .filter(tdList -> tdList.stream().anyMatch(td -> td.contains(TOTAL) && !td.contains("会計")))
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
            throw new FundanalyzerScrapingException("株式総数取得のためのキーワードが存在しなかったため、株式総数取得に失敗しました。");
        }
    }

    private boolean isTargetKey(final String td) {
        return (td.contains("事業") && td.contains("年度") && td.contains("末")
                && td.contains("現在") && td.contains("発行"))
                ||
                (td.contains("当期") && td.contains("末")
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
                .collect(Collectors.toList());
    }

    @SuppressWarnings("RedundantModifiersValueLombok")
    @Value(staticConstructor = "of")
    static class KeyMatch {
        private final String key;
        private final String match;
    }
}
