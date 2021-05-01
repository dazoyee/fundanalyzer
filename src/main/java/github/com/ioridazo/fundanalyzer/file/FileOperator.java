package github.com.ioridazo.fundanalyzer.file;

import github.com.ioridazo.fundanalyzer.domain.log.Category;
import github.com.ioridazo.fundanalyzer.domain.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.domain.log.Process;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class FileOperator {

    @Value("${app.settings.file.path.company.company}")
    String pathCompany;
    @Value("${app.settings.file.path.company.zip}")
    String pathCompanyZip;
    @Value("${app.settings.file.path.decode}")
    String pathDecode;

    FileOperator() {
    }

    public void decodeZipFile(final File fileInputPath, final File fileOutputPath) throws IOException {
        FundanalyzerLogClient.logLogic(
                MessageFormat.format("zipファイルの解凍処理を実行します。\tパス:{0}", fileInputPath.getPath()),
                Category.DOCUMENT,
                Process.DECODE
        );

        byte[] buffer = new byte[1024];

        FileInputStream fis = new FileInputStream(fileInputPath + ".zip");
        BufferedInputStream bis = new BufferedInputStream(fis);
        ZipInputStream zis = new ZipInputStream(bis, Charset.forName("MS932"));

        if (!fileOutputPath.exists()) {
            fileOutputPath.mkdir();
        }

        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            File newFile = new File(String.format("%s/%s%s", fileOutputPath, File.separator, zipEntry.getName()));

            //noinspection ResultOfMethodCallIgnored
            new File(newFile.getParent()).mkdirs();

            FileOutputStream fos = new FileOutputStream(newFile);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fos);

            int len;
            while ((len = zis.read(buffer)) > 0) {
                bufferedOutputStream.write(buffer, 0, len);
            }
            bufferedOutputStream.close();

            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();

        FundanalyzerLogClient.logLogic(
                MessageFormat.format("zipファイルの解凍処理が正常に実行されました。\tパス:{0}", fileOutputPath.getPath()),
                Category.DOCUMENT,
                Process.DECODE
        );
    }

    /**
     * デコード済みのファイルを取得する
     *
     * @param targetDate 提出日
     * @return ファイルリスト
     */
    public Optional<List<String>> findDecodedFile(final LocalDate targetDate) {
        return Optional.ofNullable(makeTargetPath(pathDecode, targetDate).listFiles())
                .map(Arrays::stream)
                .map(fileList -> fileList.map(File::getName))
                .map(fileName -> fileName.collect(Collectors.toList()))
                .or(Optional::empty);
    }

    private File makeTargetPath(final String prePath, final LocalDate targetDate) {
        return new File(String.format("%s/%d/%s/%s", prePath, targetDate.getYear(), targetDate.getMonth(), targetDate));
    }
}
