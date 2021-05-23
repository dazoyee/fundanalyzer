package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.domain.domain.dao.master.BsSubjectDao;
import github.com.ioridazo.fundanalyzer.domain.domain.dao.master.PlSubjectDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.BsSubjectEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.PlSubjectEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class SubjectSpecificationTest {

    private BsSubjectDao bsSubjectDao;
    private PlSubjectDao plSubjectDao;

    private SubjectSpecification subjectSpecification;

    @BeforeEach
    void setUp() {
        bsSubjectDao = Mockito.mock(BsSubjectDao.class);
        plSubjectDao = Mockito.mock(PlSubjectDao.class);

        subjectSpecification = Mockito.spy(new SubjectSpecification(
                bsSubjectDao,
                plSubjectDao
        ));
    }

    @DisplayName("findSubject : BSの科目情報を取得する")
    @Test
    void findSubject_bs() {
        var fs = FinancialStatementEnum.BALANCE_SHEET;
        var subjectId = "1";

        when(bsSubjectDao.selectById(subjectId))
                .thenReturn(new BsSubjectEntity("1", "1", "1", "name"));

        var actual = subjectSpecification.findSubject(fs, subjectId);

        assertAll(
                () -> assertEquals("1", actual.getId()),
                () -> assertEquals("1", actual.getOutlineSubjectId()),
                () -> assertEquals("1", actual.getDetailSubjectId()),
                () -> assertEquals("name", actual.getName())
        );
    }

    @DisplayName("findSubject : PLの科目情報を取得する")
    @Test
    void findSubject_pl() {
        var fs = FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT;
        var subjectId = "1";

        when(plSubjectDao.selectById(subjectId))
                .thenReturn(new PlSubjectEntity("1", "1", "1", "name"));

        var actual = subjectSpecification.findSubject(fs, subjectId);

        assertAll(
                () -> assertEquals("1", actual.getId()),
                () -> assertEquals("1", actual.getOutlineSubjectId()),
                () -> assertEquals("1", actual.getDetailSubjectId()),
                () -> assertEquals("name", actual.getName())
        );
    }
}