package github.com.ioridazo.fundanalyzer.domain.csv;

import com.opencsv.bean.CsvToBeanBuilder;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;

@Component
public class CsvCommander {

    public <T> List<T> readCsv(final File file, final Charset charset, final Class<? extends T> beanClass) {
        if (!file.exists()) throw new RuntimeException("ファイルがありません");

        return new CsvToBeanBuilder<T>(reader(file, charset))
                .withType(beanClass)
                .withSkipLines(0)
                .build()
                .parse();
    }

    private Reader reader(final File file, final Charset charset) {
        try {
            return Files.newBufferedReader(file.toPath(), charset);
        } catch (IOException e) {
            throw new RuntimeException("ファイル形式に問題がありました");
        }
    }
}
