package github.com.ioridazo.fundanalyzer.client.log;

import github.com.ioridazo.fundanalyzer.domain.value.Document;
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
                "message", "[" + process.getValue().toUpperCase() + "] " + message + " | " + parseDurationTime(durationTime)
        );
    }

    /**
     * /**
     * ユースケース用オブジェクト
     *
     * @param message      メッセージ
     * @param document     ドキュメント
     * @param category     処理カテゴリー
     * @param process      処理内容
     * @param durationTime 処理時間
     * @return LogObject
     */
    public static Map<String, Object> toInteractorLogObject(
            final String message,
            final Document document,
            final Category category,
            final Process process,
            final long durationTime) {
        return Map.of(
                "fundanalyzer", Map.of(
                        "documentId", document.getDocumentId(),
                        "edinetCode", document.getEdinetCode(),
                        "category", category.getValue(),
                        "process", process.getValue(),
                        "function", Function.INTERACTOR.getValue()),
                "message", message + " | " + parseDurationTime(durationTime)
        );
    }

    /**
     * ユースケース用オブジェクト
     *
     * @param message    メッセージ
     * @param documentId 書類ID
     * @param edinetCode EDINETコード
     * @param category   処理カテゴリー
     * @param process    処理内容
     * @return LogObject
     */
    public static Map<String, Object> toInteractorLogObject(
            final String message,
            final String documentId,
            final String edinetCode,
            final Category category,
            final Process process,
            final long durationTime) {
        return Map.of(
                "fundanalyzer", Map.of(
                        "documentId", documentId,
                        "edinetCode", edinetCode,
                        "category", category.getValue(),
                        "process", process.getValue(),
                        "function", Function.INTERACTOR.getValue()),
                "message", message + " | " + parseDurationTime(durationTime)
        );
    }

    /**
     * ユースケース用オブジェクト
     *
     * @param message      メッセージ
     * @param edinetCode   EDINETコード
     * @param category     処理カテゴリー
     * @param process      処理内容
     * @param durationTime 処理時間
     * @return LogObject
     */
    public static Map<String, Object> toInteractorLogObject(
            final String message,
            final String edinetCode,
            final Category category,
            final Process process,
            final long durationTime) {
        return Map.of(
                "fundanalyzer", Map.of(
                        "edinetCode", edinetCode,
                        "category", category.getValue(),
                        "process", process.getValue(),
                        "function", Function.INTERACTOR.getValue()),
                "message", message + " | " + parseDurationTime(durationTime)
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
                "message", message + " | " + parseDurationTime(durationTime)
        );
    }

    /**
     * ユースケース用オブジェクト
     *
     * @param message  メッセージ
     * @param document ドキュメント
     * @param category 処理カテゴリー
     * @param process  処理内容
     * @return LogObject
     */
    public static Map<String, Object> toInteractorLogObject(
            final String message,
            final Document document,
            final Category category,
            final Process process) {
        return Map.of(
                "fundanalyzer", Map.of(
                        "documentId", document.getDocumentId(),
                        "edinetCode", document.getEdinetCode(),
                        "category", category.getValue(),
                        "process", process.getValue(),
                        "function", Function.INTERACTOR.getValue()),
                "message", message
        );
    }

    /**
     * ユースケース用オブジェクト
     *
     * @param message    メッセージ
     * @param edinetCode EDINETコード
     * @param category   処理カテゴリー
     * @param process    処理内容
     * @return LogObject
     */
    public static Map<String, Object> toInteractorLogObject(
            final String message,
            final String edinetCode,
            final Category category,
            final Process process) {
        return Map.of(
                "fundanalyzer", Map.of(
                        "edinetCode", edinetCode,
                        "category", category.getValue(),
                        "process", process.getValue(),
                        "function", Function.INTERACTOR.getValue()),
                "message", message
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
     * @param document ドキュメント
     * @param category 処理カテゴリー
     * @param process  処理内容
     * @return LogObject
     */
    public static Map<String, Object> toSpecificationLogObject(
            final String message,
            final Document document,
            final Category category,
            final Process process) {
        return Map.of(
                "fundanalyzer", Map.of(
                        "documentId", document.getDocumentId(),
                        "edinetCode", document.getEdinetCode(),
                        "category", category.getValue(),
                        "process", process.getValue(),
                        "function", Function.SPECIFICATION.getValue()),
                "message", message
        );
    }

    /**
     * 業務処理用オブジェクト
     *
     * @param message    メッセージ
     * @param documentId 書類ID
     * @param edinetCode EDINETコード
     * @param category   処理カテゴリー
     * @param process    処理内容
     * @return LogObject
     */
    public static Map<String, Object> toSpecificationLogObject(
            final String message,
            final String documentId,
            final String edinetCode,
            final Category category,
            final Process process) {
        return Map.of(
                "fundanalyzer", Map.of(
                        "documentId", documentId,
                        "edinetCode", edinetCode,
                        "category", category.getValue(),
                        "process", process.getValue(),
                        "function", Function.SPECIFICATION.getValue()),
                "message", message
        );
    }

    /**
     * 業務処理用オブジェクト
     *
     * @param message    メッセージ
     * @param edinetCode EDINETコード
     * @param category   処理カテゴリー
     * @param process    処理内容
     * @return LogObject
     */
    public static Map<String, Object> toSpecificationLogObject(
            final String message,
            final String edinetCode,
            final Category category,
            final Process process) {
        return Map.of(
                "fundanalyzer", Map.of(
                        "edinetCode", edinetCode,
                        "category", category.getValue(),
                        "process", process.getValue(),
                        "function", Function.SPECIFICATION.getValue()),
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
    @SuppressWarnings("unused")
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
     * @param message    メッセージ
     * @param documentId 書類ID
     * @param category   処理カテゴリー
     * @param process    処理内容
     * @return LogObject
     */
    public static Map<String, Object> toClientLogObject(
            final String message,
            final String documentId,
            final Category category,
            final Process process) {
        return Map.of(
                "fundanalyzer", Map.of(
                        "documentId", documentId,
                        "category", category.getValue(),
                        "process", process.getValue(),
                        "function", Function.CLIENT.getValue()),
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

    static String parseDurationTime(final long time) {
        if (3600000 < time) {
            return time / 3600000 + "." + Math.round((double) time % 3600000) / 1000 + "h";
        } else if (60000 < time) {
            return time / 60000 + "." + Math.round((double) time % 60000) / 1000 + "m";
        } else if (1000 < time) {
            return time / 1000 + "." + Math.round((double) time % 1000) / 100 + "s";
        } else {
            return time + "ms";
        }
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
