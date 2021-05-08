package github.com.ioridazo.fundanalyzer.domain.specification;

import github.com.ioridazo.fundanalyzer.domain.dao.master.BsSubjectDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.PlSubjectDao;
import github.com.ioridazo.fundanalyzer.domain.entity.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Detail;
import github.com.ioridazo.fundanalyzer.domain.value.BsSubject;
import github.com.ioridazo.fundanalyzer.domain.value.PlSubject;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class SubjectSpecification {

    private final BsSubjectDao bsSubjectDao;
    private final PlSubjectDao plSubjectDao;

    public SubjectSpecification(
            final BsSubjectDao bsSubjectDao,
            final PlSubjectDao plSubjectDao) {
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
        return bsSubjectDao.selectAll().stream()
                .map(BsSubject::of)
                .filter(bsSubject -> subjectName.equals(bsSubject.getName()))
                .findAny();
    }

    /**
     * 損益計算書の科目を取得する
     *
     * @param subjectName 損益計算書の科目名
     * @return 損益計算書の科目
     */
    public Optional<PlSubject> findPlSubject(final String subjectName) {
        return plSubjectDao.selectAll().stream()
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
    public Detail findSubject(final FinancialStatementEnum fs, final String subjectId) {
        switch (fs) {
            case BALANCE_SHEET:
                return BsSubject.of(bsSubjectDao.selectById(subjectId));
            case PROFIT_AND_LESS_STATEMENT:
                return PlSubject.of(plSubjectDao.selectById(subjectId));
            default:
                throw new FundanalyzerRuntimeException();
        }
    }

    /**
     * 貸借対照表の科目情報を取得する
     *
     * @param bsEnum 貸借対照表の科目
     * @return 貸借対照表の科目情報
     */
    public Detail findBsSubject(final BsSubject.BsEnum bsEnum) {
        return BsSubject.of(bsSubjectDao.selectByUniqueKey(bsEnum.getOutlineSubjectId(), bsEnum.getDetailSubjectId()));
    }

    /**
     * 貸借対照表の科目情報リストを取得する
     *
     * @param bsEnum 貸借対照表の科目
     * @return 貸借対照表の科目情報リスト
     */
    public List<Detail> findBsSubjectList(final BsSubject.BsEnum bsEnum) {
        return bsSubjectDao.selectByOutlineSubjectId(bsEnum.getOutlineSubjectId()).stream()
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
    public List<Detail> findPlSubjectList(final PlSubject.PlEnum plEnum) {
        return plSubjectDao.selectByOutlineSubjectId(plEnum.getOutlineSubjectId()).stream()
                .map(PlSubject::of)
                .sorted(Comparator.comparing(PlSubject::getDetailSubjectId))
                .collect(Collectors.toList());
    }
}
