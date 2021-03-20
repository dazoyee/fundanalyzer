package github.com.ioridazo.fundanalyzer.domain.service;

import github.com.ioridazo.fundanalyzer.domain.dao.master.BsSubjectDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.PlSubjectDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.EdinetDocumentDao;
import github.com.ioridazo.fundanalyzer.domain.entity.DocumentStatus;
import github.com.ioridazo.fundanalyzer.domain.entity.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.Flag;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Company;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Document;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.EdinetDocument;
import github.com.ioridazo.fundanalyzer.domain.log.Category;
import github.com.ioridazo.fundanalyzer.domain.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.domain.log.Process;
import github.com.ioridazo.fundanalyzer.domain.logic.company.CompanyLogic;
import github.com.ioridazo.fundanalyzer.domain.logic.company.bean.EdinetCsvResultBean;
import github.com.ioridazo.fundanalyzer.domain.logic.scraping.ScrapingLogic;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerSqlForeignKeyException;
import github.com.ioridazo.fundanalyzer.proxy.edinet.EdinetProxy;
import github.com.ioridazo.fundanalyzer.proxy.edinet.entity.request.ListRequestParameter;
import github.com.ioridazo.fundanalyzer.proxy.edinet.entity.request.ListType;
import github.com.ioridazo.fundanalyzer.proxy.edinet.entity.response.EdinetResponse;
import github.com.ioridazo.fundanalyzer.proxy.edinet.entity.response.Results;
import github.com.ioridazo.fundanalyzer.proxy.selenium.SeleniumProxy;
import lombok.extern.log4j.Log4j2;
import org.seasar.doma.jdbc.UniqueConstraintException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.core.NestedRuntimeException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.sql.SQLIntegrityConstraintViolationException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
@Service
public class DocumentService {

    private final String pathCompany;
    private final String pathCompanyZip;
    private final String pathDecode;
    private final EdinetProxy edinetProxy;
    private final SeleniumProxy seleniumProxy;
    private final CompanyLogic companyLogic;
    private final ScrapingLogic scrapingLogic;
    private final CompanyDao companyDao;
    private final BsSubjectDao bsSubjectDao;
    private final PlSubjectDao plSubjectDao;
    private final EdinetDocumentDao edinetDocumentDao;
    private final DocumentDao documentDao;

    public DocumentService(
            @Value("${app.settings.file.path.company.company}") final String pathCompany,
            @Value("${app.settings.file.path.company.zip}") final String pathCompanyZip,
            @Value("${app.settings.file.path.decode}") final String pathDecode,
            final EdinetProxy edinetProxy,
            final SeleniumProxy seleniumProxy,
            final CompanyLogic companyLogic,
            final ScrapingLogic scrapingLogic,
            final CompanyDao companyDao,
            final BsSubjectDao bsSubjectDao,
            final PlSubjectDao plSubjectDao,
            final EdinetDocumentDao edinetDocumentDao,
            final DocumentDao documentDao
    ) {
        this.pathCompany = pathCompany;
        this.pathCompanyZip = pathCompanyZip;
        this.pathDecode = pathDecode;
        this.edinetProxy = edinetProxy;
        this.seleniumProxy = seleniumProxy;
        this.companyLogic = companyLogic;
        this.scrapingLogic = scrapingLogic;
        this.companyDao = companyDao;
        this.bsSubjectDao = bsSubjectDao;
        this.plSubjectDao = plSubjectDao;
        this.edinetDocumentDao = edinetDocumentDao;
        this.documentDao = documentDao;
    }

    LocalDateTime nowLocalDateTime() {
        return LocalDateTime.now();
    }

