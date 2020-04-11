package github.com.ioridazo.fundamentalanalysis.domain;

import github.com.ioridazo.fundamentalanalysis.domain.dao.BalanceSheetSubjectDao;
import github.com.ioridazo.fundamentalanalysis.domain.dao.EdinetDocumentDao;
import github.com.ioridazo.fundamentalanalysis.domain.entity.ProfitAndLossStatementEnum;
import github.com.ioridazo.fundamentalanalysis.edinet.EdinetProxy;
import github.com.ioridazo.fundamentalanalysis.edinet.entity.request.AcquisitionRequestParameter;
import github.com.ioridazo.fundamentalanalysis.edinet.entity.request.ListType;
import github.com.ioridazo.fundamentalanalysis.edinet.entity.request.ListRequestParameter;
import github.com.ioridazo.fundamentalanalysis.edinet.entity.response.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.zip.ZipInputStream;

@Service
public class AnalysisService {

    private BalanceSheetSubjectDao balanceSheetSubjectDao;

    private EdinetDocumentDao edinetDocumentDao;

    private EdinetProxy proxy;

    public AnalysisService(
            final BalanceSheetSubjectDao balanceSheetSubjectDao,
            final EdinetDocumentDao edinetDocumentDao,
            final EdinetProxy edinetProxy
    ) {
        this.balanceSheetSubjectDao = balanceSheetSubjectDao;
        this.edinetDocumentDao = edinetDocumentDao;
        this.proxy = edinetProxy;
    }

    public Response documentList() {
        return proxy.documentList(new ListRequestParameter("2020-04-01", ListType.DEFAULT));
    }

    public String insertDocumentList(ListRequestParameter parameter) {
        Response response = proxy.documentList(parameter);
        if (response.getResults() != null)
            response.getResults().forEach(results -> edinetDocumentDao.insert(EdinetMapper.map(results)));
        return "書類一覧を登録できました。\n";
    }

    public String documentAcquisition(AcquisitionRequestParameter parameter){
        proxy.documentAcquisition(parameter);
        return "書類取得できました。\n";
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
