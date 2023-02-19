package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.client.jsoup.JsoupClient;
import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.SourceOfStockPrice;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CompanySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.DocumentSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.StockSpecification;
import github.com.ioridazo.fundanalyzer.domain.usecase.StockUseCase;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerCircuitBreakerRecordException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRateLimiterException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerScrapingException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerShortCircuitException;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Component
public class StockInteractor implements StockUseCase {

    private static final Logger log = LogManager.getLogger(StockInteractor.class);

    private final CompanySpecification companySpecification;
    private final DocumentSpecification documentSpecification;
    private final StockSpecification stockSpecification;
    private final JsoupClient jsoupClient;

    @Value("${app.config.stock.nikkei.enabled}")
    boolean isNikkei;
    @Value("${app.config.stock.kabuoji3.enabled}")
    boolean isKabuoji3;
    @Value("${app.config.stock.minkabu.enabled}")
    boolean isMinkabu;
    @Value("${app.config.stock.yahoo-finance.enabled}")
    boolean isYahooFinance;
    @Value("${app.config.stock.store-stock-price-for-last-days}")
    int daysToStoreStockPrice;

    public StockInteractor(
            final CompanySpecification companySpecification,
            final DocumentSpecification documentSpecification,
            final StockSpecification stockSpecification,
            final JsoupClient jsoupClient) {
        this.companySpecification = companySpecification;
        this.documentSpecification = documentSpecification;
        this.stockSpecification = stockSpecification;
        this.jsoupClient = jsoupClient;
    }

    LocalDate nowLocalDate() {
        return LocalDate.now();
    }

    /**
     * 株価を取得する
     *
     * @param inputData 提出日
     * @param place     取得先
     */
    @Override
    public void importStockPrice(final DateInputData inputData, final SourceOfStockPrice place) {
        final long startTime = System.currentTimeMillis();
        final List<CodeInputData> inputDataList = documentSpecification.inquiryTargetDocuments(inputData).stream()
                .map(document -> companySpecification.findCompanyByEdinetCode(document.getEdinetCode()))
                .filter(Optional::isPresent)
                .map(c -> c.get().code())
                .map(CodeInputData::of)
                .distinct()
                .toList();

        try {
            if (inputDataList.size() > 10) {
                inputDataList.parallelStream().forEach(code -> importStockPrice(code, place));
            } else {
                inputDataList.forEach(code -> importStockPrice(code, place));
            }
        } catch (final FundanalyzerShortCircuitException e) {
            log.warn(FundanalyzerLogClient.toInteractorLogObject(
                    e.getMessage(),
                    Category.STOCK,
                    Process.IMPORT
            ));
        }

        log.info(FundanalyzerLogClient.toInteractorLogObject(
                MessageFormat.format(
                        "最新の株価を正常に取り込みました。\t取得先:{0}\t対象書類提出日:{1}\t株価取得企業数:{2}",
                        place.name(),
                        inputData.getDate(),
                        inputDataList.stream().distinct().count()),
                Category.STOCK,
                Process.IMPORT,
                System.currentTimeMillis() - startTime
        ));
    }

