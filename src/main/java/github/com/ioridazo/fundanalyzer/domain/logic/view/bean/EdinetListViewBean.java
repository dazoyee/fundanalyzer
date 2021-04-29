package github.com.ioridazo.fundanalyzer.domain.logic.view.bean;

import lombok.Value;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
@Entity(immutable = true)
@Table(name = "edinet_list_view")
public class EdinetListViewBean {

    // 提出日
    @Id
    private final LocalDate submitDate;

    // 総件数
    private final Long countAll;

    // 処理対象件数
    private final Long countTarget;

    // 処理済件数
    private final Long countScraped;

    // 分析済件数
    private final Long countAnalyzed;

    // 未分析ID
    private final String notAnalyzedId;

    // 処理確認ID
    private final String cantScrapedId;

    // 未処理件数
    private final Long countNotScraped;

    // 対象外件数
    private final Long countNotTarget;

    @Column(updatable = false)
    private final LocalDateTime createdAt;

    private final LocalDateTime updatedAt;

    public static EdinetListViewBean of(
            final LocalDate submitDate,
            final int countAll,
            final int countTarget,
            final int countScraped,
            final int countAnalyzed,
            final String notAnalyzedCode,
            final String cantScrapedCode,
            final int countNotScraped,
            final LocalDateTime nowLocalDateTime) {
        return new EdinetListViewBean(
                submitDate,
                (long) countAll,
                (long) countTarget,
                (long) countScraped,
                (long) countAnalyzed,
                notAnalyzedCode,
                cantScrapedCode,
                (long) countNotScraped,
                (long) (countAll - countTarget),
                nowLocalDateTime,
                nowLocalDateTime
        );
    }
}
