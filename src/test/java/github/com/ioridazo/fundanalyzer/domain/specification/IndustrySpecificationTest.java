package github.com.ioridazo.fundanalyzer.domain.specification;

import github.com.ioridazo.fundanalyzer.domain.dao.master.IndustryDao;
import github.com.ioridazo.fundanalyzer.domain.entity.master.IndustryEntity;
import github.com.ioridazo.fundanalyzer.domain.logic.company.bean.EdinetCsvResultBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IndustrySpecificationTest {

    private IndustryDao industryDao;

    private IndustrySpecification industrySpecification;

    @BeforeEach
    void setUp() {
        industryDao = Mockito.mock(IndustryDao.class);

        industrySpecification = Mockito.spy(new IndustrySpecification(industryDao));
        industrySpecification.noTargetList = List.of("銀行業", "保険業");
    }

    @Nested
    class insert {

        @BeforeEach
        void setUp() {
            when(industryDao.selectByName("既に登録されている業種"))
                    .thenReturn(new IndustryEntity(1, "既に登録されている業種", null));
        }

        @DisplayName("insert: industryが登録されていなかったら登録されることを確認する")
        @Test
        void insertIndustry_insert() {
            var edinetCsvResultBean = new EdinetCsvResultBean();
            edinetCsvResultBean.setIndustry("まだ登録されていない業種");
            var resultBeanList = List.of(edinetCsvResultBean);

            assertDoesNotThrow(() -> industrySpecification.insert(resultBeanList));

            // insertされることを確認する
            verify(industryDao, times(1)).insert(any());
        }

        @DisplayName("insert: industryが登録されていたら登録されないことを確認する")
        @Test
        void insertIndustry_not_insert() {
            var edinetCsvResultBean = new EdinetCsvResultBean();
            edinetCsvResultBean.setIndustry("既に登録されている業種");
            var resultBeanList = List.of(edinetCsvResultBean);

            assertDoesNotThrow(() -> industrySpecification.insert(resultBeanList));

            // insertされないことを確認する
            verify(industryDao, times(0)).insert(any());
        }
    }

    @Nested
    class isTarget {

        @BeforeEach
        void setUp() {
            when(industryDao.selectByName("銀行業")).thenReturn(new IndustryEntity(28, "銀行業", null));
            when(industryDao.selectByName("保険業")).thenReturn(new IndustryEntity(29, "保険業", null));
        }

        @DisplayName("isTarget : true")
        @Test
        void boolean_true() {
            var id = 1;

            assertTrue(industrySpecification.isTarget(id));

        }

        @DisplayName("isTarget : false")
        @Test
        void boolean_false() {
            var id = 28;

            assertFalse(industrySpecification.isTarget(id));

        }
    }
}