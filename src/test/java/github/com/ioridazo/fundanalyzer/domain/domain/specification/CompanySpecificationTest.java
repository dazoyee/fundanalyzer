package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.client.csv.bean.EdinetCsvResultBean;
import github.com.ioridazo.fundanalyzer.client.edinet.entity.response.Results;
import github.com.ioridazo.fundanalyzer.domain.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.CompanyEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.Consolidated;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.ListCategories;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompanySpecificationTest {

    private CompanyDao companyDao;

    private IndustrySpecification industrySpecification;

    private CompanySpecification companySpecification;

    @BeforeEach
    void setUp() {
        companyDao = Mockito.mock(CompanyDao.class);
        industrySpecification = Mockito.mock(IndustrySpecification.class);

        companySpecification = Mockito.spy(new CompanySpecification(
                companyDao,
                industrySpecification
        ));
    }

    @DisplayName("findLastUpdateDateTime : 企業情報更新日時を取得する")
    @Test
    void findLastUpdateDateTime() {
        when(companyDao.maxUpdatedAt()).thenReturn(Optional.of(LocalDateTime.of(2021, 5, 8, 23, 37)));
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

            when(companyDao.selectByCodeIsNotNull()).thenReturn(List.of(defaultCompanyEntity()));
            when(companyDao.selectByEdinetCode("edinetCode")).thenReturn(Optional.of(defaultCompanyEntity()));

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

            when(companyDao.selectByCodeIsNotNull()).thenReturn(List.of(defaultCompanyEntity()));
            when(companyDao.selectByEdinetCode("edinetCode")).thenReturn(Optional.empty());

            assertDoesNotThrow(() -> companySpecification.upsert(resultBeanList));
            verify(companyDao, times(0)).update(any());
            verify(companyDao, times(1)).insert(any());
        }
    }


    @Nested
    @DisplayName("findCompanyByEdinetCode メソッド")
    class FindCompanyByEdinetCode {

        @DisplayName("findCompanyByEdinetCode : 該当する企業が存在し証券コードを持つ場合は企業情報を返す")
        @Test
        void existsWithCode() {
            final CompanyEntity entity = livedCompanyEntity("1234", "edinetCode", 10);
            when(companyDao.selectByEdinetCode("edinetCode")).thenReturn(Optional.of(entity));
            when(industrySpecification.convertFromIdToName(10)).thenReturn("情報・通信業");

            final Optional<Company> actual = companySpecification.findCompanyByEdinetCode("edinetCode");

            assertTrue(actual.isPresent());
            final Company company = actual.orElseThrow();
            assertAll(
                    () -> assertEquals("1234", company.code()),
                    () -> assertEquals("edinetCode", company.edinetCode()),
                    () -> assertEquals(10, company.industryId()),
                    () -> assertEquals("情報・通信業", company.industryName())
            );
        }

        @DisplayName("findCompanyByEdinetCode : 該当する企業が存在するが証券コードを持たない場合は空を返す")
        @Test
        void existsWithoutCode() {
            final CompanyEntity entity = new CompanyEntity(
                    null,
                    "no-code-company",
                    10,
                    "edinetCode",
                    null,
                    null,
                    null,
                    null,
                    "0",
                    "0",
                    null,
                    null,
                    LocalDateTime.of(2021, 5, 8, 23, 37)
            );
            when(companyDao.selectByEdinetCode("edinetCode")).thenReturn(Optional.of(entity));

            final Optional<Company> actual = companySpecification.findCompanyByEdinetCode("edinetCode");

            assertTrue(actual.isEmpty());
        }

        @DisplayName("findCompanyByEdinetCode : 該当する企業が存在しない場合は空を返す")
        @Test
        void notFound() {
            when(companyDao.selectByEdinetCode("edinetCode")).thenReturn(Optional.empty());

            final Optional<Company> actual = companySpecification.findCompanyByEdinetCode("edinetCode");

            assertTrue(actual.isEmpty());
        }
    }

    @Nested
    @DisplayName("findCompanyByCode メソッド")
    class FindCompanyByCode {

        @DisplayName("findCompanyByCode : 該当する企業が存在する場合は企業情報を返す")
        @Test
        void exists() {
            final CompanyEntity entity = livedCompanyEntity("1234", "edinetCode", 10);
            when(companyDao.selectByCode("1234")).thenReturn(Optional.of(entity));
            when(industrySpecification.convertFromIdToName(10)).thenReturn("情報・通信業");

            final Optional<Company> actual = companySpecification.findCompanyByCode("1234");

            assertTrue(actual.isPresent());
            assertEquals("1234", actual.orElseThrow().code());
            assertEquals("情報・通信業", actual.orElseThrow().industryName());
        }

        @DisplayName("findCompanyByCode : 該当する企業が存在しない場合は空を返す")
        @Test
        void notFound() {
            when(companyDao.selectByCode("9999")).thenReturn(Optional.empty());

            final Optional<Company> actual = companySpecification.findCompanyByCode("9999");

            assertTrue(actual.isEmpty());
        }
    }

    @Nested
    @DisplayName("findLastUpdateDateTime メソッド")
    class FindLastUpdateDateTime {

        @DisplayName("findLastUpdateDateTime : 更新日時が存在しない場合は空を返す")
        @Test
        void empty() {
            when(companyDao.maxUpdatedAt()).thenReturn(Optional.empty());

            final Optional<String> actual = companySpecification.findLastUpdateDateTime();

            assertTrue(actual.isEmpty());
        }
    }

    @Nested
    @DisplayName("findCompanyByIndustry メソッド")
    class FindCompanyByIndustry {

        @DisplayName("findCompanyByIndustry : 証券コードを持ち存続する企業のみが返る")
        @Test
        void onlyLivedWithCode() {
            final CompanyEntity lived = livedCompanyEntity("1111", "E0001", 10);
            final CompanyEntity removed = removedCompanyEntity("2222", "E0002", 10);
            final CompanyEntity noCode = new CompanyEntity(
                    null,
                    "no-code",
                    10,
                    "E0003",
                    null,
                    null,
                    null,
                    null,
                    "0",
                    "0",
                    null,
                    null,
                    LocalDateTime.of(2021, 5, 8, 23, 37)
            );
            when(companyDao.selectByIndustryId(10)).thenReturn(List.of(lived, removed, noCode));
            when(industrySpecification.convertFromIdToName(10)).thenReturn("情報・通信業");

            final List<Company> actual = companySpecification.findCompanyByIndustry(10);

            assertEquals(1, actual.size());
            assertEquals("1111", actual.get(0).code());
            assertTrue(actual.get(0).lived());
        }

        @DisplayName("findCompanyByIndustry : 該当データがない場合は空のリストを返す")
        @Test
        void empty() {
            when(companyDao.selectByIndustryId(99)).thenReturn(List.of());

            final List<Company> actual = companySpecification.findCompanyByIndustry(99);

            assertTrue(actual.isEmpty());
        }
    }

    @Nested
    @DisplayName("findFavoriteCompanies メソッド")
    class FindFavoriteCompanies {

        @DisplayName("findFavoriteCompanies : 証券コードを持つ企業のみが返る")
        @Test
        void onlyWithCode() {
            final CompanyEntity withCode = livedCompanyEntity("1111", "E0001", 10);
            final CompanyEntity withoutCode = new CompanyEntity(
                    null,
                    "no-code",
                    10,
                    "E0002",
                    null,
                    null,
                    null,
                    null,
                    "1",
                    "0",
                    null,
                    null,
                    LocalDateTime.of(2021, 5, 8, 23, 37)
            );
            when(companyDao.selectByFavorite()).thenReturn(List.of(withCode, withoutCode));
            when(industrySpecification.convertFromIdToName(10)).thenReturn("情報・通信業");

            final List<Company> actual = companySpecification.findFavoriteCompanies();

            assertEquals(1, actual.size());
            assertEquals("1111", actual.get(0).code());
        }

        @DisplayName("findFavoriteCompanies : お気に入り企業がない場合は空のリストを返す")
        @Test
        void empty() {
            when(companyDao.selectByFavorite()).thenReturn(List.of());

            final List<Company> actual = companySpecification.findFavoriteCompanies();

            assertTrue(actual.isEmpty());
        }
    }

    @Nested
    @DisplayName("updateFavorite メソッド")
    class UpdateFavorite {

        @DisplayName("updateFavorite : お気に入りでない企業を更新するとtrueを返す")
        @Test
        void favoriteFalse() {
            final Company company = new Company(
                    "1234",
                    "テスト株式会社",
                    10,
                    "情報・通信業",
                    "edinetCode",
                    ListCategories.NULL,
                    Consolidated.NULL,
                    null,
                    null,
                    false,
                    false,
                    true
            );

            final boolean actual = companySpecification.updateFavorite(company);

            assertTrue(actual);
            verify(companyDao, times(1)).update(any(CompanyEntity.class));
        }

        @DisplayName("updateFavorite : お気に入り済み企業を更新するとfalseを返す")
        @Test
        void favoriteTrue() {
            final Company company = new Company(
                    "1234",
                    "テスト株式会社",
                    10,
                    "情報・通信業",
                    "edinetCode",
                    ListCategories.NULL,
                    Consolidated.NULL,
                    null,
                    null,
                    true,
                    false,
                    true
            );

            final boolean actual = companySpecification.updateFavorite(company);

            assertFalse(actual);
            verify(companyDao, times(1)).update(any(CompanyEntity.class));
        }
    }

    @Nested
    @DisplayName("findStarCompanies メソッド")
    class FindStarCompanies {

        @DisplayName("findStarCompanies : 証券コードを持つ企業のみが返る")
        @Test
        void onlyWithCode() {
            final CompanyEntity withCode = starCompanyEntity("1111", "E0001", 10);
            final CompanyEntity withoutCode = new CompanyEntity(
                    null,
                    "no-code",
                    10,
                    "E0002",
                    null,
                    null,
                    null,
                    null,
                    "0",
                    "0",
                    "1",
                    LocalDateTime.of(2021, 5, 8, 23, 37),
                    LocalDateTime.of(2021, 5, 8, 23, 37)
            );
            when(companyDao.selectByStar()).thenReturn(List.of(withCode, withoutCode));
            when(industrySpecification.convertFromIdToName(10)).thenReturn("情報・通信業");

            final List<Company> actual = companySpecification.findStarCompanies();

            assertEquals(1, actual.size());
            assertEquals("1111", actual.get(0).code());
            assertTrue(actual.get(0).star());
        }

        @DisplayName("findStarCompanies : 注目企業がない場合は空のリストを返す")
        @Test
        void empty() {
            when(companyDao.selectByStar()).thenReturn(List.of());

            final List<Company> actual = companySpecification.findStarCompanies();

            assertTrue(actual.isEmpty());
        }
    }

    @Nested
    @DisplayName("updateRemoved メソッド")
    class UpdateRemoved {

        @DisplayName("updateRemoved : 除外フラグを有効にするエンティティでDAOを呼び出す")
        @Test
        void update() {
            final Company company = new Company(
                    "1234",
                    "テスト株式会社",
                    10,
                    "情報・通信業",
                    "edinetCode",
                    ListCategories.NULL,
                    Consolidated.NULL,
                    null,
                    null,
                    false,
                    false,
                    true
            );

            companySpecification.updateRemoved(company);

            final ArgumentCaptor<CompanyEntity> captor = ArgumentCaptor.forClass(CompanyEntity.class);
            verify(companyDao, times(1)).update(captor.capture());
            final CompanyEntity captured = captor.getValue();
            assertAll(
                    () -> assertEquals("edinetCode", captured.getEdinetCode()),
                    () -> assertEquals("1", captured.getRemoved()),
                    () -> assertNull(captured.getFavorite())
            );
        }
    }

    @Nested
    @DisplayName("updateStar メソッド")
    class UpdateStar {

        @DisplayName("updateStar : 注目でない企業を更新するとtrueを返す")
        @Test
        void starFalse() {
            final Company company = new Company(
                    "1234",
                    "テスト株式会社",
                    10,
                    "情報・通信業",
                    "edinetCode",
                    ListCategories.NULL,
                    Consolidated.NULL,
                    null,
                    null,
                    false,
                    false,
                    true
            );

            final boolean actual = companySpecification.updateStar(company);

            assertTrue(actual);
            verify(companyDao, times(1)).update(any(CompanyEntity.class));
        }

        @DisplayName("updateStar : 注目済み企業を更新するとfalseを返す")
        @Test
        void starTrue() {
            final Company company = new Company(
                    "1234",
                    "テスト株式会社",
                    10,
                    "情報・通信業",
                    "edinetCode",
                    ListCategories.NULL,
                    Consolidated.NULL,
                    null,
                    null,
                    false,
                    true,
                    true
            );

            final boolean actual = companySpecification.updateStar(company);

            assertFalse(actual);
            verify(companyDao, times(1)).update(any(CompanyEntity.class));
        }
    }

    @Nested
    @DisplayName("inquiryAllTargetCompanies メソッド")
    class InquiryAllTargetCompanies {

        @DisplayName("inquiryAllTargetCompanies : 存続中かつ対象業種の企業のみが返る")
        @Test
        void onlyLivedAndTarget() {
            final CompanyEntity lived = livedCompanyEntity("1111", "E0001", 10);
            final CompanyEntity removed = removedCompanyEntity("2222", "E0002", 10);
            final CompanyEntity nonTarget = livedCompanyEntity("3333", "E0003", 99);
            when(companyDao.selectByCodeIsNotNull()).thenReturn(List.of(lived, removed, nonTarget));
            when(industrySpecification.convertFromIdToName(10)).thenReturn("情報・通信業");
            when(industrySpecification.convertFromIdToName(99)).thenReturn("対象外業種");
            when(industrySpecification.isTarget(10)).thenReturn(true);
            when(industrySpecification.isTarget(99)).thenReturn(false);

            final List<Company> actual = companySpecification.inquiryAllTargetCompanies();

            assertEquals(1, actual.size());
            assertEquals("1111", actual.get(0).code());
            assertTrue(actual.get(0).lived());
        }

        @DisplayName("inquiryAllTargetCompanies : 該当する企業がない場合は空のリストを返す")
        @Test
        void empty() {
            when(companyDao.selectByCodeIsNotNull()).thenReturn(List.of());

            final List<Company> actual = companySpecification.inquiryAllTargetCompanies();

            assertTrue(actual.isEmpty());
        }
    }

    @Nested
    @DisplayName("findAllTargetCompanies メソッド")
    class FindAllTargetCompanies {

        @DisplayName("findAllTargetCompanies : 存続中かつ対象業種の企業のみが返る")
        @Test
        void onlyLivedAndTarget() {
            final CompanyEntity lived = livedCompanyEntity("1111", "E0001", 10);
            final CompanyEntity removed = removedCompanyEntity("2222", "E0002", 10);
            when(companyDao.selectByCodeIsNotNull()).thenReturn(List.of(lived, removed));
            when(industrySpecification.convertFromIdToName(10)).thenReturn("情報・通信業");
            when(industrySpecification.isTarget(10)).thenReturn(true);

            final List<Company> actual = companySpecification.findAllTargetCompanies();

            assertEquals(1, actual.size());
            assertEquals("1111", actual.get(0).code());
        }
    }

    private CompanyEntity livedCompanyEntity(final String code, final String edinetCode, final Integer industryId) {
        return new CompanyEntity(
                code,
                "テスト株式会社",
                industryId,
                edinetCode,
                "1",
                "1",
                100,
                "12-31",
                "0",
                "0",
                "0",
                LocalDateTime.of(2021, 5, 8, 23, 37),
                LocalDateTime.of(2021, 5, 8, 23, 37)
        );
    }

    private CompanyEntity removedCompanyEntity(final String code, final String edinetCode, final Integer industryId) {
        return new CompanyEntity(
                code,
                "テスト株式会社",
                industryId,
                edinetCode,
                "1",
                "1",
                100,
                "12-31",
                "0",
                "1",
                "0",
                LocalDateTime.of(2021, 5, 8, 23, 37),
                LocalDateTime.of(2021, 5, 8, 23, 37)
        );
    }

    private CompanyEntity starCompanyEntity(final String code, final String edinetCode, final Integer industryId) {
        return new CompanyEntity(
                code,
                "テスト株式会社",
                industryId,
                edinetCode,
                "1",
                "1",
                100,
                "12-31",
                "0",
                "0",
                "1",
                LocalDateTime.of(2021, 5, 8, 23, 37),
                LocalDateTime.of(2021, 5, 8, 23, 37)
        );
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
                null,
                null,
                null,
                LocalDateTime.of(2021, 5, 8, 23, 37)
        );
    }
}
