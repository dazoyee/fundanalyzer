package github.com.ioridazo.fundanalyzer.domain.domain.jsoup;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.ScrapingKeywordEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.jsoup.bean.Unit;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerFileException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerScrapingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class XbrlScrapingTest {

    private XbrlScraping xbrlScraping;

    @BeforeEach
    void setUp() {
        xbrlScraping = Mockito.spy(XbrlScraping.class);
    }

    @Nested
    class findFile {

        @DisplayName("findFile : 対象のフォルダ配下にあるファイルからキーワードに合致するものを返却する")
        @Test
        void findFile_ok() {
            var scrapingKeyword = new ScrapingKeywordEntity(
                    null,
                    null,
                    "jpcrp_cor:BalanceSheetTextBlock",
                    "貸借対照表",
                    null
            );
            var filePath = new File("src/test/resources/github/com/ioridazo/fundanalyzer/domain/logic/scraping/jsoup/find-file/ok/EXAMPLE/XBRL/PublicDoc");

            var expected = new File("src/test/resources/github/com/ioridazo/fundanalyzer/domain/logic/scraping/jsoup/find-file/ok/EXAMPLE/XBRL/PublicDoc/000000_honbun.htm");

            var actual = xbrlScraping.findFile(filePath, scrapingKeyword);

            //noinspection OptionalGetWithoutIsPresent
            assertEquals(expected.getPath(), actual.get().getPath());
        }

        @DisplayName("findFile : 対象のフォルダ配下にあるファイルからキーワードに合致するものがないときはnullを返却する")
        @Test
        void findFile_target_file_has_not_keyword() {
            var scrapingKeyword = new ScrapingKeywordEntity(
                    null,
                    null,
                    "jpcrp_cor:BalanceSheetTextBlock",
                    "貸借対照表",
                    null
            );
            var filePath = new File("src/test/resources/github/com/ioridazo/fundanalyzer/domain/logic/scraping/jsoup/find-file/target_file_has_not_keyword/EXAMPLE/XBRL/PublicDoc");

            var actual = xbrlScraping.findFile(filePath, scrapingKeyword);

            assertNull(actual.orElse(null));
        }

        @DisplayName("findFile : 対象のフォルダ配下にあるファイルからキーワードに合致するものが複数存在するときはエラー発生させる")
        @Test
        void findFile_FundanalyzerFileException() {
            var scrapingKeyword = new ScrapingKeywordEntity(
                    null,
                    null,
                    "jpcrp_cor:BalanceSheetTextBlock",
                    "貸借対照表",
                    null
            );
            var filePath = new File("src/test/resources/github/com/ioridazo/fundanalyzer/domain/logic/scraping/jsoup/find-file/fundanalyzer_file_exception/EXAMPLE/XBRL/PublicDoc");

            assertThrows(FundanalyzerFileException.class, () -> xbrlScraping.findFile(filePath, scrapingKeyword));
        }
    }

    @Nested
    class scrapeFinancialStatement {

        @DisplayName("scrapeFinancialStatement : ファイルからキーワードに合致する財務諸表テーブルの科目とその値をスクレイピングする")
        @Test
        void scrapeFinancialStatement_ok_main() {
            var file = new File("src/test/resources/github/com/ioridazo/fundanalyzer/domain/logic/scraping/jsoup/scrape-financial-statement/jsoup_main.html");
            var keyword = "jpcrp_cor:BalanceSheetTextBlock";

            var actual = xbrlScraping.scrapeFinancialStatement(file, keyword);

            assertAll("FinancialTableResultBean",
                    () -> assertAll(
                            () -> assertEquals("前事業年度 (平成30年10月20日)", actual.get(0).getSubject().orElseThrow()),
                            () -> assertNull(actual.get(0).getPreviousValue().orElse(null)),
                            () -> assertEquals("当事業年度 (令和元年10月20日)", actual.get(0).getCurrentValue()),
                            () -> assertEquals(Unit.THOUSANDS_OF_YEN, actual.get(0).getUnit())
                    ),
                    () -> assertAll(
                            () -> assertEquals("現金及び預金", actual.get(1).getSubject().orElseThrow()),
                            () -> assertEquals("※ 116,109", actual.get(1).getPreviousValue().orElseThrow()),
                            () -> assertEquals("※ 476,095", actual.get(1).getCurrentValue()),
                            () -> assertEquals(Unit.THOUSANDS_OF_YEN, actual.get(1).getUnit())
                    ),
                    () -> assertAll(
                            () -> assertEquals("流動負債合計", actual.get(62).getSubject().orElseThrow()),
                            () -> assertEquals("1,070,764", actual.get(62).getPreviousValue().orElseThrow()),
                            () -> assertEquals("1,283,815", actual.get(62).getCurrentValue()),
                            () -> assertEquals(Unit.THOUSANDS_OF_YEN, actual.get(62).getUnit())
                    ),
                    () -> assertAll(
                            () -> assertEquals("負債純資産合計", actual.get(84).getSubject().orElseThrow()),
                            () -> assertEquals("5,262,964", actual.get(84).getPreviousValue().orElseThrow()),
                            () -> assertEquals("5,457,406", actual.get(84).getCurrentValue()),
                            () -> assertEquals(Unit.THOUSANDS_OF_YEN, actual.get(84).getUnit())
                    )
            );
            assertEquals(85, actual.size());
        }

        @DisplayName("scrapeFinancialStatement : ファイルからキーワードに合致する財務諸表テーブルの科目とその値をスクレイピングする（年度の順序が逆の想定）")
        @Test
        void scrapeFinancialStatement_ok_no_main() {
            var file = new File("src/test/resources/github/com/ioridazo/fundanalyzer/domain/logic/scraping/jsoup/scrape-financial-statement/jsoup_no_main.html");
            var keyword = "jpcrp_cor:BalanceSheetTextBlock";

            var actual = xbrlScraping.scrapeFinancialStatement(file, keyword);

            assertAll("FinancialTableResultBean",
                    () -> assertAll(
                            () -> assertEquals("当事業年度 (令和元年10月20日)", actual.get(0).getSubject().orElseThrow()),
                            () -> assertNull(actual.get(0).getPreviousValue().orElse(null)),
                            () -> assertEquals("前事業年度 (平成30年10月20日)", actual.get(0).getCurrentValue()),
                            () -> assertEquals(Unit.THOUSANDS_OF_YEN, actual.get(0).getUnit())
                    ),
                    () -> assertAll(
                            () -> assertEquals("現金及び預金", actual.get(1).getSubject().orElseThrow()),
                            () -> assertEquals("※ 476,095", actual.get(1).getPreviousValue().orElseThrow()),
                            () -> assertEquals("※ 116,109", actual.get(1).getCurrentValue()),
                            () -> assertEquals(Unit.THOUSANDS_OF_YEN, actual.get(1).getUnit())
                    ),
                    () -> assertAll(
                            () -> assertEquals("流動負債合計", actual.get(62).getSubject().orElseThrow()),
                            () -> assertEquals("1,283,815", actual.get(62).getPreviousValue().orElseThrow()),
                            () -> assertEquals("1,070,764", actual.get(62).getCurrentValue()),
                            () -> assertEquals(Unit.THOUSANDS_OF_YEN, actual.get(62).getUnit())
                    ),
                    () -> assertAll(
                            () -> assertEquals("負債純資産合計", actual.get(84).getSubject().orElseThrow()),
                            () -> assertEquals("5,457,406", actual.get(84).getPreviousValue().orElseThrow()),
                            () -> assertEquals("5,262,964", actual.get(84).getCurrentValue()),
                            () -> assertEquals(Unit.THOUSANDS_OF_YEN, actual.get(84).getUnit())
                    )
            );
            assertEquals(85, actual.size());
        }

        @DisplayName("scrapeFinancialStatement : ファイルからキーワードに合致する財務諸表テーブルの科目とその値をスクレイピングする")
        @Test
        void scrapeFinancialStatement_ok_IndexOutOfBoundsException() {
            var file = new File("src/test/resources/github/com/ioridazo/fundanalyzer/domain/logic/scraping/jsoup/scrape-financial-statement/jsoup_main2.html");
            var keyword = "jpcrp_cor:BalanceSheetTextBlock";

            var actual = xbrlScraping.scrapeFinancialStatement(file, keyword);

            assertAll("FinancialTableResultBean",
                    () -> assertAll(
                            () -> assertEquals("前事業年度 (平成30年10月20日)", actual.get(1).getSubject().orElseThrow()),
                            () -> assertNull(actual.get(1).getPreviousValue().orElse(null)),
                            () -> assertEquals("当事業年度 (令和元年10月20日)", actual.get(1).getCurrentValue()),
                            () -> assertEquals(Unit.THOUSANDS_OF_YEN, actual.get(1).getUnit())
                    ),
                    () -> assertAll(
                            () -> assertEquals("現金及び預金", actual.get(2).getSubject().orElseThrow()),
                            () -> assertEquals("※ 116,109", actual.get(2).getPreviousValue().orElseThrow()),
                            () -> assertEquals("※ 476,095", actual.get(2).getCurrentValue()),
                            () -> assertEquals(Unit.THOUSANDS_OF_YEN, actual.get(2).getUnit())
                    ),
                    () -> assertAll(
                            () -> assertEquals("流動負債合計", actual.get(63).getSubject().orElseThrow()),
                            () -> assertEquals("1,070,764", actual.get(63).getPreviousValue().orElseThrow()),
                            () -> assertEquals("1,283,815", actual.get(63).getCurrentValue()),
                            () -> assertEquals(Unit.THOUSANDS_OF_YEN, actual.get(63).getUnit())
                    ),
                    () -> assertAll(
                            () -> assertEquals("負債純資産合計", actual.get(85).getSubject().orElseThrow()),
                            () -> assertEquals("5,262,964", actual.get(85).getPreviousValue().orElseThrow()),
                            () -> assertEquals("5,457,406", actual.get(85).getCurrentValue()),
                            () -> assertEquals(Unit.THOUSANDS_OF_YEN, actual.get(85).getUnit())
                    )
            );
            assertEquals(86, actual.size());
        }

        @DisplayName("scrapeFinancialStatement : ファイルからキーワードに合致する財務諸表テーブルの科目とその値をスクレイピングする（四半期報告書に対応）")
        @Test
        void scrapeFinancialStatement_ok_dtc140() {
            var file = new File("src/test/resources/github/com/ioridazo/fundanalyzer/domain/logic/scraping/jsoup/scrape-financial-statement/jsoup_dtc140.html");
            var keyword = "jpigp_cor:CondensedQuarterlyConsolidatedStatementOfFinancialPositionIFRSTextBlock";

            var actual = xbrlScraping.scrapeFinancialStatement(file, keyword);

            actual.forEach(System.out::println);

            assertAll("FinancialTableResultBean",
                    () -> assertAll(
                            () -> assertEquals("前連結会計年度末 (2021年３月31日)", actual.get(0).getSubject().orElseThrow()),
                            () -> assertNull(actual.get(0).getPreviousValue().orElse(null)),
                            () -> assertEquals("当第１四半期 連結会計期間末 (2021年６月30日)", actual.get(0).getCurrentValue()),
                            () -> assertEquals(Unit.MILLIONS_OF_YEN, actual.get(0).getUnit())
                    ),
                    () -> assertAll(
                            () -> assertEquals("現金及び現金同等物", actual.get(1).getSubject().orElseThrow()),
                            () -> assertEquals("108,768", actual.get(1).getPreviousValue().orElseThrow()),
                            () -> assertEquals("127,163", actual.get(1).getCurrentValue()),
                            () -> assertEquals(Unit.MILLIONS_OF_YEN, actual.get(1).getUnit())
                    ),
                    () -> assertAll(
                            () -> assertEquals("流動負債合計", actual.get(28).getSubject().orElseThrow()),
                            () -> assertEquals("97,820", actual.get(28).getPreviousValue().orElseThrow()),
                            () -> assertEquals("93,386", actual.get(28).getCurrentValue()),
                            () -> assertEquals(Unit.MILLIONS_OF_YEN, actual.get(28).getUnit())
                    )
            );
            assertEquals(45, actual.size());
        }

        @DisplayName("scrapeFinancialStatement : ファイルからキーワードに合致する財務諸表テーブルの科目とその値をスクレイピングする（四半期報告書に対応できていないとき）")
        @Test
        void scrapeFinancialStatement_ok_dtc140_error() {
            var file = new File("src/test/resources/github/com/ioridazo/fundanalyzer/domain/logic/scraping/jsoup/scrape-financial-statement/jsoup_dtc140_error.html");
            var keyword = "jpigp_cor:CondensedQuarterlyConsolidatedStatementOfFinancialPositionIFRSTextBlock";
            assertThrows(FundanalyzerScrapingException.class, () -> xbrlScraping.scrapeFinancialStatement(file, keyword));
        }

        @DisplayName("unit : ファイルから財務諸表の金額単位（千円）をスクレイピングする")
        @Test
        void unit_thousands_ok() {
            var file = new File("src/test/resources/github/com/ioridazo/fundanalyzer/domain/logic/scraping/jsoup/scrape-financial-statement/jsoup_unit_thousands.html");
            var keyword = "jpcrp_cor:BalanceSheetTextBlock";

            var actual = xbrlScraping.unit(file, keyword);

            assertEquals(Unit.THOUSANDS_OF_YEN, actual);
        }

        @DisplayName("unit : ファイルから財務諸表の金額単位（単位：百万円）をスクレイピングする")
        @Test
        void unit_millions_ok() {
            var file = new File("src/test/resources/github/com/ioridazo/fundanalyzer/domain/logic/scraping/jsoup/scrape-financial-statement/jsoup_unit_millions.html");
            var keyword = "jpcrp_cor:BalanceSheetTextBlock";

            var actual = xbrlScraping.unit(file, keyword);

            assertEquals(Unit.MILLIONS_OF_YEN, actual);
        }

        @DisplayName("unit : ファイルから財務諸表の金額単位（単位　百万円）をスクレイピングする")
        @Test
        void unit_millions_2_ok() {
            var file = new File("src/test/resources/github/com/ioridazo/fundanalyzer/domain/logic/scraping/jsoup/scrape-financial-statement/jsoup_unit_millions_2.html");
            var keyword = "jpcrp_cor:QuarterlyConsolidatedBalanceSheetTextBlock";

            var actual = xbrlScraping.unit(file, keyword);

            assertEquals(Unit.MILLIONS_OF_YEN, actual);
        }

        @DisplayName("unit : ファイルから財務諸表の金額単位をスクレイピングに失敗したときの挙動を確認する")
        @Test
        void unit_millions_FundanalyzerFileException() {
            var file = new File("src/test/resources/github/com/ioridazo/fundanalyzer/domain/logic/scraping/jsoup/scrape-financial-statement/jsoup_unit_failure.html");
            var keyword = "jpcrp_cor:BalanceSheetTextBlock";

            var actual = assertThrows(FundanalyzerFileException.class, () -> xbrlScraping.unit(file, keyword));

            System.out.println(actual.getMessage());
        }
    }

    @Nested
    class scrapeNumberOfShares {

        @DisplayName("scrapeNumberOfShares : ファイルから株式総数を取得し、その値をスクレイピングする")
        @Test
        void scrapeNumberOfShares_ok() {
            var file = new File("src/test/resources/github/com/ioridazo/fundanalyzer/domain/logic/scraping/jsoup/scrape-number-of-shares/jsoup_ok.htm");
            var keyword = "jpcrp_cor:IssuedSharesTotalNumberOfSharesEtcTextBlock";

            var actual = xbrlScraping.scrapeNumberOfShares(file, keyword);

            assertEquals("15,560,000", actual);
        }

        @DisplayName("scrapeNumberOfShares : ファイルから株式総数を取得するためのテーブルが存在しなかったときの挙動を確認する")
        @Test
        void scrapeNumberOfShares_FundanalyzerFileException_table() {
            var file = new File("src/test/resources/github/com/ioridazo/fundanalyzer/domain/logic/scraping/jsoup/scrape-number-of-shares/jsoup_failure_table.htm");
            var keyword = "jpcrp_cor:IssuedSharesTotalNumberOfSharesEtcTextBlock";

            var actual = assertThrows(FundanalyzerFileException.class, () -> xbrlScraping.scrapeNumberOfShares(file, keyword));

            System.out.println(actual.getMessage());
        }

        @DisplayName("scrapeNumberOfShares : ファイルから株式総数を取得するためのキーワードが存在しなかったときの挙動を確認する")
        @Test
        void scrapeNumberOfShares_FundanalyzerFileException_keyword() {
            var file = new File("src/test/resources/github/com/ioridazo/fundanalyzer/domain/logic/logic/scraping/jsoup/scrape-number-of-shares/jsoup_failure_keyword.htm");
            var keyword = "jpcrp_cor:IssuedSharesTotalNumberOfSharesEtcTextBlock";

            var actual = assertThrows(FundanalyzerFileException.class, () -> xbrlScraping.scrapeNumberOfShares(file, keyword));

            System.out.println(actual.getMessage());
        }
    }
}