package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.domain.domain.dao.master.ScrapingKeywordDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.ScrapingKeywordEntity;
import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.domain.domain.jsoup.XbrlScraping;
import github.com.ioridazo.fundanalyzer.domain.domain.jsoup.bean.FinancialTableResultBean;
import github.com.ioridazo.fundanalyzer.domain.domain.jsoup.bean.Unit;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CompanySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.DocumentSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.FinancialStatementSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.SubjectSpecification;
import github.com.ioridazo.fundanalyzer.domain.usecase.ScrapingUseCase;
import github.com.ioridazo.fundanalyzer.domain.value.BsSubject;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerFileException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRestClientException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import github.com.ioridazo.fundanalyzer.client.file.FileOperator;
import github.com.ioridazo.fundanalyzer.client.edinet.EdinetClient;
import github.com.ioridazo.fundanalyzer.client.edinet.entity.request.AcquisitionRequestParameter;
import github.com.ioridazo.fundanalyzer.client.edinet.entity.request.AcquisitionType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

@Component
public class ScrapingInteractor implements ScrapingUseCase {

    private static final Logger log = LogManager.getLogger(ScrapingInteractor.class);

    private final ScrapingKeywordDao scrapingKeywordDao;
    private final CompanySpecification companySpecification;
    private final SubjectSpecification subjectSpecification;
    private final DocumentSpecification documentSpecification;
    private final FinancialStatementSpecification financialStatementSpecification;
    private final XbrlScraping xbrlScraping;
    private final FileOperator fileOperator;
    private final EdinetClient edinetClient;

    @Value("${app.settings.file.path.edinet}")
    String pathEdinet;
    @Value("${app.settings.file.path.decode}")
    String pathDecode;

    public ScrapingInteractor(
            final ScrapingKeywordDao scrapingKeywordDao,
            final CompanySpecification companySpecification,
            final SubjectSpecification subjectSpecification,
            final DocumentSpecification documentSpecification,
            final FinancialStatementSpecification financialStatementSpecification,
            final XbrlScraping xbrlScraping,
            final FileOperator fileOperator,
            final EdinetClient edinetClient) {
        this.scrapingKeywordDao = scrapingKeywordDao;
        this.companySpecification = companySpecification;
        this.subjectSpecification = subjectSpecification;
        this.documentSpecification = documentSpecification;
        this.financialStatementSpecification = financialStatementSpecification;
        this.xbrlScraping = xbrlScraping;
        this.fileOperator = fileOperator;
        this.edinetClient = edinetClient;
    }

    /**
     * ドキュメントをダウンロードする
     *
     * @param document ドキュメント
     */
    @Override
    public void download(final Document document) {
        try {
            // ファイル取得
            edinetClient.acquisition(
                    makeTargetPath(pathEdinet, document.getSubmitDate()),
                    new AcquisitionRequestParameter(document.getDocumentId(), AcquisitionType.DEFAULT)
            );
            documentSpecification.updateDownloadToDone(document);

            // ファイル解凍
            fileOperator.decodeZipFile(
                    makeTargetPath(pathEdinet, document.getSubmitDate(), document.getDocumentId()),
                    makeTargetPath(pathDecode, document.getSubmitDate(), document.getDocumentId())
            );
            documentSpecification.updateDecodeToDone(document);

        } catch (FundanalyzerRestClientException e) {
            log.error("書類のダウンロード処理に失敗しました。スタックトレースから原因を確認してください。" +
                    "\t処理対象日:{}\t書類管理番号:{}", document.getSubmitDate(), document.getDocumentId(), e);
            documentSpecification.updateDownloadToError(document);
        } catch (IOException e) {
            log.error("zipファイルの解凍処理に失敗しました。スタックトレースから原因を確認してください。" +
                    "\t処理対象日:{}\t書類管理番号:{}", document.getSubmitDate(), document.getDocumentId(), e);
            documentSpecification.updateDecodeToError(document);
        }
    }

