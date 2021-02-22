package github.com.ioridazo.fundanalyzer.domain.log;

import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.ThreadContext;

import java.util.Map;

@Log4j2
public class FundanalyzerLogClient {

    /**
     * 処理開始ログ
     *
     * @param category 処理カテゴリー
     * @param process  処理内容
     */
    public static void logProcessStart(final Category category, final Process process) {
        LogTime.putStartTime(System.currentTimeMillis());
        log.info(message(Activity.BEGINNING, category, process, 0));
    }

    /**
     * 処理終了ログ
     *
     * @param category 処理カテゴリー
     * @param process  処理内容
     */
    public static void logProcessEnd(final Category category, final Process process) {
        final long durationTime = System.currentTimeMillis() - LogTime.getStartTime();
        log.info(message(Activity.END, category, process, durationTime));
        LogTime.removeStartTime();
    }

    /**
     * サービス層ログ
     *
     * @param message  メッセージ
     * @param category 処理カテゴリー
     * @param process  処理内容
     */
    public static void logService(final String message, final Category category, final Process process) {
        log.info(message(category, process, Function.SERVICE, message));
    }

    /**
     * ビジネスロジック層ログ
     *
     * @param message  メッセージ
     * @param category 処理カテゴリー
     * @param process  処理内容
     */
    public static void logLogic(final String message, final Category category, final Process process) {
        log.info(message(category, process, Function.LOGIC, message));
    }

    /**
     * プロキシ層ログ
     *
     * @param message  メッセージ
     * @param category 処理カテゴリー
     * @param process  処理内容
     */
    public static void logProxy(final String message, final Category category, final Process process) {
        log.info(message(category, process, Function.LOGIC, message));
    }

    private static Map<String, Object> message(
            final Activity activity,
            final Category category,
            final Process process,
            final long durationTime) {
        return Map.of(
                "fundanalyzer", Map.of(
                        "activity", activity.getValue(),
                        "category", category.getValue(),
                        "process", process.getValue(),
                        "function", Function.CONTROLLER.getValue()),
                "message", "[" + activity.getValue() + "] " + category.getValue() + " : " + process.getValue() + " | " + durationTime + "ms"
        );
    }

    private static Map<String, Object> message(
            final Category category,
            final Process process,
            final Function function,
            final String message) {
        return Map.of(
                "fundanalyzer", Map.of(
                        "category", category.getValue(),
                        "process", process.getValue(),
                        "function", function.getValue()
                ),
                "message", message
        );
    }

    @Value
    static class LogObject {
        Activity activity;
        Category category;
        Process process;
        Function function;
        String message;
    }

    private enum Activity {
        BEGINNING("beginning"),
        END("end"),
        ;

        private final String value;

        Activity(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private enum Function {
        CONTROLLER("controller"),
        SERVICE("service"),
        LOGIC("logic"),
        ;

        private final String value;

        Function(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private static class LogTime {

        private static final String START_TIME = "startTime";

        static void putStartTime(final long startTime) {
            ThreadContext.put(START_TIME, String.valueOf(startTime));
        }

        static long getStartTime() {
            return Long.parseLong(ThreadContext.get(START_TIME));
        }

        static void removeStartTime() {
            ThreadContext.remove(START_TIME);
        }
    }
}