    /**
     * 株価取得
     *
     * @param inputData 企業コード
     * @param place     取得先
     * @throws FundanalyzerShortCircuitException サーキットブレーカーオープン
     */
    @Override
    public void importStockPrice(
            final CodeInputData inputData, final SourceOfStockPrice place) throws FundanalyzerShortCircuitException {
        try {
            switch (place) {
                case NIKKEI -> {
                    if (isNikkei) {
                        // 日経
                        stockSpecification.insert(inputData.getCode5(), jsoupClient.nikkei(inputData.getCode5()));
                        log.trace(FundanalyzerLogClient.toInteractorLogObject(
                                MessageFormat.format("日経 から株価を取得しました。\t企業コード:{0}", inputData.getCode5()),
                                companySpecification.findCompanyByCode(inputData.getCode()).map(Company::edinetCode).orElse("null"),
                                Category.STOCK,
                                Process.IMPORT
                        ));
                    }
                }
                case KABUOJI3 -> {
                    if (isKabuoji3) {
                        // kabuoji3
                        jsoupClient.kabuoji3(inputData.getCode5()).stream()
                                // 保存する株価を絞る
                                .filter(kabuoji3 -> LocalDate.parse(kabuoji3.targetDate())
                                        .isAfter(nowLocalDate().minusDays(daysToStoreStockPrice)))
                                .forEach(kabuoji3 -> stockSpecification.insertOfKabuoji3(inputData.getCode5(), kabuoji3));
                        log.trace(FundanalyzerLogClient.toInteractorLogObject(
                                MessageFormat.format("kabuoji3 から株価を取得しました。\t企業コード:{0}", inputData.getCode5()),
                                companySpecification.findCompanyByCode(inputData.getCode()).map(Company::edinetCode).orElse("null"),
                                Category.STOCK,
                                Process.IMPORT
                        ));
                    }
                }
                case MINKABU -> {
                    if (isMinkabu) {
                        // みんかぶ
                        jsoupClient.minkabuForStock(inputData.getCode5()).stream()
                                // 保存する株価を絞る
                                .filter(minkabu -> LocalDate.parse(minkabu.targetDate(), DateTimeFormatter.ofPattern("uuuu/MM/dd"))
                                        .isAfter(nowLocalDate().minusDays(daysToStoreStockPrice)))
                                .forEach(minkabu -> stockSpecification.insertOfMinkabu(inputData.getCode5(), minkabu));
                        stockSpecification.insert(inputData.getCode5(), jsoupClient.minkabu(inputData.getCode5()));
                        log.trace(FundanalyzerLogClient.toInteractorLogObject(
                                MessageFormat.format("みんかぶ から株価を取得しました。\t企業コード:{0}", inputData.getCode5()),
                                companySpecification.findCompanyByCode(inputData.getCode()).map(Company::edinetCode).orElse("null"),
                                Category.STOCK,
                                Process.IMPORT
                        ));
                    }
                }
                case YAHOO_FINANCE -> {
                    if (isYahooFinance) {
                        // Yahoo! ファイナンス
                        jsoupClient.yahooFinance(inputData.getCode5()).stream()
                                // 保存する株価を絞る
                                .filter(yahooFinance -> LocalDate.parse(yahooFinance.targetDate(), DateTimeFormatter.ofPattern("yyyy年M月d日"))
                                        .isAfter(nowLocalDate().minusDays(daysToStoreStockPrice)))
                                .forEach(yahooFinance -> stockSpecification.insertOfYahooFinance(inputData.getCode5(), yahooFinance));
                        log.trace(FundanalyzerLogClient.toInteractorLogObject(
                                MessageFormat.format("Yahoo!ファイナンス から株価を取得しました。\t企業コード:{0}", inputData.getCode5()),
                                companySpecification.findCompanyByCode(inputData.getCode()).map(Company::edinetCode).orElse("null"),
                                Category.STOCK,
                                Process.IMPORT
                        ));
                    }
                }
                default -> throw new FundanalyzerRuntimeException();
            }
        } catch (final FundanalyzerCircuitBreakerRecordException | FundanalyzerRateLimiterException e) {
            log.info(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format("株価取得の通信に失敗しました。\t企業コード:{0}", inputData.getCode5()),
                    companySpecification.findCompanyByCode(inputData.getCode()).map(Company::edinetCode).orElse("null"),
                    Category.STOCK,
                    Process.IMPORT
            ), e);
        } catch (final FundanalyzerScrapingException e) {
            log.warn(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format(
                            "株価取得できなかったため、DBに登録できませんでした。\t企業コード:{0}",
                            inputData.getCode5()
                    ),
                    companySpecification.findCompanyByCode(inputData.getCode()).map(Company::edinetCode).orElse("null"),
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