    /**
     * 貸借対照表をスクレイピングする
     *
     * @param document ドキュメント
     */
    @Override
    public void bs(final Document document) {
        executeScraping(
                FinancialStatementEnum.BALANCE_SHEET,
                document,
                (company, targetFile) -> {
                    final List<FinancialTableResultBean> resultBeanList =
                            xbrlScraping.scrapeFinancialStatement(targetFile.getFirst(), targetFile.getSecond().getKeyword());

                    resultBeanList.forEach(resultBean -> resultBean.getSubject()
                            .flatMap(subjectSpecification::findBsSubject)
                            .ifPresent(subject -> financialStatementSpecification.insert(
                                    company,
                                    FinancialStatementEnum.BALANCE_SHEET,
                                    subject.getId(),
                                    document,
                                    parseValue(resultBean.getCurrentValue(), resultBean.getUnit()).orElse(null)
                            ))
                    );

                    doBsOptionIfTarget(company, document);
                }
        );
    }

    /**
     * 損益計算書をスクレイピングする
     *
     * @param document ドキュメント
     */
    @Override
    public void pl(final Document document) {
        executeScraping(
                FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT,
                document,
                (company, targetFile) -> {
                    final List<FinancialTableResultBean> resultBeanList =
                            xbrlScraping.scrapeFinancialStatement(targetFile.getFirst(), targetFile.getSecond().getKeyword());

                    resultBeanList.forEach(resultBean -> resultBean.getSubject()
                            .flatMap(subjectSpecification::findPlSubject)
                            .ifPresent(subject -> financialStatementSpecification.insert(
                                    company,
                                    FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT,
                                    subject.getId(),
                                    document,
                                    parseValue(resultBean.getCurrentValue(), resultBean.getUnit()).orElse(null)
                            ))
                    );
                }
        );
    }

    /**
     * 株式総数をスクレイピングする
     *
     * @param document ドキュメント
     */
    @Override
    public void ns(final Document document) {
        executeScraping(
                FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES,
                document,
                (company, targetFile) -> {
                    final String value = xbrlScraping.scrapeNumberOfShares(targetFile.getFirst(), targetFile.getSecond().getKeyword());

                    financialStatementSpecification.insert(
                            company, FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES, "0", document, parseValue(value).orElse(null));

                }
        );
    }

    /**
     * フォルダから処理対象のファイルを取得する
     *
     * @param targetFile 対象フォルダ
     * @param fs         財務諸表種別
     * @return 対象ファイルとそのキーワード
     */
    Pair<File, ScrapingKeywordEntity> findTargetFile(final File targetFile, final FinancialStatementEnum fs) {
        final List<ScrapingKeywordEntity> scrapingKeywordList = scrapingKeywordDao.selectByFinancialStatementId(fs.toValue());

        FundanalyzerLogClient.logLogic(
                MessageFormat.format("[{0}] のスクレイピング処理を開始します。\tパス:{1}",
                        fs.getName(),
                        targetFile.getPath()),
                Category.DOCUMENT,
                Process.SCRAPING
        );

        for (final ScrapingKeywordEntity scrapingKeyword : scrapingKeywordList) {
            final Optional<File> findFile = xbrlScraping.findFile(targetFile, scrapingKeyword);

            if (findFile.isPresent()) {
                FundanalyzerLogClient.logLogic(
                        MessageFormat.format("[{0}]の対象ファイルを正常に確認できました。\tキーワード:{1}",
                                scrapingKeyword.getRemarks(),
                                scrapingKeyword.getKeyword()),
                        Category.DOCUMENT,
                        Process.SCRAPING
                );

                return Pair.of(findFile.get(), scrapingKeyword);
            }
        }
        throw new FundanalyzerFileException("キーワードに合致するファイルが存在しませんでした。");
    }

    /**
     * 貸借対照表のオプション処理を実行する
     *
     * @param company  企業情報
     * @param document ドキュメント
     */
    void doBsOptionIfTarget(final Company company, final Document document) {
        final Optional<Long> totalCurrentLiabilities = financialStatementSpecification.findValue(
                FinancialStatementEnum.BALANCE_SHEET,
                document,
                subjectSpecification.findBsSubject(BsSubject.BsEnum.TOTAL_CURRENT_LIABILITIES)
        );
        final Optional<Long> totalLiabilities = financialStatementSpecification.findValue(
                FinancialStatementEnum.BALANCE_SHEET,
                document,
                subjectSpecification.findBsSubject(BsSubject.BsEnum.TOTAL_LIABILITIES)
        );

        if ((totalCurrentLiabilities.isPresent() && totalLiabilities.isPresent())
                && (totalCurrentLiabilities.get().equals(totalLiabilities.get()))) {
            financialStatementSpecification.insert(
                    company,
                    FinancialStatementEnum.BALANCE_SHEET,
                    subjectSpecification.findBsSubject(BsSubject.BsEnum.TOTAL_FIXED_LIABILITIES).getId(),
                    document,
                    0L
            );

            FundanalyzerLogClient.logLogic(
                    MessageFormat.format(
                            "[貸借対照表] の \"固定負債合計\" が存在しなかったため、次の通りとして\"0\" にてデータベースに登録しました。" +
                                    "\t企業コード:{0}\t書類ID:{1}\t流動負債合計:{2}\t負債合計:{3}",
                            company.getCode().orElse("null"),
                            document.getDocumentId(),
                            totalCurrentLiabilities.get(),
                            totalLiabilities.get()
                    ),
                    Category.DOCUMENT,
                    Process.SCRAPING
            );
        }
    }

