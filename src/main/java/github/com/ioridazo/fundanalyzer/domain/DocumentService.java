package github.com.ioridazo.fundanalyzer.domain;

import github.com.ioridazo.fundanalyzer.domain.csv.CsvCommander;
import github.com.ioridazo.fundanalyzer.domain.csv.bean.EdinetCsvResultBean;
import github.com.ioridazo.fundanalyzer.domain.dao.master.BsSubjectDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.IndustryDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.PlSubjectDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.EdinetDocumentDao;
import github.com.ioridazo.fundanalyzer.domain.entity.DocumentStatus;
import github.com.ioridazo.fundanalyzer.domain.entity.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.Flag;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Company;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Industry;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Document;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.EdinetDocument;
import github.com.ioridazo.fundanalyzer.domain.scraping.ScrapingLogic;
import github.com.ioridazo.fundanalyzer.edinet.EdinetProxy;
import github.com.ioridazo.fundanalyzer.edinet.entity.request.ListRequestParameter;
import github.com.ioridazo.fundanalyzer.edinet.entity.request.ListType;
import github.com.ioridazo.fundanalyzer.edinet.entity.response.EdinetResponse;
import github.com.ioridazo.fundanalyzer.edinet.entity.response.Metadata;
import github.com.ioridazo.fundanalyzer.edinet.entity.response.ResultSet;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerSqlForeignKeyException;
import lombok.extern.slf4j.Slf4j;
import org.seasar.doma.jdbc.UniqueConstraintException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedRuntimeException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.charset.Charset;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class DocumentService {

    private final String pathCompany;
    private final String pathDecode;
    private final EdinetProxy proxy;
    private final CsvCommander csvCommander;
    private final ScrapingLogic scrapingLogic;
    private final IndustryDao industryDao;
    private final CompanyDao companyDao;
    private final BsSubjectDao bsSubjectDao;
    private final PlSubjectDao plSubjectDao;
    private final EdinetDocumentDao edinetDocumentDao;
    private final DocumentDao documentDao;

    public DocumentService(
            @Value("${settings.file.path.company}") final String pathCompany,
            @Value("${settings.file.path.decode}") final String pathDecode,
            final EdinetProxy proxy,
            final CsvCommander csvCommander,
            final ScrapingLogic scrapingLogic,
            final IndustryDao industryDao,
            final CompanyDao companyDao,
            final BsSubjectDao bsSubjectDao,
            final PlSubjectDao plSubjectDao,
            final EdinetDocumentDao edinetDocumentDao,
            final DocumentDao documentDao
    ) {
        this.pathCompany = pathCompany;
        this.pathDecode = pathDecode;
        this.proxy = proxy;
        this.csvCommander = csvCommander;
        this.scrapingLogic = scrapingLogic;
        this.industryDao = industryDao;
        this.companyDao = companyDao;
        this.bsSubjectDao = bsSubjectDao;
        this.plSubjectDao = plSubjectDao;
        this.edinetDocumentDao = edinetDocumentDao;
        this.documentDao = documentDao;
    }

    LocalDateTime nowLocalDateTime() {
        return LocalDateTime.now();
    }

    @Transactional
    public void company() {
        // CSV読み取り
        final var resultBeanList = csvCommander.readCsv(
                new File(pathCompany),
                Charset.forName("windows-31j"),
                EdinetCsvResultBean.class
        );

        var dbIndustryList = industryDao.selectAll().stream()
                .map(Industry::getName)
                .collect(Collectors.toList());

        // Industryの登録
        resultBeanList.stream()
                .map(EdinetCsvResultBean::getIndustry)
                .distinct()
                .forEach(resultBeanIndustry -> Stream.of(resultBeanIndustry)
                        .filter(industryName -> dbIndustryList.stream().noneMatch(industryName::equals))
                        .forEach(industryName -> industryDao.insert(new Industry(null, industryName, nowLocalDateTime())))
                );


        // Companyの登録
        final var companyList = companyDao.selectAll();
        final var industryList = industryDao.selectAll();
        resultBeanList.forEach(resultBean -> {
                    final var match = companyList.stream()
                            .map(Company::getEdinetCode)
                            .anyMatch(resultBean.getEdinetCode()::equals);
                    if (match) {
                        companyDao.update(Company.of(industryList, resultBean));
                    } else {
                        companyDao.insert(Company.of(industryList, resultBean));
                    }
                }
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
                .filter(document -> companyDao.selectByEdinetCode(document.getEdinetCode()).flatMap(Company::getCode).isPresent())
                .filter(Document::getNotRemoved)
                .map(Document::getDocumentId)
                .collect(Collectors.toList());

        if (documentIdList.isEmpty()) {
            log.info("{}付の処理対象ドキュメントは存在しませんでした。\t書類種別コード:{}", date, documentTypeCode);
        } else {
            documentIdList.parallelStream().forEach(documentId -> {
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
                                    // 万が一Companyが登録されていない場合には登録する
                                    r.getEdinetCode().ifPresent(ed -> {
                                        if (companyDao.selectByEdinetCode(ed).isEmpty()) {
                                            insertCompanyForSqlForeignKey(ed, r.getFilerName());
                                        }
                                    });

                                    documentDao.insert(Document.builder()
                                            .documentId(r.getDocId())
                                            .documentTypeCode(r.getDocTypeCode())
                                            .edinetCode(r.getEdinetCode().orElse(null))
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
        final var fileListAlready = Optional.ofNullable(makeTargetPath(pathDecode, targetDate).listFiles())
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
            scrapingLogic.download(targetDate, docId);
        }
    }

    public void scrape(final LocalDate submitDate) {
        log.info("次のドキュメントに対してスクレイピング処理を実行します。\t対象日:{}", submitDate);
        final var documentList = documentDao.selectByTypeAndSubmitDate("120", submitDate).stream()
                .filter(document -> companyDao.selectByEdinetCode(document.getEdinetCode()).flatMap(Company::getCode).isPresent())
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

    private void insertCompanyForSqlForeignKey(final String edinetCode, final String companyName) {
        companyDao.insert(Company.ofSqlForeignKey(edinetCode, companyName, nowLocalDateTime()));
        log.warn("会社情報が登録されていないため、仮情報を登録します。" +
                "\tEDINETコード:{}\t会社名:{}", edinetCode, companyName);
    }

    private void scrapeBs(final String documentId) {
        scrapeBs(documentId, documentDao.selectByDocumentId(documentId).getSubmitDate());
    }

    private void scrapeBs(final String documentId, final LocalDate targetDate) {
        scrapingLogic.scrape(
                FinancialStatementEnum.BALANCE_SHEET,
                documentId,
                targetDate,
                bsSubjectDao.selectAll()
        );
    }

    private void scrapePl(final String documentId) {
        scrapePl(documentId, documentDao.selectByDocumentId(documentId).getSubmitDate());
    }

    private void scrapePl(final String documentId, final LocalDate targetDate) {
        scrapingLogic.scrape(
                FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT,
                documentId,
                targetDate,
                plSubjectDao.selectAll()
        );
    }

    private void scrapeNs(final String documentId) {
        scrapeNs(documentId, documentDao.selectByDocumentId(documentId).getSubmitDate());
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
        return new File(prePath + "/" + targetDate.getYear() + "/" + targetDate.getMonth() + "/" + targetDate);
    }
}
