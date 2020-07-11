package github.com.ioridazo.fundanalyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FundanalyzerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FundanalyzerApplication.class, args);

        /*
        Document document = Jsoup.parse(new File("C:\\Users\\ioiso\\Desktop\\Xbrl_Search_20200331_143611\\S100HGDP\\XBRL\\PublicDoc\\0205010_honbun_jpsps070300-asr-001_G03338-000_2019-08-31_01_2019-11-21_ixbrl.htm"), "UTF-8");

        Elements tables = document.body().children().select("table");
//        Element table = tables.get(0);
        tables.forEach(table -> {
            table.select("tr").forEach(tr -> {
                ArrayList<String> tdList = new ArrayList<>();
                tr.select("td").forEach(td -> tdList.add(td.text()));
//                System.out.println(tdList.get(0));
            });
        });
//        table.select("tr").forEach(tr -> {
//            ArrayList<String> tdList = new ArrayList<>();
//            tr.select("td").forEach(td -> tdList.add(td.text()));
//            System.out.println(tdList.get(0));
//        });
         */
    }
}
