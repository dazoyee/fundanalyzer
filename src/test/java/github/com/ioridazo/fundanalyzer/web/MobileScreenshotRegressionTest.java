package github.com.ioridazo.fundanalyzer.web;

import com.google.gson.JsonObject;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.CDPSession;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.HttpCredentials;
import com.microsoft.playwright.options.WaitUntilState;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * スマホ UI 刷新の PNG ビジュアルリグレッションテスト。
 *
 * <p>5 画面 × 2 viewport = 10 ケースで baseline と現スクショを BufferedImage の ARGB 単位で比較し、
 * 差分ピクセル比率が {@value #MAX_DIFF_PIXEL_RATIO_PERCENT}% を超えたら失敗とする。
 *
 * <p>baseline 入力: src/test/resources/playwright-baselines/&lt;screen&gt;-&lt;viewport&gt;.png
 * <p>diff 出力: target/playwright-snapshots/diff-&lt;screen&gt;-&lt;viewport&gt;{,-baseline,-current}.png
 *
 * <p>baseline の更新は {@link ManualMobileScreenshotTest} を -DupdateBaselines=true で実行する。
 *
 * <p>本テストはサイズの大きい Chromium バイナリを取得するため -Dgroups=playwright が必要。
 * 通常ビルドから除外する場合は -DexcludedGroups=playwright を指定する。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"app.security.user=playwright", "app.security.password=playwright"})
@Tag("playwright")
@DisplayName("スマホ UI PNG ビジュアルリグレッション")
class MobileScreenshotRegressionTest {

    private static final Browser.NewPageOptions AUTH = new Browser.NewPageOptions()
            .setHttpCredentials(new HttpCredentials("playwright", "playwright"));
    private static final Path BASELINE_DIR = Paths.get("src", "test", "resources", "playwright-baselines");
    private static final Path DIFF_DIR = Paths.get("target", "playwright-snapshots");
    private static final double MAX_DIFF_PIXEL_RATIO = 0.02;
    private static final double MAX_DIFF_PIXEL_RATIO_PERCENT = MAX_DIFF_PIXEL_RATIO * 100.0;
    private static final int NAVIGATION_TIMEOUT_MS = 15_000;
    private static final int RENDER_WAIT_MS = 6_000;
    private static final int DIFF_HIGHLIGHT_ARGB = 0xFFFF0000;
    private static final int TRANSPARENT_ARGB = 0x00000000;

    private static Playwright playwright;
    private static Browser browser;

    @LocalServerPort
    int port;

    @BeforeAll
    static void setupClass() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
        DIFF_DIR.toFile().mkdirs();
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
     * 画面 × ビューポートのマトリクスを返す。{@link ManualMobileScreenshotTest} と
     * 同じ 5 画面 × 2 viewport = 10 ケースを網羅する。
     *
     * @return JUnit パラメタライズド引数のストリーム
     */
    static Stream<Arguments> screenViewportMatrix() {
        return Stream.of(
                Arguments.of("index", "/v3/index", "desktop", 1280, 800),
                Arguments.of("index", "/v3/index", "mobile", 390, 844),
                Arguments.of("valuation", "/v3/valuation", "desktop", 1280, 800),
                Arguments.of("valuation", "/v3/valuation", "mobile", 390, 844),
                Arguments.of("edinet-list", "/v3/edinet-list", "desktop", 1280, 800),
                Arguments.of("edinet-list", "/v3/edinet-list", "mobile", 390, 844),
                Arguments.of("edinet-list-detail", "/v3/edinet-list-detail?submitDate=2026-03-25", "desktop", 1280, 800),
                Arguments.of("edinet-list-detail", "/v3/edinet-list-detail?submitDate=2026-03-25", "mobile", 390, 844),
                Arguments.of("corporate", "/v3/corporate?code=9001", "desktop", 1280, 800),
                Arguments.of("corporate", "/v3/corporate?code=9001", "mobile", 390, 844)
        );
    }

