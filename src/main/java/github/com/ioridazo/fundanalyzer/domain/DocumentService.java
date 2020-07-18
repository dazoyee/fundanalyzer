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
import github.com.ioridazo.fundanalyzer.domain.entity.DocumentStatus;
import github.com.ioridazo.fundanalyzer.domain.entity.FinancialStatementEnum;
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
import github.com.ioridazo.fundanalyzer.mapper.EdinetMapper;
import lombok.extern.slf4j.Slf4j;
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

    public void company() {
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

        log.info("会社情報をデータベースに正常に登録しました。");
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
        final var docIdList = documentDao.selectByDateAndDocumentTypeCode(LocalDate.parse(date), documentTypeCode)
                .stream()
                .filter(document -> companyDao.selectByEdinetCode(document.getEdinetCode()).getCode().isPresent())
                .map(Document::getDocumentId)
                .collect(Collectors.toList());

        if (docIdList.isEmpty()) {
            log.warn("{}付の処理対象ドキュメントは存在しませんでした。\t書類種別コード:{}", date, documentTypeCode);
        } else {
            docIdList.forEach(docId -> {
                System.out.println("--------------------------------------------------");

                // 書類取得
                if (DocumentStatus.NOT_YET.toValue().equals(documentDao.selectByDocumentId(docId).getDownloaded())) {
                    store(LocalDate.parse(date), docId);
                }

                // スクレイピング
                if (DocumentStatus.NOT_YET.toValue().equals(documentDao.selectByDocumentId(docId).getScrapedBs())) {
                    scrape(LocalDate.parse(date), docId);
                }
            });

            log.info("{}付のドキュメントに対して処理が完了しました。\t書類種別コード:{}", date, documentTypeCode);
        }
    }

    @Transactional
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
                                try {
                                    documentDao.insert(Document.builder()
                                            .documentId(r.getDocId())
                                            .documentTypeCode(r.getDocTypeCode())
                                            .edinetCode(r.getEdinetCode())
                                            .submitDate(date)
                                            .createdAt(LocalDateTime.now())
                                            .updatedAt(LocalDateTime.now())
                                            .build()
                                    );
                                } catch (NestedRuntimeException e) {
                                    if (e.contains(SQLIntegrityConstraintViolationException.class)) {
                                        log.error("参照整合性制約違反が発生しました。スタックトレースを参考に原因を確認してください。", e.getRootCause());
                                        throw new FundanalyzerSqlForeignKeyException(e);
                                    }
                                    throw new RuntimeException(e);
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

            documentDao.update(Document.builder().documentId(docId).downloaded(DocumentStatus.DONE.toValue()).build());

            fileOperator.decodeZipFile(
                    new File(pathEdinet + "/" + targetDate.toString() + "/" + docId),
                    new File(pathDecode + "/" + targetDate.toString() + "/" + docId)
            );

            log.info("zipファイルの解凍処理が正常に実行されました。");

            documentDao.update(Document.builder().documentId(docId).decoded(DocumentStatus.DONE.toValue()).build());

        } catch (FundanalyzerRestClientException e) {
            log.error("書類のダウンロード処理に失敗しました。スタックトレースから原因を確認してください。" +
                            "\t処理対象日:{}" +
                            "\t書類管理番号:{}",
                    targetDate,
                    docId);
            documentDao.update(Document.builder().documentId(docId).downloaded(DocumentStatus.ERROR.toValue()).build());
        } catch (IOException e) {
            log.error("zipファイルの解凍処理に失敗しました。スタックトレースから原因を確認してください。" +
                            "\t処理対象日:{}" +
                            "\t書類管理番号:{}",
                    targetDate,
                    docId);
            documentDao.update(Document.builder().documentId(docId).decoded(DocumentStatus.ERROR.toValue()).build());
        }
    }

    public void scrape(final LocalDate targetDate, final String docId) {
        final var edinetDocument = edinetDocumentDao.selectByDocId(docId);
        final var company = companyDao.selectByEdinetCode(edinetDocument.getEdinetCode().orElse(null));
        final var targetDirectory = new File(pathDecode + "/" + targetDate + "/" + docId + "/XBRL/PublicDoc");

        // 貸借対照表
        try {
            final var targetFile = findTargetFile(targetDirectory, FinancialStatementEnum.BALANCE_SHEET);
            insertFinancialStatement(
                    targetFile.getFirst(),
                    targetFile.getSecond(),
                    FinancialStatementEnum.BALANCE_SHEET,
                    company,
                    bsSubjectDao.selectAll(),
                    financialStatementDao::insert,
                    edinetDocument);

            documentDao.update(Document.builder()
                    .documentId(docId)
                    .scrapedBs(DocumentStatus.DONE.toValue())
                    .bsDocumentPath(targetFile.getFirst().getPath())
                    .build()
            );

        } catch (FundanalyzerFileException e) {
            documentDao.update(Document.builder().documentId(docId).scrapedBs(DocumentStatus.ERROR.toValue()).build());
            log.error("スクレイピング処理の過程でエラー発生しました。スタックトレースを参考に原因を確認してください。" +
                            "\t対象:{}" +
                            "\t書類管理番号:{}",
                    "貸借対照表", docId);
        }

        // 損益計算書
        try {
            final var targetFile = findTargetFile(targetDirectory, FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT);
            insertFinancialStatement(
                    targetFile.getFirst(),
                    targetFile.getSecond(),
                    FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT,
                    company,
                    plSubjectDao.selectAll(),
                    financialStatementDao::insert,
                    edinetDocument);

            documentDao.update(Document.builder()
                    .documentId(docId)
                    .scrapedPl(DocumentStatus.DONE.toValue())
                    .plDocumentPath(targetFile.getFirst().getPath())
                    .build()
            );

        } catch (FundanalyzerFileException e) {
            documentDao.update(Document.builder().documentId(docId).scrapedPl(DocumentStatus.ERROR.toValue()).build());
            log.error("スクレイピング処理の過程でエラー発生しました。スタックトレースを参考に原因を確認してください。" +
                            "\t対象:{}" +
                            "\t書類管理番号:{}",
                    "損益計算書", docId);
        }

        // 株式総数
        try {
            final var targetFile = findTargetFile(targetDirectory, FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES);
            financialStatementDao.insert(new FinancialStatement(
                    null,
                    company.getCode().orElse(null),
                    company.getEdinetCode(),
                    FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES.toValue(),
                    "0",
                    LocalDate.parse(edinetDocument.getPeriodStart().orElseThrow()),
                    LocalDate.parse(edinetDocument.getPeriodEnd().orElseThrow()),
                    replaceInteger(htmlScraping.findNumberOfShares(targetFile.getFirst(), targetFile.getSecond().getKeyword())).orElse(null),
                    LocalDateTime.now()
            ));

            log.info("データベースに正常に登録されました。\t対象:{}\tファイル名:{}",
                    "株式総数", targetFile.getFirst().getName());

            documentDao.update(Document.builder()
                    .documentId(docId)
                    .scrapedNumberOfShares(DocumentStatus.DONE.toValue())
                    .numberOfSharesDocumentPath(targetFile.getFirst().getPath())
                    .build()
            );

        } catch (FundanalyzerFileException e) {
            documentDao.update(Document.builder().documentId(docId).scrapedNumberOfShares(DocumentStatus.ERROR.toValue()).build());
            log.error("スクレイピング処理の過程でエラー発生しました。スタックトレースを参考に原因を確認してください。" +
                            "\t対象:{}" +
                            "\t書類管理番号:{}",
                    "株式総数", docId);
        }
    }

    Pair<File, ScrapingKeyword> findTargetFile(
            final File targetFile,
            final FinancialStatementEnum financialStatement) throws FundanalyzerFileException {
        final var scrapingKeywordList = scrapingKeywordDao.selectByFinancialStatementId(
                financialStatement.toValue());

        for (ScrapingKeyword scrapingKeyword : scrapingKeywordList) {
            log.info("\"{}\"に合致するファイルの探索を開始します。", scrapingKeyword.getKeyword());

            try {
                final var file = htmlScraping.findFile(targetFile, scrapingKeyword.getKeyword()).orElseThrow();

                log.info("\"{}（{}）\"に合致するファイルが１つ存在しています。スクレイピング処理を開始します。" +
                                "\tファイル名:{}",
                        scrapingKeyword.getKeyword(),
                        scrapingKeyword.getRemarks(),
                        file.getPath());

                return Pair.of(file, scrapingKeyword);

            } catch (NoSuchElementException ignored) {
                log.info("\"{}\"に合致するファイルは存在しませんでした。", scrapingKeyword.getKeyword());
            }
        }
        throw new FundanalyzerFileException();
    }

    @Transactional
    <T extends Detail> void insertFinancialStatement(
            final File targetFile,
            final ScrapingKeyword scrapingKeyword,
            final FinancialStatementEnum financialStatement,
            final Company company,
            final List<T> detailList,
            final Consumer<FinancialStatement> insert,
            final EdinetDocument edinetDocument) throws FundanalyzerFileException {

        final var resultBeans = htmlScraping.scrapeFinancialStatement(targetFile, scrapingKeyword.getKeyword());

        log.info("スクレイピングの情報をデータベースに登録します。" +
                        "\t対象:{}" +
                        "\t会社コード:{}" +
                        "\tEDINETコード:{}",
                financialStatement.getName(),
                company.getCode(),
                company.getEdinetCode());

        resultBeans.forEach(resultBean -> detailList.stream()
                // スクレイピング結果とマスタから一致するものをフィルターにかける
                .filter(detail -> Objects.equals(resultBean.getSubject().orElse(null), detail.getName()))
                .findAny()
                // 一致するものが存在したら下記
                .ifPresent(detail -> insert.accept(new FinancialStatement(
                        null,
                        company.getCode().orElse(null),
                        company.getEdinetCode(),
                        financialStatement.toValue(),
                        detail.getId(),
                        LocalDate.parse(edinetDocument.getPeriodStart().orElseThrow()),
                        LocalDate.parse(edinetDocument.getPeriodEnd().orElseThrow()),
                        replaceInteger(resultBean.getCurrentValue(), resultBean.getUnit()).orElse(null),
                        LocalDateTime.now()
                )))
        );

        log.info("データベースに正常に登録されました。\t対象:{}\tファイル名:{}",
                financialStatement.getName(), targetFile.getName());
    }

    private Optional<Long> replaceInteger(final String value) {
        try {
            return Optional.of(value)
                    .filter(v -> !v.isBlank())
                    .filter(v -> !" ".equals(v))
                    .map(s -> Long.parseLong(s
                            .replace(",", "")
                            .replace("△", "-")
                            .replace("※２ ", "")
                            .replace("*2 ", "")
                            .replace("※ ", "")
                            .replace("※１ ", "")
                            .replace("※１,※２ ", "")
                    ));
        } catch (NumberFormatException e) {
            log.error("数値を正常に認識できなかったため、NULLで登録します。\tvalue:{}", value);
            return Optional.empty();
        }
    }

    private Optional<Long> replaceInteger(final String value, final Unit unit) {
        return replaceInteger(value).map(l -> l * unit.getValue());
    }
}