    /**
     * Seleniumで会社情報一覧を取得し、業種と会社情報をDBに登録する
     */
    @NewSpan("DocumentService.downloadCompanyInfo")
    public void downloadCompanyInfo() {
        final String inputFilePath = makeTargetPath(pathCompanyZip, LocalDate.now()).getPath();
        try {
            // ファイルダウンロード
            final String fileName = seleniumProxy.edinetCodeList(inputFilePath);

            // ファイル読み取り
            final List<EdinetCsvResultBean> resultBeanList = companyLogic.readFile(fileName, inputFilePath, pathCompany);

            // Industryの登録
            companyLogic.insertIndustry(resultBeanList);

            // Companyの登録
            companyLogic.upsertCompany(resultBeanList);

            FundanalyzerLogClient.logService("CSVファイルから会社情報の登録が完了しました。", Category.DOCUMENT, Process.COMPANY);
        } catch (Throwable t) {
            log.error("Selenium経由での会社情報更新処理が異常終了しました。内容を確認してください。", t);
            throw new FundanalyzerRuntimeException(t);
        }
    }

    /**
     * 格納されている会社情報一覧を取得し、業種と会社情報をDBに登録する
     */
    @NewSpan("DocumentService.readCompanyInfo")
    public void readCompanyInfo() {
        // ファイル読み取り
        final List<EdinetCsvResultBean> resultBeanList = companyLogic.readFile(pathCompany);

        // Industryの登録
        companyLogic.insertIndustry(resultBeanList);

        // Companyの登録
        companyLogic.upsertCompany(resultBeanList);

        FundanalyzerLogClient.logService("CSVファイルから会社情報の登録が完了しました。", Category.DOCUMENT, Process.COMPANY);
    }

    /**
     * 一連の処理を行う
     * edinetList（書類リストのDB登録）
     * ↓
     * store（書類取得）
     * ↓
     * scrape（スクレイピング）
     *
     * @param date             書類取得対象日（提出日）
     * @param documentTypeCode 書類種別コード
     * @return Void
     */
    @NewSpan("DocumentService.execute")
    @Async
    public CompletableFuture<Void> execute(final String date, final String documentTypeCode) {
        // 書類リストをデータベースに登録する
        edinetList(LocalDate.parse(date));

        // 対象ファイルリスト取得（CompanyCodeがnullではないドキュメントを対象とする）
        final var documentList = documentDao.selectByTypeAndSubmitDate(documentTypeCode, LocalDate.parse(date))
                .stream()
                .filter(document -> companyDao.selectByEdinetCode(document.getEdinetCode()).flatMap(Company::getCode).isPresent())
                .filter(Document::getNotRemoved)
                .collect(Collectors.toList());

        if (documentList.isEmpty()) {
            FundanalyzerLogClient.logService(
                    MessageFormat.format("{0}付の処理対象ドキュメントは存在しませんでした。\t書類種別コード:{1}", date, documentTypeCode),
                    Category.DOCUMENT,
                    Process.EDINET
            );
        } else {
            documentList.parallelStream().forEach(document -> {
                // 書類取得
                if (DocumentStatus.NOT_YET.toValue().equals(document.getDownloaded())) {
                    store(document.getDocumentId(), LocalDate.parse(date));
                }

                // スクレイピング
                // 貸借対照表
                if (DocumentStatus.NOT_YET.toValue().equals(document.getScrapedBs())) {
                    scrapeBs(document.getDocumentId(), LocalDate.parse(date));
                }
                // 損益計算書
                if (DocumentStatus.NOT_YET.toValue().equals(document.getScrapedPl())) {
                    scrapePl(document.getDocumentId(), LocalDate.parse(date));
                }
                // 株式総数
                if (DocumentStatus.NOT_YET.toValue().equals(document.getScrapedNumberOfShares())) {
                    scrapeNs(document.getDocumentId(), LocalDate.parse(date));
                }

                // 除外フラグON
                if (List.of(
                        document.getScrapedBs(),
                        document.getScrapedPl(),
                        document.getScrapedNumberOfShares()
                ).stream().allMatch(status -> DocumentStatus.ERROR.toValue().equals(status))) {
                    documentDao.update(Document.builder()
                            .documentId(document.getDocumentId())
                            .removed(Flag.ON.toValue())
                            .updatedAt(nowLocalDateTime())
                            .build()
                    );
                    FundanalyzerLogClient.logService(
                            MessageFormat.format(
                                    "処理ステータスがすべて [9（ERROR）] となったため、除外フラグをONにしました。\t書類ID:{0}"
                                    , document
                            ),
                            Category.DOCUMENT,
                            Process.EDINET
                    );
                }
            });

            FundanalyzerLogClient.logService(
                    MessageFormat.format("{0}付のドキュメントに対してすべての処理が完了しました。\t書類種別コード:{1}", date, documentTypeCode),
                    Category.DOCUMENT,
                    Process.EDINET
            );
        }
        return null;
    }

