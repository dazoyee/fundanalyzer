package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.domain.domain.jsoup.StockScraping;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CompanySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.DocumentSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.StockSpecification;
import github.com.ioridazo.fundanalyzer.domain.usecase.StockUseCase;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerScrapingException;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
     * 株価を取得する
     *
     * @param inputData 提出日
     */
    @Override
    public void importStockPrice(final DateInputData inputData) {
        final long startTime = System.currentTimeMillis();
        final List<CodeInputData> inputDataList = documentSpecification.targetList(inputData).stream()
                .map(document -> companySpecification.findCompanyByEdinetCode(document.getEdinetCode()))
                .filter(Optional::isPresent)
                .map(c -> c.get().getCode())
                .map(Optional::get)
                .map(CodeInputData::of)
                .distinct()
                .collect(Collectors.toList());

        // 株価取得処理を実施する
        inputDataList.parallelStream().forEach(this::importStockPrice);

        log.info(FundanalyzerLogClient.toInteractorLogObject(
                MessageFormat.format(
                        "最新の株価を正常に取り込みました。\t対象書類提出日:{0}\t株価取得企業数:{1}",
                        inputData.getDate(),
                        inputDataList.stream().distinct().count()),
                Category.STOCK,
                Process.IMPORT,
                System.currentTimeMillis() - startTime
        ));
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
            stockSpecification.insert(inputData.getCode5(), stockScraping.nikkei(inputData.getCode5()));

            // kabuoji3
            stockSpecification.insert(inputData.getCode5(), stockScraping.kabuoji3(inputData.getCode5()));

            // みんかぶ
            stockSpecification.insert(inputData.getCode5(), stockScraping.minkabu(inputData.getCode5()));
        } catch (FundanalyzerScrapingException e) {
            log.warn(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format(
                            "株価取得できなかったため、DBに登録できませんでした。\t企業コード:{0}",
                            inputData.getCode5()
                    ),
                    Category.STOCK,
                    Process.IMPORT
            ), e);
        }
    }

    /**
     * 株価削除
     *
     * @return 削除カウント
     */
    @Override
    public int deleteStockPrice() {
        int count = 0;
        for (final LocalDate targetDate : stockSpecification.findTargetDateToDelete()) {
            final int delete = stockSpecification.delete(targetDate);
            count += delete;
        }
        return count;
    }
}
