package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.domain.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.CompanyEntity;
import github.com.ioridazo.fundanalyzer.client.csv.bean.EdinetCsvResultBean;
import github.com.ioridazo.fundanalyzer.client.edinet.entity.response.Results;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompanySpecificationTest {

    private CompanyDao companyDao;

    private CompanySpecification companySpecification;

    @BeforeEach
    void setUp() {
        companyDao = Mockito.mock(CompanyDao.class);

        companySpecification = Mockito.spy(new CompanySpecification(
                companyDao,
                Mockito.mock(IndustrySpecification.class)
        ));
    }

    @DisplayName("findLastUpdateDateTime : ")
    @Test
    void findLastUpdateDateTime() {
        when(companyDao.selectAll()).thenReturn(List.of(defaultCompanyEntity()));

        assertEquals("2021/05/08 23:37:00", companySpecification.findLastUpdateDateTime().orElseThrow());
    }

    @Nested
    class insertIfNotExist {

        @DisplayName("insertIfNotExist : データベースに存在したらinsertしない")
        @Test
        void exist() {
            var results = new Results();
            results.setEdinetCode("edinetCode");

            when(companyDao.selectByEdinetCode("edinetCode")).thenReturn(Optional.of(defaultCompanyEntity()));

            assertDoesNotThrow(() -> companySpecification.insertIfNotExist(results));
            verify(companyDao, times(0)).insert(any());
        }

        @DisplayName("insertIfNotExist : データベースに存在しなかったらinsertする")
        @Test
        void not_exist() {
            var results = new Results();
            results.setEdinetCode("edinetCode");

            when(companyDao.selectByEdinetCode("edinetCode")).thenReturn(Optional.empty());

            assertDoesNotThrow(() -> companySpecification.insertIfNotExist(results));
            verify(companyDao, times(1)).insert(any());
        }
    }

    @Nested
    class upsert {

        @DisplayName("upsert : 企業情報がデータベースに存在したらupdateする")
        @Test
        void update() {
            var resultBean = new EdinetCsvResultBean();
            resultBean.setEdinetCode("edinetCode");
            resultBean.setSecuritiesCode("");
            resultBean.setSettlementDate("");
            var resultBeanList = List.of(resultBean);

            when(companyDao.selectAll()).thenReturn(List.of(defaultCompanyEntity()));

            assertDoesNotThrow(() -> companySpecification.upsert(resultBeanList));
            verify(companyDao, times(1)).update(any());
            verify(companyDao, times(0)).insert(any());
        }

        @DisplayName("upsert : 企業情報がデータベースに存在しなかったらinsertする")
        @Test
        void insert() {
            var resultBean = new EdinetCsvResultBean();
            resultBean.setEdinetCode("edinetCode2");
            resultBean.setSecuritiesCode("");
            resultBean.setSettlementDate("");
            var resultBeanList = List.of(resultBean);

            when(companyDao.selectAll()).thenReturn(List.of(defaultCompanyEntity()));

            assertDoesNotThrow(() -> companySpecification.upsert(resultBeanList));
            verify(companyDao, times(0)).update(any());
            verify(companyDao, times(1)).insert(any());
        }
    }


    private CompanyEntity defaultCompanyEntity() {
        return new CompanyEntity(
                null,
                null,
                null,
                "edinetCode",
                null,
                null,
                null,
                null,
                null,
                LocalDateTime.of(2021, 5, 8, 23, 37)
        );
    }
}