package github.com.ioridazo.fundanalyzer.domain.service;

import github.com.ioridazo.fundanalyzer.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.MinkabuDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.StockPriceDao;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Document;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Minkabu;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.StockPrice;
import github.com.ioridazo.fundanalyzer.domain.log.Category;
import github.com.ioridazo.fundanalyzer.domain.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.domain.log.Process;
import github.com.ioridazo.fundanalyzer.domain.logic.scraping.jsoup.StockScraping;
import github.com.ioridazo.fundanalyzer.domain.util.Converter;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import lombok.extern.log4j.Log4j2;
import org.seasar.doma.jdbc.UniqueConstraintException;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.core.NestedRuntimeException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Log4j2
@Service
public class StockService {

    private final StockScraping stockScraping;
    private final CompanyDao companyDao;
    private final DocumentDao documentDao;
    private final StockPriceDao stockPriceDao;
    private final MinkabuDao minkabuDao;

    public StockService(
            final StockScraping stockScraping,
            final CompanyDao companyDao,
            final DocumentDao documentDao,
            final StockPriceDao stockPriceDao,
            final MinkabuDao minkabuDao) {
        this.stockScraping = stockScraping;
        this.companyDao = companyDao;
        this.documentDao = documentDao;
        this.stockPriceDao = stockPriceDao;
        this.minkabuDao = minkabuDao;
    }

    LocalDateTime nowLocalDateTime() {
        return LocalDateTime.now();
    }

    /**
     * 指定日付に提出された企業の株価を取得する
     *
     * @param submitDate 提出日
     * @return null
     */
    @NewSpan("StockService.importStockPrice.submitDate")
    public CompletableFuture<Void> importStockPrice(final LocalDate submitDate) {
        documentDao.selectByTypeAndSubmitDate("120", submitDate).stream()
                .map(Document::getEdinetCode)
                .map(edinetCode -> Converter.toCompanyCode(edinetCode, companyDao.selectAll()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .parallel()
                .forEach(this::importStockPrice);

        FundanalyzerLogClient.logService(
                MessageFormat.format("最新の株価を正常に取り込みました。\t対象書類提出日:{0}", submitDate),
                Category.STOCK,
                Process.IMPORT
        );
        return null;
    }

    /**
     * 指定企業の株価を取得する
     *
     * @param code 会社コード
     */
    @NewSpan("StockService.importStockPrice.code")
    @Transactional
    public void importStockPrice(final String code) {
        try {
            final var nikkei = stockScraping.nikkei(code);
            final var kabuoji3List = stockScraping.kabuoji3(code);
            final var stockPriceList = stockPriceDao.selectByCode(code);
            final var minkabu = stockScraping.minkabu(code);
            final var minkabuList = minkabuDao.selectByCode(code);

            // 日経
            if (isNotInsertedStockPrice(nikkei.getTargetDate(), stockPriceList)) {
                stockPriceDao.insert(StockPrice.ofNikkeiResultBean(code, nikkei, nowLocalDateTime()));
            }

            // kabuoji3
            kabuoji3List.forEach(kabuoji3 -> {
                if (isNotInsertedStockPrice(kabuoji3.getTargetDate(), stockPriceList)) {
                    try {
                        stockPriceDao.insert(StockPrice.ofKabuoji3ResultBean(code, kabuoji3, nowLocalDateTime()));
                    } catch (NestedRuntimeException e) {
                        if (e.contains(UniqueConstraintException.class)) {
                            log.info("一意制約違反のため、株価情報のデータベース登録をスキップします。" +
                                    "\t企業コード:{}\t対象日:{}", code, kabuoji3.getTargetDate());
                        } else {
                            throw new FundanalyzerRuntimeException("想定外のエラーが発生しました。", e);
                        }
                    }
                }
            });

            // みんかぶ
            if (isNotInsertedMinkabu(minkabu.getTargetDate(), minkabuList)) {
                final var m = Minkabu.ofMinkabuResultBean(code, minkabu, nowLocalDateTime());
                if (Objects.isNull(m.getGoalsStock())) {
                    log.warn("みんかぶからスクレイピングした目標株価が存在していません。スクレイピング処理の詳細を確認してください。" +
                            "\t会社コード:{}\t対象日付:{}\tURL:{}", code, m.getTargetDate(), "https://minkabu.jp/stock/" + code.substring(0, 4));
                }
                minkabuDao.insert(m);
            }

        } catch (FundanalyzerRuntimeException e) {
            log.warn("株価取得できなかったため、DBに登録できませんでした。\t企業コード:{}", code);
        }
    }

    /**
     * 株価がデータベースに登録されているかを確認する
     *
     * @param targetDateAsString 対象日
     * @param stockPriceList     データベースリスト
     * @return bool
     */
    private boolean isNotInsertedStockPrice(final String targetDateAsString, final List<StockPrice> stockPriceList) {
        final LocalDate targetDate;
        if (targetDateAsString.contains("/")) {
            targetDate = LocalDate.parse(targetDateAsString, DateTimeFormatter.ofPattern("yyyy/M/d"));
        } else {
            targetDate = LocalDate.parse(targetDateAsString);
        }
        return stockPriceList.stream()
                .map(StockPrice::getTargetDate)
                .noneMatch(targetDate::equals);
    }

    private boolean isNotInsertedMinkabu(final String targetDateAsString, final List<Minkabu> minkabuList) {
        final var targetDate = MonthDay.parse(targetDateAsString, DateTimeFormatter.ofPattern("MM/dd")).atYear(LocalDate.now().getYear());
        return minkabuList.stream()
                .map(Minkabu::getTargetDate)
                .noneMatch(targetDate::equals);
    }
}
