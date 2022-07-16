package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.client.edinet.entity.response.EdinetResponse;
import github.com.ioridazo.fundanalyzer.client.edinet.entity.response.Results;
import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.DocumentEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.DocumentStatus;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.domain.value.EdinetDocument;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerSqlForeignKeyException;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.model.IdInputData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seasar.doma.jdbc.NonUniqueResultException;
import org.seasar.doma.jdbc.UniqueConstraintException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedRuntimeException;
import org.springframework.data.util.Pair;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.sql.SQLIntegrityConstraintViolationException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class DocumentSpecification {

    private static final Logger log = LogManager.getLogger(DocumentSpecification.class);

    private final DocumentDao documentDao;
    private final IndustrySpecification industrySpecification;
    private final CompanySpecification companySpecification;
    private final EdinetDocumentSpecification edinetDocumentSpecification;
    private final AnalysisResultSpecification analysisResultSpecification;

    @Value("${app.config.scraping.document-type-code}")
    List<String> targetTypeCodes;
    @Value("${app.config.remove-document.document-type-code}")
    List<String> targetTypeCodesToRemove;

    public DocumentSpecification(
            final DocumentDao documentDao,
            final IndustrySpecification industrySpecification,
            final CompanySpecification companySpecification,
            final EdinetDocumentSpecification edinetDocumentSpecification,
            final AnalysisResultSpecification analysisResultSpecification) {
        this.documentDao = documentDao;
        this.industrySpecification = industrySpecification;
        this.companySpecification = companySpecification;
        this.edinetDocumentSpecification = edinetDocumentSpecification;
        this.analysisResultSpecification = analysisResultSpecification;
    }

    LocalDateTime nowLocalDateTime() {
        return LocalDateTime.now();
    }

    /**
     * ドキュメント情報を取得する
     *
     * @param documentId 書類ID
     * @return ドキュメント情報
     */
    public Document findDocument(final String documentId) {
        return Document.of(
                documentDao.selectByDocumentId(documentId),
                edinetDocumentSpecification.inquiryLimitedEdinetDocument(documentId)
        );
    }

    /**
     * ドキュメント情報を取得する
     *
     * @param inputData 書類ID
     * @return ドキュメント情報
     */
    public Document findDocument(final IdInputData inputData) {
        return Document.of(
                documentDao.selectByDocumentId(inputData.getId()),
                edinetDocumentSpecification.inquiryLimitedEdinetDocument(inputData.getId())
        );
    }

    /**
     * 直近提出日のドキュメント情報を取得する
     *
     * @param company 企業情報
     * @return ドキュメント情報
     */
    public Optional<Document> latestDocument(final Company company) {
        return documentDao.maxSubmitDateByEdinetCodeAndType(company.getEdinetCode(), targetTypeCodes).stream()
                .map(entity -> Document.of(entity, edinetDocumentSpecification.inquiryLimitedEdinetDocument(entity.getDocumentId())))
                .findFirst();
    }

    /**
     * ドキュメント情報リストを取得する
     *
     * @param inputData 提出日
     * @return ドキュメント情報リスト
     */
    public List<Document> documentList(final DateInputData inputData) {
        return documentDao.selectByTypeAndSubmitDate(targetTypeCodes, inputData.getDate()).stream()
                .filter(entity -> entity.getEdinetCode().isPresent())
                .map(entity -> Document.of(entity, edinetDocumentSpecification.inquiryLimitedEdinetDocument(entity.getDocumentId())))
                .collect(Collectors.toList());
    }

    /**
     * 処理対象となるドキュメント情報リストを取得する
     *
     * @param inputData 提出日
     * @return ドキュメント情報リスト
     */
    public List<Document> targetList(final DateInputData inputData) {
        return documentDao.selectByTypeAndSubmitDate(targetTypeCodes, inputData.getDate()).stream()
                .filter(entity -> entity.getEdinetCode().isPresent())
                .map(entity -> Document.of(entity, edinetDocumentSpecification.inquiryLimitedEdinetDocument(entity.getDocumentId())))
                .filter(this::isTarget)
                .filter(Document::isTarget)
                .collect(Collectors.toList());
    }

    /**
     * 分析対象となるドキュメント情報リストを取得する
     *
     * @param inputData 提出日
     * @return ドキュメント情報リスト
     */
    public List<Document> analysisTargetList(final DateInputData inputData) {
        return targetList(inputData).stream()
                // all match status done
                .filter(this::allStatusDone)
                // documentPeriod is present
                .filter(document -> document.getDocumentPeriod().isPresent())
                // only not analyze
                .filter(document -> !analysisResultSpecification.isAnalyzed(document))
                .collect(Collectors.toList());
    }

    public List<Document> noDocumentPeriodList(final DateInputData inputData) {
        return documentDao.selectByTypeAndNoPeriodAndSubmitDate(targetTypeCodes, inputData.getDate()).stream()
                .filter(entity -> entity.getEdinetCode().isPresent())
                .map(entity -> Document.of(entity, edinetDocumentSpecification.inquiryLimitedEdinetDocument(entity.getDocumentId())))
                .filter(Document::isTarget)
                .collect(Collectors.toList());
    }

    /**
     * 除外対象となるドキュメント情報リストを取得する
     *
     * @param inputData 提出日
     * @return ドキュメント情報リスト
     */
    public List<Document> removeTargetList(final DateInputData inputData) {
        return documentDao.selectByTypeAndSubmitDate(targetTypeCodesToRemove, inputData.getDate()).stream()
                .filter(entity -> entity.getEdinetCode().isPresent())
                .map(entity -> Document.of(entity, edinetDocumentSpecification.inquiryLimitedEdinetDocument(entity.getDocumentId())))
                .filter(this::isTarget)
                .filter(Document::isTarget)
                .collect(Collectors.toList());
    }

    /**
     * すべての提出日を取得する
     *
     * @return 提出日リスト
     */
    public List<LocalDate> submitDateList() {
        return documentDao.selectDistinctSubmitDateByDocumentTypeCode(targetTypeCodes);
    }

    /**
     * スクレイピング処理状況に関するドキュメント情報リストを取得する
     *
     * @param targetList 処理対象となるドキュメント情報リスト
     * @return スクレイピング処理済リスト/スクレイピング未処理リスト
     */
    public Pair<List<Document>, List<Document>> extractScrapedList(final List<Document> targetList) {
        final List<Document> scrapedList = new ArrayList<>();
        final List<Document> notScrapedList = new ArrayList<>();

        targetList.forEach(document -> {
            if (allStatusDone(document)) {
                scrapedList.add(document);
            } else {
                notScrapedList.add(document);
            }
        });

        return Pair.of(scrapedList, notScrapedList);
    }

    /**
     * 分析状況に関するドキュメント情報リストを取得する
     *
     * @param scrapedList 分析対象となるドキュメント情報リスト
     * @return 分析済リスト/未分析リスト
     */
    public Pair<List<Document>, List<Document>> extractAnalyzedList(final List<Document> scrapedList) {
        final List<Document> analyzedList = new ArrayList<>();
        final List<Document> notAnalyzedList = new ArrayList<>();

        scrapedList.forEach(document -> {
            if (analysisResultSpecification.isAnalyzed(document)) {
                analyzedList.add(document);
            } else {
                notAnalyzedList.add(document);
            }
        });

        return Pair.of(analyzedList, notAnalyzedList);
    }

    /**
     * ドキュメント情報を登録する
     *
     * @param submitDate     提出日
     * @param edinetResponse EDINETレスポンス
     */
    public void insert(final LocalDate submitDate, final EdinetResponse edinetResponse) {
        final List<DocumentEntity> insertedList = documentDao.selectBySubmitDate(submitDate);
        edinetResponse.getResults().stream()
                .filter(results -> insertedList.stream()
                        .map(DocumentEntity::getDocumentId)
                        .noneMatch(inserted -> results.getDocId().equals(inserted)))
                .forEach(results -> insert(submitDate, results));
    }

    /**
     * ドキュメント情報を登録する
     *
     * @param submitDate 提出日
     * @param results    EDINETレスポンス
     */
    private void insert(final LocalDate submitDate, final Results results) {
        try {
            documentDao.insert(DocumentEntity.of(submitDate, parseDocumentPeriod(results).orElse(null), results, nowLocalDateTime()));
        } catch (NestedRuntimeException e) {
            if (e.contains(UniqueConstraintException.class)) {
                log.debug(FundanalyzerLogClient.toSpecificationLogObject(
                        MessageFormat.format(
                                "一意制約違反のため、データベースへの登録をスキップします。" +
                                        "\tテーブル名:{0}\t書類ID:{1}\tEDINETコード:{2}\t提出者名:{3}\t書類種別コード:{4}",
                                "document",
                                results.getDocId(),
                                results.getEdinetCode(),
                                results.getFilerName(),
                                results.getDocTypeCode()
                        ),
                        Category.DOCUMENT,
                        Process.REGISTER
                ));
            } else if (e.contains(SQLIntegrityConstraintViolationException.class)) {
                log.error(FundanalyzerLogClient.toSpecificationLogObject(
                        "参照整合性制約違反が発生しました。スタックトレースを参考に原因を確認してください。",
                        Category.DOCUMENT,
                        Process.REGISTER
                ), e.getRootCause());
                throw new FundanalyzerSqlForeignKeyException(e);
            } else {
                throw new FundanalyzerRuntimeException("想定外のエラーが発生しました。", e);
            }
        }
    }

    /**
     * ダウンロード/デコード処理を完了に更新する
     *
     * @param document ドキュメント情報
     */
    public void updateStoreToDone(final Document document) {
        documentDao.update(DocumentEntity.ofUpdateStoreToDone(document, nowLocalDateTime()));
    }

    /**
     * ダウンロード処理を完了に更新する
     *
     * @param document ドキュメント情報
     */
    public void updateDownloadToDone(final Document document) {
        documentDao.update(DocumentEntity.ofUpdateDownloadToDone(document, nowLocalDateTime()));
    }

    /**
     * ダウンロード処理をエラーに更新する
     *
     * @param document ドキュメント情報
     */
    public void updateDownloadToError(final Document document) {
        documentDao.update(DocumentEntity.ofUpdateDownloadToError(document, nowLocalDateTime()));
    }

    /**
     * デコード処理を完了に更新する
     *
     * @param document ドキュメント情報
     */
    public void updateDecodeToDone(final Document document) {
        documentDao.update(DocumentEntity.ofUpdateDecodeToDone(document, nowLocalDateTime()));
    }

    /**
     * デコード処理をエラーに更新する
     *
     * @param document ドキュメント情報
     */
    public void updateDecodeToError(final Document document) {
        documentDao.update(DocumentEntity.ofUpdateDecodeToError(document, nowLocalDateTime()));
    }

    /**
     * 財務諸表毎に処理を完了に更新する
     *
     * @param document ドキュメント情報
     * @param fs       財務諸表種別
     * @param path     ドキュメントパス
     */
    public void updateFsToDone(final Document document, final FinancialStatementEnum fs, final String path) {
        documentDao.update(DocumentEntity.ofUpdateSwitchFs(
                fs,
                document.getDocumentId(),
                DocumentStatus.DONE,
                path,
                nowLocalDateTime()
        ));
    }

    /**
     * 財務諸表毎に処理をエラーに更新する
     *
     * @param document ドキュメント情報
     * @param fs       財務諸表種別
     */
    public void updateFsToError(final Document document, final FinancialStatementEnum fs) {
        documentDao.update(DocumentEntity.ofUpdateSwitchFs(
                fs,
                document.getDocumentId(),
                DocumentStatus.ERROR,
                null,
                nowLocalDateTime()
        ));
    }

    /**
     * 財務諸表毎に処理を処理途中に更新する
     *
     * @param document ドキュメント情報
     * @param fs       財務諸表種別
     */
    public void updateFsToHalfWay(final Document document, final FinancialStatementEnum fs) {
        try {
            switch (fs) {
                case BALANCE_SHEET:
                    if (DocumentStatus.DONE == document.getScrapedBs()) {
                        documentDao.update(DocumentEntity.ofUpdateSwitchFs(
                                fs,
                                document.getDocumentId(),
                                DocumentStatus.HALF_WAY,
                                null,
                                nowLocalDateTime()
                        ));
                    }
                    break;
                case PROFIT_AND_LESS_STATEMENT:
                    if (DocumentStatus.DONE == document.getScrapedPl()) {
                        documentDao.update(DocumentEntity.ofUpdateSwitchFs(
                                fs,
                                document.getDocumentId(),
                                DocumentStatus.HALF_WAY,
                                null,
                                nowLocalDateTime()
                        ));
                    }
                    break;
                case TOTAL_NUMBER_OF_SHARES:
                    if (DocumentStatus.DONE == document.getScrapedNumberOfShares()) {
                        documentDao.update(DocumentEntity.ofUpdateSwitchFs(
                                fs,
                                document.getDocumentId(),
                                DocumentStatus.HALF_WAY,
                                null,
                                nowLocalDateTime()
                        ));
                    }
                    break;
                default:
                    throw new FundanalyzerRuntimeException();
            }
        } catch (NestedRuntimeException e) {
            if (e.contains(NonUniqueResultException.class)) {
                log.warn(FundanalyzerLogClient.toSpecificationLogObject(
                        MessageFormat.format(
                                "期待値1件に対し、複数のドキュメントが見つかりました。次の項目を確認してください。" +
                                        "\t会社コード:{0}\t書類ID:{1}\t書類種別コード:{2}\t提出日:{3}\t対象年:{4}",
                                companySpecification.findCompanyByEdinetCode(document.getEdinetCode()).map(Company::getCode).orElse("null"),
                                document.getDocumentId(),
                                document.getDocumentTypeCode().toValue(),
                                document.getSubmitDate(),
                                document.getDocumentPeriod().map(String::valueOf).orElse("null")
                        ),
                        document,
                        Category.DOCUMENT,
                        Process.UPDATE
                ))
                ;
            } else {
                log.error("想定外のエラーが発生しました。", e);
            }
        }
    }

    /**
     * すべての処理ステータスを完了に更新する
     *
     * @param documentId 書類ID
     */
    public void updateAllDone(final String documentId) {
        documentDao.update(DocumentEntity.ofUpdateAllDone(documentId, nowLocalDateTime()));
    }

    /**
     * 対象期間を更新する
     *
     * @param periodDocument 対象期間
     * @param document       ドキュメント
     */
    public void updateDocumentPeriod(final LocalDate periodDocument, final Document document) {
        documentDao.update(DocumentEntity.ofUpdateDocumentPeriod(
                document.getDocumentId(), periodDocument, nowLocalDateTime()));
    }

    /**
     * 処理対象から除外する
     *
     * @param documentId 書類ID
     */
    public void updateRemoved(final String documentId) {
        updateRemoved(findDocument(documentId));
    }

    /**
     * 処理対象から除外する
     *
     * @param document ドキュメント情報
     */
    public void updateRemoved(final Document document) {
        documentDao.update(DocumentEntity.ofUpdateRemoved(document, nowLocalDateTime()));
    }

    /**
     * すべての処理が完了したかどうか
     *
     * @param document ドキュメント情報
     * @return boolean
     */
    public boolean allStatusDone(final Document document) {
        return Stream.of(
                document.getScrapedBs(),
                document.getScrapedPl(),
                document.getScrapedNumberOfShares()
        ).allMatch(DocumentStatus.DONE::equals);
    }

    /**
     * 対象期間が存在していないドキュメントの期間を取得する
     *
     * @param document ドキュメント
     * @return 対象期間
     */
    public LocalDate recoverDocumentPeriod(final Document document) {
        if (document.getDocumentPeriod().isPresent()) {
            return document.getDocumentPeriod().orElseThrow();
        } else {
            final EdinetDocument edinetDocument = edinetDocumentSpecification.inquiryLimitedEdinetDocument(document.getDocumentId());
            return parseDocumentPeriod(
                    edinetDocument.getPeriodEnd().map(LocalDate::toString).orElse(null),
                    edinetDocument.getParentDocId().orElse(null)
            );
        }
    }

    /**
     * 対象期間を取得する
     *
     * @param results EDINETレスポンス
     * @return 期間
     */
    Optional<LocalDate> parseDocumentPeriod(final Results results) {
        final boolean anyMatchTargetTypes = results.getDocTypeCode()
                .filter(docTypeCode -> targetTypeCodes.contains(docTypeCode))
                .isPresent();

        if (anyMatchTargetTypes) {
            // target
            return Optional.of(parseDocumentPeriod(
                    results.getPeriodEnd().orElse(null),
                    results.getParentDocID().orElse(null)
            ));
        } else {
            // no target
            return Optional.empty();
        }
    }

    /**
     * 対象期間をパースする
     *
     * @param periodEnd   期間（至）
     * @param parentDocId 親書類管理番号
     * @return 対象期間
     */
    private LocalDate parseDocumentPeriod(@Nullable final String periodEnd, @Nullable final String parentDocId) {
        if (Objects.nonNull(periodEnd)) {
            // period end is present
            return LocalDate.of(Integer.parseInt(periodEnd.substring(0, 4)), 1, 1);
        } else {
            if (Objects.nonNull(parentDocId)) {
                final DocumentEntity documentEntity = documentDao.selectByDocumentId(parentDocId);
                if (Objects.nonNull(documentEntity) && documentEntity.getDocumentPeriod().isPresent()) {
                    // parent document is present
                    return documentEntity.getDocumentPeriod().orElseThrow();
                } else {
                    // parent document is null
                    return LocalDate.EPOCH;
                }
            } else {
                // period document is null
                return LocalDate.EPOCH;
            }
        }
    }

    /**
     * 処理対象かどうか
     *
     * @param document ドキュメント情報
     * @return boolean
     */
    private boolean isTarget(final Document document) {
        return companySpecification.findCompanyByEdinetCode(document.getEdinetCode()).stream()
                .anyMatch(company -> industrySpecification.isTarget(company.getIndustryId()));
    }
}
