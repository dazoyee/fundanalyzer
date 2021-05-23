package github.com.ioridazo.fundanalyzer.web.view.model.edinet;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.view.EdinetListViewBean;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
public class EdinetListViewModel {

    // 提出日
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

    public static EdinetListViewModel of(final EdinetListViewBean viewBean) {
        return new EdinetListViewModel(
                viewBean.getSubmitDate(),
                viewBean.getCountAll(),
                viewBean.getCountTarget(),
                viewBean.getCountScraped(),
                viewBean.getCountAnalyzed(),
                viewBean.getNotAnalyzedId(),
                viewBean.getCantScrapedId(),
                viewBean.getCountNotScraped(),
                viewBean.getCountNotTarget()
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