    /**
     * EDINETから提供される書類リストをDBに登録する
     *
     * @param startDate ここ日から
     * @param endDate   ここ日まで
     */
    @NewSpan("DocumentService.edinetList")
    public void edinetList(final String startDate, final String endDate) {
        final var dateList = LocalDate.parse(startDate)
                .datesUntil(LocalDate.parse(endDate).plusDays(1))
                .collect(Collectors.toList());

        // 書類リストをデータベースに登録する
        dateList.forEach(this::edinetList);
    }

    /**
     * EDINETから提供される書類リストをDBに登録する
     *
     * @param date この日
     */
    @NewSpan("DocumentService.edinetList.date")
    public void edinetList(final LocalDate date) {
        final var docIdList = edinetDocumentDao.selectAll().stream()
                .map(EdinetDocument::getDocId)
                .collect(Collectors.toList());

        final boolean isPresent = Stream.of(date.toString())
                // EDINETに提出書類の問い合わせ
                .map(d -> edinetProxy.list(new ListRequestParameter(d, ListType.DEFAULT)))
                .map(edinetResponse -> edinetResponse.getMetadata().getResultset().getCount())
                .anyMatch(c -> !"0".equals(c));

        // 書類が0件ではないときは書類リストを取得してデータベースに登録する
        if (isPresent) {
            final EdinetResponse edinetResponse = edinetProxy.list(new ListRequestParameter(date.toString(), ListType.GET_LIST));
            edinetResponse.getResults().forEach(results -> Stream.of(results)
                    .filter(r -> docIdList.stream().noneMatch(docId -> r.getDocId().equals(docId)))
                    .forEach(r -> insertDocument(date, r)));
        }

        FundanalyzerLogClient.logService(
                MessageFormat.format("データベースへの書類一覧登録作業が正常に終了しました。\t指定ファイル日付:{0}", date),
                Category.DOCUMENT,
                Process.EDINET
        );
    }

    private void insertDocument(final LocalDate date, final Results results) {
        try {
            edinetDocumentDao.insert(EdinetDocument.of(results, nowLocalDateTime()));
        } catch (NestedRuntimeException e) {
            if (e.contains(UniqueConstraintException.class)) {
                log.debug("一意制約違反のため、データベースへの登録をスキップします。" +
                                "\tテーブル名:{}\t書類ID:{}\tEDINETコード:{}\t提出者名:{}\t書類種別コード:{}",
                        "edinet_document",
                        results.getDocId(),
                        results.getEdinetCode(),
                        results.getFilerName(),
                        results.getDocTypeCode()
                );
            }
        }
        try {
            // 万が一Companyが登録されていない場合には登録する
            results.getEdinetCode().ifPresent(ed -> {
                if (companyDao.selectByEdinetCode(ed).isEmpty()) {
                    insertCompanyForSqlForeignKey(ed, results.getFilerName());
                }
            });

            documentDao.insert(Document.of(date, results, nowLocalDateTime()));
        } catch (NestedRuntimeException e) {
            if (e.contains(UniqueConstraintException.class)) {
                log.debug("一意制約違反のため、データベースへの登録をスキップします。" +
                                "\tテーブル名:{}\t書類ID:{}\tEDINETコード:{}\t提出者名:{}\t書類種別コード:{}",
                        "document",
                        results.getDocId(),
                        results.getEdinetCode(),
                        results.getFilerName(),
                        results.getDocTypeCode()
                );
            } else if (e.contains(SQLIntegrityConstraintViolationException.class)) {
                log.error("参照整合性制約違反が発生しました。スタックトレースを参考に原因を確認してください。", e.getRootCause());
                throw new FundanalyzerSqlForeignKeyException(e);
            } else {
                throw new FundanalyzerRuntimeException("想定外のエラーが発生しました。", e);
            }
        }
    }

