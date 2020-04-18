package github.com.ioridazo.fundamentalanalysis.domain;

import github.com.ioridazo.fundamentalanalysis.domain.dao.BalanceSheetDao;
import github.com.ioridazo.fundamentalanalysis.domain.dao.BalanceSheetDetailDao;
import github.com.ioridazo.fundamentalanalysis.domain.dao.BalanceSheetSubjectDao;
import github.com.ioridazo.fundamentalanalysis.domain.dao.EdinetDocumentDao;
import github.com.ioridazo.fundamentalanalysis.domain.dao.FinancialStatementDao;
import github.com.ioridazo.fundamentalanalysis.domain.entity.BalanceSheet;
import github.com.ioridazo.fundamentalanalysis.domain.entity.FinancialStatementEnum;
import github.com.ioridazo.fundamentalanalysis.domain.file.FileOperator;
import github.com.ioridazo.fundamentalanalysis.domain.jsoup.HtmlScraping;
import github.com.ioridazo.fundamentalanalysis.domain.jsoup.bean.FinancialTableResultBean;
import github.com.ioridazo.fundamentalanalysis.edinet.EdinetProxy;
import github.com.ioridazo.fundamentalanalysis.edinet.entity.request.AcquisitionRequestParameter;
import github.com.ioridazo.fundamentalanalysis.edinet.entity.request.ListRequestParameter;
import github.com.ioridazo.fundamentalanalysis.edinet.entity.request.ListType;
import github.com.ioridazo.fundamentalanalysis.edinet.entity.response.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class AnalysisService {

    private final File pathEdinet;
    private final File pathDecode;
    private EdinetProxy proxy;
    private FileOperator fileOperator;
    private HtmlScraping htmlScraping;
    private FinancialStatementDao financialStatementDao;
    private BalanceSheetSubjectDao balanceSheetSubjectDao;
    private BalanceSheetDetailDao balanceSheetDetailDao;
    private EdinetDocumentDao edinetDocumentDao;
    private BalanceSheetDao balanceSheetDao;

    public AnalysisService(
            @Value("${settings.file.path.edinet}") final File pathEdinet,
            @Value("${settings.file.path.decode}") final File pathDecode,
            final EdinetProxy proxy,
            final FileOperator fileOperator,
            final HtmlScraping htmlScraping,
            final FinancialStatementDao financialStatementDao,
            final BalanceSheetSubjectDao balanceSheetSubjectDao,
            final BalanceSheetDetailDao balanceSheetDetailDao,
            final EdinetDocumentDao edinetDocumentDao,
            final BalanceSheetDao balanceSheetDao
    ) {
        this.pathEdinet = pathEdinet;
        this.pathDecode = pathDecode;
        this.proxy = proxy;
        this.fileOperator = fileOperator;
        this.htmlScraping = htmlScraping;
        this.financialStatementDao = financialStatementDao;
        this.balanceSheetSubjectDao = balanceSheetSubjectDao;
        this.balanceSheetDetailDao = balanceSheetDetailDao;
        this.edinetDocumentDao = edinetDocumentDao;
        this.balanceSheetDao = balanceSheetDao;
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

    public List<String> docIdList(String docTypeCode) {
        ArrayList<String> docIdList = new ArrayList<>();
        edinetDocumentDao.findByDocTypeCode(docTypeCode).forEach(document -> docIdList.add(document.getDocId()));
        return docIdList;
    }

    public String documentAcquisition(AcquisitionRequestParameter parameter) {
        proxy.documentAcquisition(
                pathEdinet,
                parameter
        );
        return "書類取得できました。\n";
    }

    public String operateFile(String docId) {
        fileOperator.decodeZipFile(
                new File(pathEdinet + "/" + docId),
                new File(pathDecode + "/" + docId)
        );
        return "解凍できました。\n";
    }

    public List<FinancialTableResultBean> scrape(String docId) {
        var fileList = htmlScraping.findFile(
                new File(pathDecode + "/" + docId + "/XBRL/PublicDoc"),
                "honbun"
        );
        if (fileList.stream().distinct().count() != 1) {
            throw new RuntimeException("ファイルが複数ありました");
        }
        // StatementOfIncomeTextBlock
        try {
            return htmlScraping.scrape(
                    fileList.stream().distinct().findAny().get(),
                    "BalanceSheetTextBlock"
            );
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("スクレイピングに失敗しました");
        }
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
