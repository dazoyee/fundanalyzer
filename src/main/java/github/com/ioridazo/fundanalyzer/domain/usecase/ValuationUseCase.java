package github.com.ioridazo.fundanalyzer.domain.usecase;

import github.com.ioridazo.fundanalyzer.domain.value.ValuationCatchUpPreview;
import github.com.ioridazo.fundanalyzer.domain.value.ValuationCatchUpResult;
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

    /**
     * catch-up バッチの対象件数を事前確認する
     *
     * @return 対象会社数
     */
    @Observed
    ValuationCatchUpPreview previewCatchUp();

    /**
     * catch-up バッチを実行する
     *
     * @return 実行結果
     */
    @Observed
    ValuationCatchUpResult catchUp();
}
