package github.com.ioridazo.fundanalyzer.client.file;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FileOperator のテスト。
 */
@DisplayName("FileOperator のテスト")
class FileOperatorTest {

    private FileOperator fileOperator;

    @BeforeEach
    void setUp() {
        fileOperator = new FileOperator();
    }

    @Nested
    @DisplayName("decodeZipFile メソッド")
    class DecodeZipFile {

        @Test
        @DisplayName("単一ファイルのZIP→指定された出力ディレクトリに展開される")
        void decodeZipFile_単一ファイル_出力ディレクトリに展開される(@TempDir final Path tempDir) throws IOException {
            // 入力 ZIP を tempDir/input.zip として作成（FileOperator は ".zip" を末尾に付与する）
            final Path inputBase = tempDir.resolve("input");
            final Path zipFile = tempDir.resolve("input.zip");
            final String entryName = "hello.txt";
            final byte[] content = "こんにちは".getBytes(StandardCharsets.UTF_8);
            createZip(zipFile, List.of(new ZipEntryData(entryName, content)));

            final Path outputDir = tempDir.resolve("output");

            // 実行
            fileOperator.decodeZipFile(inputBase.toFile(), outputDir.toFile());

            // 検証
            final Path extracted = outputDir.resolve(entryName);
            assertTrue(Files.exists(extracted), "展開されたファイルが存在すること");
            assertEquals(
                    new String(content, StandardCharsets.UTF_8),
                    Files.readString(extracted, StandardCharsets.UTF_8)
            );
        }

        @Test
        @DisplayName("複数ファイルのZIP→すべて展開される")
        void decodeZipFile_複数ファイル_すべて展開される(@TempDir final Path tempDir) throws IOException {
            final Path inputBase = tempDir.resolve("multi");
            final Path zipFile = tempDir.resolve("multi.zip");
            final byte[] contentA = "AAA".getBytes(StandardCharsets.UTF_8);
            final byte[] contentB = "BBBB".getBytes(StandardCharsets.UTF_8);
            createZip(zipFile, List.of(
                    new ZipEntryData("a.txt", contentA),
                    new ZipEntryData("b.txt", contentB)
            ));

            final Path outputDir = tempDir.resolve("multi-output");

            fileOperator.decodeZipFile(inputBase.toFile(), outputDir.toFile());

            assertTrue(Files.exists(outputDir.resolve("a.txt")));
            assertTrue(Files.exists(outputDir.resolve("b.txt")));
            assertEquals("AAA", Files.readString(outputDir.resolve("a.txt"), StandardCharsets.UTF_8));
            assertEquals("BBBB", Files.readString(outputDir.resolve("b.txt"), StandardCharsets.UTF_8));
        }

        @Test
        @DisplayName("出力先ディレクトリが存在しない場合→自動的に作成される")
        void decodeZipFile_出力先ディレクトリが存在しない_作成される(@TempDir final Path tempDir) throws IOException {
            final Path inputBase = tempDir.resolve("auto");
            final Path zipFile = tempDir.resolve("auto.zip");
            createZip(zipFile, List.of(new ZipEntryData("x.txt", "X".getBytes(StandardCharsets.UTF_8))));

            final Path outputDir = tempDir.resolve("not-exists-yet");
            assertFalse(Files.exists(outputDir), "事前に出力先が存在しないこと");

            fileOperator.decodeZipFile(inputBase.toFile(), outputDir.toFile());

            assertTrue(Files.isDirectory(outputDir), "実行後に出力先ディレクトリが作成されていること");
            assertTrue(Files.exists(outputDir.resolve("x.txt")));
        }

        @Test
        @DisplayName("入力ZIPが存在しない→IOException が送出される")
        void decodeZipFile_入力ファイル不在_IOException(@TempDir final Path tempDir) {
            final Path inputBase = tempDir.resolve("missing");
            final Path outputDir = tempDir.resolve("output-missing");

            assertThrows(IOException.class, () -> fileOperator.decodeZipFile(inputBase.toFile(), outputDir.toFile()));
        }

        @Test
        @DisplayName("空のZIP→例外なく終了し、出力ディレクトリのみ作成される")
        void decodeZipFile_空ZIP_出力ディレクトリのみ作成(@TempDir final Path tempDir) throws IOException {
            final Path inputBase = tempDir.resolve("empty");
            final Path zipFile = tempDir.resolve("empty.zip");
            createZip(zipFile, List.of());

            final Path outputDir = tempDir.resolve("empty-output");

            assertDoesNotThrow(() -> fileOperator.decodeZipFile(inputBase.toFile(), outputDir.toFile()));

            assertTrue(Files.isDirectory(outputDir));
            try (java.util.stream.Stream<Path> stream = Files.list(outputDir)) {
                assertEquals(0L, stream.count(), "空ZIPの場合は何も展開されないこと");
            }
        }
    }

