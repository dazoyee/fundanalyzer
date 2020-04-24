package github.com.ioridazo.fundanalyzer.domain.jsoup;

import github.com.ioridazo.fundanalyzer.domain.jsoup.bean.FinancialTableResultBean;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class HtmlScraping {

    public HtmlScraping() {
    }

    public List<File> findFile(File filePath, String keyWord) {

        List<File> filePathList = new ArrayList<>();
        for (final File file : Objects.requireNonNull(filePath.listFiles())) {
            var filePathName = new File(filePath + "/" + file.getName());
            // 対象のディレクトリから"honbun"ファイルを取得
            if (file.getName().contains(keyWord)) {
                // クエリをhに絞って探索
                List.of("h1", "h2", "h3", "h4", "h5").forEach(query -> {
                    try {
                        // hクエリの存在する分だけ回す
                        Jsoup.parse(filePathName, "UTF-8")
                                .body()
                                .children()
                                .select(query)
                                .forEach(hQuery -> {
                                    if (hQuery.text().contains("貸借対照表")) filePathList.add(filePathName);
                                    if (hQuery.text().contains("損益計算書")) filePathList.add(filePathName);
                                });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        }
        return filePathList;
    }

    public List<FinancialTableResultBean> scrape(File file, String keyWord) throws IOException {
        var resultBeanList = new ArrayList<FinancialTableResultBean>();
        // ファイルをスクレイピング
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
        return resultBeanList;
    }
}
