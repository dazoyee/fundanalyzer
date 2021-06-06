package github.com.ioridazo.fundanalyzer.domain.service;

import github.com.ioridazo.fundanalyzer.domain.usecase.CompanyUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.DocumentUseCase;
import github.com.ioridazo.fundanalyzer.web.model.BetweenDateInputData;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.model.IdInputData;
import org.springframework.cloud.sleuth.annotation.NewSpan;
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
    @NewSpan
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
    @NewSpan
    public void saveEdinetList(final BetweenDateInputData inputData) {
        inputData.getFromDate()
                .datesUntil(inputData.getToDate().plusDays(1))
                .map(DateInputData::of)
                // edinet
                .forEach(documentUseCase::saveEdinetList);
    }

    /**
     * 書類の除外
     *
     * @param inputData 書類ID
     */
    @NewSpan
    public void removeDocument(final IdInputData inputData) {
        // remove
        documentUseCase.removeDocument(inputData);
    }
}
