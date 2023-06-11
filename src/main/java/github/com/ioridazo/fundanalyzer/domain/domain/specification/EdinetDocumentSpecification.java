package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.client.edinet.entity.response.EdinetResponse;
import github.com.ioridazo.fundanalyzer.client.edinet.entity.response.Results;
import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction.EdinetDocumentDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.EdinetDocumentEntity;
import github.com.ioridazo.fundanalyzer.domain.value.EdinetDocument;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seasar.doma.jdbc.UniqueConstraintException;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.NestedRuntimeException;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class EdinetDocumentSpecification {

    private static final String CACHE_KEY_LIMITED_EDINET_DOCUMENT = "limitedEdinetDocument";
    private static final String MESSAGE_EDINET_DOCUMENT = "EDINETに提出された書類";

    private static final Logger log = LogManager.getLogger(EdinetDocumentSpecification.class);

    private final EdinetDocumentDao edinetDocumentDao;

    public EdinetDocumentSpecification(final EdinetDocumentDao edinetDocumentDao) {
        this.edinetDocumentDao = edinetDocumentDao;
    }

    LocalDateTime nowLocalDateTime() {
        return LocalDateTime.now();
    }

    /**
     * EDINETドキュメント情報を取得する
     * <ul>
     *    <li>キャッシュがあるときはキャッシュから取得する<li/>
     *    <li>キャッシュがないときはデータベースから取得する<li/>
     * </>
     *
     * @param documentId 書類ID
     * @return EDINETドキュメント情報
     */
    @Cacheable(CACHE_KEY_LIMITED_EDINET_DOCUMENT)
    public EdinetDocument inquiryLimitedEdinetDocument(final String documentId) {
        return findLimitedEdinetDocument(documentId);
    }

    @CachePut(CACHE_KEY_LIMITED_EDINET_DOCUMENT)
    public EdinetDocument findLimitedEdinetDocument(final String documentId) {
        final EdinetDocument edinetDocument = parsePeriod(documentId);
        final EdinetDocumentEntity entity = edinetDocumentDao.selectByDocId(documentId)
                .orElseThrow(() -> new FundanalyzerNotExistException(MESSAGE_EDINET_DOCUMENT));
        edinetDocument.setDocDescription(entity.getDocDescription().orElse(null));
        edinetDocument.setParentDocId(entity.getParentDocId().orElse(null));
        return edinetDocument;
    }

    /**
     * EDINETドキュメントの件数を取得する
     *
     * @param inputData 提出日
     * @return 件数
     */
    public int count(final DateInputData inputData) {
        return edinetDocumentDao.count(inputData.getDate().toString());
    }

    /**
     * 期間を取得する
     *
     * @param documentId 書類ID
     * @return 期間（EdinetDocument）
     */
    public EdinetDocument parsePeriod(final String documentId) {
        final Optional<EdinetDocumentEntity> edinetDocumentEntity = edinetDocumentDao.selectByDocId(documentId);
        final EdinetDocument edinetDocument = new EdinetDocument();

        if (edinetDocumentEntity.isEmpty()) {
            edinetDocument.setPeriodStart(LocalDate.EPOCH);
            edinetDocument.setPeriodEnd(LocalDate.EPOCH);
            return edinetDocument;
        }

        // periodStart
        if (edinetDocumentEntity.flatMap(EdinetDocumentEntity::getPeriodStart).isPresent()) {
            // period start is present
            edinetDocument.setPeriodStart(LocalDate.parse(
                    edinetDocumentEntity.flatMap(EdinetDocumentEntity::getPeriodStart).orElseThrow()));
        } else if (edinetDocumentEntity.map(EdinetDocumentEntity::getParentDocId).isPresent()) {
            final Optional<EdinetDocumentEntity> parentEdinetDocumentEntity =
                    edinetDocumentDao.selectByDocId(edinetDocumentEntity.flatMap(EdinetDocumentEntity::getParentDocId).orElseThrow());
            if (parentEdinetDocumentEntity.flatMap(EdinetDocumentEntity::getPeriodStart).isPresent()) {
                // parent edinet document is present
                edinetDocument.setPeriodStart(LocalDate.parse(
                        parentEdinetDocumentEntity.flatMap(EdinetDocumentEntity::getPeriodStart).orElseThrow()));
            } else {
                // parent edinet document is null
                edinetDocument.setPeriodStart(LocalDate.EPOCH);
            }
        } else {
            edinetDocument.setPeriodStart(LocalDate.EPOCH);
        }

        // periodEnd
        if (edinetDocumentEntity.flatMap(EdinetDocumentEntity::getPeriodEnd).isPresent()) {
            // period end is present
            edinetDocument.setPeriodEnd(LocalDate.parse(
                    edinetDocumentEntity.flatMap(EdinetDocumentEntity::getPeriodEnd).orElseThrow()));
        } else if (edinetDocumentEntity.map(EdinetDocumentEntity::getParentDocId).isPresent()) {
            final Optional<EdinetDocumentEntity> parentEdinetDocumentEntity =
                    edinetDocumentDao.selectByDocId(edinetDocumentEntity.flatMap(EdinetDocumentEntity::getParentDocId).orElseThrow());
            if (parentEdinetDocumentEntity.flatMap(EdinetDocumentEntity::getPeriodEnd).isPresent()) {
                // parent edinet document is present
                edinetDocument.setPeriodEnd(LocalDate.parse(
                        parentEdinetDocumentEntity.flatMap(EdinetDocumentEntity::getPeriodEnd).orElseThrow()));
            } else {
                // parent edinet document is null
                edinetDocument.setPeriodEnd(LocalDate.EPOCH);
            }
        } else {
            edinetDocument.setPeriodEnd(LocalDate.EPOCH);
        }

        return edinetDocument;
    }

    /**
     * EDINETドキュメントを登録する
     *
     * @param submitDate     提出日
     * @param edinetResponse EDINETレスポンス
     */
    public void insert(final LocalDate submitDate, final EdinetResponse edinetResponse) {
        final List<EdinetDocumentEntity> insertedList = edinetDocumentDao.selectBySubmitDate(submitDate.toString());
        edinetResponse.getResults().stream()
                .filter(results -> insertedList.stream()
                        .map(EdinetDocumentEntity::getDocId)
                        .noneMatch(inserted -> results.getDocId().equals(inserted)))
                .filter(results -> edinetDocumentDao.selectByDocId(results.getDocId()).isEmpty())
                .forEach(this::insert);
    }

    /**
     * EDINETドキュメントを登録する
     *
     * @param results EDINETレスポンス
     */
    private void insert(final Results results) {
        try {
            edinetDocumentDao.insert(EdinetDocumentEntity.of(results, nowLocalDateTime()));
        } catch (NestedRuntimeException e) {
            if (e.contains(UniqueConstraintException.class)) {
                log.debug(FundanalyzerLogClient.toSpecificationLogObject(
                        MessageFormat.format(
                                "一意制約違反のため、データベースへの登録をスキップします。" +
                                        "\tテーブル名:{0}\t書類ID:{1}\tEDINETコード:{2}\t提出者名:{3}\t書類種別コード:{4}",
                                "edinet_document",
                                results.getDocId(),
                                results.getEdinetCode().orElse("null"),
                                results.getFilerName(),
                                results.getDocTypeCode().orElse("null")
                        ),
                        results.getDocId(),
                        results.getEdinetCode().orElse("null"),
                        Category.DOCUMENT,
                        Process.REGISTER
                ));
            } else {
                throw new FundanalyzerRuntimeException("想定外のエラーが発生しました。", e);
            }
        }
    }
}
