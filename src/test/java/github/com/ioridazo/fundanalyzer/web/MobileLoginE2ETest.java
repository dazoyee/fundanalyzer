package github.com.ioridazo.fundanalyzer.web;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * iPhone Safari ビューポートでのログインフロー E2E テスト。
 *
 * <p>フォームログインの入力・送信・成功リダイレクトを iPhone 14 Pro 相当の
 * 390×844 ビューポートで検証する。
 *
 * <p>実行方法: {@code ./mvnw test -Dtest=MobileLoginE2ETest -Dgroups=playwright -DfailIfNoTests=false}
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"app.security.user=playwright", "app.security.password=playwright"})
@Tag("playwright")
@DisplayName("モバイルログイン E2E テスト")
class MobileLoginE2ETest {

    private static final int MOBILE_WIDTH = 390;
    private static final int MOBILE_HEIGHT = 844;
    private static final int DESKTOP_WIDTH = 1280;
    private static final int DESKTOP_HEIGHT = 800;
    private static final int NAVIGATION_TIMEOUT_MS = 15_000;
    private static final int RENDER_WAIT_MS = 2_000;

    private static Playwright playwright;
    private static Browser browser;

    @LocalServerPort
    int port;

    @BeforeAll
    static void setupClass() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }

    @AfterAll
    static void teardownClass() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    /**
     * ログインページを表示し、指定の認証情報でログインしてリダイレクト先 URL を返す。
     *
     * @param width  ビューポート幅
     * @param height ビューポート高さ
     * @return ログイン後の URL
     */
    private String performLogin(final int width, final int height) {
        try (Page page = browser.newPage()) {
            page.setViewportSize(width, height);
            page.setDefaultNavigationTimeout(NAVIGATION_TIMEOUT_MS);
            page.navigate("http://localhost:" + port + "/fundanalyzer/login",
                    new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

            page.fill("input[name='username']", "playwright");
            page.fill("input[name='password']", "playwright");
            page.click("button[type='submit']");

            page.waitForTimeout(RENDER_WAIT_MS);
            return page.url();
        }
    }

    @Nested
    @DisplayName("iPhone ビューポート (390x844) のテスト")
    class MobileViewport {

        @Test
        @DisplayName("ログインページが表示される")
        void loginPage_表示される() {
            try (Page page = browser.newPage()) {
                page.setViewportSize(MOBILE_WIDTH, MOBILE_HEIGHT);
                page.setDefaultNavigationTimeout(NAVIGATION_TIMEOUT_MS);
                page.navigate("http://localhost:" + port + "/fundanalyzer/login",
                        new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

                assertTrue(page.isVisible("input[name='username']"),
                        "ユーザー名フィールドが表示されていること");
                assertTrue(page.isVisible("input[name='password']"),
                        "パスワードフィールドが表示されていること");
                assertTrue(page.isVisible("button[type='submit']"),
                        "ログインボタンが表示されていること");
            }
        }

        @Test
        @DisplayName("正しい認証情報でログインすると保護ページへリダイレクトされる")
        void login_正認証_リダイレクト() {
            final String url = performLogin(MOBILE_WIDTH, MOBILE_HEIGHT);
            assertTrue(url.contains("/fundanalyzer/"),
                    "ログイン後に /fundanalyzer/ 配下へリダイレクトされること: " + url);
            assertTrue(!url.contains("/login"),
                    "ログイン後にログインページに留まっていないこと: " + url);
        }

        @Test
        @DisplayName("未認証でアクセスするとログインページへリダイレクトされる")
        void unauthenticated_ログインページへリダイレクト() {
            try (Page page = browser.newPage()) {
                page.setViewportSize(MOBILE_WIDTH, MOBILE_HEIGHT);
                page.setDefaultNavigationTimeout(NAVIGATION_TIMEOUT_MS);
                page.navigate("http://localhost:" + port + "/fundanalyzer/v3/index",
                        new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

                assertTrue(page.url().contains("/login"),
                        "未認証アクセスでログインページへリダイレクトされること: " + page.url());
            }
        }
    }

    @Nested
    @DisplayName("デスクトップ ビューポート (1280x800) のテスト")
    class DesktopViewport {

        @Test
        @DisplayName("正しい認証情報でログインすると保護ページへリダイレクトされる")
        void login_正認証_リダイレクト() {
            final String url = performLogin(DESKTOP_WIDTH, DESKTOP_HEIGHT);
            assertTrue(url.contains("/fundanalyzer/"),
                    "ログイン後に /fundanalyzer/ 配下へリダイレクトされること: " + url);
            assertTrue(!url.contains("/login"),
                    "ログイン後にログインページに留まっていないこと: " + url);
        }
    }
}
