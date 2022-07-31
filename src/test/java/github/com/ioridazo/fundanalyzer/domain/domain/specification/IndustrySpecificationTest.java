package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.client.csv.bean.EdinetCsvResultBean;
import github.com.ioridazo.fundanalyzer.domain.domain.dao.master.IndustryDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.IndustryEntity;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    class convertFromNameToId {

        @BeforeEach
        void setUp() {
            when(industrySpecification.inquiryIndustryList()).thenReturn(List.of(new IndustryEntity(1, "水産・農林業", null)));
        }

        @DisplayName("convertFromNameToId : 業種名から業種IDに変換する")
        @Test
        void ok() {
            assertEquals(1, industrySpecification.convertFromNameToId("水産・農林業"));
        }

        @DisplayName("convertFromNameToId : 業種名から業種IDに変換できないときはエラーを発生する")
        @Test
        void ng() {
            assertThrows(FundanalyzerRuntimeException.class, () -> industrySpecification.convertFromNameToId("建設業"));
        }
    }

    @Nested
    class convertFromIdToName {

        @BeforeEach
        void setUp() {
            when(industrySpecification.inquiryIndustryList()).thenReturn(List.of(new IndustryEntity(1, "水産・農林業", null)));
        }

        @DisplayName("convertFromIdToName : 業種IDから業種名に変換する")
        @Test
        void ok() {
            assertEquals("水産・農林業", industrySpecification.convertFromIdToName(1));
        }

        @DisplayName("convertFromIdToName : 業種IDから業種名に変換できないときはエラーを発生する")
        @Test
        void ng() {
            assertThrows(FundanalyzerRuntimeException.class, () -> industrySpecification.convertFromIdToName(2));
        }
    }

    @Nested
    class insert {

        @BeforeEach
        void setUp() {
            when(industrySpecification.inquiryIndustryList())
                    .thenReturn(List.of(new IndustryEntity(1, "既に登録されている業種", null)));
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
            when(industrySpecification.inquiryIndustryList()).thenReturn(List.of(
                    new IndustryEntity(28, "銀行業", null),
                    new IndustryEntity(29, "保険業", null)));
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