    /**
     * フォルダに対象ファイルが存在するかを確認して、ダウンロードorステータス更新する
     *
     * @param docId      書類管理番号
     * @param targetDate 書類取得対象日（提出日）
     */
    @NewSpan("DocumentService.store")
    public void store(final String docId, final LocalDate targetDate) {
        // 既にファイルが存在しているか確認する
        if (fileListAlready(targetDate).stream()
                .anyMatch(docIdList -> docIdList.stream().anyMatch(docId::equals))) {
            documentDao.update(Document.ofUpdateStoreToDone(docId, nowLocalDateTime()));
        } else {
            // ファイル取得
            scrapingLogic.download(docId, targetDate);
        }
    }

    /**
     * 既にダウンロード済のファイルリスト
     *
     * @param targetDate 書類取得対象日（提出日）
     * @return 既にダウンロード済のファイルリスト
     */
    Optional<List<String>> fileListAlready(final LocalDate targetDate) {
        // 取得済ファイルリスト
        return Optional.ofNullable(makeTargetPath(pathDecode, targetDate).listFiles())
                .map(Arrays::stream)
                .map(fileList -> fileList.map(File::getName))
                .map(fileName -> fileName.collect(Collectors.toList()))
                .or(Optional::empty);
    }

    /**
     * 書類取得対象日（提出日）に紐づくドキュメントのステータスを確認して、<br/>
     * 財務諸表のスクレイピング処理を行う
     *
     * @param targetDate 書類取得対象日（提出日）
     */
    @NewSpan("DocumentService.scrape.targetDate")
    public void scrape(final LocalDate targetDate) {
        final List<Document> documentList = documentDao.selectByTypeAndSubmitDate("120", targetDate);
        if (documentList.isEmpty()) {
            FundanalyzerLogClient.logService(
                    MessageFormat.format("次のドキュメントはデータベースに存在しませんでした。\t対象提出日:{0}", targetDate),
                    Category.DOCUMENT,
                    Process.SCRAPING
            );
        } else {
            documentList.stream()
                    .filter(document -> companyDao.selectByEdinetCode(document.getEdinetCode()).flatMap(Company::getCode).isPresent())
                    .filter(Document::getNotRemoved)
                    .forEach(d -> {
                        if (DocumentStatus.NOT_YET.toValue().equals(d.getDownloaded())) {
                            store(d.getDocumentId(), d.getSubmitDate());
                        }
                        if (DocumentStatus.NOT_YET.toValue().equals(d.getScrapedBs())) {
                            scrapeBs(d.getDocumentId(), targetDate);
                        }
                        if (DocumentStatus.NOT_YET.toValue().equals(d.getScrapedPl())) {
                            scrapePl(d.getDocumentId(), targetDate);
                        }
                        if (DocumentStatus.NOT_YET.toValue().equals(d.getScrapedNumberOfShares())) {
                            scrapeNs(d.getDocumentId(), targetDate);
                        }
                    });

            FundanalyzerLogClient.logService(
                    MessageFormat.format("次のドキュメントに対してスクレイピング処理を正常に終了しました。\t対象提出日:{0}", targetDate),
                    Category.DOCUMENT,
                    Process.SCRAPING
            );
        }
    }

