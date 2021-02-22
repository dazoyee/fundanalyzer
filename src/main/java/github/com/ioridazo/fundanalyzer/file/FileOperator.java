package github.com.ioridazo.fundanalyzer.file;

import lombok.extern.log4j.Log4j2;
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

@Log4j2
@Component
public class FileOperator {

    public FileOperator() {
    }

    public void decodeZipFile(final File fileInputPath, final File fileOutputPath) throws IOException {
        byte[] buffer = new byte[1024];

        FileInputStream fis = new FileInputStream(fileInputPath + ".zip");
        BufferedInputStream bis = new BufferedInputStream(fis);
        ZipInputStream zis = new ZipInputStream(bis, Charset.forName("MS932"));

        if (!fileOutputPath.exists()) //noinspection ResultOfMethodCallIgnored
            fileOutputPath.mkdir();

        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            File newFile = new File(fileOutputPath + "/" + File.separator + zipEntry.getName());

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
    }
}
