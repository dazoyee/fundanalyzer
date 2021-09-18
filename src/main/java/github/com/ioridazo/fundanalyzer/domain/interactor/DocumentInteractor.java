package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.client.edinet.EdinetClient;
import github.com.ioridazo.fundanalyzer.client.edinet.entity.request.ListRequestParameter;
import github.com.ioridazo.fundanalyzer.client.edinet.entity.request.ListType;
import github.com.ioridazo.fundanalyzer.client.edinet.entity.response.EdinetResponse;
import github.com.ioridazo.fundanalyzer.client.file.FileOperator;
import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.CreatedType;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.DocumentStatus;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CompanySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.DocumentSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.EdinetDocumentSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.FinancialStatementSpecification;
import github.com.ioridazo.fundanalyzer.domain.usecase.DocumentUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.ScrapingUseCase;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.domain.value.Result;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.model.FinancialStatementInputData;
import github.com.ioridazo.fundanalyzer.web.model.IdInputData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

@Component
public class DocumentInteractor implements DocumentUseCase {

    private static final Logger log = LogManager.getLogger(DocumentInteractor.class);

    private final ScrapingUseCase scraping;
    private final CompanySpecification companySpecification;
    private final DocumentSpecification documentSpecification;
    private final EdinetDocumentSpecification edinetDocumentSpecification;
    private final FinancialStatementSpecification financialStatementSpecification;
    private final FileOperator fileOperator;
    private final EdinetClient edinetClient;

    @Value("${app.config.scraping.document-type-code}")
    List<String> targetTypeCodes;

    public DocumentInteractor(
            final ScrapingUseCase scraping,
            final CompanySpecification companySpecification,
            final DocumentSpecification documentSpecification,
            final EdinetDocumentSpecification edinetDocumentSpecification,
            final FinancialStatementSpecification financialStatementSpecification,
            final FileOperator fileOperator,
            final EdinetClient edinetClient) {
        this.companySpecification = companySpecification;
        this.edinetDocumentSpecification = edinetDocumentSpecification;
        this.financialStatementSpecification = financialStatementSpecification;
        this.fileOperator = fileOperator;
        this.scraping = scraping;
        this.documentSpecification = documentSpecification;
        this.edinetClient = edinetClient;
    }

