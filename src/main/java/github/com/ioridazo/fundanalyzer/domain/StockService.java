package github.com.ioridazo.fundanalyzer.domain;

import github.com.ioridazo.fundanalyzer.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.StockPriceDao;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Company;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Document;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.StockPrice;
import github.com.ioridazo.fundanalyzer.domain.scraping.jsoup.HtmlScraping;
import github.com.ioridazo.fundanalyzer.domain.scraping.jsoup.bean.Kabuoji3ResultBean;
import github.com.ioridazo.fundanalyzer.domain.scraping.jsoup.bean.NikkeiResultBean;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class StockService {

    private final HtmlScraping htmlScraping;
    private final CompanyDao companyDao;
    private final DocumentDao documentDao;
    private final StockPriceDao stockPriceDao;

    public StockService(
            final HtmlScraping htmlScraping,
            final CompanyDao companyDao,
            final DocumentDao documentDao,
            final StockPriceDao stockPriceDao) {
        this.htmlScraping = htmlScraping;
        this.companyDao = companyDao;
        this.documentDao = documentDao;
        this.stockPriceDao = stockPriceDao;
    }

    public void importStockPrice(final LocalDate date) {
        documentDao.selectByTypeAndSubmitDate("120", date).stream()
                .map(Document::getEdinetCode)
                .map(this::convertToCompanyCode)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .parallel()
                .forEach(code -> {
                    try {
                        final var nikkei = htmlScraping.nikkei(code);
                        final var kabuoji3List = htmlScraping.kabuoji3(code);
                        final var stockPriceList = stockPriceDao.selectByCode(code);

                        // 日経
                        if (isNotInserted(nikkei.getTargetDate(), stockPriceList)) {
                            insertStockPrice(code, nikkei);
                        }

                        // kabuoji3
                        kabuoji3List.forEach(kabuoji3 -> {
                            if (isNotInserted(kabuoji3.getTargetDate(), stockPriceList)) {
                                insertStockPrice(code, kabuoji3);
                            }
                        });
                    } catch (FundanalyzerRuntimeException e) {
                        log.info("株価取得できなかったため、DBに登録できませんでした。\t企業コード:{}", code);
                    }
                });
        log.info("最新の株価を正常に取り込みました。\t対象書類提出日:{}", date);
    }

    @Transactional
    private void insertStockPrice(final String code, final NikkeiResultBean nikkei) {
        stockPriceDao.insert(new StockPrice(
                null,
                code,
                LocalDate.parse(nikkei.getTargetDate(), DateTimeFormatter.ofPattern("yyyy/M/d")),
                parseDouble(nikkei.getStockPrice()).orElse(null),
                parseDouble(nikkei.getOpeningPrice()).orElse(null),
                parseDouble(nikkei.getHighPrice()).orElse(null),
                parseDouble(nikkei.getLowPrice()).orElse(null),
                parseInteger(nikkei.getVolume()).orElse(null),
                parse(nikkei.getPer()),
                parse(nikkei.getPbr()),
                parse(nikkei.getRoe()),
                parse(nikkei.getNumberOfShares()),
                parse(nikkei.getMarketCapitalization()),
                parse(nikkei.getDividendYield()),
                nikkei.getShareholderBenefit().replace("株主優待 ", ""),
                "1",
                LocalDateTime.now()
        ));
    }

    @Transactional
    private void insertStockPrice(final String code, final Kabuoji3ResultBean kabuoji3) {
        stockPriceDao.insert(new StockPrice(
                null,
                code,
                LocalDate.parse(kabuoji3.getTargetDate()),
                parseDouble(kabuoji3.getClosingPrice()).orElse(null),
                parseDouble(kabuoji3.getOpeningPrice()).orElse(null),
                parseDouble(kabuoji3.getHighPrice()).orElse(null),
                parseDouble(kabuoji3.getLowPrice()).orElse(null),
                parseInteger(kabuoji3.getVolume()).orElse(null),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "2",
                LocalDateTime.now()
        ));
    }

    private boolean isNotInserted(final String targetDateAsString, final List<StockPrice> stockPriceList) {
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

    private String parse(final String value) {
        return Optional.of(value)
                .map(v -> v.substring(v.lastIndexOf(("）")) + 1))
                .map(String::trim)
                .map(v -> v.replace(" ", ""))
                .orElseThrow(() -> {
                    log.warn("値の変換に失敗したため、NULLで登録します。\tvalue:{}", value);
                    return null;
                });
    }

    private Optional<Integer> parseInteger(final String value) {
        try {
            return Optional.of(Integer.valueOf(value));
        } catch (NumberFormatException e1) {
            return Optional.of(value)
                    .map(v -> v.substring(v.lastIndexOf(("売買高")) + 4, v.length() - 1))
                    .map(String::trim)
                    .map(v -> v.replace(" ", ""))
                    .map(v -> v.replace(",", ""))
                    .map(v -> {
                        try {
                            if (v.equals("--")) {
                                return null;
                            } else {
                                return Integer.valueOf(v);
                            }
                        } catch (NumberFormatException e2) {
                            log.warn("株価変換処理において数値を正常に認識できなかったため、NULLで登録します。\tvalue:{}", v);
                            return null;
                        }
                    });
        }
    }

    private Optional<Double> parseDouble(final String value) {
        try {
            return Optional.of(Double.valueOf(value));
        } catch (NumberFormatException e1) {
            return Optional.of(value)
                    .map(v -> {
                        if (v.contains(")")) {
                            return v.substring(v.lastIndexOf((")")) + 1, v.length() - 1);
                        } else if (v.contains("）")) {
                            return v.substring(v.lastIndexOf(("）")) + 1, v.length() - 1);
                        } else {
                            return v.substring(0, v.length() - 1);
                        }
                    })
                    .map(String::trim)
                    .map(v -> v.replace(",", ""))
                    .map(v -> {
                        try {
                            if (v.equals("--")) {
                                return null;
                            } else {
                                return Double.valueOf(v);
                            }
                        } catch (NumberFormatException e2) {
                            log.warn("株価変換処理において数値を正常に認識できなかったため、NULLで登録します。\tvalue:{}", v);
                            return null;
                        }
                    });
        }
    }

    private Optional<String> convertToCompanyCode(final String edinetCode) {
        final var companyAll = companyDao.selectAll();
        return companyAll.stream()
                .filter(company -> company.getCode().isPresent())
                .filter(company -> edinetCode.equals(company.getEdinetCode()))
                .map(Company::getCode)
                .map(Optional::get)
                .findAny();
    }
}
