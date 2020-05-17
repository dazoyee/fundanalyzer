package github.com.ioridazo.fundanalyzer.domain;

import github.com.ioridazo.fundanalyzer.domain.csv.CsvCommander;
import github.com.ioridazo.fundanalyzer.domain.csv.bean.EdinetCsvResultBean;
import github.com.ioridazo.fundanalyzer.domain.dao.master.BalanceSheetSubjectDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.IndustryDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.EdinetDocumentDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.FinancialStatementDao;
import github.com.ioridazo.fundanalyzer.domain.entity.DocumentStatus;
import github.com.ioridazo.fundanalyzer.domain.entity.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Detail;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Industry;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Document;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.EdinetDocument;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.FinancialStatement;
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
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerFileException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import github.com.ioridazo.fundanalyzer.mapper.CsvMapper;
import github.com.ioridazo.fundanalyzer.mapper.EdinetMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
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
    private final BalanceSheetSubjectDao balanceSheetSubjectDao;
    private final EdinetDocumentDao edinetDocumentDao;
    private final DocumentDao documentDao;
    private final FinancialStatementDao financialStatementDao;

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
            final BalanceSheetSubjectDao balanceSheetSubjectDao,
            final EdinetDocumentDao edinetDocumentDao,
            final DocumentDao documentDao,
            final FinancialStatementDao financialStatementDao
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
        this.balanceSheetSubjectDao = balanceSheetSubjectDao;
        this.edinetDocumentDao = edinetDocumentDao;
        this.documentDao = documentDao;
        this.financialStatementDao = financialStatementDao;
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
            // TODO ２回目のINSERTへの処理
            csvMapper.map(resultBean).ifPresent(companyDao::insert);
        });
        return "会社を登録しました\n";
    }

    public String document(final String startDate, final String endDate, final String docTypeCode) {
        // 書類リストをデータベースに登録する
        insertDocument(LocalDate.parse(startDate), LocalDate.parse(endDate));

        // FIXME 応急処置 取得済ファイルのステータス更新
        for (final File file : Objects.requireNonNull(pathDecode.listFiles())) {
            documentDao.update(Document.builder()
                    .docId(file.getName())
                    .downloaded(DocumentStatus.DONE.toValue())
                    .decoded(DocumentStatus.DONE.toValue())
                    .build()
            );
        }

        // zipファイルを取得して解凍する
        // FIXME
        var docIdList = unZipFiles(docTypeCode).getFirst();
        docIdList = documentDao.selectByDocTypeCode("120").stream()
                .filter(document -> document.getDecoded().equals(DocumentStatus.DONE.toValue()))
                .map(Document::getDocId)
                .collect(Collectors.toList());

        docIdList.forEach(docId -> {
            final var edinetCode = edinetDocumentDao.selectByDocId(docId).stream()
                    .map(EdinetDocument::getEdinetCode)
                    .distinct()
                    .findAny()
                    .orElseThrow();

            // FIXME
            var company = "00000";
            if (companyDao.selectByEdinetCode(edinetCode) != null) {
                company = companyDao.selectByEdinetCode(edinetCode).getCode();
            }

            try {
                try {
                    // スクレイピングする
                    final var beanList = scrape(docId, FinancialStatementEnum.BALANCE_SHEET);
                    // DBに登録する
                    insert(
                            FinancialStatementEnum.BALANCE_SHEET,
                            balanceSheetSubjectDao.selectAll(),
                            beanList,
                            financialStatementDao::insert,
                            company
                    );
                } catch (FundanalyzerFileException e) {
                    // CONSOLIDATED_BALANCE_SHEET
                    final var beanList = scrape(docId, FinancialStatementEnum.CONSOLIDATED_BALANCE_SHEET);
                    insert(
                            FinancialStatementEnum.CONSOLIDATED_BALANCE_SHEET,
                            balanceSheetSubjectDao.selectAll(),
                            beanList,
                            financialStatementDao::insert,
                            company
                    );
                }
                documentDao.update(Document.builder().docId(docId).scrapedBalanceSheet(DocumentStatus.DONE.toValue()).build());
            } catch (FundanalyzerRuntimeException | FundanalyzerFileException e) {
                log.error(e.getMessage());  // TODO エラーハンドリング
                documentDao.update(Document.builder().docId(docId).scrapedBalanceSheet(DocumentStatus.ERROR.toValue()).build());
                System.out.println("スクレイピング失敗：" + docId);
            }
        });

        // TODO ステータス更新のタイミングがバラバラなので、統一する

        final var documents = documentDao.selectByDocTypeCode(docTypeCode);
        var count = documents.stream()
                .map(Document::getScrapedBalanceSheet)
                .filter(s -> s.equals(DocumentStatus.ERROR.toValue()))
                .count();
        // TODO return
        return "成功：" + (documents.stream().count() - count) + "\t失敗：" + count + "\n";
    }

    public void insertDocument(final LocalDate startDate, final LocalDate endDate) {
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
                            if ("0".equals(edinetDocumentDao.countByDocId(results.getDocId()))) {
                                edinetDocumentDao.insert(EdinetMapper.map(results));
                                documentDao.insert(Document.builder()
                                        .docId(results.getDocId())
                                        .docTypeCode(results.getDocTypeCode())
                                        .filerName(results.getFilerName())
                                        .build());
                            }
                        }))
                );
    }

    // TODO 戻り値をList<String> -> String にする
    Pair<List<String>, List<String>> unZipFiles(final String docTypeCode) {
        List<String> successList = new ArrayList<>();
        List<String> failureList = new ArrayList<>();

        // 対象書類をリストにする
        documentDao.selectByDocTypeCode(docTypeCode).forEach(document -> {
            if (DocumentStatus.NOT_YET.toValue().equals(document.getDownloaded())) {
                // 書類をダウンロードする
                log.info("書類をダウンロードを開始\t書類種別コード:{}\t銘柄名:{}", document.getDocTypeCode(), document.getFilerName());
                proxy.acquisition(
                        pathEdinet,
                        new AcquisitionRequestParameter(document.getDocId(), AcquisitionType.DEFAULT)
                );
                documentDao.update(Document.builder().docId(document.getDocId()).downloaded(DocumentStatus.DONE.toValue()).build());
                // 書類を解凍する
                try {
                    fileOperator.decodeZipFile(
                            new File(pathEdinet + "/" + document.getDocId()),
                            new File(pathDecode + "/" + document.getDocId())
                    );
                    documentDao.update(Document.builder().docId(document.getDocId()).decoded(DocumentStatus.DONE.toValue()).build());
                    successList.add(document.getDocId());
                    log.info("正常終了\t書類コード:{}", document.getDocId());
                } catch (IOException e) {
                    log.error("zipファイルの解凍に失敗しました。対象ファイル：{}", document.getDocId());
                    failureList.add(document.getDocId());
                }
            }
        });
        // TODO return はデコードできたかどうかを返す -> DocumentStatusを返すか？
        return Pair.of(successList, failureList);
    }

    List<FinancialTableResultBean> scrape(
            final String docId,
            final FinancialStatementEnum financialStatement) throws FundanalyzerFileException {
            final var file = htmlScraping.findFile(
                    new File(pathDecode + "/" + docId + "/XBRL/PublicDoc"),
                    financialStatement
            );
            // スクレイピング処理
            return htmlScraping.scrapeFinancialStatement(
                    file.orElseThrow(() ->
                            new FundanalyzerRuntimeException(financialStatement.getName() + "に関連するファイルが存在しませんでした。")),
                    financialStatement.getKeyWord()
            );
        // "損益計算書"
        // StatementOfIncomeTextBlock
    }

    <T extends Detail> void insert(
            final FinancialStatementEnum financialStatement,
            final List<T> detailList,
            final List<FinancialTableResultBean> beanList,
            final Consumer<FinancialStatement> insert,
            final String companyCode) {
        beanList.forEach(resultBean -> detailList.stream()
                // スクレイピング結果とマスタから一致するものをフィルターにかける
                .filter(detail -> resultBean.getSubject().equals(detail.getName()))
                .findAny()
                // 一致するものが存在したら下記
                .ifPresent(detail -> insert.accept(new FinancialStatement(
                        null,
                        companyCode,
                        financialStatement.toValue(),
                        detail.getId(),
                        LocalDate.now(),
                        replaceStringWithInteger(resultBean.getCurrentValue()).orElse(null)
                )))
        );
    }

    private Optional<Long> replaceStringWithInteger(final String value) {
        System.out.println(value);
        return Optional.of(value)
                .filter(s -> !s.isEmpty())
                .map(s -> Long.parseLong(s
                        .replace(",", "")
                        .replace("△", "-")
                        .replace("※２ ", "")
                        .replace("*2 ", "")
                ));
        // TODO 数値に変換できなかったときのエラーハンドリング
    }
}
