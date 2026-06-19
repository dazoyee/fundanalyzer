package github.com.ioridazo.fundanalyzer.web;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.HttpCredentials;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase B0 プロトタイプ確認用のスマホスクリーンショット手動取得テスト。
 *
 * <p>外部で起動済の Spring Boot（localhost:8889）に直接アクセスしてスクショを保存する。
 * 通常テストでは実行されないよう @Tag("manual-screenshot") を付与する。
 *
 * <p>事前条件: ./mvnw spring-boot:run などで localhost:8889 が動作していること。
 *
 * <p>実行方法（手動確認用・既定の出力先）: ./mvnw test -Dtest=ManualMobileScreenshotTest -Dgroups=manual-screenshot -DfailIfNoTests=false
 *
 * <p>baseline 更新モード: 上記コマンドに -DupdateBaselines=true を追加すると、出力先が
 * src/test/resources/playwright-baselines/ に切り替わり、{@link MobileScreenshotRegressionTest}
 * の比較基準を直接上書きする。意図した UI 変更を反映する場合のみ使用し、PR で目視レビューすること。
 *
 * <p>出力（既定）: target/manual-screenshots/&lt;screen&gt;-{mobile,desktop}.png
 * <p>出力（updateBaselines=true）: src/test/resources/playwright-baselines/&lt;screen&gt;-{mobile,desktop}.png
 */
@Tag("manual-screenshot")
@DisplayName("Phase B0 プロトタイプ確認用スクショ取得")
class ManualMobileScreenshotTest {

    private static final String BASE = "http://localhost:8889/fundanalyzer";
    // dev サーバーの Basic 認証。環境変数 SECURITY_USER / SECURITY_PASSWORD 未設定時の application.yml 既定値に合わせる。
    // 別資格情報で起動している場合は -DmanualScreenshotUser / -DmanualScreenshotPassword で上書きする。
    private static final Browser.NewPageOptions AUTH = new Browser.NewPageOptions()
            .setHttpCredentials(new HttpCredentials(
                    System.getProperty("manualScreenshotUser", "admin"),
                    System.getProperty("manualScreenshotPassword", "fundanalyzer-local-dev")));
    private static final Path SHOT_DIR = Paths.get("target", "manual-screenshots");
    private static final Path BASELINE_DIR = Paths.get("src", "test", "resources", "playwright-baselines");
    private static final boolean UPDATE_BASELINES = Boolean.getBoolean("updateBaselines");

    private static Playwright playwright;
    private static Browser browser;

    /**
     * 出力先ディレクトリを返す。-DupdateBaselines=true 指定時は baseline ディレクトリ、
     * 既定は target/manual-screenshots/。
     *
     * @return 出力先ディレクトリの Path
     */
    private static Path outputDir() {
        return UPDATE_BASELINES ? BASELINE_DIR : SHOT_DIR;
    }