    /**
     * 書類IDに紐づく財務諸表のスクレイピング処理を行う
     *
     * @param documentId 書類ID
     */
    @NewSpan("DocumentService.scrape.documentId")
    public void scrape(final String documentId) {
        final var document = documentDao.selectByDocumentId(documentId);

        if (!DocumentStatus.DONE.toValue().equals(document.getDownloaded())) {
            store(documentId, document.getSubmitDate());
        }
        if (!DocumentStatus.DONE.toValue().equals(document.getScrapedBs())) {
            scrapeBs(documentId, document.getSubmitDate());
        }
        if (!DocumentStatus.DONE.toValue().equals(document.getScrapedPl())) {
            scrapePl(documentId, document.getSubmitDate());
        }
        if (!DocumentStatus.DONE.toValue().equals(document.getScrapedNumberOfShares())) {
            scrapeNs(documentId, document.getSubmitDate());
        }

        FundanalyzerLogClient.logService(
                MessageFormat.format("次のドキュメントに対してスクレイピング処理を正常に終了しました。\t書類ID:{0}", documentId),
                Category.DOCUMENT,
                Process.SCRAPING
        );
    }

    /**
     * 貸借対照表と損益計算書の処理ステータスが処理中（5）またはエラー（9）の
     * ドキュメントをステータス初期化する
     */
    public void resetForRetry() {
        final var documentList = documentDao.selectByDocumentTypeCode("120");
        // 貸借対照表
        documentList.stream()
                .filter(document -> !DocumentStatus.DONE.toValue().equals(document.getScrapedBs()))
                .filter(document -> !DocumentStatus.NOT_YET.toValue().equals(document.getScrapedBs()))
                .filter(Document::getNotRemoved)
                .map(Document::getDocumentId)
                .forEach(documentId -> {
                    documentDao.update(Document.ofUpdateBsToNotYet(documentId, nowLocalDateTime()));
                    log.info("次のドキュメントステータスを初期化しました。\t書類ID:{}\t財務諸表名:{}", documentId, "貸借対照表");
                });

        // 損益計算書
        documentList.stream()
                .filter(document -> !DocumentStatus.DONE.toValue().equals(document.getScrapedPl()))
                .filter(document -> !DocumentStatus.NOT_YET.toValue().equals(document.getScrapedPl()))
                .filter(Document::getNotRemoved)
                .map(Document::getDocumentId)
                .forEach(documentId -> {
                    documentDao.update(Document.ofUpdatePlToNotYet(documentId, nowLocalDateTime()));
                    log.info("次のドキュメントステータスを初期化しました。\t書類ID:{}\t財務諸表名:{}", documentId, "損益計算書");
                });
    }

    /**
     * 指定書類IDを処理対象から除外する
     *
     * @param documentId 書類ID
     */
    @NewSpan("DocumentService.removeDocument")
    @Transactional
    public void removeDocument(final String documentId) {
        documentDao.update(Document.ofUpdateRemoved(documentId, nowLocalDateTime()));

        FundanalyzerLogClient.logService(
                MessageFormat.format("ドキュメントを処理対象外にしました。\t書類ID:{0}", documentId),
                Category.DOCUMENT,
                Process.UPDATE
        );
    }

    private void insertCompanyForSqlForeignKey(final String edinetCode, final String companyName) {
        companyDao.insert(Company.ofSqlForeignKey(edinetCode, companyName, nowLocalDateTime()));
        log.warn("会社情報が登録されていないため、仮情報を登録します。" +
                "\tEDINETコード:{}\t会社名:{}", edinetCode, companyName);
    }

    private void scrapeBs(final String documentId, final LocalDate targetDate) {
        scrapingLogic.scrape(
                FinancialStatementEnum.BALANCE_SHEET,
                documentId,
                targetDate,
                bsSubjectDao.selectAll()
        );
    }

    private void scrapePl(final String documentId, final LocalDate targetDate) {
        scrapingLogic.scrape(
                FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT,
                documentId,
                targetDate,
                plSubjectDao.selectAll()
        );
    }

    private void scrapeNs(final String documentId, final LocalDate targetDate) {
        scrapingLogic.scrape(
                FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES,
                documentId,
                targetDate,
                null
        );
    }

    private File makeTargetPath(final String prePath, final LocalDate targetDate) {
        return new File(String.format("%s/%d/%s/%s", prePath, targetDate.getYear(), targetDate.getMonth(), targetDate));
    }
}
