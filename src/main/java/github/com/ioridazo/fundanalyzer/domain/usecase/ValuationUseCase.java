package github.com.ioridazo.fundanalyzer.domain.usecase;

import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import org.springframework.cloud.sleuth.annotation.NewSpan;

public interface ValuationUseCase {

    /**
     * 株価評価
     */
    @NewSpan
    int evaluate();

    /**
     * 株価評価
     *
     * @param inputData 企業コード
     */
    @NewSpan
    boolean evaluate(CodeInputData inputData);

}
