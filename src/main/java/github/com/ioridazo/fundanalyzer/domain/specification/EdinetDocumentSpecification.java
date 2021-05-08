package github.com.ioridazo.fundanalyzer.domain.specification;

import github.com.ioridazo.fundanalyzer.domain.dao.transaction.EdinetDocumentDao;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.EdinetDocumentEntity;
import github.com.ioridazo.fundanalyzer.domain.value.EdinetDocument;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import github.com.ioridazo.fundanalyzer.proxy.edinet.entity.response.EdinetResponse;
import github.com.ioridazo.fundanalyzer.proxy.edinet.entity.response.Results;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seasar.doma.jdbc.UniqueConstraintException;
import org.springframework.core.NestedRuntimeException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Component
public class EdinetDocumentSpecification {

    private static final Logger log = LogManager.getLogger(EdinetDocumentSpecification.class);

    private final EdinetDocumentDao edinetDocumentDao;

    public EdinetDocumentSpecification(final EdinetDocumentDao edinetDocumentDao) {
        this.edinetDocumentDao = edinetDocumentDao;
    }

    LocalDateTime nowLocalDateTime() {
        return LocalDateTime.now();
    }

    /**
     * 期間を取得する
     *
     * @param documentId 書類ID
     * @return 期間（EdinetDocument）
     */
    public EdinetDocument parsePeriod(final String documentId) {
        final EdinetDocumentEntity edinetDocumentEntity = edinetDocumentDao.selectByDocId(documentId);
        final EdinetDocument edinetDocument = new EdinetDocument();

        // periodStart
        if (edinetDocumentEntity.getPeriodStart().isPresent()) {
            // period start is present
            edinetDocument.setPeriodStart(LocalDate.parse(edinetDocumentEntity.getPeriodStart().orElseThrow()));
        } else if (Objects.nonNull(edinetDocumentEntity.getParentDocId())) {
            final EdinetDocumentEntity parentEdinetDocumentEntity = edinetDocumentDao.selectByDocId(edinetDocumentEntity.getParentDocId());
            if (Objects.nonNull(parentEdinetDocumentEntity)) {
                // parent edinet document is present
                edinetDocument.setPeriodStart(LocalDate.parse(parentEdinetDocumentEntity.getPeriodStart().orElseThrow()));
            } else {
                // parent edinet document is null
                edinetDocument.setPeriodStart(LocalDate.EPOCH);
            }
        } else {
            edinetDocument.setPeriodStart(LocalDate.EPOCH);
        }

        // periodEnd
        if (edinetDocumentEntity.getPeriodEnd().isPresent()) {
            // period end is present
            edinetDocument.setPeriodEnd(LocalDate.parse(edinetDocumentEntity.getPeriodEnd().orElseThrow()));
        } else if (Objects.nonNull(edinetDocumentEntity.getParentDocId())) {
            final EdinetDocumentEntity parentEdinetDocumentEntity = edinetDocumentDao.selectByDocId(edinetDocumentEntity.getParentDocId());
            if (Objects.nonNull(parentEdinetDocumentEntity)) {
                // parent edinet document is present
                edinetDocument.setPeriodEnd(LocalDate.parse(parentEdinetDocumentEntity.getPeriodEnd().orElseThrow()));
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
     * @param edinetResponse EDINETレスポンス
     */
    public void insert(final EdinetResponse edinetResponse) {
        edinetResponse.getResults().stream()
                .filter(results -> isEmpty(results.getDocId()))
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
                log.debug("一意制約違反のため、データベースへの登録をスキップします。" +
                                "\tテーブル名:{}\t書類ID:{}\tEDINETコード:{}\t提出者名:{}\t書類種別コード:{}",
                        "edinet_document",
                        results.getDocId(),
                        results.getEdinetCode(),
                        results.getFilerName(),
                        results.getDocTypeCode()
                );
            } else {
                throw new FundanalyzerRuntimeException("想定外のエラーが発生しました。", e);
            }
        }
    }

    /**
     * EDINETドキュメントがデータベースに存在するか
     *
     * @param docId 書類ID
     * @return boolean
     */
    private boolean isEmpty(final String docId) {
        return edinetDocumentDao.selectAll().stream()
                .map(EdinetDocumentEntity::getDocId)
                .noneMatch(docId::equals);
    }
}
