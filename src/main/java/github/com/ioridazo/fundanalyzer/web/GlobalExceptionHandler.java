package github.com.ioridazo.fundanalyzer.web;

import github.com.ioridazo.fundanalyzer.domain.ViewService;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerSqlForeignKeyException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    private final ViewService viewService;

    public GlobalExceptionHandler(
            final ViewService viewService) {
        this.viewService = viewService;
    }

    @ExceptionHandler({FundanalyzerSqlForeignKeyException.class})
    public String SqlForeignKeyException(final Exception e, final Model model) {
        model.addAttribute("error", "マスタに登録されていないEDINETコードが存在していたため、登録できませんでした。");
        model.addAttribute("companyUpdated", viewService.companyUpdated());
        model.addAttribute("edinetList", viewService.edinetList("120"));
        return "edinet";
    }
}
