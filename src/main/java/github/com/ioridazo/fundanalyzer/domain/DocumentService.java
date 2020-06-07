package github.com.ioridazo.fundanalyzer.domain;

import github.com.ioridazo.fundanalyzer.domain.csv.CsvCommander;
import github.com.ioridazo.fundanalyzer.domain.csv.bean.EdinetCsvResultBean;
import github.com.ioridazo.fundanalyzer.domain.dao.master.BalanceSheetSubjectDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.IndustryDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.ProfitAndLessStatementSubjectDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.EdinetDocumentDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.FinancialStatementDao;
import github.com.ioridazo.fundanalyzer.domain.entity.DocumentStatus;
import github.com.ioridazo.fundanalyzer.domain.entity.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Company;
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
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRestClientException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import github.com.ioridazo.fundanalyzer.mapper.CsvMapper;
import github.com.ioridazo.fundanalyzer.mapper.EdinetMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class DocumentService {

    private final File pathCompany;
    private final File pathEdinet;
    private final File pathDecode;
    private final EdinetProxy proxy;
    private final CsvCommander csvCommander;
    private final FileOperator fileOperator;
    private final HtmlScraping htmlScraping;
    private final IndustryDao industryDao;
    private final CompanyDao companyDao;
    private final BalanceSheetSubjectDao bsSubjectDao;
    private final ProfitAndLessStatementSubjectDao plSubjectDao;
    private final EdinetDocumentDao edinetDocumentDao;
    private final DocumentDao documentDao;
    private final FinancialStatementDao financialStatementDao;

    public DocumentService(
            @Value("${settings.file.path.company}") final File pathCompany,
            @Value("${settings.file.path.edinet}") final File pathEdinet,
            @Value("${settings.file.path.decode}") final File pathDecode,
            final EdinetProxy proxy,
            final CsvCommander csvCommander,
            final FileOperator fileOperator,
            final HtmlScraping htmlScraping,
            final IndustryDao industryDao,
            final CompanyDao companyDao,
            final BalanceSheetSubjectDao bsSubjectDao,
            final ProfitAndLessStatementSubjectDao plSubjectDao,
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
        this.industryDao = industryDao;
        this.companyDao = companyDao;
        this.bsSubjectDao = bsSubjectDao;
        this.plSubjectDao = plSubjectDao;
        this.edinetDocumentDao = edinetDocumentDao;
        this.documentDao = documentDao;
        this.financialStatementDao = financialStatementDao;
    }

    public String company() {
        log.info("CSVファイルから会社情報の取得処理を開始します。");

        final var resultBeanList = csvCommander.readCsv(
                pathCompany,
                Charset.forName("windows-31j"),
                EdinetCsvResultBean.class
        );

        final var resultBeanIndustryList = resultBeanList.stream()
                .map(EdinetCsvResultBean::getIndustry)
                .distinct()
                .collect(Collectors.toList());

        var dbIndustryList = industryDao.selectAll().stream()
                .map(Industry::getName)
                .collect(Collectors.toList());

        // Industryの登録
        resultBeanIndustryList.forEach(resultBeanIndustry -> Stream.of(resultBeanIndustry)
                .filter(rb -> dbIndustryList.stream().noneMatch(rb::equals))
                .forEach(rb -> industryDao.insert(new Industry(null, rb)))
        );

        final var industryList = industryDao.selectAll();
        final var companyList = companyDao.selectAll();
        resultBeanList.forEach(resultBean -> Stream.of(resultBean)
                .filter(rb -> companyList.stream()
                        .map(Company::getCode)
                        .noneMatch(code -> resultBean.getSecuritiesCode().equals(code)))
                .forEach(rb -> companyDao.insert(CsvMapper.map(industryList, rb)))
        );

        log.info("会社情報をデータベースに正常に登録しました。");

        return "会社を登録しました\n";
    }

    public String document(final String startDate, final String endDate, final String docTypeCode) {
        final var dateList = LocalDate.parse(startDate)
                .datesUntil(LocalDate.parse(endDate).plusDays(1))
                .collect(Collectors.toList());

        // 書類リストをデータベースに登録する
        dateList.forEach(this::insertDocumentList);

        return "ドキュメントリストをDBに登録しました\n";
    }

    public String document(final String date, final String docTypeCode) {
        // 書類リストをデータベースに登録する
        insertDocumentList(LocalDate.parse(date));

        // 対象ファイルリスト取得
        final var docIdList = documentDao.selectByDateAndDocTypeCode(LocalDate.parse(date), docTypeCode)
                .stream()
                .map(Document::getDocId)
                .collect(Collectors.toList());

        docIdList.forEach(docId -> {
            // 書類取得
            store(LocalDate.parse(date), docId);

            // スクレイピング
            scrape(LocalDate.parse(date), docId);
        });

        return "ドキュメントを登録しました\n";
    }

    public void insertDocumentList(final LocalDate date) {
        final var docIdList = edinetDocumentDao.selectAll().stream()
                .map(EdinetDocument::getDocId)
                .collect(Collectors.toList());

        Stream.of(date.toString())
                .filter(dateString -> Stream.of(dateString)
                        .peek(d -> log.info("書類一覧（メタデータ）取得処理を実行します。\t取得対象日:{}", d))
                        // EDINETに提出書類の問い合わせ
                        .map(d -> proxy.list(new ListRequestParameter(d, ListType.DEFAULT)))
                        .map(EdinetResponse::getMetadata)
                        .map(Metadata::getResultset)
                        .map(ResultSet::getCount)
                        .peek(c -> log.info("書類一覧（メタデータ）を正常に取得しました。\t対象ファイル件数:{}", c))
                        .anyMatch(c -> !"0".equals(c))
                )
                // 書類が0件ではないときは書類リストを取得する
                .peek(dateString -> log.info("書類一覧（提出書類一覧及びメタデータ）取得処理を実行します。\t取得対象日:{}", dateString))
                .map(dateString -> proxy.list(new ListRequestParameter(dateString, ListType.GET_LIST)))
                .peek(er -> log.info("書類一覧（提出書類一覧及びメタデータ）を正常に取得しました。データベースへの登録作業を開始します。"))
                .map(EdinetResponse::getResults)
                .forEach(resultsList -> resultsList.forEach(results -> {
                    Stream.of(results)
                            .filter(r -> docIdList.stream().noneMatch(docId -> r.getDocId().equals(docId)))
                            .forEach(r -> {
                                edinetDocumentDao.insert(EdinetMapper.map(r));
                                documentDao.insert(Document.builder()
                                        .docId(r.getDocId())
                                        .docTypeCode(r.getDocTypeCode())
                                        .filerName(r.getFilerName())
                                        .submitDate(date)
                                        .build()
                                );
                            });
                }));
        log.info("データベースへの書類一覧登録作業が正常に終了しました。\t指定ファイル日付:{}", date);
    }

    public void store(final LocalDate targetDate, final String docId) {
        // 取得済ファイルリスト
        final var fileListAlready = Optional.ofNullable(new File(pathDecode.getPath() + "/" + targetDate).listFiles())
                .map(Arrays::stream)
                .map(fileList -> fileList.map(File::getName))
                .map(fileName -> fileName.collect(Collectors.toList()))
                .or(Optional::empty);

        if (fileListAlready.isPresent() && fileListAlready.get().stream().anyMatch(docId::equals)) {
            documentDao.update(Document.builder()
                    .docId(docId)
                    .downloaded(DocumentStatus.DONE.toValue())
                    .decoded(DocumentStatus.DONE.toValue())
                    .build()
            );
        } else {
            // ファイル取得
            download(targetDate, docId);
        }
    }

    void download(final LocalDate targetDate, final String docId) {
        try {
            log.info("書類のダウンロード処理を実行します。\t書類管理番号:{}", docId);

            proxy.acquisition(
                    new File(pathEdinet.getPath() + "/" + targetDate),
                    new AcquisitionRequestParameter(docId, AcquisitionType.DEFAULT)
            );

            log.info("書類のダウンロード処理が完了しました。zipファイルの解凍処理を実行します。");

            documentDao.update(Document.builder().docId(docId).downloaded(DocumentStatus.DONE.toValue()).build());

            fileOperator.decodeZipFile(
                    new File(pathEdinet + "/" + targetDate.toString() + "/" + docId),
                    new File(pathDecode + "/" + targetDate.toString() + "/" + docId)
            );

            log.info("zipファイルの解凍処理が正常に実行されました。");

            documentDao.update(Document.builder().docId(docId).decoded(DocumentStatus.DONE.toValue()).build());

        } catch (FundanalyzerRestClientException e) {
            log.error("書類のダウンロード処理に失敗しました。スタックトレースから原因を確認してください。" +
                            "\t処理対象日:{}" +
                            "\t書類管理番号:{}",
                    targetDate,
                    docId);
            documentDao.update(Document.builder().docId(docId).downloaded(DocumentStatus.ERROR.toValue()).build());
        } catch (IOException e) {
            log.error("zipファイルの解凍処理に失敗しました。スタックトレースから原因を確認してください。" +
                            "\t処理対象日:{}" +
                            "\t書類管理番号:{}",
                    targetDate,
                    docId);
            documentDao.update(Document.builder().docId(docId).decoded(DocumentStatus.ERROR.toValue()).build());
        }
    }

    public void scrape(final LocalDate targetDate, final String docId) {
        final var edinetDocument = edinetDocumentDao.selectByDocId(docId);
        final var company = companyDao.selectByEdinetCode(edinetDocument.getEdinetCode());

        final var targetFile = new File(pathDecode + "/" + targetDate + "/" + docId + "/XBRL/PublicDoc");

        log.info("スクレイピング処理を開始します。\t対象ファイルパス:\"{}\"", targetFile.getPath());

        try {
            scrapeBalanceSheet(targetFile, company, edinetDocument);
            log.info("ファイル情報をデータベースに正常に登録されました。\t書類管理番号:{}\t対象:{}", docId, "貸借対照表");
            documentDao.update(Document.builder().docId(docId).scrapedBalanceSheet(DocumentStatus.DONE.toValue()).build());
        } catch (FundanalyzerFileException e) {
            documentDao.update(Document.builder().docId(docId).scrapedBalanceSheet(DocumentStatus.ERROR.toValue()).build());
            log.error("ファイル情報をデータベースに登録できませんでした。\t書類管理番号:{}\t対象:{}", docId, "貸借対照表");
        }

        try {
            scrapeProfitAndLessStatement(targetFile, company, edinetDocument);
            log.info("ファイル情報をデータベースに正常に登録されました。\t書類管理番号:{}\t対象:{}", docId, "損益計算書");
            documentDao.update(Document.builder().docId(docId).scrapedProfitAndLessStatement(DocumentStatus.DONE.toValue()).build());
        } catch (FundanalyzerFileException e) {
            documentDao.update(Document.builder().docId(docId).scrapedProfitAndLessStatement(DocumentStatus.ERROR.toValue()).build());
            log.error("ファイル情報をデータベースに登録できませんでした。\t書類管理番号:{}\t対象:{}", docId, "損益計算書");
        }

        scrapeNumberOfShares(targetFile, company, edinetDocument);
        log.info("ファイル情報をデータベースに正常に登録されました。\t書類管理番号:{}\t対象:{}", docId, "株式総数");
        documentDao.update(Document.builder().docId(docId).scrapedNumberOfShares(DocumentStatus.DONE.toValue()).build());

        log.info("スクレイピング処理が正常に完了しました。");
    }

    void scrapeBalanceSheet(
            final File targetFile,
            final Company company,
            final EdinetDocument edinetDocument) throws FundanalyzerFileException {
        try {
            // スクレイピングする
            final var file = htmlScraping.findFile(targetFile, FinancialStatementEnum.BALANCE_SHEET).orElseThrow(FundanalyzerRuntimeException::new);
            final var resultBeans = htmlScraping.scrapeFinancialStatement(file, FinancialStatementEnum.BALANCE_SHEET.getKeyWord());
            // DBに登録する
            insertFinancialStatement(
                    FinancialStatementEnum.BALANCE_SHEET,
                    bsSubjectDao.selectAll(),
                    resultBeans,
                    financialStatementDao::insert,
                    company,
                    edinetDocument
            );
        } catch (FundanalyzerRuntimeException e) {
            // CONSOLIDATED_BALANCE_SHEET
            final var file = htmlScraping.findFile(targetFile, FinancialStatementEnum.CONSOLIDATED_BALANCE_SHEET).orElseThrow(FundanalyzerRuntimeException::new);
            final var resultBeans = htmlScraping.scrapeFinancialStatement(file, FinancialStatementEnum.CONSOLIDATED_BALANCE_SHEET.getKeyWord());
            // DBに登録する
            insertFinancialStatement(
                    FinancialStatementEnum.CONSOLIDATED_BALANCE_SHEET,
                    bsSubjectDao.selectAll(),
                    resultBeans,
                    financialStatementDao::insert,
                    company,
                    edinetDocument
            );
        }
    }

    void scrapeProfitAndLessStatement(
            final File targetFile,
            final Company company,
            final EdinetDocument edinetDocument) throws FundanalyzerFileException {
        try {
            // スクレイピングする
            final var file = htmlScraping.findFile(targetFile, FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT).orElseThrow(FundanalyzerRuntimeException::new);
            final var resultBeans = htmlScraping.scrapeFinancialStatement(file, FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT.getKeyWord());
            // DBに登録する
            insertFinancialStatement(
                    FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT,
                    plSubjectDao.selectAll(),
                    resultBeans,
                    financialStatementDao::insert,
                    company,
                    edinetDocument
            );
        } catch (FundanalyzerRuntimeException e) {
            // INCOME_AND_SURPLUS_STATEMENT
            final var file = htmlScraping.findFile(targetFile, FinancialStatementEnum.INCOME_AND_SURPLUS_STATEMENT).orElseThrow(FundanalyzerRuntimeException::new);
            final var resultBeans = htmlScraping.scrapeFinancialStatement(file, FinancialStatementEnum.INCOME_AND_SURPLUS_STATEMENT.getKeyWord());
            // DBに登録する
            insertFinancialStatement(
                    FinancialStatementEnum.INCOME_AND_SURPLUS_STATEMENT,
                    plSubjectDao.selectAll(),
                    resultBeans,
                    financialStatementDao::insert,
                    company,
                    edinetDocument
            );
        }
    }

    void scrapeNumberOfShares(
            final File targetFile,
            final Company company,
            final EdinetDocument edinetDocument) {
        financialStatementDao.insert(new FinancialStatement(
                null,
                company.getCode(),
                company.getEdinetCode(),
                FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES.toValue(),
                "0",
                LocalDate.parse(edinetDocument.getPeriodStart()),
                LocalDate.parse(edinetDocument.getPeriodEnd()),
                null,
                htmlScraping.findNumberOfShares(targetFile)
        ));
    }

    <T extends Detail> void insertFinancialStatement(
            final FinancialStatementEnum financialStatement,
            final List<T> detailList,
            final List<FinancialTableResultBean> beanList,
            final Consumer<FinancialStatement> insert,
            final Company company,
            final EdinetDocument edinetDocument) {
        log.info("スクレイピングの情報をデータベースに登録します。" +
                        "\t対象:{}" +
                        "\t会社コード:{}" +
                        "\tEDINETコード:{}",
                financialStatement.getName(),
                company.getCode(),
                company.getEdinetCode());

        beanList.forEach(resultBean -> detailList.stream()
                // スクレイピング結果とマスタから一致するものをフィルターにかける
                .filter(detail -> Objects.equals(resultBean.getSubject().orElse(null), detail.getName()))
                .findAny()
                // 一致するものが存在したら下記
                .ifPresent(detail -> insert.accept(new FinancialStatement(
                        null,
                        company.getCode(),
                        company.getEdinetCode(),
                        financialStatement.toValue(),
                        detail.getId(),
                        LocalDate.parse(edinetDocument.getPeriodStart()),
                        LocalDate.parse(edinetDocument.getPeriodEnd()),
                        replaceStringWithInteger(resultBean.getCurrentValue()).orElse(null),
                        null
                )))
        );

        log.info("スクレイピング情報のデータベース登録処理が正常に終了しました。");
    }

    private Optional<Long> replaceStringWithInteger(final String value) {
        return Optional.of(value)
                .filter(v -> !v.isEmpty())
                .map(s -> Long.parseLong(s
                        .replace(",", "")
                        .replace("△", "-")
                        .replace("※２ ", "")
                        .replace("*2 ", "")
                ));
        // TODO 数値に変換できなかったときのエラーハンドリング
    }
}
