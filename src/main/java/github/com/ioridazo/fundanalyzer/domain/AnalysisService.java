package github.com.ioridazo.fundanalyzer.domain;

import github.com.ioridazo.fundanalyzer.domain.csv.CsvCommander;
import github.com.ioridazo.fundanalyzer.domain.csv.bean.EdinetCsvResultBean;
import github.com.ioridazo.fundanalyzer.domain.dao.master.BalanceSheetDetailDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.BalanceSheetSubjectDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.FinancialStatementDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.IndustryDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.BalanceSheetDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.EdinetDocumentDao;
import github.com.ioridazo.fundanalyzer.domain.entity.DocumentStatus;
import github.com.ioridazo.fundanalyzer.domain.entity.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Industry;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.BalanceSheet;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Document;
import github.com.ioridazo.fundanalyzer.domain.file.FileOperator;
import github.com.ioridazo.fundanalyzer.domain.jsoup.HtmlScraping;
import github.com.ioridazo.fundanalyzer.domain.jsoup.bean.FinancialTableResultBean;
import github.com.ioridazo.fundanalyzer.edinet.EdinetProxy;
import github.com.ioridazo.fundanalyzer.edinet.entity.request.AcquisitionRequestParameter;
import github.com.ioridazo.fundanalyzer.edinet.entity.request.AcquisitionType;
import github.com.ioridazo.fundanalyzer.edinet.entity.request.ListRequestParameter;
import github.com.ioridazo.fundanalyzer.edinet.entity.request.ListType;
import github.com.ioridazo.fundanalyzer.edinet.entity.response.EdinetResponse;
import github.com.ioridazo.fundanalyzer.edinet.entity.response.Metadata;
import github.com.ioridazo.fundanalyzer.edinet.entity.response.ResultSet;
import github.com.ioridazo.fundanalyzer.mapper.CsvMapper;
import github.com.ioridazo.fundanalyzer.mapper.EdinetMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class AnalysisService {

    private final File pathCompany;
    private final File pathEdinet;
    private final File pathDecode;
    private final EdinetProxy proxy;
    private final CsvCommander csvCommander;
    private final FileOperator fileOperator;
    private final HtmlScraping htmlScraping;
    private final CsvMapper csvMapper;
    private final IndustryDao industryDao;
    private final CompanyDao companyDao;
    private final FinancialStatementDao financialStatementDao;
    private final BalanceSheetSubjectDao balanceSheetSubjectDao;
    private final BalanceSheetDetailDao balanceSheetDetailDao;
    private final EdinetDocumentDao edinetDocumentDao;
    private final DocumentDao documentDao;
    private final BalanceSheetDao balanceSheetDao;

    public AnalysisService(
            @Value("${settings.file.path.company}") final File pathCompany,
            @Value("${settings.file.path.edinet}") final File pathEdinet,
            @Value("${settings.file.path.decode}") final File pathDecode,
            final EdinetProxy proxy,
            final CsvCommander csvCommander,
            final FileOperator fileOperator,
            final HtmlScraping htmlScraping,
            final CsvMapper csvMapper,
            final IndustryDao industryDao,
            final CompanyDao companyDao,
            final FinancialStatementDao financialStatementDao,
            final BalanceSheetSubjectDao balanceSheetSubjectDao,
            final BalanceSheetDetailDao balanceSheetDetailDao,
            final EdinetDocumentDao edinetDocumentDao,
            final DocumentDao documentDao,
            final BalanceSheetDao balanceSheetDao
    ) {
        this.pathCompany = pathCompany;
        this.pathEdinet = pathEdinet;
        this.pathDecode = pathDecode;
        this.proxy = proxy;
        this.csvCommander = csvCommander;
        this.fileOperator = fileOperator;
        this.htmlScraping = htmlScraping;
        this.csvMapper = csvMapper;
        this.industryDao = industryDao;
        this.companyDao = companyDao;
        this.financialStatementDao = financialStatementDao;
        this.balanceSheetSubjectDao = balanceSheetSubjectDao;
        this.balanceSheetDetailDao = balanceSheetDetailDao;
        this.edinetDocumentDao = edinetDocumentDao;
        this.documentDao = documentDao;
        this.balanceSheetDao = balanceSheetDao;
    }

    public String company() {
        var resultBeanList = csvCommander.readCsv(
                pathCompany,
                Charset.forName("windows-31j"),
                EdinetCsvResultBean.class
        );
        resultBeanList.forEach(resultBean -> {
            if ("0".equals(industryDao.countByName(resultBean.getIndustry())))
                industryDao.insert(new Industry(null, resultBean.getIndustry()));
            csvMapper.map(resultBean).ifPresent(companyDao::insert);
        });
        return "会社を登録しました\n";
    }

    public String getDocumentFile(final String startDate, final String endDate, final String docTypeCode) {
        // 書類リストをデータベースに登録する
        insertDocument(LocalDate.parse(startDate), LocalDate.parse(endDate));

        // zipファイルを取得して解凍する
        unZipFiles(docTypeCode);
        return "書類一覧を登録できました。\n";
    }

    private void insertDocument(final LocalDate startDate, final LocalDate endDate) {
        startDate.datesUntil(endDate.plusDays(1)).collect(Collectors.toList())
                .forEach(localDate -> Stream.of(localDate.toString())
                        .filter(date -> Stream.of(date)
                                // EDINETに提出書類の問い合わせ
                                .map(d -> proxy.list(new ListRequestParameter(d, ListType.DEFAULT)))
                                .map(EdinetResponse::getMetadata)
                                .map(Metadata::getResultset)
                                .map(ResultSet::getCount)
                                .peek(c -> log.info("EDINETに提出された書類\tdate:{}\tcount:{}", localDate, c))
                                .anyMatch(c -> !"0".equals(c))
                        )
                        // 書類が0件ではないときは書類リストを取得する
                        .map(date -> proxy.list(new ListRequestParameter(date, ListType.GET_LIST)))
                        .map(EdinetResponse::getResults)
                        .forEach(resultsList -> resultsList.forEach(results -> {
                            if ("0".equals(edinetDocumentDao.countByDocId(results.getDocId())))
                                edinetDocumentDao.insert(EdinetMapper.map(results));
                            documentDao.insert(Document.builder()
                                    .docId(results.getDocId())
                                    .docTypeCode(results.getDocTypeCode())
                                    .filerName(results.getFilerName())
                                    .build());
                        }))
                );
    }

    private void unZipFiles(final String docTypeCode) {
        // 対象書類をリストにする
        documentDao.selectByDocTypeCode(docTypeCode).forEach(document -> {
            if (DocumentStatus.NOT_YET.toValue().equals(document.getDownloaded())) {
                // 書類をダウンロードする
                log.info("書類をダウンロードを開始\t書類種別コード:{}\t銘柄名:{}", document.getDocTypeCode(), document.getFilerName());
                proxy.acquisition(
                        pathEdinet,
                        new AcquisitionRequestParameter(document.getDocId(), AcquisitionType.DEFAULT)
                );
                documentDao.update(Document.builder().downloaded(DocumentStatus.DONE.toValue()).build());
                // 書類を解凍する
                fileOperator.decodeZipFile(
                        new File(pathEdinet + "/" + document.getDocId()),
                        new File(pathDecode + "/" + document.getDocId())
                );
                documentDao.update(Document.builder().decoded(DocumentStatus.DONE.toValue()).build());
                log.info("正常終了\t書類コード:{}", document.getDocId());
            }
        });
    }

    public List<FinancialTableResultBean> scrape(final String docId) {
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
                    fileList.stream().distinct().findAny().orElse(null),
                    "BalanceSheetTextBlock"
            );
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("スクレイピングに失敗しました");
        }
    }

    public void insert(final List<FinancialTableResultBean> beanList) {

        beanList.forEach(resultBean -> balanceSheetDetailDao.selectAll().stream()
                // スクレイピング結果とマスタから一致するものをフィルターにかける
                .filter(balanceSheetDetail -> resultBean.getSubject().equals(balanceSheetDetail.getName()))
                .findAny()
                // 一致するものが存在したら下記
                .ifPresent(balanceSheetDetail -> balanceSheetDao.insert(new BalanceSheet(
                        null,
                        "0000",
                        FinancialStatementEnum.BALANCE_SHEET.toValue(),
                        balanceSheetDetail.getId(),
                        LocalDate.now(),
                        replaceStringWithInteger(resultBean.getCurrentValue())
                )))
        );
    }

    private Integer replaceStringWithInteger(final String value) {
        return Integer.valueOf(value
                .replace(",", "")
                .replace("△", "-")
                .replace("※２ ", ""));
    }
}
