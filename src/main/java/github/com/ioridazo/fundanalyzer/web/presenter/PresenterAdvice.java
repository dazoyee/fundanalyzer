package github.com.ioridazo.fundanalyzer.web.presenter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class PresenterAdvice {

    @Value("${app.version}")
    private String applicationVersion;

    private PresenterAdvice() {
    }

    @ModelAttribute("applicationVersion")
    public String applicationVersion() {
        return applicationVersion;
    }
}
