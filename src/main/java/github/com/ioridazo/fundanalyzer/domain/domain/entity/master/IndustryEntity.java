package github.com.ioridazo.fundanalyzer.domain.domain.entity.master;

import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.GeneratedValue;
import org.seasar.doma.GenerationType;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity(immutable = true)
@Table(name = "industry")
public record IndustryEntity(

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        Integer id,

        String name,

        BigDecimal operatingProfitWeight,

        BigDecimal currentLiabilitiesRatio,

        @Column(updatable = false)
        LocalDateTime createdAt
) {

    /**
     * 係数列を指定しない簡易コンストラクタ（係数は DB 既定値に委ねる用途・テスト用）。
     *
     * @param id        業種ID
     * @param name      業種名
     * @param createdAt 登録日時
     */
    public IndustryEntity(final Integer id, final String name, final LocalDateTime createdAt) {
        this(id, name, null, null, createdAt);
    }

    public static IndustryEntity of(final String industryName, final LocalDateTime nowLocalDateTime) {
        return new IndustryEntity(
                null,
                industryName,
                nowLocalDateTime
        );
    }
}
