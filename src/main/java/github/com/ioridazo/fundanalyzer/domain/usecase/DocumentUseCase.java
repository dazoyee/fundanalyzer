package github.com.ioridazo.fundanalyzer.domain.usecase;

import github.com.ioridazo.fundanalyzer.domain.value.Result;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.model.FinancialStatementInputData;
import github.com.ioridazo.fundanalyzer.web.model.IdInputData;
import org.springframework.cloud.sleuth.annotation.NewSpan;

public interface DocumentUseCase {

    /**
     * EDINETリストをデータベース登録
     * ↓
     * スクレイピング
     *
     * @param inputData 提出日
     */
    @NewSpan
    void allProcess(DateInputData inputData);

    /**
     * EDINETに書類有無を問い合わせ
     * ↓
     * 書類をデータベース登録
     *
     * @param inputData 提出日
     */
    @NewSpan
    void saveEdinetList(DateInputData inputData);

    /**
     * スクレイピング
     *
     * @param inputData 提出日
     */
    @NewSpan
    void scrape(DateInputData inputData);

    /**
     * スクレイピング
     *
     * @param inputData 書類ID
     */
    @NewSpan
    void scrape(IdInputData inputData);

    /**
     * 財務諸表の値の登録
     *
     * @param inputData 財務諸表の登録情報
     * @return 処理結果
     */
    @NewSpan
    Result registerFinancialStatementValue(FinancialStatementInputData inputData);

    /**
     * ステータス更新
     *
     * @param inputData 書類ID
     * @return 処理結果
     */
    @NewSpan
    Result updateAllDoneStatus(IdInputData inputData);

    /**
     * 処理対象外に更新
     *
     * @param inputData 書類ID
     */
    @NewSpan
    void removeDocument(IdInputData inputData);
}