    @Nested
    @DisplayName("findDecodedFile メソッド")
    class FindDecodedFile {

        @Test
        @DisplayName("対象ディレクトリにファイルが存在する→ファイル名のリストを返す")
        void findDecodedFile_ファイルあり_リストを返す(@TempDir final Path tempDir) throws IOException {
            final LocalDate targetDate = LocalDate.of(2024, 5, 1);
            final Path targetDir = buildTargetDir(tempDir, targetDate);
            Files.createDirectories(targetDir);
            Files.writeString(targetDir.resolve("doc1.xbrl"), "dummy1");
            Files.writeString(targetDir.resolve("doc2.xbrl"), "dummy2");

            fileOperator.pathDecode = tempDir.toString();

            final Optional<List<String>> result = fileOperator.findDecodedFile(targetDate);

            assertTrue(result.isPresent(), "Optional は present であること");
            final List<String> names = result.get();
            assertEquals(2, names.size());
            assertTrue(names.contains("doc1.xbrl"));
            assertTrue(names.contains("doc2.xbrl"));
        }

        @Test
        @DisplayName("対象ディレクトリが存在しない→Optional.empty を返す")
        void findDecodedFile_ディレクトリ不在_empty(@TempDir final Path tempDir) {
            fileOperator.pathDecode = tempDir.toString();

            final Optional<List<String>> result = fileOperator.findDecodedFile(LocalDate.of(2099, 12, 31));

            assertNotNull(result);
            assertTrue(result.isEmpty(), "ディレクトリ不在時は Optional.empty を返すこと");
        }

        @Test
        @DisplayName("対象ディレクトリが空→空のリストを返す")
        void findDecodedFile_空ディレクトリ_空リスト(@TempDir final Path tempDir) throws IOException {
            final LocalDate targetDate = LocalDate.of(2024, 1, 15);
            final Path targetDir = buildTargetDir(tempDir, targetDate);
            Files.createDirectories(targetDir);

            fileOperator.pathDecode = tempDir.toString();

            final Optional<List<String>> result = fileOperator.findDecodedFile(targetDate);

            assertTrue(result.isPresent(), "存在する空ディレクトリでは present であること");
            assertEquals(0, result.get().size(), "空ディレクトリは空のリストを返すこと");
        }

        @Test
        @DisplayName("月名フォルダ・年フォルダのパス構造を正しく解決する")
        void findDecodedFile_パス構造_正しく解決(@TempDir final Path tempDir) throws IOException {
            final LocalDate targetDate = LocalDate.of(2023, 7, 20);
            // FileOperator は "{prePath}/{year}/{Month}/{date}" のパスを参照する
            final Path targetDir = Path.of(
                    tempDir.toString(),
                    String.valueOf(targetDate.getYear()),
                    targetDate.getMonth().toString(),
                    targetDate.toString()
            );
            Files.createDirectories(targetDir);
            Files.writeString(targetDir.resolve("only.txt"), "only");

            fileOperator.pathDecode = tempDir.toString();

            final Optional<List<String>> result = fileOperator.findDecodedFile(targetDate);

            assertTrue(result.isPresent());
            assertEquals(List.of("only.txt"), result.get());
        }
    }

    /**
     * ZIP データの構築用レコード。
     *
     * @param name    エントリ名
     * @param content バイトコンテンツ
     */
    private record ZipEntryData(String name, byte[] content) {
    }

    /**
     * テスト用に ZIP ファイルを作成する（FileOperator が読み取る MS932 をエントリ名エンコーディングに使用）。
     *
     * @param zipPath ZIP ファイルの絶対パス
     * @param entries 書き込むエントリ
     * @throws IOException 入出力例外
     */
    private static void createZip(final Path zipPath, final List<ZipEntryData> entries) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(
                new BufferedOutputStream(new FileOutputStream(zipPath.toFile())),
                Charset.forName("MS932")
        )) {
            for (final ZipEntryData entry : entries) {
                final ZipEntry zipEntry = new ZipEntry(entry.name());
                zos.putNextEntry(zipEntry);
                zos.write(entry.content());
                zos.closeEntry();
            }
        }
    }

    /**
     * findDecodedFile が参照するターゲットディレクトリを組み立てる。
     *
     * @param base       ベースとなる pathDecode
     * @param targetDate 提出日
     * @return ターゲットディレクトリパス
     */
    private static Path buildTargetDir(final Path base, final LocalDate targetDate) {
        return Path.of(
                base.toString(),
                String.valueOf(targetDate.getYear()),
                targetDate.getMonth().toString(),
                targetDate.toString()
        );
    }
}
