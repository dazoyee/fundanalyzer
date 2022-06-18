package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.client.jsoup.JsoupClient;
import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CompanySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.DocumentSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.StockSpecification;
import github.com.ioridazo.fundanalyzer.domain.usecase.StockUseCase;
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
import java.util.stream.Collectors;

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
     */
    @Override
    public void importStockPrice(final DateInputData inputData) {
        final long startTime = System.currentTimeMillis();
        final List<CodeInputData> inputDataList = documentSpecification.targetList(inputData).stream()
                .map(document -> companySpecification.findCompanyByEdinetCode(document.getEdinetCode()))
                .filter(Optional::isPresent)
                .map(c -> c.get().getCode())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(CodeInputData::of)
                .distinct()
                .collect(Collectors.toList());

        List.of(Place.NIKKEI, Place.KABUOJI3, Place.MINKABU, Place.YAHOO_FINANCE).forEach(place -> {
            try {
                inputDataList.parallelStream().forEach(code -> importStockPrice(code, place));
            } catch (final FundanalyzerShortCircuitException e) {
                log.warn(FundanalyzerLogClient.toInteractorLogObject(
                        e.getMessage(),
                        Category.STOCK,
                        Process.IMPORT
                ));
            }
        });

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
     * 株価取得
     *
     * @param inputData 企業コード
     * @param place     取得先
     * @throws FundanalyzerShortCircuitException サーキットブレーカーオープン
     */
    @Override
    public void importStockPrice(
            final CodeInputData inputData, final Place place) throws FundanalyzerShortCircuitException {
        try {
            switch (place) {
                case NIKKEI:
                    if (isNikkei) {
                        // 日経
                        stockSpecification.insert(inputData.getCode5(), jsoupClient.nikkei(inputData.getCode5()));
                        log.info(FundanalyzerLogClient.toInteractorLogObject(
                                MessageFormat.format("日経 から株価を取得しました。\t企業コード:{0}", inputData.getCode5()),
                                Category.STOCK,
                                Process.IMPORT
                        ));
                    }
                    break;
                case KABUOJI3:
                    if (isKabuoji3) {
                        // kabuoji3
                        jsoupClient.kabuoji3(inputData.getCode5()).stream()
                                // 保存する株価を絞る
                                .filter(kabuoji3 -> LocalDate.parse(kabuoji3.getTargetDate())
                                        .isAfter(nowLocalDate().minusDays(daysToStoreStockPrice)))
                                .forEach(kabuoji3 -> stockSpecification.insert(inputData.getCode5(), kabuoji3));
                        log.info(FundanalyzerLogClient.toInteractorLogObject(
                                MessageFormat.format("kabuoji3 から株価を取得しました。\t企業コード:{0}", inputData.getCode5()),
                                Category.STOCK,
                                Process.IMPORT
                        ));
                    }
                    break;
                case MINKABU:
                    if (isMinkabu) {
                        // みんかぶ
                        stockSpecification.insert(inputData.getCode5(), jsoupClient.minkabu(inputData.getCode5()));
                        log.info(FundanalyzerLogClient.toInteractorLogObject(
                                MessageFormat.format("みんかぶ から株価を取得しました。\t企業コード:{0}", inputData.getCode5()),
                                Category.STOCK,
                                Process.IMPORT
                        ));
                    }
                    break;
                case YAHOO_FINANCE:
                    if (isYahooFinance) {
                        // Yahoo! ファイナンス
                        jsoupClient.yahooFinance(inputData.getCode5()).stream()
                                // 保存する株価を絞る
                                .filter(yahooFinance -> LocalDate.parse(yahooFinance.getTargetDate(), DateTimeFormatter.ofPattern("yyyy年M月d日"))
                                        .isAfter(nowLocalDate().minusDays(daysToStoreStockPrice)))
                                .forEach(yahooFinance -> stockSpecification.insert(inputData.getCode5(), yahooFinance));
                        log.info(FundanalyzerLogClient.toInteractorLogObject(
                                MessageFormat.format("Yahoo!ファイナンス から株価を取得しました。\t企業コード:{0}", inputData.getCode5()),
                                Category.STOCK,
                                Process.IMPORT
                        ));
                    }
                    break;
                default:
                    throw new FundanalyzerRuntimeException();
            }
        } catch (final FundanalyzerCircuitBreakerRecordException | FundanalyzerRateLimiterException e) {
            log.info(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format("株価取得の通信に失敗しました。\t企業コード:{0}", inputData.getCode5()),
                    Category.STOCK,
                    Process.IMPORT
            ), e);
        } catch (final FundanalyzerScrapingException e) {
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
