package github.com.ioridazo.fundanalyzer.domain.domain.entity.view;

import github.com.ioridazo.fundanalyzer.web.view.model.edinet.EdinetListViewModel;
import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @param submitDate      提出日
 * @param countAll        総件数
 * @param countTarget     処理対象件数
 * @param countScraped    処理済件数
 * @param countAnalyzed   分析済件数
 * @param notAnalyzedId   未分析ID
 * @param cantScrapedId   処理確認ID
 * @param countNotScraped 未処理件数
 * @param countNotTarget  対象外件数
 */
@Entity(immutable = true)
@Table(name = "edinet_list_view")
public record EdinetListViewBean(

        @Id
        LocalDate submitDate,

        Long countAll,

        Long countTarget,

        Long countScraped,

        Long countAnalyzed,

        String notAnalyzedId,

        String cantScrapedId,

        Long countNotScraped,

        Long countNotTarget,

        @Column(updatable = false)
        LocalDateTime createdAt,

        LocalDateTime updatedAt
) {

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

    public static EdinetListViewBean of(final EdinetListViewModel viewModel, final LocalDateTime nowLocalDateTime) {
        return new EdinetListViewBean(
                viewModel.submitDate(),
                viewModel.countAll(),
                viewModel.countTarget(),
                viewModel.countScraped(),
                viewModel.countAnalyzed(),
                viewModel.notAnalyzedId(),
                viewModel.cantScrapedId(),
                viewModel.countNotScraped(),
                viewModel.countNotTarget(),
                nowLocalDateTime,
                nowLocalDateTime
        );
    }
}