    /**
     * ドキュメントを取得してスクレイピングする
     *
     * @param inputData 提出日
     */
    @Override
    public void allProcess(final DateInputData inputData) {
        final long startTime = System.currentTimeMillis();

        // 書類リストをデータベースに登録する
        saveEdinetList(inputData);

        // 対象ファイルリスト取得（CompanyCodeがnullではないドキュメントを対象とする）
        final var documentList = documentSpecification.targetList(inputData);

        if (documentList.isEmpty()) {
            log.info(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format(
                            "{0}付の処理対象ドキュメントは存在しませんでした。\t書類種別コード:{1}",
                            inputData.getDate(),
                            String.join(",", targetTypeCodes)
                    ),
                    Category.DOCUMENT,
                    Process.EDINET,
                    System.currentTimeMillis() - startTime
            ));
        } else {
            documentList.forEach(this::scrape);

            log.info(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format(
                            "{0}付のドキュメントに対してすべての処理が完了しました。\t書類種別コード:{1}",
                            inputData.getDate(),
                            String.join(",", targetTypeCodes)
                    ),
                    Category.DOCUMENT,
                    Process.SCRAPING,
                    System.currentTimeMillis() - startTime
            ));
        }
    }

    /**
     * EDINETリストを保存する
     *
     * @param inputData 提出日
     */
    @Override
    public void saveEdinetList(final DateInputData inputData) {
        final long startTime = System.currentTimeMillis();

        if (isPresentEdinet(inputData.getDate())) {
            // 書類が0件ではないときは書類リストを取得してデータベースに登録する
            final EdinetResponse edinetResponse = edinetClient.list(new ListRequestParameter(inputData.getDate(), ListType.GET_LIST));

            // edinet document
            edinetDocumentSpecification.insert(edinetResponse);

            // company
            edinetResponse.getResults().forEach(companySpecification::insertIfNotExist);

            // document
            documentSpecification.insert(inputData.getDate(), edinetResponse);

            log.info(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format(
                            "データベースへの書類一覧登録作業が正常に終了しました。\t指定ファイル日付:{0}",
                            inputData.getDate()
                    ),
                    Category.DOCUMENT,
                    Process.EDINET,
                    System.currentTimeMillis() - startTime
            ));
        } else {
            log.info(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format(
                            "データベースへ登録する書類一覧は存在しませんでした。\t指定ファイル日付:{0}",
                            inputData.getDate()
                    ),
                    Category.DOCUMENT,
                    Process.EDINET,
                    System.currentTimeMillis() - startTime
            ));
        }
    }

    /**
     * ドキュメントをスクレイピングする
     *
     * @param inputData 提出日
     */
    @Override
    public void scrape(final DateInputData inputData) {
        final long startTime = System.currentTimeMillis();

        final List<Document> targetList = documentSpecification.targetList(inputData);
        if (targetList.isEmpty()) {
            log.info(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format(
                            "次の提出日におけるドキュメントはデータベースに存在しませんでした。\t対象提出日:{0}",
                            inputData.getDate()
                    ),
                    Category.DOCUMENT,
                    Process.SCRAPING,
                    System.currentTimeMillis() - startTime
            ));
        } else {
            targetList.forEach(this::scrape);

            log.info(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format(
                            "次の提出日におけるドキュメントに対してスクレイピング処理が終了しました。\t対象提出日:{0}",
                            inputData.getDate()
                    ),
                    Category.DOCUMENT,
                    Process.SCRAPING,
                    System.currentTimeMillis() - startTime
            ));
        }
    }

    /**
     * ドキュメントをスクレイピングする
     *
     * @param inputData 書類ID
     */
    @Override
    public void scrape(final IdInputData inputData) {
        final long startTime = System.currentTimeMillis();

        scrape(documentSpecification.findDocument(inputData));

        log.info(FundanalyzerLogClient.toInteractorLogObject(
                MessageFormat.format(
                        "次のドキュメントに対してスクレイピング処理を正常に終了しました。\t書類ID:{0}",
                        inputData.getId()
                ),
                Category.DOCUMENT,
                Process.SCRAPING,
                System.currentTimeMillis() - startTime
        ));
    }

    /**
     * 財務諸表の値を登録する
     *
     * @param inputData 財務諸表の登録情報
     * @return 処理結果
     */
    @Override
    public Result registerFinancialStatementValue(final FinancialStatementInputData inputData) {
        final long startTime = System.currentTimeMillis();

        try {
            financialStatementSpecification.insert(
                    companySpecification.findCompanyByEdinetCode(inputData.getEdinetCode()).orElseThrow(FundanalyzerNotExistException::new),
                    FinancialStatementEnum.fromValue(inputData.getFinancialStatementId()),
                    inputData.getSubjectId(),
                    documentSpecification.findDocument(inputData.getDocumentId()),
                    inputData.getValue(),
                    CreatedType.MANUAL
            );

            log.info(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format(
                            "財務諸表の値を登録しました。\t書類ID:{0}\t財務諸表名:{1}\t科目ID:{2}\t値:{3}",
                            inputData.getDocumentId(),
                            FinancialStatementEnum.fromValue(inputData.getFinancialStatementId()).getName(),
                            inputData.getSubjectId(),
                            inputData.getValue()
                    ),
                    Category.DOCUMENT,
                    Process.REGISTER,
                    System.currentTimeMillis() - startTime
            ));

            return Result.OK;

        } catch (final FundanalyzerNotExistException e) {
            log.warn(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format("企業情報が見つかりませんでした。\tEDINETコード:{0}", inputData.getEdinetCode()),
                    Category.DOCUMENT,
                    Process.REGISTER,
                    System.currentTimeMillis() - startTime
            ));

            return Result.NG;
        } catch (final FundanalyzerRuntimeException e) {
            log.warn(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format(
                            "エラーが発生したため、財務諸表の値を登録できませんでした。詳細を確認してください。\t{0}",
                            inputData
                    ),
                    Category.DOCUMENT,
                    Process.REGISTER,
                    System.currentTimeMillis() - startTime
            ));

            return Result.NG;
        }
    }

    /**
     * 処理ステータスを更新する
     *
     * @param inputData 書類ID
     * @return 処理結果
     */
    @Override
    public Result updateAllDoneStatus(final IdInputData inputData) {
        final long startTime = System.currentTimeMillis();
        try {
            documentSpecification.updateAllDone(inputData.getId());

            log.info(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format("すべての処理ステータスを完了に更新しました。\t書類ID:{0}", inputData.getId()),
                    Category.DOCUMENT,
                    Process.UPDATE,
                    System.currentTimeMillis() - startTime
            ));

            return Result.OK;

        } catch (final FundanalyzerRuntimeException e) {
            log.warn(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format(
                            "エラーが発生したため、処理ステータスを完了の更新に失敗しました。。\t書類ID:{0}",
                            inputData.getId()
                    ),
                    Category.DOCUMENT,
                    Process.UPDATE,
                    System.currentTimeMillis() - startTime
            ));

            return Result.NG;
        }
    }

    /**
     * ドキュメントを処理対象外に更新する
     *
     * @param inputData 書類ID
     */
    @Override
    public void removeDocument(final IdInputData inputData) {
        final long startTime = System.currentTimeMillis();

        documentSpecification.updateRemoved(inputData.getId());

        log.info(FundanalyzerLogClient.toInteractorLogObject(
                MessageFormat.format("ドキュメントを処理対象外にしました。\t書類ID:{0}", inputData.getId()),
                Category.DOCUMENT,
                Process.REMOVE,
                System.currentTimeMillis() - startTime
        ));
    }

    /**
     * ドキュメントをスクレイピングする
     *
     * @param document ドキュメント
     */
    void scrape(final Document document) {
        // 書類取得
        if (DocumentStatus.NOT_YET == document.getDownloaded()) {
            final boolean isPresent = fileOperator.findDecodedFile(document.getSubmitDate()).stream()
                    .anyMatch(folderList -> folderList.stream().anyMatch(document.getDocumentId()::equals));

            if (isPresent) {
                documentSpecification.updateStoreToDone(document);
            } else {
                // ファイル取得
                scraping.download(document);
            }
        } else if (DocumentStatus.ERROR == document.getDownloaded()) {
            log.warn(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format(
                            "書類のダウンロードが完了していません。詳細を確認してください。\t書類ID:{0}",
                            document.getDocumentId()
                    ),
                    Category.DOCUMENT,
                    Process.DOWNLOAD
            ));
        }

        final Document decodedDocument = documentSpecification.findDocument(document.getDocumentId());
        if (DocumentStatus.DONE == decodedDocument.getDecoded()) {
            // スクレイピング
            // 貸借対照表
            if (DocumentStatus.DONE != decodedDocument.getScrapedBs()) {
                scraping.bs(document);
            }
            // 損益計算書
            if (DocumentStatus.DONE != decodedDocument.getScrapedPl()) {
                scraping.pl(document);
            }
            // 株式総数
            if (DocumentStatus.DONE != decodedDocument.getScrapedNumberOfShares()) {
                scraping.ns(document);
            }
        }

        final Document processedDocument = documentSpecification.findDocument(document.getDocumentId());
        // 除外フラグON
        if (Stream.of(
                processedDocument.getScrapedBs(),
                processedDocument.getScrapedPl(),
                processedDocument.getScrapedNumberOfShares()
        ).allMatch(status -> DocumentStatus.ERROR == status)) {
            documentSpecification.updateRemoved(document);
            log.info(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format(
                            "処理ステータスがすべて [9（ERROR）] となったため、除外フラグをONにしました。\t書類ID:{0}",
                            document
                    ),
                    Category.DOCUMENT,
                    Process.SCRAPING
            ));
        }
    }

    /**
     * EDINETに書類有無を問い合わせる
     *
     * @param submitDate 提出日
     * @return boolean
     */
    boolean isPresentEdinet(final LocalDate submitDate) {
        return Stream.of(submitDate)
                // EDINETに提出書類の問い合わせ
                .map(d -> edinetClient.list(new ListRequestParameter(d, ListType.DEFAULT)))
                .map(edinetResponse -> edinetResponse.getMetadata().getResultset().getCount())
                .anyMatch(c -> !"0".equals(c));
    }
}
