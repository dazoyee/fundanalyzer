package github.com.ioridazo.fundanalyzer.domain.domain.entity.master;

import github.com.ioridazo.fundanalyzer.domain.value.Company;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompanyEntityTest {

    @DisplayName("ofUpdateFavorite : お気に入りを更新する")
    @Test
    void ofUpdateFavorite() {
        var companyEntity = CompanyEntity.ofUpdateFavorite(
                new Company(
                        null,
                        null,
                        null,
                        null,
                        "edinetCode",
                        null,
                        null,
                        null,
                        null,
                        false,
                        true
                ),
                LocalDateTime.of(2022, 6, 18, 23, 0)
        );
        assertEquals("1", companyEntity.getFavorite());
    }
}