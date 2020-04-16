package github.com.ioridazo.fundamentalanalysis.domain.jsoup;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
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

    public void scrape(File file) {
//        Document document = Jsoup.parse(new File("C:\\Users\\ioiso\\Desktop\\Xbrl_Search_20200331_143611\\S100HGDP\\XBRL\\PublicDoc\\0205010_honbun_jpsps070300-asr-001_G03338-000_2019-08-31_01_2019-11-21_ixbrl.htm"), "UTF-8");
        Document document = null;
        try {
            document = Jsoup.parse(file, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

        Elements tables = document.body().children().select("table");
//        Element table = tables.get(0);
        tables.forEach(table -> {
            table.select("tr").forEach(tr -> {
                ArrayList<String> tdList = new ArrayList<>();
                tr.select("td").forEach(td -> tdList.add(td.text()));
                System.out.println(tdList.get(0) + "\t" + tdList.get(1) + "\t" + tdList.get(2));
            });
        });
//        table.select("tr").forEach(tr -> {
//            ArrayList<String> tdList = new ArrayList<>();
//            tr.select("td").forEach(td -> tdList.add(td.text()));
//            System.out.println(tdList.get(0));
//        });

    }
}
