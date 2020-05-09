package github.com.ioridazo.fundanalyzer.domain.jsoup;

import github.com.ioridazo.fundanalyzer.domain.jsoup.bean.FinancialTableResultBean;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerFileException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
public class HtmlScraping {

    public HtmlScraping() {
    }

    public Optional<File> findFile(final File filePath, final String keyWord)
            throws FundanalyzerFileException, FundanalyzerRuntimeException {
        List<File> filePathList = new ArrayList<>();

        for (final File file : Objects.requireNonNull(filePath.listFiles())) {
            final var filePathName = new File(filePath + "/" + file.getName());
            // 対象のディレクトリから"honbun"ファイルを取得
            if (file.getName().contains("honbun")) {
                // クエリをhに絞って探索
                List.of("h1", "h2", "h3", "h4", "h5").forEach(query -> {
                    try {
                        // hクエリの存在する分だけ回す
                        Jsoup.parse(filePathName, "UTF-8")
                                .body()
                                .children()
                                .select(query)
                                .forEach(hQuery -> {
                                    if (hQuery.text().contains(keyWord)) filePathList.add(filePathName);
                                });
                    } catch (IOException e) {
                        log.error("ファイル認識エラー：{}", filePathName.getName());
                        throw new FundanalyzerRuntimeException("ファイルの認識に失敗しました。スタックトレースから詳細を確認してください。", e);
                    }
                });
            }
        }
        if (filePathList.size() > 1) {
            filePathList.forEach(file -> log.error("複数ファイルエラー\tキーワード：{}\t対象ファイル：{}", keyWord, file));
            throw new FundanalyzerFileException(keyWord + "に関するファイルが複数検出されました。スタックトレースを参考に詳細を確認してください。");
        }
        return filePathList.stream().findAny();
    }

    public List<FinancialTableResultBean> scrapeFinancialStatement(
            final File file, final String keyWord) throws FundanalyzerFileException {
        var resultBeanList = new ArrayList<FinancialTableResultBean>();
        // ファイルをスクレイピング
        try {
            Jsoup.parse(file, "UTF-8")
                    // 条件に沿った要素を得る
                    .getElementsByAttributeValueContaining("name", keyWord)
                    .select("table")
                    .select("tr")
                    .forEach(tr -> {
                        var tdList = new ArrayList<String>();
                        tr.select("td").forEach(td -> tdList.add(td.text()));
                        // 各要素をbeanに詰める
                        resultBeanList.add(new FinancialTableResultBean(tdList.get(0), tdList.get(1), tdList.get(2)));
                    });
        } catch (IOException e) {
            log.error("スクレイピングエラー：{}", file.getName());
            throw new FundanalyzerFileException("ファイルの認識に失敗しました。スタックトレースから詳細を確認してください。", e);
        }
        return resultBeanList;
    }
}
