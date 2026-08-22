package github.com.ioridazo.fundanalyzer.web.view.model.index;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * システムイベントのメッセージから表示用の項目を取り出すユーティリティ。
 * <p>
 * {@code system_event} テーブルはメッセージを 1 カラムの文字列として保持するため、
 * 画面で企業・書類単位に集約するには記録側が組み立てた
 * 「ラベル:値」形式のメッセージを読み解く必要がある。
 * ラベルに対応する値が見つからないメッセージ（スケジューラのエラー等）は
 * 集約対象外として扱えるよう empty を返す。
 */
final class SystemEventMessageFields {

    private static final String COMPANY_CODE = "企業コード";
    private static final String DOCUMENT_ID = "書類ID";
    private static final String FINANCIAL_STATEMENT = "財務諸表";
    private static final String SUBJECT_ID = "科目ID";
    private static final String PREVIOUS_VALUE = "前回値";
    private static final String CURRENT_VALUE = "今回値";
    private static final String RATIO = "比率";

    private SystemEventMessageFields() {
    }

    /**
     * 集約キーとなる企業コードを取り出す。
     *
     * @param message メッセージ
     * @return 企業コード
     */
    static Optional<String> companyCode(final String message) {
        return field(message, COMPANY_CODE);
    }

    /**
     * 集約キーとなる書類IDを取り出す。
     *
     * @param message メッセージ
     * @return 書類ID
     */
    static Optional<String> documentId(final String message) {
        return field(message, DOCUMENT_ID);
    }

    /**
     * 明細行に表示する要約を組み立てる。
     * ラベル形式ではないメッセージはそのまま返す。
     *
     * @param message メッセージ
     * @return 明細行に表示する文字列
     */
    static String detail(final String message) {
        final Optional<String> statement = field(message, FINANCIAL_STATEMENT);
        final Optional<String> subjectId = field(message, SUBJECT_ID);
        if (statement.isEmpty() || subjectId.isEmpty()) {
            return message;
        }
        return String.format(
                "%s 科目ID:%s %s → %s（比率 %s）",
                statement.get(),
                subjectId.get(),
                field(message, PREVIOUS_VALUE).orElse("-"),
                field(message, CURRENT_VALUE).orElse("-"),
                field(message, RATIO).orElse("-")
        );
    }

    private static Optional<String> field(final String message, final String label) {
        if (message == null) {
            return Optional.empty();
        }
        final Matcher matcher = Pattern.compile(Pattern.quote(label) + ":(\\S+)").matcher(message);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }
}
