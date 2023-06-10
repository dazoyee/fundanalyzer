package github.com.ioridazo.fundanalyzer.domain.service;

import github.com.ioridazo.fundanalyzer.domain.usecase.CompanyUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.DocumentUseCase;
import github.com.ioridazo.fundanalyzer.domain.value.Result;
import github.com.ioridazo.fundanalyzer.web.model.BetweenDateInputData;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.model.IdInputData;
import io.micrometer.observation.annotation.Observed;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EdinetService {

    private final CompanyUseCase companyUseCase;
    private final DocumentUseCase documentUseCase;

    public EdinetService(
            final CompanyUseCase companyUseCase,
            final DocumentUseCase documentUseCase) {
        this.companyUseCase = companyUseCase;
        this.documentUseCase = documentUseCase;
    }

    /**
     * 企業情報の更新
     */
    @Observed
    @Async
    public void updateCompany() {
        // company
        companyUseCase.importCompanyInfo();
    }

    /**
     * EDINET 書類問い合わせ
     *
     * @param inputData 複数の提出日
     */
    @Observed
    public void saveEdinetList(final BetweenDateInputData inputData) {
        inputData.getFromDate()
                .datesUntil(inputData.getToDate().plusDays(1))
                .map(DateInputData::of)
                // edinet
                .forEach(documentUseCase::saveEdinetList);
    }

    /**
     * ステータス更新
     *
     * @param inputData 書類ID
     * @return 処理結果
     */
    @Observed
    public Result updateAllDoneStatus(final IdInputData inputData) {
        // update
        return documentUseCase.updateAllDoneStatus(inputData);
    }

    /**
     * 書類の除外
     *
     * @param inputData 書類ID
     */
    @Observed
    public void removeDocument(final IdInputData inputData) {
        // remove
        documentUseCase.removeDocument(inputData);
    }
}
