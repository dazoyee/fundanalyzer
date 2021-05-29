package github.com.ioridazo.fundanalyzer.client.selenium;

import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRestClientException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SeleniumClientRetryTest {

    @MockBean(name = "restTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private SeleniumClient client;

    @DisplayName("edinetCodeList : 通信をリトライする")
    @Test
    void retryable() {
        var inputFilePath = "inputFilePath";
        BDDMockito.given(restTemplate.getForObject(any(), any())).willThrow(new RestClientException(""));

        assertThrows(FundanalyzerRestClientException.class, () -> client.edinetCodeList(inputFilePath));
        verify(restTemplate, times(5)).getForObject(any(), any());
    }
}