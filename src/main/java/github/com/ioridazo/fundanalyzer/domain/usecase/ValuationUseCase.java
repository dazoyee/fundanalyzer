package github.com.ioridazo.fundanalyzer.domain.usecase;

import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import io.micrometer.observation.annotation.Observed;

public interface ValuationUseCase {

    /**
     * 株価評価
     */
    @Observed
    int evaluate();

    /**
     * 株価評価
     *
     * @param inputData 企業コード
     */
    @Observed
    boolean evaluate(CodeInputData inputData);
}
