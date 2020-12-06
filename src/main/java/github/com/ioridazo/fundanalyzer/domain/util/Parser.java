package github.com.ioridazo.fundanalyzer.domain.util;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class Parser {

    private Parser() {
    }

    public static String parseStringNikkei(final String value) {
        return Optional.of(value)
                .map(v -> v.substring(v.lastIndexOf(("）")) + 1))
                .map(String::trim)
                .map(v -> v.replace(" ", ""))
                .orElseThrow(() -> {
                    log.warn("値の変換に失敗したため、NULLで登録します。\tvalue:{}", value);
                    return null;
                });
    }

    public static Optional<Integer> parseIntegerStock(final String value) {
        try {
            return Optional.of(Integer.valueOf(value));
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
                            log.warn("株価変換処理において数値を正常に認識できなかったため、NULLで登録します。\tvalue:{}", v);
                            return null;
                        }
                    });
        }
    }

    public static Optional<Double> parseDoubleStock(final String value) {
        try {
            return Optional.of(Double.valueOf(value));
        } catch (NumberFormatException e1) {
            return Optional.of(value)
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
                            log.warn("株価変換処理において数値を正常に認識できなかったため、NULLで登録します。\tvalue:{}", v);
                            return null;
                        }
                    });
        }
    }

    public static Optional<Double> parseDoubleMinkabu(final String value) {
        try {
            return Optional.of(Double.valueOf(value));
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
                            if (v.equals("--")) {
                                return null;
                            } else {
                                return Double.valueOf(v);
                            }
                        } catch (NumberFormatException e2) {
                            log.warn("株価変換処理において数値を正常に認識できなかったため、NULLで登録します。\tvalue:{}", v);
                            return null;
                        }
                    });
        }
    }
}