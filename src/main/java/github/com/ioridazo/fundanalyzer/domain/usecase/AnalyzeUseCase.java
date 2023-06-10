package github.com.ioridazo.fundanalyzer.domain.usecase;

import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.CorporateValue;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.model.IdInputData;
import io.micrometer.observation.annotation.Observed;

public interface AnalyzeUseCase {

    /**
     * 企業価値をデータベース登録
     *
     * @param inputData 書類ID
     */
    @Observed
    void analyze(IdInputData inputData);

    /**
     * 企業価値をデータベース登録
     *
     * @param inputData 提出日
     */
    @Observed
    void analyze(DateInputData inputData);

    /**
     * 企業価値情報の取得
     *
     * @param company 企業情報
     * @return 企業価値
     */
    @Observed
    CorporateValue calculateCorporateValue(Company company);

    /**
     * 投資指標の算出
     *
     * @param inputData 企業コード
     */
    @Observed
    void indicate(CodeInputData inputData);
}
