package github.com.ioridazo.fundanalyzer.domain.logic.scraping;

import github.com.ioridazo.fundanalyzer.domain.dao.master.BsSubjectDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.CompanyDao;
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
import github.com.ioridazo.fundanalyzer.domain.entity.master.ScrapingKeyword;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Document;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.EdinetDocument;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.FinancialStatement;
import github.com.ioridazo.fundanalyzer.domain.log.Category;
import github.com.ioridazo.fundanalyzer.domain.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.domain.log.Process;
import github.com.ioridazo.fundanalyzer.domain.logic.scraping.jsoup.XbrlScraping;
import github.com.ioridazo.fundanalyzer.domain.logic.scraping.jsoup.bean.Unit;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerFileException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRestClientException;
import github.com.ioridazo.fundanalyzer.file.FileOperator;
import github.com.ioridazo.fundanalyzer.proxy.edinet.EdinetProxy;
import github.com.ioridazo.fundanalyzer.proxy.edinet.entity.request.AcquisitionRequestParameter;
import github.com.ioridazo.fundanalyzer.proxy.edinet.entity.request.AcquisitionType;
import lombok.extern.log4j.Log4j2;
import org.seasar.doma.jdbc.UniqueConstraintException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.core.NestedRuntimeException;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Log4j2
@Component
public class ScrapingLogic {

    private final String pathEdinet;
    private final String pathDecode;
    private final EdinetProxy proxy;
    private final FileOperator fileOperator;
    private final XbrlScraping xbrlScraping;
    private final CompanyDao companyDao;
    private final DocumentDao documentDao;
    private final EdinetDocumentDao edinetDocumentDao;
    private final BsSubjectDao bsSubjectDao;
    private final FinancialStatementDao financialStatementDao;
    private final ScrapingKeywordDao scrapingKeywordDao;

    public ScrapingLogic(
            @Value("${app.settings.file.path.edinet}") final String pathEdinet,
            @Value("${app.settings.file.path.decode}") final String pathDecode,
            final EdinetProxy proxy,
            final FileOperator fileOperator,
            final XbrlScraping xbrlScraping,
            final CompanyDao companyDao,
            final DocumentDao documentDao,
            final EdinetDocumentDao edinetDocumentDao,
            final BsSubjectDao bsSubjectDao,
            final FinancialStatementDao financialStatementDao,
            final ScrapingKeywordDao scrapingKeywordDao) {
        this.pathEdinet = pathEdinet;
        this.pathDecode = pathDecode;
        this.proxy = proxy;
        this.fileOperator = fileOperator;
        this.xbrlScraping = xbrlScraping;
        this.companyDao = companyDao;
        this.documentDao = documentDao;
        this.edinetDocumentDao = edinetDocumentDao;
        this.bsSubjectDao = bsSubjectDao;
        this.financialStatementDao = financialStatementDao;
        this.scrapingKeywordDao = scrapingKeywordDao;
    }

    LocalDateTime nowLocalDateTime() {
        return LocalDateTime.now();
    }

    /**
     * 書類をダウンロードしてステータス更新する
     *
     * @param docId      書類ID
     * @param targetDate 提出日
     */
    @NewSpan("ScrapingLogic.download")
    public void download(final String docId, final LocalDate targetDate) {
        try {
            // ファイル取得
            proxy.acquisition(
                    makeTargetPath(pathEdinet, targetDate),
                    new AcquisitionRequestParameter(docId, AcquisitionType.DEFAULT)
            );

            documentDao.update(Document.builder()
                    .documentId(docId)
                    .downloaded(DocumentStatus.DONE.toValue())
                    .updatedAt(nowLocalDateTime())
                    .build()
            );

            // ファイル解凍
            fileOperator.decodeZipFile(
                    makeTargetPath(pathEdinet, targetDate, docId),
                    makeTargetPath(pathDecode, targetDate, docId)
            );

            documentDao.update(Document.builder()
                    .documentId(docId)
                    .decoded(DocumentStatus.DONE.toValue())
                    .updatedAt(nowLocalDateTime())
                    .build());

        } catch (FundanalyzerRestClientException e) {
            log.error("書類のダウンロード処理に失敗しました。スタックトレースから原因を確認してください。" +
                    "\t処理対象日:{}\t書類管理番号:{}", targetDate, docId, e);
            documentDao.update(Document.builder()
                    .documentId(docId)
                    .downloaded(DocumentStatus.ERROR.toValue())
                    .updatedAt(nowLocalDateTime())
                    .build()
            );
        } catch (IOException e) {
            log.error("zipファイルの解凍処理に失敗しました。スタックトレースから原因を確認してください。" +
                    "\t処理対象日:{}\t書類管理番号:{}", targetDate, docId, e);
            documentDao.update(Document.builder()
                    .documentId(docId)
                    .decoded(DocumentStatus.ERROR.toValue())
                    .updatedAt(nowLocalDateTime())
                    .build()
            );
        }
    }

