package github.com.ioridazo.fundanalyzer.domain.usecase;

import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.CorporateValue;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.scheduling.annotation.Async;

import java.util.concurrent.CompletableFuture;

public interface AnalyzeUseCase {

    /**
     * 企業価値をデータベース登録
     *
     * @param document ドキュメント
     */
    @NewSpan
    void analyze(Document document);

    /**
     * 企業価値をデータベース登録
     *
     * @param inputData 提出日
     * @return Void
     */
    @Async
    @NewSpan
    CompletableFuture<Void> analyze(DateInputData inputData);

    /**
     * 企業価値情報の取得
     *
     * @param company 企業情報
     * @return 企業価値
     */
    @NewSpan
    CorporateValue calculateCorporateValue(Company company);
}
