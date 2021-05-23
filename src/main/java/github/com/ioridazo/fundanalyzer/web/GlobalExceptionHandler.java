package github.com.ioridazo.fundanalyzer.web;

import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerSqlForeignKeyException;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.text.MessageFormat;

@ControllerAdvice
public class GlobalExceptionHandler {

    private final ViewService viewService;

    public GlobalExceptionHandler(
            final ViewService viewService) {
        this.viewService = viewService;
    }

    @NewSpan("GlobalExceptionHandler.FundanalyzerRuntimeException")
    @ExceptionHandler({FundanalyzerRuntimeException.class})
    public String runtimeException(final Exception e) {
        return MessageFormat.format("想定外のエラー\n{0}", e);
    }

    @ExceptionHandler({FundanalyzerSqlForeignKeyException.class})
    public String sqlForeignKeyException(final Exception e, final Model model) {
        model.addAttribute("error", "マスタに登録されていないEDINETコードが存在していたため、登録できませんでした。");
        model.addAttribute("companyUpdated", viewService.getUpdateDate());
        model.addAttribute("edinetList", viewService.getEdinetListView());
        return "edinet";
    }
}