    /**
     * スクレイピング処理
     *
     * @param fs         財務諸表
     * @param documentId 書類ID
     * @param date       提出日
     * @param detailList 財務諸表科目リスト
     * @param <T>        財務諸表の型
     */
    @NewSpan("ScrapingLogic.scrape")
    public <T extends Detail> void scrape(
            final FinancialStatementEnum fs,
            final String documentId,
            final LocalDate date,
            final List<T> detailList) {
        final var edinetDocument = edinetDocumentDao.selectByDocId(documentId);
        final var company = companyDao.selectByEdinetCode(edinetDocument.getEdinetCode().orElse(null)).orElseThrow();
        final var targetDirectory = makeDocumentPath(pathDecode, date, documentId);

        // 財務諸表登録年（period_endの年）が重複していないか確認する
        if (beforeCheck(company, fs, edinetDocument)) {
            try {
                final var targetFile = findTargetFile(targetDirectory, fs);
                if (FinancialStatementEnum.BALANCE_SHEET.equals(fs) ||
                        FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT.equals(fs)) {
                    // 貸借対照表、損益計算書
                    insertFinancialStatement(
                            targetFile.getFirst(),
                            targetFile.getSecond(),
                            fs,
                            company,
                            detailList,
                            edinetDocument
                    );

                    if (FinancialStatementEnum.BALANCE_SHEET.equals(fs)) {
                        checkBs(company, edinetDocument);
                    }
                } else if (FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES.equals(fs)) {
                    // 株式総数
                    final var value = xbrlScraping.scrapeNumberOfShares(
                            targetFile.getFirst(),
                            targetFile.getSecond().getKeyword()
                    );
                    insertFinancialStatement(
                            company,
                            FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES,
                            "0",
                            edinetDocument,
                            parseValue(value).orElse(null)
                    );
                }

                FundanalyzerLogClient.logLogic(
                        MessageFormat.format("次のスクレイピング情報を正常に登録しました。\n企業コード:{0}\tEDINETコード:{1}\t財務諸表名:{2}\tファイル名:{3}",
                                company.getCode().orElseThrow(),
                                company.getEdinetCode(),
                                fs.getName(),
                                targetFile.getFirst().getPath()),
                        Category.DOCUMENT,
                        Process.SCRAPING
                );

                documentDao.update(Document.ofUpdated(
                        fs,
                        documentId,
                        DocumentStatus.DONE,
                        targetFile.getFirst().getPath(),
                        nowLocalDateTime()
                ));

            } catch (FundanalyzerFileException e) {
                documentDao.update(Document.ofUpdated(
                        fs,
                        documentId,
                        DocumentStatus.ERROR,
                        null,
                        nowLocalDateTime()
                ));
                log.error("スクレイピング処理の過程でエラー発生しました。スタックトレースを参考に原因を確認してください。" +
                                "\n企業コード:{}\tEDINETコード:{}\t財務諸表名:{}\tファイルパス:{}",
                        company.getCode().orElseThrow(),
                        company.getEdinetCode(),
                        fs.getName(),
                        targetDirectory.getPath(), e
                );
            }
        } else {
            documentDao.update(Document.builder()
                    .documentId(documentId)
                    .removed(Flag.ON.toValue())
                    .updatedAt(nowLocalDateTime())
                    .build()
            );
            log.warn("対象年が重複しており一意制約違反を避けるため、スクレイピング処理を実施せずに後続処理を続けます。" +
                            "\t企業コード:{}\tEDINETコード:{}\t会社名:{}\t財務諸表名:{}\t書類ID:{}\tperiodEnd:{}",
                    company.getCode().orElseThrow(),
                    company.getEdinetCode(),
                    company.getCompanyName(),
                    fs.getName(),
                    documentId,
                    edinetDocument.getPeriodEnd().orElseThrow()
            );
        }
    }

    /**
     * スクレイピング処理前に財務諸表登録年（period_endの年）が重複していないか確認する
     *
     * @param company        会社
     * @param fs             財務諸表
     * @param edinetDocument EDINETドキュメント
     * @return boolean
     */
    boolean beforeCheck(
            final Company company,
            final FinancialStatementEnum fs,
            final EdinetDocument edinetDocument) {
        final var targetYear = edinetDocument.getPeriodEnd().orElseThrow().substring(0, 4);
        final var fsList = financialStatementDao.selectByEdinetCodeAndFsAndYear(company.getEdinetCode(), fs.toValue(), targetYear);

        if (fsList.isEmpty()) {
            return true;
        } else return fsList.stream()
                .map(FinancialStatement::getPeriodEnd)
                .distinct()
                .map(LocalDate::getYear)
                .map(String::valueOf)
                .noneMatch(targetYear::equals);
    }

