package github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuarterTypeTest {

    @Nested
    class from {

        @Test
        void semi_by_doc_type_160() {
            assertEquals(
                    QuarterType.QT_SEMI,
                    QuarterType.from(DocumentTypeCode.DTC_160, "半期報告書")
            );
        }

        @Test
        void semi_by_doc_type_170() {
            assertEquals(
                    QuarterType.QT_SEMI,
                    QuarterType.from(DocumentTypeCode.DTC_170, "訂正半期報告書")
            );
        }

        @Test
        void quarterly_regression_uses_doc_description() {
            assertEquals(
                    QuarterType.QT_2,
                    QuarterType.from(
                            DocumentTypeCode.DTC_140,
                            "四半期報告書－第21期第2四半期(令和1年12月1日－令和2年2月29日)"
                    )
            );
        }

        @Test
        void unknown_format_falls_back_to_other() {
            assertEquals(
                    QuarterType.QT_OTHER,
                    QuarterType.from(DocumentTypeCode.DTC_140, "フォーマット不明")
            );
        }
    }

    @Nested
    class fromDocDescription {

        @Test
        void qt_1() {
            assertEquals(
                    QuarterType.QT_1,
                    QuarterType.fromDocDescription("四半期報告書－第145期第1四半期(令和2年1月1日－令和2年3月31日)")
            );
        }

        @Test
        void qt_2() {
            assertEquals(
                    QuarterType.QT_2,
                    QuarterType.fromDocDescription("四半期報告書－第21期第2四半期(令和1年12月1日－令和2年2月29日)")
            );
        }

        @Test
        void qt_3() {
            assertEquals(
                    QuarterType.QT_3,
                    QuarterType.fromDocDescription("四半期報告書－第14期第3四半期(令和2年1月1日－令和2年3月31日)")
            );
        }

        @Test
        void qt_other() {
            assertEquals(
                    QuarterType.QT_OTHER,
                    QuarterType.fromDocDescription("有価証券報告書（内国投資信託受益証券）－第28期(令和1年8月27日－令和2年2月25日)")
            );
        }

        @Test
        void _null() {
            assertEquals(
                    QuarterType.QT_OTHER,
                    QuarterType.fromDocDescription(null)
            );
        }
    }
}
