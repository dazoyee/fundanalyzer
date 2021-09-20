package github.com.ioridazo.fundanalyzer.client.log;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FundanalyzerLogClientTest {

    @Test
    void ms() {
        assertEquals("1000ms", FundanalyzerLogClient.parseDurationTime(1000));
    }

    @Test
    void s_min() {
        assertEquals("1.3s", FundanalyzerLogClient.parseDurationTime(1303));
    }

    @Test
    void s_max() {
        assertEquals("59.0s", FundanalyzerLogClient.parseDurationTime(59003));
    }

    @Test
    void m_min() {
        assertEquals("1.1m", FundanalyzerLogClient.parseDurationTime(61500));
    }

    @Test
    void m_max() {
        assertEquals("59.50m", FundanalyzerLogClient.parseDurationTime(3590000));
    }

    @Test
    void h() {
        assertEquals("1.10h", FundanalyzerLogClient.parseDurationTime(3610000));
    }
}