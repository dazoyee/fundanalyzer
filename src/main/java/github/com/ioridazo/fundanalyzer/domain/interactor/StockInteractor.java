package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.domain.log.Category;
import github.com.ioridazo.fundanalyzer.domain.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.domain.log.Process;
import github.com.ioridazo.fundanalyzer.domain.logic.scraping.jsoup.StockScraping;
import github.com.ioridazo.fundanalyzer.domain.specification.CompanySpecification;
import github.com.ioridazo.fundanalyzer.domain.specification.DocumentSpecification;
import github.com.ioridazo.fundanalyzer.domain.specification.StockSpecification;
import github.com.ioridazo.fundanalyzer.domain.usecase.StockUseCase;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerScrapingException;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
public class StockInteractor implements StockUseCase {

    private static final Logger log = LogManager.getLogger(StockInteractor.class);

    private final CompanySpecification companySpecification;
    private final DocumentSpecification documentSpecification;
    private final StockSpecification stockSpecification;
    private final StockScraping stockScraping;

    public StockInteractor(
            final CompanySpecification companySpecification,
            final DocumentSpecification documentSpecification,
            final StockSpecification stockSpecification,
            final StockScraping stockScraping) {
        this.companySpecification = companySpecification;
        this.documentSpecification = documentSpecification;
        this.stockSpecification = stockSpecification;
        this.stockScraping = stockScraping;
    }

    /**
     * 並列で株価を取得する
     *
     * @param inputData 提出日
     * @return Void
     */
    @Override
    public CompletableFuture<Void> importStockPrice(final DateInputData inputData) {
        try {
            final List<String> companyCodeList = documentSpecification.targetList(inputData).stream()
                    .map(document -> companySpecification.findCompanyByEdinetCode(document.getEdinetCode()))
                    .filter(Optional::isPresent)
                    .map(c -> c.get().getCode())
                    .map(Optional::get)
                    .collect(Collectors.toList());

            // 並列で株価取得処理を実施する
            companyCodeList.stream()
                    .map(CodeInputData::of)
                    .collect(Collectors.toList())
                    .parallelStream().forEach(this::importStockPrice);

            FundanalyzerLogClient.logService(
                    MessageFormat.format("最新の株価を正常に取り込みました。\t対象書類提出日:{0}\t株価取得件数:{1}",
                            inputData.getDate(),
                            companyCodeList.size()),
                    Category.STOCK,
                    Process.IMPORT
            );
            return null;
        } catch (Throwable t) {
            FundanalyzerLogClient.logError(t);
            throw new FundanalyzerRuntimeException(t);
        }
    }

    /**
     * 株価を取得する
     *
     * @param inputData 企業コード
     */
    @Override
    public void importStockPrice(final CodeInputData inputData) {
        try {
            // 日経
            stockSpecification.insert(inputData.getCode(), stockScraping.nikkei(inputData.getCode()));

            // kabuoji3
            stockSpecification.insert(inputData.getCode(), stockScraping.kabuoji3(inputData.getCode()));

            // みんかぶ
            stockSpecification.insert(inputData.getCode(), stockScraping.minkabu(inputData.getCode()));
        } catch (FundanalyzerScrapingException e) {
            log.warn("株価取得できなかったため、DBに登録できませんでした。\t企業コード:{}", inputData.getCode(), e);
        }
    }
}
