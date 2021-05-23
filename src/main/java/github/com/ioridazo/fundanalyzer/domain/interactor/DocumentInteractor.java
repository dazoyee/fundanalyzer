package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.DocumentStatus;
import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CompanySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.DocumentSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.EdinetDocumentSpecification;
import github.com.ioridazo.fundanalyzer.domain.usecase.DocumentUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.ScrapingUseCase;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.client.file.FileOperator;
import github.com.ioridazo.fundanalyzer.client.edinet.EdinetClient;
import github.com.ioridazo.fundanalyzer.client.edinet.entity.request.ListRequestParameter;
import github.com.ioridazo.fundanalyzer.client.edinet.entity.request.ListType;
import github.com.ioridazo.fundanalyzer.client.edinet.entity.response.EdinetResponse;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.model.IdInputData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

@Component
public class DocumentInteractor implements DocumentUseCase {

    private final ScrapingUseCase scraping;
    private final CompanySpecification companySpecification;
    private final DocumentSpecification documentSpecification;
    private final EdinetDocumentSpecification edinetDocumentSpecification;
    private final FileOperator fileOperator;
    private final EdinetClient edinetClient;

    @Value("${app.config.target.document-type-code}")
    List<String> targetTypeCodes;

    public DocumentInteractor(
            final ScrapingUseCase scraping,
            final CompanySpecification companySpecification,
            final DocumentSpecification documentSpecification,
            final EdinetDocumentSpecification edinetDocumentSpecification,
            final FileOperator fileOperator,
            final EdinetClient edinetClient) {
        this.companySpecification = companySpecification;
        this.edinetDocumentSpecification = edinetDocumentSpecification;
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
        // 書類リストをデータベースに登録する
        saveEdinetList(inputData);

        // 対象ファイルリスト取得（CompanyCodeがnullではないドキュメントを対象とする）
        final var documentList = documentSpecification.targetList(inputData);

        if (documentList.isEmpty()) {
            FundanalyzerLogClient.logService(
                    MessageFormat.format(
                            "{0}付の処理対象ドキュメントは存在しませんでした。\t書類種別コード:{1}",
                            inputData.getDate(),
                            String.join(",", targetTypeCodes)
                    ),
                    Category.DOCUMENT,
                    Process.EDINET
            );
        } else {
            documentList.parallelStream().forEach(this::scrape);

            FundanalyzerLogClient.logService(
                    MessageFormat.format(
                            "{0}付のドキュメントに対してすべての処理が完了しました。\t書類種別コード:{1}",
                            inputData.getDate(),
                            String.join(",", targetTypeCodes)
                    ),
                    Category.DOCUMENT,
                    Process.EDINET
            );
        }
    }

    /**
     * EDINETリストを保存する
     *
     * @param inputData 提出日
     */
    @Override
    public void saveEdinetList(final DateInputData inputData) {
        if (isPresentEdinet(inputData.getDate())) {
            // 書類が0件ではないときは書類リストを取得してデータベースに登録する
            final EdinetResponse edinetResponse = edinetClient.list(new ListRequestParameter(inputData.getDate(), ListType.GET_LIST));

            // edinet document
            edinetDocumentSpecification.insert(edinetResponse);

            // company
            edinetResponse.getResults().forEach(companySpecification::insertIfNotExist);

            // document
            documentSpecification.insert(inputData.getDate(), edinetResponse);

            FundanalyzerLogClient.logService(
                    MessageFormat.format("データベースへの書類一覧登録作業が正常に終了しました。\t指定ファイル日付:{0}", inputData.getDate()),
                    Category.DOCUMENT,
                    Process.EDINET
            );
        } else {
            FundanalyzerLogClient.logService(
                    MessageFormat.format("データベースへ登録する書類一覧は存在しませんでした。\t指定ファイル日付:{0}", inputData.getDate()),
                    Category.DOCUMENT,
                    Process.EDINET
            );
        }
    }

    /**
     * ドキュメントをスクレイピングする
     *
     * @param inputData 提出日
     */
    @Override
    public void scrape(final DateInputData inputData) {
        final List<Document> targetList = documentSpecification.targetList(inputData);
        if (targetList.isEmpty()) {
            FundanalyzerLogClient.logService(
                    MessageFormat.format(
                            "次の提出日におけるドキュメントはデータベースに存在しませんでした。\t対象提出日:{0}",
                            inputData.getDate()
                    ),
                    Category.DOCUMENT,
                    Process.SCRAPING
            );
        } else {
            targetList.forEach(this::scrape);

            FundanalyzerLogClient.logService(
                    MessageFormat.format(
                            "次の提出日におけるドキュメントに対してスクレイピング処理が終了しました。\t対象提出日:{0}",
                            inputData.getDate()
                    ),
                    Category.DOCUMENT,
                    Process.SCRAPING
            );
        }
    }

    /**
     * ドキュメントをスクレイピングする
     *
     * @param inputData 書類ID
     */
    @Override
    public void scrape(final IdInputData inputData) {
        scrape(documentSpecification.findDocument(inputData));

        FundanalyzerLogClient.logService(
                MessageFormat.format("次のドキュメントに対してスクレイピング処理を正常に終了しました。\t書類ID:{0}", inputData.getId()),
                Category.DOCUMENT,
                Process.SCRAPING
        );
    }

    /**
     * ドキュメントを処理対象外に更新する
     *
     * @param inputData 書類ID
     */
    @Override
    public void removeDocument(final IdInputData inputData) {
        documentSpecification.updateRemoved(inputData.getId());

        FundanalyzerLogClient.logService(
                MessageFormat.format("ドキュメントを処理対象外にしました。\t書類ID:{0}", inputData.getId()),
                Category.DOCUMENT,
                Process.UPDATE
        );
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
        }

        final Document decodedDocument = documentSpecification.findDocument(document.getDocumentId());
        if (DocumentStatus.DONE == decodedDocument.getDecoded()) {
            // スクレイピング
            // 貸借対照表
            if (DocumentStatus.NOT_YET == decodedDocument.getScrapedBs()) {
                scraping.bs(document);
            }
            // 損益計算書
            if (DocumentStatus.NOT_YET == decodedDocument.getScrapedPl()) {
                scraping.pl(document);
            }
            // 株式総数
            if (DocumentStatus.NOT_YET == decodedDocument.getScrapedNumberOfShares()) {
                scraping.ns(document);
            }
        }

        final Document processedDocument = documentSpecification.findDocument(document.getDocumentId());
        // 除外フラグON
        if (List.of(
                processedDocument.getScrapedBs(),
                processedDocument.getScrapedPl(),
                processedDocument.getScrapedNumberOfShares()
        ).stream().allMatch(status -> DocumentStatus.ERROR == status)) {
            documentSpecification.updateRemoved(document);
            FundanalyzerLogClient.logService(
                    MessageFormat.format(
                            "処理ステータスがすべて [9（ERROR）] となったため、除外フラグをONにしました。\t書類ID:{0}",
                            document
                    ),
                    Category.DOCUMENT,
                    Process.EDINET
            );
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