    @ParameterizedTest(name = "{0} / {2} ({3}x{4})")
    @MethodSource("screenViewportMatrix")
    @DisplayName("baseline と現スクショの差分が許容閾値以下であることを確認")
    void compareScreenshotAgainstBaseline(
            final String screenName,
            final String path,
            final String viewportName,
            final int width,
            final int height) throws IOException {
        final Path baselinePath = BASELINE_DIR.resolve(screenName + "-" + viewportName + ".png");
        assertTrue(baselinePath.toFile().exists(),
                "baseline が存在しない: " + baselinePath
                        + " (ManualMobileScreenshotTest を -DupdateBaselines=true で実行して再生成すること)");

        try (Page page = browser.newPage(AUTH)) {
            page.setViewportSize(width, height);
            page.setDefaultNavigationTimeout(NAVIGATION_TIMEOUT_MS);
            try {
                page.navigate(
                        "http://localhost:" + port + "/fundanalyzer" + path,
                        new Page.NavigateOptions()
                                .setWaitUntil(WaitUntilState.COMMIT)
                                .setTimeout(NAVIGATION_TIMEOUT_MS));
            } catch (final Exception ignored) {
                // navigate timeout は許容（COMMIT 状態到達後の段階的レンダリングを待つ）
            }
            page.waitForTimeout(RENDER_WAIT_MS);

            final byte[] currentBytes = cdpFullPageScreenshot(page);
            final BufferedImage baseline = ImageIO.read(baselinePath.toFile());
            assertNotNull(baseline, "baseline 読み込み失敗: " + baselinePath);
            final BufferedImage current = ImageIO.read(new ByteArrayInputStream(currentBytes));
            assertNotNull(current, "現スクショの PNG 読み込み失敗 (" + screenName + "-" + viewportName + ")");

            if (baseline.getWidth() != current.getWidth() || baseline.getHeight() != current.getHeight()) {
                writeDiffArtifacts(screenName, viewportName, currentBytes, baseline, null);
                fail(String.format(
                        "サイズ不一致 baseline=%dx%d current=%dx%d (artifacts: %s)",
                        baseline.getWidth(), baseline.getHeight(),
                        current.getWidth(), current.getHeight(),
                        DIFF_DIR.toAbsolutePath()));
            }

            final BufferedImage diff = new BufferedImage(
                    current.getWidth(), current.getHeight(), BufferedImage.TYPE_INT_ARGB);
            final long total = (long) current.getWidth() * current.getHeight();
            long diffCount = 0;
            for (int y = 0; y < current.getHeight(); y++) {
                for (int x = 0; x < current.getWidth(); x++) {
                    final int b = baseline.getRGB(x, y);
                    final int c = current.getRGB(x, y);
                    if (b == c) {
                        diff.setRGB(x, y, TRANSPARENT_ARGB);
                    } else {
                        diff.setRGB(x, y, DIFF_HIGHLIGHT_ARGB);
                        diffCount++;
                    }
                }
            }
            final double ratio = (double) diffCount / total;
            if (ratio > MAX_DIFF_PIXEL_RATIO) {
                writeDiffArtifacts(screenName, viewportName, currentBytes, baseline, diff);
                fail(String.format(
                        "差分比率 %.4f%% が許容閾値 %.2f%% を超過 (差分 %d / 総 %d, artifacts: %s)",
                        ratio * 100.0, MAX_DIFF_PIXEL_RATIO_PERCENT, diffCount, total,
                        DIFF_DIR.toAbsolutePath()));
            }
        }
    }

    /**
     * CDP 経由で viewport のスクショを取得する。Playwright 標準の page.screenshot() は
     * fonts.ready 完了を待つため、Web Font が遅延ロードされるケースで timeout する。
     * Page.captureScreenshot を直接呼ぶことで該当待ちを回避する。
     *
     * @param page 対象ページ
     * @return PNG バイナリ
     */
    private static byte[] cdpFullPageScreenshot(final Page page) {
        final CDPSession cdp = page.context().newCDPSession(page);
        final JsonObject params = new JsonObject();
        params.addProperty("format", "png");
        params.addProperty("captureBeyondViewport", false);
        final JsonObject result = cdp.send("Page.captureScreenshot", params);
        cdp.detach();
        return Base64.getDecoder().decode(result.get("data").getAsString());
    }

    /**
     * 差分発生時の調査用アーティファクトを {@link #DIFF_DIR} に書き出す。
     *
     * @param screenName 画面名
     * @param viewportName ビューポート名
     * @param currentBytes 現スクショ PNG バイナリ
     * @param baseline baseline 画像
     * @param diff 差分強調画像（サイズ不一致時は null）
     * @throws IOException PNG 書き出し失敗時
     */
    private static void writeDiffArtifacts(
            final String screenName,
            final String viewportName,
            final byte[] currentBytes,
            final BufferedImage baseline,
            final BufferedImage diff) throws IOException {
        final String prefix = "diff-" + screenName + "-" + viewportName;
        Files.write(DIFF_DIR.resolve(prefix + "-current.png"), currentBytes);
        ImageIO.write(baseline, "png", DIFF_DIR.resolve(prefix + "-baseline.png").toFile());
        if (diff != null) {
            ImageIO.write(diff, "png", DIFF_DIR.resolve(prefix + ".png").toFile());
        }
    }
}
