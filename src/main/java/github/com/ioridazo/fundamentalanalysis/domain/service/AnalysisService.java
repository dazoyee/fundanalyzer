package github.com.ioridazo.fundamentalanalysis.domain.service;

import github.com.ioridazo.fundamentalanalysis.domain.dao.BalanceSheetSubjectDao;
import github.com.ioridazo.fundamentalanalysis.domain.entity.ProfitAndLossStatementEnum;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;

@Service
public class AnalysisService {

    private BalanceSheetSubjectDao balanceSheetSubjectDao;

    public AnalysisService(final BalanceSheetSubjectDao balanceSheetSubjectDao) {
        this.balanceSheetSubjectDao = balanceSheetSubjectDao;
    }

    public void insert() throws Exception {

        Document document = Jsoup.parse(new File("C:\\Users\\ioiso\\Desktop\\Xbrl_Search_20200331_143611\\S100HGDP\\XBRL\\PublicDoc\\0205010_honbun_jpsps070300-asr-001_G03338-000_2019-08-31_01_2019-11-21_ixbrl.htm"), "UTF-8");
        String x = ProfitAndLossStatementEnum.OPERATING_PROFIT.toValue();
        System.out.println(x);

        Elements tables = document.body().children().select("table");
        Element table = tables.get(2);
        table.select("tr").forEach(tr -> {
            ArrayList<String> tdList = new ArrayList<>();
            tr.select("td").forEach(td -> tdList.add(td.text()));

            System.out.println(tdList.get(0));
//            if (ProfitAndLossStatementEnum.OPERATING_PROFIT.toValue().equals(tdList.get(0))){
//            }
        });
    }
}
