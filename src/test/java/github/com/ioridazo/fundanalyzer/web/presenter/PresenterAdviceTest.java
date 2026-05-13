package github.com.ioridazo.fundanalyzer.web.presenter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("PresenterAdviceのテスト")
class PresenterAdviceTest {

    private PresenterAdvice advice;

    @BeforeEach
    void setUp() throws Exception {
        final Constructor<PresenterAdvice> constructor = PresenterAdvice.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        this.advice = constructor.newInstance();
    }

    @Nested
    @DisplayName("applicationVersion メソッド")
    class ApplicationVersion {

        @Test
        @DisplayName("applicationVersionフィールドに値が設定されている場合 → その値を返す")
        void versionSet_returnsConfiguredVersion() {
            ReflectionTestUtils.setField(advice, "applicationVersion", "v1.2.3");

            final String result = advice.applicationVersion();

            assertEquals("v1.2.3", result);
        }

        @Test
        @DisplayName("applicationVersionフィールドが空文字の場合 → 空文字を返す")
        void versionEmpty_returnsEmptyString() {
            ReflectionTestUtils.setField(advice, "applicationVersion", "");

            final String result = advice.applicationVersion();

            assertEquals("", result);
        }

        @Test
        @DisplayName("applicationVersionフィールドが未設定の場合 → nullを返す")
        void versionUnset_returnsNull() {
            final String result = advice.applicationVersion();

            assertNull(result);
        }

        @Test
        @DisplayName("インスタンスが正しく生成されている")
        void instanceIsCreated() {
            assertNotNull(advice);
        }
    }
}
