package github.com.ioridazo.fundanalyzer.web;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.ViewportSize;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 画面刷新タスク Phase 8 のスナップショット回帰検証テスト。
 *
 * <p>主要 3 画面（/v3/index / /v3/valuation / /v3/edinet-list）× 2 ビューポート（desktop 1280x800 /
 * mobile 375x812）= 6 ケースで Playwright Chromium を起動し HTML スナップショットを取得する。
 *
 * <p>200 OK + 主要要素（layout-v2 のサイドバー / ヘッダー / main）の存在を JUnit 5 標準アサーションで検証する。
 * フルカラー比較は ADR-001 で不採用と決定済のため採用しない。
 *
 * <p>初回実行時に ~/.cache/ms-playwright/ に Chromium バイナリ（約 200 MB）が自動取得される。
 * 通常ビルドから除外する場合は -DexcludedGroups=playwright を指定する。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"app.security.user=playwright", "app.security.password=playwright"})
@Tag("playwright")
@DisplayName("Phase 8 スナップショット回帰検証")
class Phase8ScreenSnapshotTest {

    private static final Path SNAPSHOT_DIR = Paths.get("target", "playwright-snapshots");

    private static Playwright playwright;
    private static Browser browser;

    @LocalServerPort
    int port;

    @BeforeAll
    static void setupClass() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
        SNAPSHOT_DIR.toFile().mkdirs();
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
     * フォームログインを実行する。
     *
     * @param page ログイン操作を行う Page
     */
    private void login(final Page page) {
        page.navigate("http://localhost:" + port + "/fundanalyzer/login");
        page.fill("input[name='username']", "playwright");
        page.fill("input[name='password']", "playwright");
        page.click("button[type='submit']");
        page.waitForLoadState();
    }

    static Stream<Arguments> screenViewportMatrix() {
        return Stream.of(
                Arguments.of("index", "/v3/index", "desktop", 1280, 800),
                Arguments.of("index", "/v3/index", "mobile", 375, 812),
                Arguments.of("valuation", "/v3/valuation", "desktop", 1280, 800),
                Arguments.of("valuation", "/v3/valuation", "mobile", 375, 812),
                Arguments.of("analysis", "/v3/analysis", "desktop", 1280, 800),
                Arguments.of("analysis", "/v3/analysis", "mobile", 375, 812),
                Arguments.of("edinet-list", "/v3/edinet-list", "desktop", 1280, 800),
                Arguments.of("edinet-list", "/v3/edinet-list", "mobile", 375, 812)
        );
    }

    @ParameterizedTest(name = "{0} / {2} ({3}x{4})")
    @MethodSource("screenViewportMatrix")
    @DisplayName("3 画面 × 2 ビューポート = 6 ケースのスナップショット取得 + 主要要素存在確認")
    void screenshotEachScreen(
            final String screenName,
            final String path,
            final String viewportName,
            final int width,
            final int height) {
        try (final Page page = browser.newPage()) {
            page.setViewportSize(width, height);
            login(page);
            final String url = "http://localhost:" + port + "/fundanalyzer" + path;
            page.navigate(url);
            page.waitForLoadState();

            assertNotNull(page.url(), "ページ URL が null");
            assertTrue(page.url().contains(path), "期待するパスに遷移していない: " + page.url());

            final String title = page.title();
            assertNotNull(title, "ページ title が null");
            assertTrue(!title.isBlank(), "ページ title が空");

            assertTrue(page.locator("aside").count() > 0, "サイドバー aside が存在しない");
            assertTrue(page.locator("header").count() > 0, "ヘッダー header が存在しない");
            assertTrue(page.locator("main").count() > 0, "main コンテンツが存在しない");

            final Path snapshotPath = SNAPSHOT_DIR.resolve(screenName + "-" + viewportName + ".png");
            page.screenshot(new Page.ScreenshotOptions().setPath(snapshotPath).setFullPage(true));
            assertTrue(snapshotPath.toFile().exists(), "スナップショット書き出しに失敗: " + snapshotPath);
        }
    }

    @ParameterizedTest(name = "viewport={0}")
    @MethodSource("viewportNames")
    @DisplayName("layout-v2 のダークモードトグルが各ビューポートで存在")
    void darkModeToggleExists(final ViewportSize viewport) {
        try (final Page page = browser.newPage()) {
            page.setViewportSize(viewport.width, viewport.height);
            login(page);
            page.navigate("http://localhost:" + port + "/fundanalyzer/v3/index");
            page.waitForLoadState();

            assertTrue(page.locator("button[aria-label='ダークモード切替']").count() > 0,
                    "ダークモードトグルボタンが見つからない");
        }
    }

    @Test
    @DisplayName("バックテストタブをクリックすると backtest fragment が描画される")
    void backtestTabRenders() {
        try (final Page page = browser.newPage()) {
            page.setViewportSize(1280, 800);
            login(page);
            page.navigate("http://localhost:" + port + "/fundanalyzer/v3/analysis");
            page.waitForLoadState();
            page.click("button:has-text('バックテスト')");
            page.waitForSelector("#backtest-panel");
            page.waitForTimeout(1500);
            final String panel = page.locator("#backtest-panel").innerText();
            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(SNAPSHOT_DIR.resolve("analysis-backtest.png")).setFullPage(true));
            assertTrue(!panel.contains("読み込み中"), "backtest fragment が読み込まれていない: " + panel);
            assertTrue(!page.content().contains("Whitelabel"), "エラーページが表示された");
        }
    }

    static Stream<Arguments> viewportNames() {
        return Stream.of(
                Arguments.of(new ViewportSize(1280, 800)),
                Arguments.of(new ViewportSize(375, 812))
        );
    }
}
