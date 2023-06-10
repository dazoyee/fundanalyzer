package github.com.ioridazo.fundanalyzer.domain.usecase;

import github.com.ioridazo.fundanalyzer.domain.value.Result;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.model.FinancialStatementInputData;
import github.com.ioridazo.fundanalyzer.web.model.IdInputData;
import io.micrometer.observation.annotation.Observed;

public interface DocumentUseCase {

    /**
     * EDINETリストをデータベース登録
     * ↓
     * スクレイピング
     *
     * @param inputData 提出日
     */
    @Observed
    void allProcess(DateInputData inputData);

    /**
     * EDINETに書類有無を問い合わせ
     * ↓
     * 書類をデータベース登録
     *
     * @param inputData 提出日
     */
    @Observed
    void saveEdinetList(DateInputData inputData);

    /**
     * スクレイピング
     *
     * @param inputData 提出日
     */
    @Observed
    void scrape(DateInputData inputData);

    /**
     * スクレイピング
     *
     * @param inputData 書類ID
     */
    @Observed
    void scrape(IdInputData inputData);

    /**
     * 財務諸表の値の登録
     *
     * @param inputData 財務諸表の登録情報
     * @return 処理結果
     */
    @Observed
    Result registerFinancialStatementValue(FinancialStatementInputData inputData);

    /**
     * ステータス更新
     *
     * @param inputData 書類ID
     * @return 処理結果
     */
    @Observed
    Result updateAllDoneStatus(IdInputData inputData);

    /**
     * 対象期間の更新（存在しない場合）
     *
     * @param inputData 提出日
     */
    @Observed
    void updateDocumentPeriodIfNotExist(DateInputData inputData);

    /**
     * 処理対象外に更新
     *
     * @param inputData 書類ID
     */
    @Observed
    void removeDocument(IdInputData inputData);

    /**
     * 処理対象外に更新
     *
     * @param inputData 提出日
     */
    @Observed
    void removeDocument(DateInputData inputData);
}