    @BeforeAll
    static void setupClass() throws Exception {
        // 事前ヘルスチェック: dev サーバー起動済か確認
        final HttpURLConnection conn = (HttpURLConnection) URI.create(BASE + "/v3/index").toURL().openConnection();
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(5000);
        conn.setRequestMethod("GET");
        try {
            final int code = conn.getResponseCode();
            Assumptions.assumeTrue(code == 200, "localhost:8889 dev サーバーが起動していない (HTTP " + code + ")");
        } catch (final Exception e) {
            Assumptions.abort("localhost:8889 dev サーバーが起動していない: " + e.getMessage());
        } finally {
            conn.disconnect();
        }

        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
        outputDir().toFile().mkdirs();
        if (UPDATE_BASELINES) {
            System.out.println("[ManualScreenshot] -DupdateBaselines=true: 出力先を " + BASELINE_DIR + " に切り替え");
        }
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

    private static byte[] cdpFullPageScreenshot(final Page page) {
        // viewport だけをスクショ（フルページは aside translate などで誤判定する場合あり）
        final com.microsoft.playwright.CDPSession cdp = page.context().newCDPSession(page);
        final com.google.gson.JsonObject params = new com.google.gson.JsonObject();
        params.addProperty("format", "png");
        params.addProperty("captureBeyondViewport", false);
        final com.google.gson.JsonObject result = cdp.send("Page.captureScreenshot", params);
        cdp.detach();
        final String base64 = result.get("data").getAsString();
        return java.util.Base64.getDecoder().decode(base64);
    }

    private static String dumpPageState(final Page page) {
        final Object data = page.evaluate(
                "() => ({title: document.title, url: location.href, bodyClass: document.body.className, "
                + "asideTranslate: getComputedStyle(document.querySelector('aside')).transform, "
                + "navMobileExists: !!document.querySelector('nav[aria-label=\"モバイルナビ\"]'), "
                + "h1: document.querySelector('h1') ? document.querySelector('h1').innerText : null, "
                + "mainCardCount: document.querySelectorAll('[data-mobile-card]').length, "
                + "tableHidden: document.querySelector('table') ? getComputedStyle(document.querySelector('table').closest('div')).display : null, "
                + "documentHeight: document.documentElement.scrollHeight, "
                + "documentWidth: document.documentElement.scrollWidth"
                + "})");
        return String.valueOf(data);
    }

    @Test
    @DisplayName("index 画面 mobile 390x844 のフルページスクショを撮る")
    void shootIndexMobile() throws Exception {
        try (final Page page = browser.newPage(AUTH)) {
            page.setViewportSize(390, 844);
            page.setDefaultNavigationTimeout(15_000);
            try {
                page.navigate(BASE + "/v3/index", new Page.NavigateOptions().setWaitUntil(com.microsoft.playwright.options.WaitUntilState.COMMIT).setTimeout(15_000));
            } catch (final Exception ignored) {
                // navigate may time out; we still try to capture whatever rendered
            }
            page.waitForTimeout(3500);

            System.out.println("[ManualScreenshot mobile] state=" + dumpPageState(page));

            final byte[] bytes = cdpFullPageScreenshot(page);
            final Path out = outputDir().resolve("index-mobile.png");
            java.nio.file.Files.write(out, bytes);
            assertTrue(out.toFile().exists(), "mobile スクショ書き出しに失敗: " + out);
        }
    }

    @Test
    @DisplayName("index 画面 desktop 1280x800 のフルページスクショを撮る")
    void shootIndexDesktop() throws Exception {
        try (final Page page = browser.newPage(AUTH)) {
            page.setViewportSize(1280, 800);
            page.setDefaultNavigationTimeout(15_000);
            try {
                page.navigate(BASE + "/v3/index", new Page.NavigateOptions().setWaitUntil(com.microsoft.playwright.options.WaitUntilState.COMMIT).setTimeout(15_000));
            } catch (final Exception ignored) {
                // navigate may time out; we still try to capture whatever rendered
            }
            page.waitForTimeout(3500);

            System.out.println("[ManualScreenshot desktop] state=" + dumpPageState(page));

            final byte[] bytes = cdpFullPageScreenshot(page);
            final Path out = outputDir().resolve("index-desktop.png");
            java.nio.file.Files.write(out, bytes);
            assertTrue(out.toFile().exists(), "desktop スクショ書き出しに失敗: " + out);
        }
    }

    private void shootMobile(final String label, final String path) throws Exception {
        shootViewport(label, path, 390, 844, "mobile");
    }

    private void shootDesktop(final String label, final String path) throws Exception {
        shootViewport(label, path, 1280, 800, "desktop");
    }

    private void shootViewport(final String label, final String path, final int width, final int height, final String viewport) throws Exception {
        try (final Page page = browser.newPage(AUTH)) {
            page.setViewportSize(width, height);
            page.setDefaultNavigationTimeout(15_000);
            try {
                page.navigate(BASE + path, new Page.NavigateOptions().setWaitUntil(com.microsoft.playwright.options.WaitUntilState.COMMIT).setTimeout(15_000));
            } catch (final Exception ignored) {
                // navigate may time out; we still try to capture whatever rendered
            }
            page.waitForTimeout(6000);

            System.out.println("[ManualScreenshot " + label + "/" + viewport + "] state=" + dumpPageState(page));

            final byte[] bytes = cdpFullPageScreenshot(page);
            final Path out = outputDir().resolve(label + "-" + viewport + ".png");
            java.nio.file.Files.write(out, bytes);
            assertTrue(out.toFile().exists(), label + "/" + viewport + " スクショ書き出しに失敗: " + out);
        }
    }

    @Test
    @DisplayName("valuation 画面 mobile 390x844")
    void shootValuationMobile() throws Exception {
        shootMobile("valuation", "/v3/valuation");
    }

    @Test
    @DisplayName("valuation 画面 desktop 1280x800")
    void shootValuationDesktop() throws Exception {
        shootDesktop("valuation", "/v3/valuation");
    }

    @Test
    @DisplayName("edinet-list 画面 mobile 390x844")
    void shootEdinetListMobile() throws Exception {
        shootMobile("edinet-list", "/v3/edinet-list");
    }

    @Test
    @DisplayName("edinet-list 画面 desktop 1280x800")
    void shootEdinetListDesktop() throws Exception {
        shootDesktop("edinet-list", "/v3/edinet-list");
    }

    @Test
    @DisplayName("edinet-list-detail 画面 mobile 390x844")
    void shootEdinetListDetailMobile() throws Exception {
        // 過去の TemplateInputException (documentDetailList の null 要素起因) は
        // commit 6db2fd07 で EdinetDetailPresenter#sanitize() + th:if ガードにより解消済み。
        shootMobile("edinet-list-detail", "/v3/edinet-list-detail?submitDate=2026-03-25");
    }

    @Test
    @DisplayName("edinet-list-detail 画面 desktop 1280x800")
    void shootEdinetListDetailDesktop() throws Exception {
        shootDesktop("edinet-list-detail", "/v3/edinet-list-detail?submitDate=2026-03-25");
    }

    @Test
    @DisplayName("corporate 画面 mobile 390x844")
    void shootCorporateMobile() throws Exception {
        shootMobile("corporate", "/v3/corporate?code=9001");
    }

    @Test
    @DisplayName("corporate 画面 desktop 1280x800")
    void shootCorporateDesktop() throws Exception {
        shootDesktop("corporate", "/v3/corporate?code=9001");
    }
}
