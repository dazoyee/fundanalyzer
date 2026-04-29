package github.com.ioridazo.fundanalyzer.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("FundanalyzerNotExistException の型階層テスト")
class FundanalyzerNotExistExceptionTest {

    @Nested
    @DisplayName("型階層")
    class TypeHierarchy {

        @DisplayName("FundanalyzerRuntimeException のサブクラスである")
        @Test
        void isSubclassOfFundanalyzerRuntimeException() {
            final FundanalyzerNotExistException actual = new FundanalyzerNotExistException("subject");

            assertTrue(actual instanceof FundanalyzerRuntimeException);
        }

        @DisplayName("RuntimeException のサブクラスとしても扱える")
        @Test
        void isSubclassOfRuntimeException() {
            final FundanalyzerNotExistException actual = new FundanalyzerNotExistException("subject");

            assertAll(
                    () -> assertTrue(actual instanceof RuntimeException),
                    () -> assertTrue(actual instanceof Exception),
                    () -> assertTrue(actual instanceof Throwable)
            );
        }
    }
}