    /**
     * 対象ファイルとスクレイピングキーワードを見つける
     *
     * @param targetFile 対象フォルダ
     * @param fs         財務諸表
     * @return 対象ファイルとスクレイピングキーワード
     * @throws FundanalyzerFileException 見つからなかったとき
     */
    Pair<File, ScrapingKeyword> findTargetFile(final File targetFile, final FinancialStatementEnum fs) {
        final var scrapingKeywordList = scrapingKeywordDao.selectByFinancialStatementId(fs.toValue());

        log.info("\"{}\" のスクレイピング処理を開始します。", fs.getName());

        for (ScrapingKeyword scrapingKeyword : scrapingKeywordList) {
            final var findFile = xbrlScraping.findFile(targetFile, scrapingKeyword);

            if (findFile.isPresent()) {
                log.info("対象ファイルの存在を正常に確認できました。\t財務諸表名:{}\tキーワード:{}",
                        scrapingKeyword.getRemarks(), scrapingKeyword.getKeyword());
                return Pair.of(findFile.get(), scrapingKeyword);
            }
        }
        throw new FundanalyzerFileException("対象のファイルの探索に失敗しました。");
    }

    /**
     * 財務諸表をスクレイピングして<br/>
     * その結果とマスタの科目が一致したときにDBに登録する
     *
     * @param targetFile      対象ファイル
     * @param scrapingKeyword スクレイピングキーワード
     * @param fs              財務諸表
     * @param company         会社
     * @param detailList      科目リスト
     * @param edinetDocument  EDINETドキュメント
     * @param <T>             財務諸表の型
     */
    <T extends Detail> void insertFinancialStatement(
            final File targetFile,
            final ScrapingKeyword scrapingKeyword,
            final FinancialStatementEnum fs,
            final Company company,
            final List<T> detailList,
            final EdinetDocument edinetDocument) {
        final var resultBeans = xbrlScraping.scrapeFinancialStatement(targetFile, scrapingKeyword.getKeyword());

        resultBeans.forEach(resultBean -> detailList.stream()
                // スクレイピング結果とマスタから一致するものをフィルターにかける
                .filter(detail -> Objects.equals(resultBean.getSubject().orElse(null), detail.getName()))
                .findAny()
                // 一致するものが存在したら下記
                .ifPresent(detail -> insertFinancialStatement(
                        company,
                        fs,
                        detail.getId(),
                        edinetDocument,
                        parseValue(resultBean.getCurrentValue(), resultBean.getUnit()).orElse(null)
                )));
    }

    /**
     * DBに財務諸表の科目とその値を登録する
     *
     * @param company        会社
     * @param fs             財務諸表
     * @param dId            科目ID
     * @param edinetDocument EDINETドキュメント
     * @param value          値
     */
    @Transactional
    void insertFinancialStatement(
            final Company company,
            final FinancialStatementEnum fs,
            final String dId,
            final EdinetDocument edinetDocument,
            final Long value) {
        try {
            financialStatementDao.insert(new FinancialStatement(
                    null,
                    company.getCode().orElse(null),
                    company.getEdinetCode(),
                    fs.toValue(),
                    dId,
                    LocalDate.parse(edinetDocument.getPeriodStart().orElseThrow()),
                    LocalDate.parse(edinetDocument.getPeriodEnd().orElseThrow()),
                    value,
                    nowLocalDateTime()
            ));
        } catch (NestedRuntimeException e) {
            if (e.contains(UniqueConstraintException.class)) {
                log.info("一意制約違反のため、データベースへの登録をスキップします。" +
                                "\t企業コード:{}\t財務諸表名:{}\t科目ID:{}\t対象年:{}",
                        company.getCode().orElse(null),
                        fs.getName(),
                        dId,
                        edinetDocument.getPeriodEnd().orElseThrow().substring(0, 4)
                );
            } else {
                throw e;
            }
        }
    }

    /**
     * 貸借対照表の流動負債合計と負債合計の値が一致したときに<br/>
     * 流動負債合計の値を0としてDBに登録する
     *
     * @param company        会社
     * @param edinetDocument EDINETドキュメント
     */
    void checkBs(final Company company, final EdinetDocument edinetDocument) {
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

        if ((totalCurrentLiabilities.isPresent() && totalLiabilities.isPresent()) &&
                (totalCurrentLiabilities.get().equals(totalLiabilities.get()))) {
            insertFinancialStatement(
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
                            .replace("注1", "").replace("注１", "")
                            .replace("注2", "").replace("注２", "")
                            .replace("注3", "").replace("注３", "")
                            .replace("注4", "").replace("注４", "")
                            .replace("注5", "").replace("注５", "")
                            .replace("*1", "").replace("*2", "")
                            .replace("株", "")
                            .replace("－", "0").replace("―", "0")
                            .replace("-", "0")
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

    private File makeTargetPath(final String prePath, final LocalDate targetDate) {
        return new File(prePath + "/" + targetDate.getYear() + "/" + targetDate.getMonth() + "/" + targetDate);
    }

    private File makeTargetPath(final String prePath, final LocalDate targetDate, final String docId) {
        return new File(prePath + "/" + targetDate.getYear() + "/" + targetDate.getMonth() + "/" + targetDate
                + "/" + docId);
    }

    private File makeDocumentPath(final String prePath, final LocalDate targetDate, final String docId) {
        return new File(prePath + "/" + targetDate.getYear() + "/" + targetDate.getMonth() + "/" + targetDate
                + "/" + docId + "/XBRL/PublicDoc");
    }
}
