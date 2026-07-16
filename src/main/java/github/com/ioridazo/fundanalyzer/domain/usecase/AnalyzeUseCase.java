package github.com.ioridazo.fundanalyzer.domain.usecase;

import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.CorporateValue;
import github.com.ioridazo.fundanalyzer.domain.value.RecalculationPreview;
import github.com.ioridazo.fundanalyzer.domain.value.RecalculationResult;
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
     * 係数一括再計算バッチの対象件数を事前確認する。
     *
     * @return 対象件数（analysis_result / valuation の全件数）
     */
    @Observed
    RecalculationPreview previewRecalculation();

    /**
     * 業種係数変更に伴い、全期間の企業価値・RIM理論株価を現行係数で一括再計算する。
     *
     * <p>値が変わる行のみ更新し、連動して valuation の割引値・割引率も一括更新する。
     *
     * @return 再計算結果
     */
    @Observed
    RecalculationResult recalculate();

    /**
     * 投資指標の算出
     *
     * @param inputData 企業コード
     */
    @Observed
    void indicate(CodeInputData inputData);
}
