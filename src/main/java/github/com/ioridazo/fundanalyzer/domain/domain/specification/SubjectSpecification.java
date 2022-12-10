package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.domain.domain.cache.SubjectCache;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.Subject;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.value.BsSubject;
import github.com.ioridazo.fundanalyzer.domain.value.PlSubject;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class SubjectSpecification {

    private final SubjectCache subjectCache;

    public SubjectSpecification(
            final SubjectCache subjectCache) {
        this.subjectCache = subjectCache;
    }

    /**
     * 貸借対照表の科目を取得する
     *
     * @param subjectName 貸借対照表の科目名
     * @return 貸借対照表の科目
     */
    public Optional<BsSubject> findBsSubject(final String subjectName) {
        return subjectCache.inquiryBsSubjectList().stream()
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
    public Subject findBsSubject(final BsSubject.BsEnum bsEnum) {
        return subjectCache.inquiryBsSubjectList().stream()
                .filter(bsSubject -> Objects.equals(bsEnum.getOutlineSubjectId(), bsSubject.getOutlineSubjectId()))
                .map(BsSubject::of)
                .findFirst()
                .orElseThrow(() -> new FundanalyzerRuntimeException("貸借対照表の科目が存在しません"));
    }

    /**
     * 損益計算書の科目を取得する
     *
     * @param subjectName 損益計算書の科目名
     * @return 損益計算書の科目
     */
    public Optional<PlSubject> findPlSubject(final String subjectName) {
        return subjectCache.inquiryPlSubjectList().stream()
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
        switch (fs) {
            case BALANCE_SHEET:
                return subjectCache.inquiryBsSubjectList().stream()
                        .filter(bsSubject -> Objects.equals(subjectId, bsSubject.getId()))
                        .map(BsSubject::of)
                        .findFirst()
                        .orElseThrow(() -> new FundanalyzerRuntimeException("貸借対照表の科目が存在しません"));
            case PROFIT_AND_LESS_STATEMENT:
                return subjectCache.inquiryPlSubjectList().stream()
                        .filter(plSubject -> Objects.equals(subjectId, plSubject.getId()))
                        .map(PlSubject::of)
                        .findFirst()
                        .orElseThrow(() -> new FundanalyzerRuntimeException("損益計算書の科目が存在しません"));
            default:
                throw new FundanalyzerRuntimeException("存在しない財務諸表");
        }
    }

    /**
     * 貸借対照表の科目情報リストを取得する
     *
     * @param bsEnum 貸借対照表の科目
     * @return 貸借対照表の科目情報リスト
     */
    public List<Subject> findBsSubjectList(final BsSubject.BsEnum bsEnum) {
        return subjectCache.inquiryBsSubjectList().stream()
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
        return subjectCache.inquiryPlSubjectList().stream()
                .filter(plSubject -> Objects.equals(plEnum.getOutlineSubjectId(), plSubject.getOutlineSubjectId()))
                .map(PlSubject::of)
                .sorted(Comparator.comparing(PlSubject::getDetailSubjectId))
                .collect(Collectors.toList());
    }
}
