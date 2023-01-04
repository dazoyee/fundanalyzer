package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.domain.domain.dao.master.BsSubjectDao;
import github.com.ioridazo.fundanalyzer.domain.domain.dao.master.PlSubjectDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.BsSubjectEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.PlSubjectEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.Subject;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.value.BsSubject;
import github.com.ioridazo.fundanalyzer.domain.value.PlSubject;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class SubjectSpecification {

    private static final String CACHE_KEY_BS_SUBJECT_LIST = "bsSubjectList";
    private static final String CACHE_KEY_PL_SUBJECT_LIST = "plSubjectList";


    private final BsSubjectDao bsSubjectDao;
    private final PlSubjectDao plSubjectDao;

    public SubjectSpecification(final BsSubjectDao bsSubjectDao, final PlSubjectDao plSubjectDao) {
        this.bsSubjectDao = bsSubjectDao;
        this.plSubjectDao = plSubjectDao;
    }

    /**
     * 貸借対照表の科目を取得する
     *
     * @param subjectName 貸借対照表の科目名
     * @return 貸借対照表の科目
     */
    public Optional<BsSubject> findBsSubject(final String subjectName) {
        return inquiryBsSubjectList().stream()
                .map(BsSubject::of)
                .filter(bsSubject -> subjectName.equals(bsSubject.getName()))
                .findAny();
    }

    /**
     * 貸借対照表の科目情報を取得する
     *
     * @param bsEnum 貸借対照表の科目
     * @return 貸借対照表の科目情報
     */
    public List<Subject> findBsSubject(final BsSubject.BsEnum bsEnum) {
        final List<Subject> subjectList = inquiryBsSubjectList().stream()
                .filter(bsSubject -> Objects.equals(bsEnum.getOutlineSubjectId(), bsSubject.getOutlineSubjectId()))
                .map(BsSubject::of)
                .collect(Collectors.toList());

        if (subjectList.isEmpty()) {
            throw new FundanalyzerRuntimeException("貸借対照表の科目が存在しません");
        } else {
            return subjectList;
        }
    }

    /**
     * 損益計算書の科目を取得する
     *
     * @param subjectName 損益計算書の科目名
     * @return 損益計算書の科目
     */
    public Optional<PlSubject> findPlSubject(final String subjectName) {
        return inquiryPlSubjectList().stream()
                .map(PlSubject::of)
                .filter(plSubject -> subjectName.equals(plSubject.getName()))
                .findAny();
    }

    /**
     * 科目情報を取得する
     *
     * @param fs        財務諸表種別
     * @param subjectId 科目ID
     * @return 科目情報
     */
    public Subject findSubject(final FinancialStatementEnum fs, final String subjectId) {
        return switch (fs) {
            case BALANCE_SHEET -> inquiryBsSubjectList().stream()
                    .filter(bsSubject -> Objects.equals(subjectId, bsSubject.getId()))
                    .map(BsSubject::of)
                    .findFirst()
                    .orElseThrow(() -> new FundanalyzerRuntimeException("貸借対照表の科目が存在しません"));
            case PROFIT_AND_LESS_STATEMENT -> inquiryPlSubjectList().stream()
                    .filter(plSubject -> Objects.equals(subjectId, plSubject.getId()))
                    .map(PlSubject::of)
                    .findFirst()
                    .orElseThrow(() -> new FundanalyzerRuntimeException("損益計算書の科目が存在しません"));
            default -> throw new FundanalyzerRuntimeException("存在しない財務諸表");
        };
    }

    /**
     * 貸借対照表の科目情報リストを取得する
     *
     * @param bsEnum 貸借対照表の科目
     * @return 貸借対照表の科目情報リスト
     */
    public List<Subject> findBsSubjectList(final BsSubject.BsEnum bsEnum) {
        return inquiryBsSubjectList().stream()
                .filter(bsSubject -> Objects.equals(bsEnum.getOutlineSubjectId(), bsSubject.getOutlineSubjectId()))
                .map(BsSubject::of)
                .sorted(Comparator.comparing(BsSubject::getDetailSubjectId))
                .collect(Collectors.toList());
    }

    /**
     * 損益計算書の科目情報リストを取得する
     *
     * @param plEnum 損益計算書の科目
     * @return 損益計算書の科目情報リスト
     */
    public List<Subject> findPlSubjectList(final PlSubject.PlEnum plEnum) {
        return inquiryPlSubjectList().stream()
                .filter(plSubject -> Objects.equals(plEnum.getOutlineSubjectId(), plSubject.getOutlineSubjectId()))
                .map(PlSubject::of)
                .sorted(Comparator.comparing(PlSubject::getDetailSubjectId))
                .collect(Collectors.toList());
    }

    /**
     * 貸借対照表の科目情報を取得する
     * <ul>
     *    <li>キャッシュがあるときはキャッシュから取得する<li/>
     *    <li>キャッシュがないときはデータベースから取得する<li/>
     * </>
     *
     * @return 貸借対照表の科目情報
     */
    @Cacheable(CACHE_KEY_BS_SUBJECT_LIST)
    public List<BsSubjectEntity> inquiryBsSubjectList() {
        return findBsSubjectList();
    }

    @CachePut(CACHE_KEY_BS_SUBJECT_LIST)
    public List<BsSubjectEntity> findBsSubjectList() {
        return bsSubjectDao.selectAll();
    }

    /**
     * 損益計算書の科目情報を取得する
     * <ul>
     *    <li>キャッシュがあるときはキャッシュから取得する<li/>
     *    <li>キャッシュがないときはデータベースから取得する<li/>
     * </>
     *
     * @return 損益計算書の科目情報
     */
    @Cacheable(CACHE_KEY_PL_SUBJECT_LIST)
    public List<PlSubjectEntity> inquiryPlSubjectList() {
        return findPlSubjectList();
    }

    @CachePut(CACHE_KEY_PL_SUBJECT_LIST)
    public List<PlSubjectEntity> findPlSubjectList() {
        return plSubjectDao.selectAll();
    }
}
