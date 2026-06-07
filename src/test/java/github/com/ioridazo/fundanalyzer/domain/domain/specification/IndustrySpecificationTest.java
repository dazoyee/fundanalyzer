package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.client.csv.bean.EdinetCsvResultBean;
import github.com.ioridazo.fundanalyzer.config.AnalysisCoefficient;
import github.com.ioridazo.fundanalyzer.domain.domain.dao.master.IndustryDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.IndustryEntity;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    @Nested
    @DisplayName("resolveCoefficient メソッド")
    class ResolveCoefficient {

        @BeforeEach
        void setUp() {
            when(industrySpecification.inquiryIndustryList()).thenReturn(List.of(
                    new IndustryEntity(12, "情報・通信業", BigDecimal.valueOf(15), BigDecimal.valueOf(1.1), BigDecimal.valueOf(0.10), LocalDateTime.now()),
                    new IndustryEntity(14, "電気・ガス業", BigDecimal.valueOf(6), BigDecimal.valueOf(1.2), BigDecimal.valueOf(0.06), LocalDateTime.now())));
        }

        @DisplayName("resolveCoefficient : 業種行の係数（資本コスト含む）を返す")
        @Test
        void ok() {
            final AnalysisCoefficient actual = industrySpecification.resolveCoefficient(12);
            assertEquals(BigDecimal.valueOf(15), actual.getOperatingProfitWeight());
            assertEquals(BigDecimal.valueOf(1.1), actual.getCurrentLiabilitiesRatio());
            assertEquals(BigDecimal.valueOf(0.10), actual.getCostOfEquity());
        }

        @DisplayName("resolveCoefficient : 業種ごとに正しい行の係数を返す（取り違えない）")
        @Test
        void differentIndustry() {
            final AnalysisCoefficient actual = industrySpecification.resolveCoefficient(14);
            assertEquals(BigDecimal.valueOf(6), actual.getOperatingProfitWeight());
            assertEquals(BigDecimal.valueOf(1.2), actual.getCurrentLiabilitiesRatio());
            assertEquals(BigDecimal.valueOf(0.06), actual.getCostOfEquity());
        }

        @DisplayName("resolveCoefficient : 業種IDがnullのときは例外")
        @Test
        void nullId() {
            assertThrows(FundanalyzerNotExistException.class, () -> industrySpecification.resolveCoefficient(null));
        }

        @DisplayName("resolveCoefficient : 該当業種が存在しないときは例外")
        @Test
        void unknownId() {
            assertThrows(FundanalyzerNotExistException.class, () -> industrySpecification.resolveCoefficient(99));
        }
    }
}