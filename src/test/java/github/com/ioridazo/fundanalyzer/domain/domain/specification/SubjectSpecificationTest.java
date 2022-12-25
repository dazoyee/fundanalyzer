package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.domain.domain.cache.SubjectCache;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.BsSubjectEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.PlSubjectEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.value.BsSubject;
import github.com.ioridazo.fundanalyzer.domain.value.PlSubject;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class SubjectSpecificationTest {

    private SubjectCache subjectCache;

    private SubjectSpecification subjectSpecification;

    @BeforeEach
    void setUp() {
        subjectCache = Mockito.mock(SubjectCache.class);

        subjectSpecification = Mockito.spy(new SubjectSpecification(subjectCache));
    }

    @Nested
    class findBsSubject {

        @BeforeEach
        void setUp() {
            when(subjectCache.inquiryBsSubjectList()).thenReturn(List.of(new BsSubjectEntity("2", "1", "1", "流動資産合計")));
        }

        @DisplayName("findBsSubject : 貸借対照表の科目を取得する")
        @Test
        void ok_subjectName() {
            var actual = subjectSpecification.findBsSubject("流動資産合計").orElseThrow();
            assertAll(
                    () -> assertEquals("2", actual.getId()),
                    () -> assertEquals("1", actual.getOutlineSubjectId()),
                    () -> assertEquals("1", actual.getDetailSubjectId()),
                    () -> assertEquals("流動資産合計", actual.getName())
            );
        }

        @DisplayName("findBsSubject : 貸借対照表の科目を取得できないときはエラーを発生する")
        @Test
        void error_subjectName() {
            assertNull(subjectSpecification.findBsSubject("投資その他の資産合計").orElse(null));
        }

        @DisplayName("findBsSubject : 貸借対照表の科目を取得する")
        @Test
        void ok_bsEnum() {
            var actual = subjectSpecification.findBsSubject(BsSubject.BsEnum.TOTAL_CURRENT_ASSETS);
            assertAll(
                    () -> assertEquals("2", actual.get(0).getId()),
                    () -> assertEquals("1", actual.get(0).getOutlineSubjectId()),
                    () -> assertEquals("1", actual.get(0).getDetailSubjectId()),
                    () -> assertEquals("流動資産合計", actual.get(0).getName())
            );
        }

        @DisplayName("findBsSubject : 貸借対照表の科目を取得できないときはエラーを発生する")
        @Test
        void error_bsEnum() {
            assertThrows(FundanalyzerRuntimeException.class, () -> subjectSpecification.findBsSubject(BsSubject.BsEnum.TOTAL_CURRENT_LIABILITIES));
        }
    }

    @Nested
    class findPlSubject {

        @BeforeEach
        void setUp() {
            when(subjectCache.inquiryPlSubjectList()).thenReturn(List.of(new PlSubjectEntity("3", "3", "1", "営業利益")));
        }

        @DisplayName("findPlSubject : 損益計算書の科目を取得する")
        @Test
        void ok() {
            var actual = subjectSpecification.findPlSubject("営業利益").orElseThrow();
            assertAll(
                    () -> assertEquals("3", actual.getId()),
                    () -> assertEquals("3", actual.getOutlineSubjectId()),
                    () -> assertEquals("1", actual.getDetailSubjectId()),
                    () -> assertEquals("営業利益", actual.getName())
            );
        }

        @DisplayName("findPlSubject : 損益計算書の科目を取得できないときはエラーを発生する")
        @Test
        void error() {
            assertNull(subjectSpecification.findPlSubject("投資その他の資産合計").orElse(null));
        }
    }

    @Nested
    class findSubject {

        @BeforeEach
        void setUp() {
            when(subjectCache.inquiryBsSubjectList()).thenReturn(List.of(new BsSubjectEntity("1", "1", "1", "name")));
            when(subjectCache.inquiryPlSubjectList()).thenReturn(List.of(new PlSubjectEntity("1", "1", "1", "name")));
        }

        @DisplayName("findSubject : BSの科目情報を取得する")
        @Test
        void bs() {
            var fs = FinancialStatementEnum.BALANCE_SHEET;
            var subjectId = "1";

            var actual = subjectSpecification.findSubject(fs, subjectId);
            assertAll(
                    () -> assertEquals("1", actual.getId()),
                    () -> assertEquals("1", actual.getOutlineSubjectId()),
                    () -> assertEquals("1", actual.getDetailSubjectId()),
                    () -> assertEquals("name", actual.getName())
            );
        }

        @DisplayName("findSubject : BSの科目情報を取得できないときはエラーを発生する")
        @Test
        void bs_error() {
            var fs = FinancialStatementEnum.BALANCE_SHEET;
            var subjectId = "2";

            assertThrows(FundanalyzerRuntimeException.class, () -> subjectSpecification.findSubject(fs, subjectId));
        }

        @DisplayName("findSubject : PLの科目情報を取得する")
        @Test
        void pl() {
            var fs = FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT;
            var subjectId = "1";

            var actual = subjectSpecification.findSubject(fs, subjectId);
            assertAll(
                    () -> assertEquals("1", actual.getId()),
                    () -> assertEquals("1", actual.getOutlineSubjectId()),
                    () -> assertEquals("1", actual.getDetailSubjectId()),
                    () -> assertEquals("name", actual.getName())
            );
        }

        @DisplayName("findSubject : PLの科目情報を取得できないときはエラーを発生する")
        @Test
        void pl_error() {
            var fs = FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT;
            var subjectId = "2";

            assertThrows(FundanalyzerRuntimeException.class, () -> subjectSpecification.findSubject(fs, subjectId));
        }
    }

    @Nested
    class findBsSubjectList {

        @BeforeEach
        void setUp() {
            when(subjectCache.inquiryBsSubjectList()).thenReturn(List.of(
                    new BsSubjectEntity("2", "1", "1", "流動資産合計"),
                    new BsSubjectEntity("20", "1", "2", "流動資産計")));
        }

        @DisplayName("findBsSubjectList : 貸借対照表の科目を取得する")
        @Test
        void ok() {
            var actual = subjectSpecification.findBsSubjectList(BsSubject.BsEnum.TOTAL_CURRENT_ASSETS);
            assertAll(
                    () -> assertEquals("2", actual.get(0).getId()),
                    () -> assertEquals("1", actual.get(0).getOutlineSubjectId()),
                    () -> assertEquals("1", actual.get(0).getDetailSubjectId()),
                    () -> assertEquals("流動資産合計", actual.get(0).getName()),
                    () -> assertEquals("20", actual.get(1).getId()),
                    () -> assertEquals("1", actual.get(1).getOutlineSubjectId()),
                    () -> assertEquals("2", actual.get(1).getDetailSubjectId()),
                    () -> assertEquals("流動資産計", actual.get(1).getName())
            );
        }

        @DisplayName("findBsSubjectList : 貸借対照表の科目を取得できないときはエラーを発生する")
        @Test
        void error() {
            assertEquals(List.of(), subjectSpecification.findBsSubjectList(BsSubject.BsEnum.TOTAL_CURRENT_LIABILITIES));
        }
    }

    @Nested
    class findPlSubjectList {

        @BeforeEach
        void setUp() {
            when(subjectCache.inquiryPlSubjectList()).thenReturn(List.of(
                    new PlSubjectEntity("3", "3", "1", "営業利益"),
                    new PlSubjectEntity("4", "3", "2", "営業利益又は営業損失（△）")));
        }

        @DisplayName("findPlSubjectList : 損益計算書の科目を取得する")
        @Test
        void ok() {
            var actual = subjectSpecification.findPlSubjectList(PlSubject.PlEnum.OPERATING_PROFIT);
            assertAll(
                    () -> assertEquals("3", actual.get(0).getId()),
                    () -> assertEquals("3", actual.get(0).getOutlineSubjectId()),
                    () -> assertEquals("1", actual.get(0).getDetailSubjectId()),
                    () -> assertEquals("営業利益", actual.get(0).getName()),
                    () -> assertEquals("4", actual.get(1).getId()),
                    () -> assertEquals("3", actual.get(1).getOutlineSubjectId()),
                    () -> assertEquals("2", actual.get(1).getDetailSubjectId()),
                    () -> assertEquals("営業利益又は営業損失（△）", actual.get(1).getName())
            );
        }

        @DisplayName("findPlSubjectList : 損益計算書の科目を取得できないときはエラーを発生する")
            // @Test
        void error() {
            assertEquals(List.of(), subjectSpecification.findPlSubjectList(PlSubject.PlEnum.OPERATING_PROFIT));
        }
    }
}