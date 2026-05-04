package github.com.ioridazo.fundanalyzer.web.presenter;

import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.view.model.edinet.detail.DocumentViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.edinet.detail.EdinetDetailViewModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Controller
public class EdinetDetailPresenter {

    private static final Logger log = LogManager.getLogger(EdinetDetailPresenter.class);

    private final ViewService viewService;

    public EdinetDetailPresenter(final ViewService viewService) {
        this.viewService = viewService;
    }

    /**
     * EDINET処理状況 v3（Tailwind + layout-v2 継承）。
     *
     * @param submitDate 提出日（ISO_LOCAL_DATE 文字列）
     * @param model      model
     * @return edinet-list-detail-v2 テンプレート名
     */
    @GetMapping("/v3/edinet-list-detail")
    public String edinetListDetailV3(
            @RequestParam(name = "submitDate") final String submitDate,
            final Model model) {
        model.addAttribute("submitDate", submitDate);
        model.addAttribute("edinetDetail",
                sanitize(viewService.getEdinetDetailView(DateInputData.of(LocalDate.parse(submitDate))), submitDate));
        return "edinet-list-detail-v2";
    }

    /**
     * documentDetailList から null 要素を除外したビューモデルを返す。Thymeleaf の EL1007E を防ぐ防御層。
     *
     * @param source     ViewService から取得した元のビューモデル
     * @param submitDate ログ出力用の提出日
     * @return null 要素を除外したビューモデル
     */
    private EdinetDetailViewModel sanitize(final EdinetDetailViewModel source, final String submitDate) {
        if (source == null) {
            return null;
        }
        final List<DocumentViewModel> rawList = source.getDocumentDetailList();
        if (rawList == null || rawList.isEmpty()) {
            return source;
        }
        final List<DocumentViewModel> sanitized = rawList.stream()
                .filter(Objects::nonNull)
                .toList();
        final int removed = rawList.size() - sanitized.size();
        if (removed > 0) {
            log.warn("EDINET詳細ビューに null の DocumentViewModel が含まれていたため除外しました。submitDate={}, removed={}, total={}",
                    submitDate, removed, rawList.size());
        }
        return EdinetDetailViewModel.of(source.getEdinetList(), sanitized);
    }
}
