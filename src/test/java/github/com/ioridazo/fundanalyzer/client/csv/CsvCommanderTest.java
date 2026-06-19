package github.com.ioridazo.fundanalyzer.client.csv;

import com.opencsv.bean.CsvBindByName;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CsvCommander のテスト。
 */
@DisplayName("CsvCommander のテスト")
class CsvCommanderTest {

    private CsvCommander csvCommander;

    @BeforeEach
    void setUp() {
        csvCommander = new CsvCommander();
    }

    @Nested
    @DisplayName("readCsv メソッド")
    class ReadCsv {

        @Test
        @DisplayName("正常系: 1 行目のメタデータをスキップしヘッダ + 複数レコードをBeanに変換する")
        void readCsv_正常系_複数レコード変換(@TempDir final Path tempDir) throws Exception {
            final Path csvFile = tempDir.resolve("sample.csv");
            final String csv = "metadata-line\n"
                    + "name,age\n"
                    + "Alice,20\n"
                    + "Bob,30\n";
            Files.writeString(csvFile, csv, StandardCharsets.UTF_8);

            final List<TestBean> result = csvCommander.readCsv(
                    csvFile.toFile(), StandardCharsets.UTF_8, TestBean.class);

            assertNotNull(result);
            assertEquals(2, result.size());
            final TestBean first = result.get(0);
            final TestBean second = result.get(1);
            assertEquals("Alice", first.getName());
            assertEquals(20, first.getAge());
            assertEquals("Bob", second.getName());
            assertEquals(30, second.getAge());
        }

        @Test
        @DisplayName("正常系: メタデータ行 + ヘッダ行のみのCSVは空リストを返す")
        void readCsv_ヘッダのみ_空リスト(@TempDir final Path tempDir) throws Exception {
            final Path csvFile = tempDir.resolve("header_only.csv");
            Files.writeString(csvFile, "metadata-line\nname,age\n", StandardCharsets.UTF_8);

            final List<TestBean> result = csvCommander.readCsv(
                    csvFile.toFile(), StandardCharsets.UTF_8, TestBean.class);

            assertNotNull(result);
            assertTrue(result.isEmpty(), "ヘッダのみの場合は空リストを返すこと");
        }

        @Test
        @DisplayName("正常系: MS932(Shift_JIS) で書かれた日本語を正しく読み込む")
        void readCsv_MS932_日本語デコード(@TempDir final Path tempDir) throws Exception {
            final Path csvFile = tempDir.resolve("ms932.csv");
            final Charset charset = Charset.forName("MS932");
            final String csv = "metadata-line\n"
                    + "name,age\n"
                    + "山田,42\n";
            Files.write(csvFile, csv.getBytes(charset));

            final List<TestBean> result = csvCommander.readCsv(
                    csvFile.toFile(), charset, TestBean.class);

            assertEquals(1, result.size());
            assertEquals("山田", result.get(0).getName());
            assertEquals(42, result.get(0).getAge());
        }

        @Test
        @DisplayName("異常系: 対象ファイルが存在しない場合 FundanalyzerRuntimeException が送出される")
        void readCsv_ファイル不在_例外(@TempDir final Path tempDir) {
            final File missing = tempDir.resolve("not-exists.csv").toFile();
            assertFalse(missing.exists(), "事前にファイルが存在しないこと");

            final FundanalyzerRuntimeException ex = assertThrows(
                    FundanalyzerRuntimeException.class,
                    () -> csvCommander.readCsv(missing, StandardCharsets.UTF_8, TestBean.class)
            );
            assertEquals("対象ファイルがありませんでした。", ex.getMessage());
        }

        @Test
        @DisplayName("異常系: ファイル不在時の副作用としてディレクトリが作成される")
        void readCsv_ファイル不在_ディレクトリ作成副作用(@TempDir final Path tempDir) {
            final File missing = tempDir.resolve("auto-mkdirs").toFile();
            assertFalse(missing.exists());

            assertThrows(
                    FundanalyzerRuntimeException.class,
                    () -> csvCommander.readCsv(missing, StandardCharsets.UTF_8, TestBean.class)
            );

            assertTrue(missing.exists(), "存在しないパスに対し mkdirs が呼ばれてディレクトリが作成されていること");
            assertTrue(missing.isDirectory(), "作成されたものはディレクトリであること");
        }

        @Test
        @DisplayName("異常系: ファイルではなくディレクトリを渡すと読み取り失敗で RuntimeException が伝播する")
        void readCsv_ディレクトリを指定_例外(@TempDir final Path tempDir) throws Exception {
            final Path dir = tempDir.resolve("a-directory");
            Files.createDirectories(dir);

            assertThrows(
                    RuntimeException.class,
                    () -> csvCommander.readCsv(dir.toFile(), StandardCharsets.UTF_8, TestBean.class)
            );
        }

        @Test
        @DisplayName("異常系: 空ファイル(ヘッダなし)はパース失敗で RuntimeException が送出される")
        void readCsv_空ファイル_例外(@TempDir final Path tempDir) throws Exception {
            final Path empty = tempDir.resolve("empty.csv");
            Files.createFile(empty);

            assertThrows(
                    RuntimeException.class,
                    () -> csvCommander.readCsv(empty.toFile(), StandardCharsets.UTF_8, TestBean.class)
            );
        }
    }

    /**
     * テスト用 CSV Bean。opencsv が利用するため public no-args constructor と setter を持つ。
     */
    public static class TestBean {

        @CsvBindByName(column = "name", required = true)
        private String name;

        @CsvBindByName(column = "age")
        private int age;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(final int age) {
            this.age = age;
        }
    }
}
