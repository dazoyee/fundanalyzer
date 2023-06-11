package github.com.ioridazo.fundanalyzer.web.view.model.edinet;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.view.EdinetListViewBean;

import java.time.LocalDate;
import java.util.List;

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
public record EdinetListViewModel(
        LocalDate submitDate,
        Long countAll,
        Long countTarget,
        Long countScraped,
        Long countAnalyzed,
        String notAnalyzedId,
        String cantScrapedId,
        Long countNotScraped,
        Long countNotTarget
) {

    public static EdinetListViewModel of(final EdinetListViewBean viewBean) {
        return new EdinetListViewModel(
                viewBean.submitDate(),
                viewBean.countAll(),
                viewBean.countTarget(),
                viewBean.countScraped(),
                viewBean.countAnalyzed(),
                viewBean.notAnalyzedId(),
                viewBean.cantScrapedId(),
                viewBean.countNotScraped(),
                viewBean.countNotTarget()
        );
    }

    public static EdinetListViewModel of(
            final LocalDate submitDate,
            final int countAll,
            final int countTarget,
            final int countScraped,
            final int countAnalyzed,
            final String notAnalyzedCode,
            final String cantScrapedCode,
            final int countNotScraped) {
        return new EdinetListViewModel(
                submitDate,
                (long) countAll,
                (long) countTarget,
                (long) countScraped,
                (long) countAnalyzed,
                notAnalyzedCode,
                cantScrapedCode,
                (long) countNotScraped,
                (long) (countAll - countTarget)
        );
    }

    public boolean isAllDone() {
        return List.of(
                countTarget,
                countScraped,
                countAnalyzed
        ).stream().distinct().count() == 1;
    }
}
