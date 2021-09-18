package github.com.ioridazo.fundanalyzer.client.log;

import lombok.extern.log4j.Log4j2;

import java.util.Map;

@Log4j2
public class FundanalyzerLogClient {

    /**
     * アクセスログ用オブジェクト
     *
     * @param category     処理カテゴリー
     * @param process      処理内容
     * @param message      処理内容
     * @param durationTime 処理時間
     * @return LogObject
     */
    public static Map<String, Object> toAccessLogObject(
            final Category category,
            final Process process,
            final String message,
            final long durationTime) {
        return Map.of(
                "fundanalyzer", Map.of(
                        "category", category.getValue(),
                        "process", process.getValue(),
                        "function", Function.ACCESS.getValue()),
                "message", "[" + process.getValue().toUpperCase() + "] " + message + " | " + durationTime + "ms"
        );
    }

    /**
     * ユースケース用オブジェクト
     *
     * @param message      メッセージ
     * @param category     処理カテゴリー
     * @param process      処理内容
     * @param durationTime 処理時間
     * @return LogObject
     */
    public static Map<String, Object> toInteractorLogObject(
            final String message,
            final Category category,
            final Process process,
            final long durationTime) {
        return Map.of(
                "fundanalyzer", Map.of(
                        "category", category.getValue(),
                        "process", process.getValue(),
                        "function", Function.INTERACTOR.getValue()),
                "message", message + " | " + durationTime + "ms"
        );
    }

    /**
     * ユースケース用オブジェクト
     *
     * @param message  メッセージ
     * @param category 処理カテゴリー
     * @param process  処理内容
     * @return LogObject
     */
    public static Map<String, Object> toInteractorLogObject(
            final String message,
            final Category category,
            final Process process) {
        return Map.of(
                "fundanalyzer", Map.of(
                        "category", category.getValue(),
                        "process", process.getValue(),
                        "function", Function.INTERACTOR.getValue()),
                "message", message
        );
    }

    /**
     * 業務処理用オブジェクト
     *
     * @param message  メッセージ
     * @param category 処理カテゴリー
     * @param process  処理内容
     * @return LogObject
     */
    public static Map<String, Object> toSpecificationLogObject(
            final String message,
            final Category category,
            final Process process) {
        return Map.of(
                "fundanalyzer", Map.of(
                        "category", category.getValue(),
                        "process", process.getValue(),
                        "function", Function.SPECIFICATION.getValue()),
                "message", message
        );
    }

    /**
     * クライアント用オブジェクト
     *
     * @param message  メッセージ
     * @param category 処理カテゴリー
     * @param process  処理内容
     * @return LogObject
     */
    public static Map<String, Object> toClientLogObject(
            final String message,
            final Category category,
            final Process process) {
        return Map.of(
                "fundanalyzer", Map.of(
                        "category", category.getValue(),
                        "process", process.getValue(),
                        "function", Function.CLIENT.getValue()),
                "message", message
        );
    }

    private enum Function {
        ACCESS("access"),
        CONTROLLER("controller"),
        SERVICE("service"),
        INTERACTOR("interactor"),
        SPECIFICATION("specification"),
        CLIENT("client"),
        ;

        private final String value;

        Function(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