    /**
     * スクレイピングを実行する
     *
     * @param fs         財務諸表種別
     * @param document   ドキュメント
     * @param doScraping 財務諸表別処理実行
     */
    private void executeScraping(
            final FinancialStatementEnum fs,
            final Document document,
            final BiConsumer<Company, Pair<File, ScrapingKeywordEntity>> doScraping) {
        final Company company = companySpecification.findCompanyByEdinetCode(document.getEdinetCode())
                .orElseThrow(FundanalyzerRuntimeException::new);
        final File targetDirectory = makeDocumentPath(pathDecode, document.getSubmitDate(), document.getDocumentId());

        try {
            final Pair<File, ScrapingKeywordEntity> targetFile = findTargetFile(targetDirectory, fs);

            doScraping.accept(company, targetFile);

            FundanalyzerLogClient.logLogic(
                    MessageFormat.format(
                            "次のスクレイピング情報を正常に登録しました。\n企業コード:{0}\tEDINETコード:{1}\t財務諸表名:{2}\tファイル名:{3}",
                            company.getCode().orElse("null"),
                            company.getEdinetCode(),
                            fs.getName(),
                            targetFile.getFirst().getPath()
                    ),
                    Category.DOCUMENT,
                    Process.SCRAPING
            );

            documentSpecification.updateFsToDone(document, fs, targetFile.getFirst().getPath());
        } catch (FundanalyzerFileException e) {
            documentSpecification.updateFsToError(document, fs);
            log.warn(
                    "スクレイピング処理の過程でエラー発生しました。スタックトレースを参考に原因を確認してください。" +
                            "\n企業コード:{}\tEDINETコード:{}\t財務諸表名:{}\tファイルパス:{}",
                    company.getCode().orElse("null"),
                    company.getEdinetCode(),
                    fs.getName(),
                    targetDirectory.getPath(),
                    e
            );
        }
    }

    /**
     * 値×単位
     *
     * @param value 値
     * @param unit  単位
     * @return 値
     */
    private Optional<Long> parseValue(final String value, final Unit unit) {
        return parseValue(value).map(l -> l * unit.getValue());
    }

    /**
     * 数値を解析する
     *
     * @param value 数値
     * @return 値
     */
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

    /**
     * ファイルパスを生成する
     *
     * @param prePath    前パス
     * @param targetDate 対象日
     * @return ファイルパス
     */
    private File makeTargetPath(final String prePath, final LocalDate targetDate) {
        return new File(String.format("%s/%d/%s/%s", prePath, targetDate.getYear(), targetDate.getMonth(), targetDate));
    }

    /**
     * ファイルパスを生成する
     *
     * @param prePath    前パス
     * @param targetDate 対象日
     * @param docId      書類ID
     * @return ファイルパス
     */
    private File makeTargetPath(final String prePath, final LocalDate targetDate, final String docId) {
        return new File(String.format("%s/%d/%s/%s/%s", prePath, targetDate.getYear(), targetDate.getMonth(), targetDate, docId));
    }

    /**
     * フォルダパスを生成する
     *
     * @param prePath    前パス
     * @param targetDate 対象日
     * @param docId      書類ID
     * @return フォルダパス
     */
    private File makeDocumentPath(final String prePath, final LocalDate targetDate, final String docId) {
        return new File(String.format("%s/%d/%s/%s/%s/XBRL/PublicDoc", prePath, targetDate.getYear(), targetDate.getMonth(), targetDate, docId));
    }
}
