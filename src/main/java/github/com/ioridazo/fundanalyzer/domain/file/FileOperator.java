package github.com.ioridazo.fundanalyzer.domain.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Component
public class FileOperator {

    public FileOperator() {
    }

    public void decodeZipFile(File fileInputPath, File fileOutputPath) {
        byte[] buffer = new byte[1024];

        try {
            FileInputStream fis = new FileInputStream(fileInputPath + ".zip");
            BufferedInputStream bis = new BufferedInputStream(fis);
            ZipInputStream zis = new ZipInputStream(bis, Charset.forName("MS932"));

            if (!fileOutputPath.exists()) fileOutputPath.mkdir();

            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = new File(fileOutputPath + "/" + File.separator + zipEntry.getName());

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
        } catch (IOException e) {
            e.printStackTrace();
            log.error("zipファイルの解凍に失敗しました。");
        }
    }
}
