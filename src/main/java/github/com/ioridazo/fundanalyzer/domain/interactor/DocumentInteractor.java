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
    @Value("${app.config.scraping.no-company}")
    List<String> noTargetEdinetCodes;
    @Value("${app.config.remove-document.document-type-code}")
    List<String> removeTypeCodes;
    @Value("${app.config.remove-document.company}")
    List<String> removeEdinetCodes;

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

        try {
            // 書類リストをデータベースに登録する
            saveEdinetList(inputData);

            // 対象ファイルリスト取得（CompanyCodeがnullではないドキュメントを対象とする）
            final var documentList = documentSpecification.inquiryTargetDocuments(inputData);

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
                if (documentList.size() > 10) {
                    documentList.parallelStream().forEach(this::scrape);
                } else {
                    documentList.forEach(this::scrape);
                }

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
        } catch (final Exception e) {
            log.error(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format(
                            "{0}付のドキュメントに対して想定外のエラーが発生しました。",
                            inputData.getDate()
                    ),
                    Category.DOCUMENT,
                    Process.SCRAPING
            ), e);
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

        try {
            // EDINETに提出書類の問い合わせ
            final int count = Integer.parseInt(edinetClient.list(new ListRequestParameter(inputData.getDate(), ListType.DEFAULT))
                    .getMetadata().getResultset().getCount());

            if (0 == count) {
                log.info(FundanalyzerLogClient.toInteractorLogObject(
                        MessageFormat.format(
                                "データベースへ登録する書類一覧は存在しませんでした。\t指定ファイル日付:{0}",
                                inputData.getDate()
                        ),
                        Category.DOCUMENT,
                        Process.EDINET,
                        System.currentTimeMillis() - startTime
                ));
            } else {
                if (count == edinetDocumentSpecification.count(inputData)) {
                    log.info(FundanalyzerLogClient.toInteractorLogObject(
                            MessageFormat.format(
                                    "データベースへ登録済みの書類件数と一致するため、登録をスキップしました。\t指定ファイル日付:{0}",
                                    inputData.getDate()
                            ),
                            Category.DOCUMENT,
                            Process.EDINET,
                            System.currentTimeMillis() - startTime
                    ));
                } else {
                    // 書類が0件ではないときは書類リストを取得してデータベースに登録する
                    final EdinetResponse edinetResponse = edinetClient.list(new ListRequestParameter(inputData.getDate(), ListType.GET_LIST));

                    // edinet document
                    edinetDocumentSpecification.insert(inputData.getDate(), edinetResponse);

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
                }
            }
        } catch (final Exception e) {
            log.error(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format(
                            "{0}付のドキュメントに対して想定外のエラーが発生しました。",
                            inputData.getDate()
                    ),
                    Category.DOCUMENT,
                    Process.SCRAPING
            ), e);
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

        try {
            final List<Document> targetList = documentSpecification.inquiryTargetDocuments(inputData);
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
                if (targetList.size() > 10) {
                    targetList.parallelStream().forEach(this::scrape);
                } else {
                    targetList.forEach(this::scrape);
                }

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
        } catch (final Exception e) {
            log.error(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format(
                            "{0}付のドキュメントに対して想定外のエラーが発生しました。",
                            inputData.getDate()
                    ),
                    Category.DOCUMENT,
                    Process.SCRAPING
            ), e);
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

        final Document document = documentSpecification.findDocument(inputData);
        scrape(document);

        log.info(FundanalyzerLogClient.toInteractorLogObject(
                MessageFormat.format(
                        "次のドキュメントに対してスクレイピング処理を正常に終了しました。\t書類ID:{0}",
                        inputData.getId()
                ),
                document.getDocumentId(),
                document.getEdinetCode(),
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
                    companySpecification.findCompanyByEdinetCode(inputData.getEdinetCode())
                            .orElseThrow(() -> {
                                throw new FundanalyzerNotExistException("EDINETコード");
                            }),
                    FinancialStatementEnum.fromId(inputData.getFinancialStatementId()),
                    inputData.getSubjectId(),
                    documentSpecification.findDocument(inputData.getDocumentId()),
                    inputData.getValue(),
                    CreatedType.MANUAL
            );

            log.info(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format(
                            "財務諸表の値を登録しました。\t書類ID:{0}\t財務諸表名:{1}\t科目ID:{2}\t値:{3}",
                            inputData.getDocumentId(),
                            FinancialStatementEnum.fromId(inputData.getFinancialStatementId()).getName(),
                            inputData.getSubjectId(),
                            inputData.getValue()
                    ),
                    inputData.getDocumentId(),
                    inputData.getEdinetCode(),
                    Category.DOCUMENT,
                    Process.REGISTER,
                    System.currentTimeMillis() - startTime
            ));

            return Result.OK;

        } catch (final FundanalyzerNotExistException e) {
            log.warn(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format("企業情報が見つかりませんでした。\tEDINETコード:{0}", inputData.getEdinetCode()),
                    inputData.getDocumentId(),
                    inputData.getEdinetCode(),
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
                    inputData.getDocumentId(),
                    inputData.getEdinetCode(),
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
                    inputData.getId(),
                    documentSpecification.findDocument(inputData).getEdinetCode(),
                    Category.DOCUMENT,
                    Process.UPDATE,
                    System.currentTimeMillis() - startTime
            ));

            return Result.OK;

        } catch (final FundanalyzerRuntimeException e) {
            log.warn(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format(
                            "エラーが発生したため、処理ステータスを完了の更新に失敗しました。\t書類ID:{0}",
                            inputData.getId()
                    ),
                    inputData.getId(),
                    documentSpecification.findDocument(inputData).getEdinetCode(),
                    Category.DOCUMENT,
                    Process.UPDATE,
                    System.currentTimeMillis() - startTime
            ));

            return Result.NG;
        }
    }

    /**
     * 対象期間の更新（存在しない場合）
     *
     * @param inputData 提出日
     */
    @Override
    public void updateDocumentPeriodIfNotExist(final DateInputData inputData) {
        final List<Document> targetList = documentSpecification.noDocumentPeriodList(inputData);
        if (!targetList.isEmpty()) {
            targetList.forEach(document -> {
                final LocalDate periodDocument = documentSpecification.recoverDocumentPeriod(document);
                documentSpecification.updateDocumentPeriod(periodDocument, document);

                log.info(FundanalyzerLogClient.toInteractorLogObject(
                        MessageFormat.format(
                                "分析対象の書類で対象期間が存在しないためリカバリ更新しました。\t書類ID:{0}\t対象期間:{1}",
                                document.getDocumentId(),
                                periodDocument
                        ),
                        document,
                        Category.DOCUMENT,
                        Process.UPDATE
                ));
            });
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
                inputData.getId(),
                documentSpecification.findDocument(inputData).getEdinetCode(),
                Category.DOCUMENT,
                Process.REMOVE,
                System.currentTimeMillis() - startTime
        ));
    }

    /**
     * 除外条件に合致するドキュメントを処理対象外に更新する
     *
     * @param inputData 提出日
     */
    @Override
    public void removeDocument(final DateInputData inputData) {
        // 会社単位
        documentSpecification.inquiryTargetDocuments(inputData).stream()
                .filter(document -> {
                    // 特定の会社を除外する
                    if (noTargetEdinetCodes.stream().anyMatch(noTarget -> document.getEdinetCode().equals(noTarget))) {
                        return true;
                    } else {
                        // 四半期報告書において特定の会社を除外する
                        return removeTypeCodes.stream().anyMatch(noTarget -> document.getDocumentTypeCode().toValue().equals(noTarget))
                                && removeEdinetCodes.stream().anyMatch(noTarget -> document.getEdinetCode().equals(noTarget));
                    }
                })
                .forEach(document -> {
                    documentSpecification.updateRemoved(document);
                    log.info(FundanalyzerLogClient.toInteractorLogObject(
                            MessageFormat.format(
                                    "ドキュメントを処理対象外にしました。\t書類ID:{0}\tEDINETコード:{1}",
                                    document.getDocumentId(),
                                    document.getEdinetCode()
                            ),
                            document,
                            Category.DOCUMENT,
                            Process.REMOVE
                    ));
                });

        // 四半期報告書単位
        documentSpecification.removeTargetList(inputData).stream()
                // download is done
                .filter(document -> DocumentStatus.DONE.equals(document.getDownloaded()))
                // decode is done
                .filter(document -> DocumentStatus.DONE.equals(document.getDecoded()))
                // ns is not null
                .filter(document -> !DocumentStatus.NOT_YET.equals(document.getScrapedNumberOfShares()))
                // bs path is null
                .filter(document -> document.getBsDocumentPath().isEmpty())
                // pl path is null
                .filter(document -> document.getPlDocumentPath().isEmpty())
                .forEach(document -> {
                    documentSpecification.updateRemoved(document);
                    log.info(FundanalyzerLogClient.toInteractorLogObject(
                            MessageFormat.format(
                                    "ドキュメントを処理対象外にしました。\t書類ID:{0}\t書類種別コード:{1}",
                                    document.getDocumentId(),
                                    document.getDocumentTypeCode().getName()
                            ),
                            document,
                            Category.DOCUMENT,
                            Process.REMOVE
                    ));
                });
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
            log.info(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format(
                            "書類のダウンロードが完了していません。詳細を確認してください。\t書類ID:{0}",
                            document.getDocumentId()
                    ),
                    document,
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
                            document.getDocumentId()
                    ),
                    document,
                    Category.DOCUMENT,
                    Process.SCRAPING
            ));
        }
    }
}
