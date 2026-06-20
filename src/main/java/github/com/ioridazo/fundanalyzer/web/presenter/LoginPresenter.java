package github.com.ioridazo.fundanalyzer.web.presenter;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * ログイン画面を表示する Presenter。
 */
@Controller
public class LoginPresenter {

    /**
     * ログイン画面を返す。
     *
     * @return ログインテンプレート名
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
