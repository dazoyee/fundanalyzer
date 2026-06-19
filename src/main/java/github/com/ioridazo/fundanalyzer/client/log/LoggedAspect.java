package github.com.ioridazo.fundanalyzer.client.log;

import github.com.ioridazo.fundanalyzer.domain.value.Document;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * {@link Logged} 注釈付きメソッドの完了時に構造化ログを出力する Aspect
 *
 * <p>処理時間（startTime → endTime）の計測とログ出力を集約し、各メソッドから
 * {@code final long startTime = System.currentTimeMillis();} および
 * {@code log.info(FundanalyzerLogClient.toInteractorLogObject(...))} のボイラープレートを
 * 削除する目的で導入した。</p>
 */
@Aspect
@Component
public class LoggedAspect {

    private static final Logger log = LogManager.getLogger(LoggedAspect.class);

    /**
     * {@link Logged} 注釈の付いたメソッドを実行時に包み、完了時に構造化ログを出力する
     *
     * @param pjp 実行する結合点
     * @return 元メソッドの戻り値
     * @throws Throwable 元メソッドが送出した例外
     */
    @Around("@annotation(github.com.ioridazo.fundanalyzer.client.log.Logged)")
    public Object aroundLogged(final ProceedingJoinPoint pjp) throws Throwable {
        final long startTime = System.currentTimeMillis();
        final Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        final Logged logged = method.getAnnotation(Logged.class);

        final Object result = pjp.proceed();

        final Document document = findDocumentArgument(pjp.getArgs());
        final long durationTime = System.currentTimeMillis() - startTime;
        if (document != null) {
            log.info(FundanalyzerLogClient.toInteractorLogObject(
                    logged.message(), document, logged.category(), logged.process(), durationTime));
        } else {
            log.info(FundanalyzerLogClient.toInteractorLogObject(
                    logged.message(), logged.category(), logged.process(), durationTime));
        }
        return result;
    }

    /**
     * 引数列から先頭の {@link Document} を返す
     *
     * @param args 引数列
     * @return 該当する Document、なければ null
     */
    private Document findDocumentArgument(final Object[] args) {
        for (final Object arg : args) {
            if (arg instanceof Document doc) {
                return doc;
            }
        }
        return null;
    }
}
