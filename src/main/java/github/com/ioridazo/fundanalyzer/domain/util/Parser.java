package github.com.ioridazo.fundanalyzer.domain.util;

import lombok.extern.log4j.Log4j2;

import java.text.MessageFormat;
import java.util.Optional;

@Log4j2
public final class Parser {

    private static final String MESSAGE = "株価変換処理において数値を正常に認識できなかったため、NULLで登録します。\tvalue:{0}";

    private Parser() {
    }

    public static String parseStringNikkei(final String value) {
        return Optional.ofNullable(value)
                .map(v -> v.substring(v.lastIndexOf(("）")) + 1))
                .map(String::trim)
                .map(v -> v.replace(" ", ""))
                .orElseGet(() -> {
                    log.warn("値の変換に失敗したため、NULLで登録します。");
                    return null;
                });
    }

    public static Optional<Integer> parseIntegerVolume(final String value) {
        try {
            return Optional.ofNullable(value)
                    .map(v -> v.replace(",", ""))
                    .map(Integer::valueOf);
        } catch (NumberFormatException e1) {
            return Optional.of(value)
                    .map(v -> v.substring(v.lastIndexOf(("売買高")) + 4, v.length() - 1))
                    .map(String::trim)
                    .map(v -> v.replace(" ", ""))
                    .map(v -> v.replace(",", ""))
                    .map(v -> {
                        try {
                            if (v.equals("--")) {
                                return null;
                            } else {
                                return Integer.valueOf(v);
                            }
                        } catch (NumberFormatException e2) {
                            log.warn(MessageFormat.format(MESSAGE, v));
                            return null;
                        }
                    });
        }
    }

    public static Optional<Double> parseDoubleNikkei(final String value) {
        return Optional.ofNullable(value)
                .map(v -> {
                    if (v.contains(")")) {
                        return v.substring(v.lastIndexOf((")")) + 1, v.length() - 1);
                    } else if (v.contains("）")) {
                        return v.substring(v.lastIndexOf(("）")) + 1, v.length() - 1);
                    } else {
                        return v.substring(0, v.length() - 1);
                    }
                })
                .map(String::trim)
                .map(v -> v.replace(",", ""))
                .map(v -> {
                    try {
                        if (v.equals("--")) {
                            return null;
                        } else {
                            return Double.valueOf(v);
                        }
                    } catch (NumberFormatException e2) {
                        log.warn(MessageFormat.format(MESSAGE, v));
                        return null;
                    }
                });
    }

    public static Optional<Double> parseDoubleKabuoji3(final String value) {
        try {
            return Optional.ofNullable(value).map(Double::valueOf);
        } catch (NumberFormatException e1) {
            log.warn(MessageFormat.format(MESSAGE, value));
            return Optional.empty();
        }
    }

    public static Optional<Double> parseDoubleMinkabu(final String value) {
        try {
            return Optional.ofNullable(value).map(Double::valueOf);
        } catch (NumberFormatException e1) {
            return Optional.of(value)
                    .map(v -> {
                        if (v.contains(". ") && v.contains("円")) {
                            return v.replace(". ", ".").replace("円", "");
                        } else if (v.contains("円")) {
                            return v.replace("円", "");
                        } else if (v.equals("")) {
                            return null;
                        } else {
                            return v;
                        }
                    })
                    .map(String::trim)
                    .map(v -> v.replace(",", ""))
                    .map(v -> {
                        try {
                            if (v.equals("--") || v.equals("---")) {
                                return null;
                            } else {
                                return Double.valueOf(v);
                            }
                        } catch (NumberFormatException e2) {
                            log.warn(MessageFormat.format(MESSAGE, v));
                            return null;
                        }
                    });
        }
    }

    public static Optional<Double> parseDoubleYahooFinance(final String value) {
        try {
            return Optional.ofNullable(value)
                    .map(v -> v.replace(",", ""))
                    .map(Double::valueOf);
        } catch (NumberFormatException e1) {
            log.warn(MessageFormat.format(MESSAGE, value));
            return Optional.empty();
        }
    }
}
