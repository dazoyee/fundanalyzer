package github.com.ioridazo.fundanalyzer.domain;

import github.com.ioridazo.fundanalyzer.domain.csv.CsvCommander;
import github.com.ioridazo.fundanalyzer.domain.csv.bean.EdinetCsvResultBean;
import github.com.ioridazo.fundanalyzer.domain.dao.master.BsSubjectDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.IndustryDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.PlSubjectDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.ScrapingKeywordDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.EdinetDocumentDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.FinancialStatementDao;
import github.com.ioridazo.fundanalyzer.domain.entity.BsEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.DocumentStatus;
import github.com.ioridazo.fundanalyzer.domain.entity.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.Flag;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Company;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Detail;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Industry;
import github.com.ioridazo.fundanalyzer.domain.entity.master.ScrapingKeyword;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Document;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.EdinetDocument;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.FinancialStatement;
import github.com.ioridazo.fundanalyzer.domain.file.FileOperator;
import github.com.ioridazo.fundanalyzer.domain.jsoup.HtmlScraping;
import github.com.ioridazo.fundanalyzer.domain.jsoup.bean.Unit;
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
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerSqlForeignKeyException;
import github.com.ioridazo.fundanalyzer.mapper.CsvMapper;
import lombok.extern.slf4j.Slf4j;
import org.seasar.doma.jdbc.UniqueConstraintException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedRuntimeException;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
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
    private final ScrapingKeywordDao scrapingKeywordDao;
    private final BsSubjectDao bsSubjectDao;
    private final PlSubjectDao plSubjectDao;
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
            final ScrapingKeywordDao scrapingKeywordDao,
            final BsSubjectDao bsSubjectDao,
            final PlSubjectDao plSubjectDao,
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
        this.scrapingKeywordDao = scrapingKeywordDao;
        this.bsSubjectDao = bsSubjectDao;
        this.plSubjectDao = plSubjectDao;
        this.edinetDocumentDao = edinetDocumentDao;
        this.documentDao = documentDao;
        this.financialStatementDao = financialStatementDao;
    }

    @Transactional
    public void company() {
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
                .forEach(rb -> industryDao.insert(new Industry(null, rb, LocalDateTime.now())))
        );

        final var industryList = industryDao.selectAll();
        final var companyList = companyDao.selectAll();
        resultBeanList.forEach(resultBean -> Stream.of(resultBean)
                .filter(rb -> companyList.stream()
                        .map(Company::getEdinetCode)
                        .noneMatch(rb.getEdinetCode()::equals))
                .forEach(rb -> companyDao.insert(CsvMapper.map(industryList, rb)))
        );

        log.info("CSVファイルから会社情報の登録が完了しました。");
    }

    public void edinetList(final String startDate, final String endDate) {
        final var dateList = LocalDate.parse(startDate)
                .datesUntil(LocalDate.parse(endDate).plusDays(1))
                .collect(Collectors.toList());

        // 書類リストをデータベースに登録する
        dateList.forEach(this::insertDocumentList);
    }

    public void document(final String date, final String documentTypeCode) {
        // 書類リストをデータベースに登録する
        insertDocumentList(LocalDate.parse(date));

        // 対象ファイルリスト取得（CompanyCodeがnullではないドキュメントを対象とする）
        final var documentIdList = documentDao.selectByTypeAndSubmitDate(documentTypeCode, LocalDate.parse(date))
                .stream()
                .filter(document -> companyDao.selectByEdinetCode(document.getEdinetCode()).getCode().isPresent())
                .filter(Document::getNotRemoved)
                .map(Document::getDocumentId)
                .collect(Collectors.toList());

        if (documentIdList.isEmpty()) {
            log.warn("{}付の処理対象ドキュメントは存在しませんでした。\t書類種別コード:{}", date, documentTypeCode);
        } else {
            documentIdList.forEach(documentId -> {
                System.out.println("-------------" + documentId + "-------------");
                final var document = documentDao.selectByDocumentId(documentId);

                // 書類取得
                if (DocumentStatus.NOT_YET.toValue().equals(document.getDownloaded())) {
                    store(LocalDate.parse(date), documentId);
                }

                // スクレイピング
                // 貸借対照表
                if (DocumentStatus.NOT_YET.toValue().equals(document.getScrapedBs())) {
                    scrapeBs(documentId, LocalDate.parse(date));
                }
                // 損益計算書
                if (DocumentStatus.NOT_YET.toValue().equals(document.getScrapedPl())) {
                    scrapePl(documentId, LocalDate.parse(date));
                }
                // 株式総数
                if (DocumentStatus.NOT_YET.toValue().equals(document.getScrapedNumberOfShares())) {
                    scrapeNs(documentId, LocalDate.parse(date));
                }

                // 除外フラグON
                if (List.of(
                        document.getScrapedBs(),
                        document.getScrapedPl(),
                        document.getScrapedNumberOfShares()
                ).stream().allMatch(status -> DocumentStatus.ERROR.toValue().equals(status))) {
                    documentDao.update(Document.builder()
                            .documentId(documentId)
                            .removed(Flag.ON.toValue())
                            .updatedAt(LocalDateTime.now())
                            .build()
                    );
                    log.info("処理ステータスがすべて \"9（ERROR）\" となったため、除外フラグをONにしました。\t書類ID:{}", documentId);
                }
            });

            log.info("{}付のドキュメントに対してすべての処理が完了しました。\t書類種別コード:{}", date, documentTypeCode);
        }
    }

    @Transactional
    public void insertDocumentList(final LocalDate date) {
        final var docIdList = edinetDocumentDao.selectAll().stream()
                .map(EdinetDocument::getDocId)
                .collect(Collectors.toList());

        Stream.of(date.toString())
                .filter(dateString -> Stream.of(dateString)
                        .peek(d -> log.debug("書類一覧（メタデータ）取得処理を実行します。\t取得対象日:{}", d))
                        // EDINETに提出書類の問い合わせ
                        .map(d -> proxy.list(new ListRequestParameter(d, ListType.DEFAULT)))
                        .map(EdinetResponse::getMetadata)
                        .map(Metadata::getResultset)
                        .map(ResultSet::getCount)
                        .peek(c -> log.info("書類一覧（メタデータ）を正常に取得しました。\t取得対象日:{}\t対象ファイル件数:{}", dateString, c))
                        .anyMatch(c -> !"0".equals(c))
                )
                // 書類が0件ではないときは書類リストを取得する
                .peek(dateString -> log.debug("書類一覧（提出書類一覧及びメタデータ）取得処理を実行します。\t取得対象日:{}", dateString))
                .map(dateString -> proxy.list(new ListRequestParameter(dateString, ListType.GET_LIST)))
                .peek(er -> log.debug("書類一覧（提出書類一覧及びメタデータ）を正常に取得しました。データベースへの登録作業を開始します。"))
                .map(EdinetResponse::getResults)
                .forEach(resultsList -> resultsList.forEach(results -> {
                    Stream.of(results)
                            .filter(r -> docIdList.stream().noneMatch(docId -> r.getDocId().equals(docId)))
                            .forEach(r -> {
                                try {
                                    edinetDocumentDao.insert(EdinetDocument.of(r));
                                } catch (NestedRuntimeException e) {
                                    if (e.contains(UniqueConstraintException.class)) {
                                        log.info("一意制約違反のため、データベースへの登録をスキップします。" +
                                                        "\tテーブル名:{}\t書類ID:{}\tEDINETコード:{}\t提出者名:{}\t書類種別コード:{}",
                                                "edinet_document",
                                                r.getDocId(),
                                                r.getEdinetCode(),
                                                r.getFilerName(),
                                                r.getDocTypeCode()
                                        );
                                    }
                                }
                                try {
                                    documentDao.insert(Document.builder()
                                            .documentId(r.getDocId())
                                            .documentTypeCode(r.getDocTypeCode())
                                            .edinetCode(r.getEdinetCode())
                                            .period(r.getPeriodEnd() != null ? LocalDate.of(Integer.parseInt(r.getPeriodEnd().substring(0, 4)), 1, 1) : null)
                                            .submitDate(date)
                                            .createdAt(LocalDateTime.now())
                                            .updatedAt(LocalDateTime.now())
                                            .build()
                                    );
                                } catch (NestedRuntimeException e) {
                                    if (e.contains(UniqueConstraintException.class)) {
                                        log.info("一意制約違反のため、データベースへの登録をスキップします。" +
                                                        "\tテーブル名:{}\t書類ID:{}\tEDINETコード:{}\t提出者名:{}\t書類種別コード:{}",
                                                "document",
                                                r.getDocId(),
                                                r.getEdinetCode(),
                                                r.getFilerName(),
                                                r.getDocTypeCode()
                                        );
                                    } else if (e.contains(SQLIntegrityConstraintViolationException.class)) {
                                        log.error("参照整合性制約違反が発生しました。スタックトレースを参考に原因を確認してください。", e.getRootCause());
                                        throw new FundanalyzerSqlForeignKeyException(e);
                                    } else {
                                        log.error("想定外のエラーが発生しました。", e);
                                        throw new RuntimeException(e);
                                    }
                                }
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
                    .documentId(docId)
                    .downloaded(DocumentStatus.DONE.toValue())
                    .decoded(DocumentStatus.DONE.toValue())
                    .updatedAt(LocalDateTime.now())
                    .build()
            );
        } else {
            // ファイル取得
            download(targetDate, docId);
        }
    }

    public void scrape(final LocalDate submitDate) {
        log.info("次のドキュメントに対してスクレイピング処理を実行します。\t対象日:{}", submitDate);
        final var documentList = documentDao.selectByTypeAndSubmitDate("120", submitDate).stream()
                .filter(document -> companyDao.selectByEdinetCode(document.getEdinetCode()).getCode().isPresent())
                .filter(Document::getNotRemoved)
                .collect(Collectors.toList());

        documentList.forEach(d -> {
            if (DocumentStatus.NOT_YET.toValue().equals(d.getScrapedBs())) scrapeBs(d.getDocumentId());
            if (DocumentStatus.NOT_YET.toValue().equals(d.getScrapedPl())) scrapePl(d.getDocumentId());
            if (DocumentStatus.NOT_YET.toValue().equals(d.getScrapedNumberOfShares())) scrapeNs(d.getDocumentId());
        });
    }

    public void scrape(final String documentId) {
        log.info("次のドキュメントに対してスクレイピング処理を実行します。\t書類ID:{}", documentId);
        scrapeBs(documentId);
        scrapePl(documentId);
        scrapeNs(documentId);
    }

    public void resetForRetry() {
        final var documentList = documentDao.selectByDocumentTypeCode("120");
        // 貸借対照表
        documentList.stream()
                .filter(document -> !DocumentStatus.DONE.toValue().equals(document.getScrapedBs()))
                .filter(document -> !DocumentStatus.NOT_YET.toValue().equals(document.getScrapedBs()))
                .filter(Document::getNotRemoved)
                .map(Document::getDocumentId)
                .forEach(documentId -> {
                    documentDao.update(Document.builder()
                            .documentId(documentId)
                            .scrapedBs(DocumentStatus.NOT_YET.toValue())
                            .updatedAt(LocalDateTime.now())
                            .build()
                    );
                    log.info("次のドキュメントステータスを初期化しました。\t書類ID:{}\t財務諸表名:{}", documentId, "貸借対照表");
                });

        // 損益計算書
        documentList.stream()
                .filter(document -> !DocumentStatus.DONE.toValue().equals(document.getScrapedPl()))
                .filter(document -> !DocumentStatus.NOT_YET.toValue().equals(document.getScrapedPl()))
                .filter(Document::getNotRemoved)
                .map(Document::getDocumentId)
                .forEach(documentId -> {
                    documentDao.update(Document.builder()
                            .documentId(documentId)
                            .scrapedPl(DocumentStatus.NOT_YET.toValue())
                            .updatedAt(LocalDateTime.now())
                            .build()
                    );
                    log.info("次のドキュメントステータスを初期化しました。\t書類ID:{}\t財務諸表名:{}", documentId, "損益計算書");
                });
    }

    void download(final LocalDate targetDate, final String docId) {
        try {
            log.info("書類のダウンロードおよびzipファイルの解凍処理を実行します。\t書類管理番号:{}", docId);

            proxy.acquisition(
                    new File(pathEdinet.getPath() + "/" + targetDate),
                    new AcquisitionRequestParameter(docId, AcquisitionType.DEFAULT)
            );

            documentDao.update(Document.builder()
                    .documentId(docId)
                    .downloaded(DocumentStatus.DONE.toValue())
                    .updatedAt(LocalDateTime.now())
                    .build()
            );

            fileOperator.decodeZipFile(
                    new File(pathEdinet + "/" + targetDate.toString() + "/" + docId),
                    new File(pathDecode + "/" + targetDate.toString() + "/" + docId)
            );

            log.info("書類のダウンロードおよびzipファイルの解凍処理が正常に実行されました。");

            documentDao.update(Document.builder()
                    .documentId(docId)
                    .decoded(DocumentStatus.DONE.toValue())
                    .updatedAt(LocalDateTime.now())
                    .build());

        } catch (FundanalyzerRestClientException e) {
            log.error("書類のダウンロード処理に失敗しました。スタックトレースから原因を確認してください。" +
                            "\t処理対象日:{}\t書類管理番号:{}",
                    targetDate, docId
            );
            documentDao.update(Document.builder()
                    .documentId(docId)
                    .downloaded(DocumentStatus.ERROR.toValue())
                    .updatedAt(LocalDateTime.now())
                    .build()
            );
        } catch (IOException e) {
            log.error("zipファイルの解凍処理に失敗しました。スタックトレースから原因を確認してください。" +
                            "\t処理対象日:{}\t書類管理番号:{}",
                    targetDate, docId
            );
            documentDao.update(Document.builder()
                    .documentId(docId)
                    .decoded(DocumentStatus.ERROR.toValue())
                    .updatedAt(LocalDateTime.now())
                    .build()
            );
        }
    }

    void scrapeBs(final String documentId) {
        scrapeBs(documentId, documentDao.selectByDocumentId(documentId).getSubmitDate());
    }

    void scrapeBs(final String documentId, final LocalDate date) {
        final var edinetDocument = edinetDocumentDao.selectByDocId(documentId);
        final var company = companyDao.selectByEdinetCode(edinetDocument.getEdinetCode().orElse(null));
        final var targetDirectory = new File(pathDecode + "/" + date + "/" + documentId + "/XBRL/PublicDoc");

        try {
            final var targetFile = findTargetFile(targetDirectory, FinancialStatementEnum.BALANCE_SHEET);
            insertFinancialStatement(
                    targetFile.getFirst(),
                    targetFile.getSecond(),
                    FinancialStatementEnum.BALANCE_SHEET,
                    company,
                    bsSubjectDao.selectAll(),
                    edinetDocument);
            checkBs(company, edinetDocument);

            documentDao.update(Document.builder()
                    .documentId(documentId)
                    .scrapedBs(DocumentStatus.DONE.toValue())
                    .bsDocumentPath(targetFile.getFirst().getPath())
                    .updatedAt(LocalDateTime.now())
                    .build()
            );

        } catch (FundanalyzerFileException e) {
            documentDao.update(Document.builder()
                    .documentId(documentId)
                    .scrapedBs(DocumentStatus.ERROR.toValue())
                    .updatedAt(LocalDateTime.now())
                    .build()
            );
            log.error("スクレイピング処理の過程でエラー発生しました。スタックトレースを参考に原因を確認してください。" +
                            "\n企業コード:{}\tEDINETコード:{}\t財務諸表名:{}\tファイルパス:{}",
                    company.getCode().orElseThrow(),
                    company.getEdinetCode(),
                    "貸借対照表",
                    targetDirectory.getPath()
            );
        }
    }

    void scrapePl(final String documentId) {
        scrapePl(documentId, documentDao.selectByDocumentId(documentId).getSubmitDate());
    }

    void scrapePl(final String documentId, final LocalDate date) {
        final var edinetDocument = edinetDocumentDao.selectByDocId(documentId);
        final var company = companyDao.selectByEdinetCode(edinetDocument.getEdinetCode().orElse(null));
        final var targetDirectory = new File(pathDecode + "/" + date + "/" + documentId + "/XBRL/PublicDoc");

        try {
            final var targetFile = findTargetFile(targetDirectory, FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT);
            insertFinancialStatement(
                    targetFile.getFirst(),
                    targetFile.getSecond(),
                    FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT,
                    company,
                    plSubjectDao.selectAll(),
                    edinetDocument);

            documentDao.update(Document.builder()
                    .documentId(documentId)
                    .scrapedPl(DocumentStatus.DONE.toValue())
                    .plDocumentPath(targetFile.getFirst().getPath())
                    .updatedAt(LocalDateTime.now())
                    .build()
            );

        } catch (FundanalyzerFileException e) {
            documentDao.update(Document.builder()
                    .documentId(documentId)
                    .scrapedPl(DocumentStatus.ERROR.toValue())
                    .updatedAt(LocalDateTime.now())
                    .build()
            );
            log.error("スクレイピング処理の過程でエラー発生しました。スタックトレースを参考に原因を確認してください。" +
                            "\n企業コード:{}\tEDINETコード:{}\t財務諸表名:{}\tファイルパス:{}",
                    company.getCode().orElseThrow(),
                    company.getEdinetCode(),
                    "損益計算書",
                    targetDirectory.getPath()
            );
        }
    }

    void scrapeNs(final String documentId) {
        scrapeNs(documentId, documentDao.selectByDocumentId(documentId).getSubmitDate());
    }

    void scrapeNs(final String documentId, final LocalDate date) {
        final var edinetDocument = edinetDocumentDao.selectByDocId(documentId);
        final var company = companyDao.selectByEdinetCode(edinetDocument.getEdinetCode().orElse(null));
        final var targetDirectory = new File(pathDecode + "/" + date + "/" + documentId + "/XBRL/PublicDoc");

        try {
            final var targetFile = findTargetFile(targetDirectory, FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES);
            insertOfFinancialStatement(
                    company,
                    FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES,
                    "0",
                    edinetDocument,
                    parseValue(htmlScraping.findNumberOfShares(targetFile.getFirst(), targetFile.getSecond().getKeyword())).orElse(null)
            );

            log.info("次のスクレイピング情報を正常に登録しました。\n企業コード:{}\tEDINETコード:{}\t財務諸表名:{}\tファイル名:{}",
                    company.getCode().orElseThrow(),
                    company.getEdinetCode(),
                    "株式総数",
                    targetFile.getFirst().getPath()
            );

            documentDao.update(Document.builder()
                    .documentId(documentId)
                    .scrapedNumberOfShares(DocumentStatus.DONE.toValue())
                    .numberOfSharesDocumentPath(targetFile.getFirst().getPath())
                    .updatedAt(LocalDateTime.now())
                    .build()
            );

        } catch (FundanalyzerFileException e) {
            documentDao.update(Document.builder()
                    .documentId(documentId)
                    .scrapedNumberOfShares(DocumentStatus.ERROR.toValue())
                    .updatedAt(LocalDateTime.now())
                    .build()
            );
            log.error("スクレイピング処理の過程でエラー発生しました。スタックトレースを参考に原因を確認してください。" +
                            "\n企業コード:{}\tEDINETコード:{}\t財務諸表名:{}\tファイルパス:{}",
                    company.getCode().orElseThrow(),
                    company.getEdinetCode(),
                    "株式総数",
                    targetDirectory.getPath()
            );
        }
    }

    Pair<File, ScrapingKeyword> findTargetFile(
            final File targetFile,
            final FinancialStatementEnum financialStatement) throws FundanalyzerFileException {
        final var scrapingKeywordList = scrapingKeywordDao.selectByFinancialStatementId(
                financialStatement.toValue());

        System.out.println("↓ ↓ ↓ ↓ ↓ " + financialStatement.getName() + " ↓ ↓ ↓ ↓ ↓");
        log.info("\"{}\" のスクレイピング処理を開始します。", financialStatement.getName());

        for (ScrapingKeyword scrapingKeyword : scrapingKeywordList) {
            try {
                final var file = htmlScraping.findFile(targetFile, scrapingKeyword.getKeyword()).orElseThrow();

                log.info("対象ファイルの存在を正常に確認できました。\t財務諸表名:{}\tキーワード:{}",
                        scrapingKeyword.getRemarks(), scrapingKeyword.getKeyword()
                );

                return Pair.of(file, scrapingKeyword);

            } catch (NoSuchElementException ignored) {
                log.info("次のキーワードに合致するファイルは存在しませんでした。\t財務諸表名:{}\tキーワード:{}",
                        scrapingKeyword.getRemarks(), scrapingKeyword.getKeyword()
                );
            }
        }
        throw new FundanalyzerFileException();
    }

    <T extends Detail> void insertFinancialStatement(
            final File targetFile,
            final ScrapingKeyword scrapingKeyword,
            final FinancialStatementEnum financialStatement,
            final Company company,
            final List<T> detailList,
            final EdinetDocument edinetDocument) throws FundanalyzerFileException {
        final var resultBeans = htmlScraping.scrapeFinancialStatement(targetFile, scrapingKeyword.getKeyword());

        resultBeans.forEach(resultBean -> detailList.stream()
                // スクレイピング結果とマスタから一致するものをフィルターにかける
                .filter(detail -> Objects.equals(resultBean.getSubject().orElse(null), detail.getName()))
                .findAny()
                // 一致するものが存在したら下記
                .ifPresent(detail -> insertOfFinancialStatement(
                        company,
                        financialStatement,
                        detail.getId(),
                        edinetDocument,
                        parseValue(resultBean.getCurrentValue(), resultBean.getUnit()).orElse(null)
                )));

        log.info("次のスクレイピング情報を正常に登録しました。\n企業コード:{}\tEDINETコード:{}\t財務諸表名:{}\tファイルパス:{}",
                company.getCode().orElseThrow(),
                company.getEdinetCode(),
                financialStatement.getName(),
                targetFile.getPath()
        );
    }

    @Transactional
    private void insertOfFinancialStatement(
            final Company company,
            final FinancialStatementEnum financialStatement,
            final String dId,
            final EdinetDocument edinetDocument,
            final Long value) {
        try {
            financialStatementDao.insert(new FinancialStatement(
                    null,
                    company.getCode().orElse(null),
                    company.getEdinetCode(),
                    financialStatement.toValue(),
                    dId,
                    LocalDate.parse(edinetDocument.getPeriodStart().orElseThrow()),
                    LocalDate.parse(edinetDocument.getPeriodEnd().orElseThrow()),
                    value,
                    LocalDateTime.now()
            ));
        } catch (NestedRuntimeException e) {
            if (e.contains(UniqueConstraintException.class)) {
                log.info("一意制約違反のため、データベースへの登録をスキップします。" +
                                "\t企業コード:{}\t財務諸表名:{}\t科目ID:{}\t対象年:{}",
                        company.getCode().orElse(null),
                        financialStatement.getName(),
                        dId,
                        edinetDocument.getPeriodEnd().orElseThrow().substring(0, 4)
                );
            } else {
                throw e;
            }
        }
    }

    private void checkBs(final Company company, final EdinetDocument edinetDocument) {
        final var totalCurrentLiabilities = bsSubjectDao.selectByOutlineSubjectId(
                BsEnum.TOTAL_CURRENT_LIABILITIES.getOutlineSubjectId()).stream()
                .map(bsSubject -> financialStatementDao.selectByUniqueKey(
                        edinetDocument.getEdinetCode().orElse(null),
                        FinancialStatementEnum.BALANCE_SHEET.toValue(),
                        bsSubject.getId(),
                        edinetDocument.getPeriodEnd().map(d -> d.substring(0, 4)).orElse(null)
                        ).flatMap(FinancialStatement::getValue)
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findAny();

        final var totalLiabilities = bsSubjectDao.selectByOutlineSubjectId(
                BsEnum.TOTAL_LIABILITIES.getOutlineSubjectId()).stream()
                .map(bsSubject -> financialStatementDao.selectByUniqueKey(
                        edinetDocument.getEdinetCode().orElse(null),
                        FinancialStatementEnum.BALANCE_SHEET.toValue(),
                        bsSubject.getId(),
                        edinetDocument.getPeriodEnd().map(d -> d.substring(0, 4)).orElse(null)
                        ).flatMap(FinancialStatement::getValue)
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findAny();

        if (totalCurrentLiabilities.isPresent() && totalLiabilities.isPresent()) {
            if (totalCurrentLiabilities.get().equals(totalLiabilities.get())) {
                insertOfFinancialStatement(
                        company,
                        FinancialStatementEnum.BALANCE_SHEET,
                        bsSubjectDao.selectByUniqueKey(
                                BsEnum.TOTAL_FIXED_LIABILITIES.getOutlineSubjectId(),
                                BsEnum.TOTAL_FIXED_LIABILITIES.getDetailSubjectId()
                        ).getId(),
                        edinetDocument,
                        0L
                );

                log.info("\"貸借対照表\" の \"固定負債合計\" が存在しなかったため、次の通りとして\"0\" にてデータベースに登録しました。" +
                                "\t企業コード:{}\t書類ID:{}\t流動負債合計:{}\t負債合計:{}",
                        company.getCode().orElseThrow(),
                        edinetDocument.getDocId(),
                        totalCurrentLiabilities.get(),
                        totalLiabilities.get()
                );
            }
        }
    }

    private Optional<Long> parseValue(final String value) {
        try {
            return Optional.of(value)
                    .filter(v -> !v.isBlank())
                    .filter(v -> !" ".equals(v))
                    .map(s -> Long.parseLong(s
                            .replace("※ ", "")
                            .replace("※1", "").replace("※１", "")
                            .replace("※2", "").replace("※２", "")
                            .replace("※3", "").replace("※３", "")
                            .replace("※4", "").replace("※４", "")
                            .replace("※5", "").replace("※５", "")
                            .replace("※6", "").replace("※６", "")
                            .replace("※7", "").replace("※７", "")
                            .replace("※8", "").replace("※８", "")
                            .replace("※9", "").replace("※９", "")
                            .replace("※10", "").replace("※11", "")
                            .replace("※12", "").replace("※13", "")
                            .replace("※14", "").replace("※15", "")
                            .replace("※16", "").replace("※17", "")
                            .replace("*1", "").replace("*2", "")
                            .replace("株", "")
                            .replace("－", "0").replace("―", "0")
                            .replace(" ", "").replace(" ", "")
                            .replace(",", "")
                            .replace("△", "-")
                    ));
        } catch (NumberFormatException e) {
            log.error("数値を正常に認識できなかったため、NULLで登録します。\tvalue:{}", value);
            return Optional.empty();
        }
    }

    private Optional<Long> parseValue(final String value, final Unit unit) {
        return parseValue(value).map(l -> l * unit.getValue());
    }
}
