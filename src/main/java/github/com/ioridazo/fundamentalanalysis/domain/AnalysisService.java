package github.com.ioridazo.fundamentalanalysis.domain;

import github.com.ioridazo.fundamentalanalysis.domain.csv.CsvCommander;
import github.com.ioridazo.fundamentalanalysis.domain.csv.bean.EdinetCsvResultBean;
import github.com.ioridazo.fundamentalanalysis.domain.dao.master.BalanceSheetDetailDao;
import github.com.ioridazo.fundamentalanalysis.domain.dao.master.BalanceSheetSubjectDao;
import github.com.ioridazo.fundamentalanalysis.domain.dao.master.FinancialStatementDao;
import github.com.ioridazo.fundamentalanalysis.domain.dao.transaction.BalanceSheetDao;
import github.com.ioridazo.fundamentalanalysis.domain.dao.transaction.EdinetDocumentDao;
import github.com.ioridazo.fundamentalanalysis.domain.entity.FinancialStatementEnum;
import github.com.ioridazo.fundamentalanalysis.domain.entity.transaction.BalanceSheet;
import github.com.ioridazo.fundamentalanalysis.domain.file.FileOperator;
import github.com.ioridazo.fundamentalanalysis.domain.jsoup.HtmlScraping;
import github.com.ioridazo.fundamentalanalysis.domain.jsoup.bean.FinancialTableResultBean;
import github.com.ioridazo.fundamentalanalysis.edinet.EdinetProxy;
import github.com.ioridazo.fundamentalanalysis.edinet.entity.request.AcquisitionRequestParameter;
import github.com.ioridazo.fundamentalanalysis.edinet.entity.request.ListRequestParameter;
import github.com.ioridazo.fundamentalanalysis.edinet.entity.request.ListType;
import github.com.ioridazo.fundamentalanalysis.edinet.entity.response.Response;
import github.com.ioridazo.fundamentalanalysis.mapper.EdinetMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class AnalysisService {

    private final File pathCompany;
    private final File pathEdinet;
    private final File pathDecode;
    private EdinetProxy proxy;
    private CsvCommander csvCommander;
    private FileOperator fileOperator;
    private HtmlScraping htmlScraping;
    private FinancialStatementDao financialStatementDao;
    private BalanceSheetSubjectDao balanceSheetSubjectDao;
    private BalanceSheetDetailDao balanceSheetDetailDao;
    private EdinetDocumentDao edinetDocumentDao;
    private BalanceSheetDao balanceSheetDao;

    public AnalysisService(
            @Value("${settings.file.path.company}") final File pathCompany,
            @Value("${settings.file.path.edinet}") final File pathEdinet,
            @Value("${settings.file.path.decode}") final File pathDecode,
            final EdinetProxy proxy,
            final CsvCommander csvCommander,
            final FileOperator fileOperator,
            final HtmlScraping htmlScraping,
            final FinancialStatementDao financialStatementDao,
            final BalanceSheetSubjectDao balanceSheetSubjectDao,
            final BalanceSheetDetailDao balanceSheetDetailDao,
            final EdinetDocumentDao edinetDocumentDao,
            final BalanceSheetDao balanceSheetDao
    ) {
        this.pathCompany = pathCompany;
        this.pathEdinet = pathEdinet;
        this.pathDecode = pathDecode;
        this.proxy = proxy;
        this.csvCommander = csvCommander;
        this.fileOperator = fileOperator;
        this.htmlScraping = htmlScraping;
        this.financialStatementDao = financialStatementDao;
        this.balanceSheetSubjectDao = balanceSheetSubjectDao;
        this.balanceSheetDetailDao = balanceSheetDetailDao;
        this.edinetDocumentDao = edinetDocumentDao;
        this.balanceSheetDao = balanceSheetDao;
    }

    public String company() {
        var resultBeanList = csvCommander.readCsv(
                pathCompany,
                Charset.forName("windows-31j"),
                EdinetCsvResultBean.class
        );
        return "会社を登録しました\n";
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

    public void insert(List<FinancialTableResultBean> beanList) {

        beanList.forEach(resultBean -> {
            balanceSheetDetailDao.findAll().stream()
                    // スクレイピング結果とマスタから一致するものをフィルターにかける
                    .filter(balanceSheetDetail -> resultBean.getSubject().equals(balanceSheetDetail.getName()))
                    .findAny()
                    // 一致するものが存在したら下記
                    .ifPresent(balanceSheetDetail -> {
                        balanceSheetDao.insert(new BalanceSheet(
                                null,
                                "0000",
                                FinancialStatementEnum.BALANCE_SHEET.toValue(),
                                balanceSheetDetail.getId(),
                                LocalDate.now(),
                                replaceStringWithInteger(resultBean.getCurrentValue())
                        ));
                    });
        });
    }

    private Integer replaceStringWithInteger(String value) {
        return Integer.valueOf(value
                .replace(",", "")
                .replace("△", "-")
                .replace("※２ ", ""));
    }
}
