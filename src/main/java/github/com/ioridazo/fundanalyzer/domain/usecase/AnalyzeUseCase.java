package github.com.ioridazo.fundanalyzer.domain.usecase;

import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.CorporateValue;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.model.IdInputData;
import org.springframework.cloud.sleuth.annotation.NewSpan;

public interface AnalyzeUseCase {

    /**
     * 企業価値をデータベース登録
     *
     * @param inputData 書類ID
     */
    @NewSpan
    void analyze(IdInputData inputData);

    /**
     * 企業価値をデータベース登録
     *
     * @param inputData 提出日
     */
    @NewSpan
    void analyze(DateInputData inputData);

    /**
     * 企業価値情報の取得
     *
     * @param company 企業情報
     * @return 企業価値
     */
    @NewSpan
    CorporateValue calculateCorporateValue(Company company);

    /**
     * 投資指標の算出
     *
     * @param inputData 企業コード
     */
    @NewSpan
    void indicate(CodeInputData inputData);
}
