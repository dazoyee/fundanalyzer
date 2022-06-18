package github.com.ioridazo.fundanalyzer.domain.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParserTest {

    @Test
    void parseStringNikkei() {
        assertEquals("1.02倍", Parser.parseStringNikkei("PBR（実績）（解説） 1.02 倍"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"売買高 17,847,600 株", "17847600", "17,847,600"})
    void parseIntegerVolume(String value) {
        assertEquals("17847600", Parser.parseIntegerVolume(value).orElseThrow().toString());
    }

    @Test
    void parseDoubleNikkei() {
        assertEquals("1047.0", Parser.parseDoubleNikkei("1,047 円").orElseThrow().toString());
        assertEquals("1047.0", Parser.parseDoubleNikkei("始値 (15:00) 1,047 円").orElseThrow().toString());
    }

    @Test
    void parseDoubleKabuoji3() {
        assertEquals("1274.5", Parser.parseDoubleKabuoji3("1274.5").orElseThrow().toString());
    }

    @Test
    void parseDoubleMinkabu() {
        assertEquals("408.0", Parser.parseDoubleMinkabu("408. 0 円").orElseThrow().toString());
    }

    @Test
    void parseDoubleYahooFinance() {
        assertEquals("1092.0", Parser.parseDoubleYahooFinance("1,092").orElseThrow().toString());
    }
}