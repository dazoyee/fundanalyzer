package github.com.ioridazo.fundanalyzer.domain.bean;

import github.com.ioridazo.fundanalyzer.domain.entity.master.Company;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.StockPrice;
import lombok.Value;

import java.util.Comparator;
import java.util.List;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
public class BrandDetailCorporateViewBean {

    private final String code;

    private final String name;

    private final String industry;

    private final String edinetCode;

    private final Integer capitalStock;

    private final String settlementDate;

    private final String per;

    private final String pbr;

    private final String roe;

    private final String numberOfShares;

    private final String marketCapitalization;

    private final String dividendYield;

    private final String shareholderBenefit;

    public static BrandDetailCorporateViewBean of(
            final Company company,
            final String industry,
            final List<StockPrice> stockPrices) {
        return new BrandDetailCorporateViewBean(
                company.getCode().orElseThrow(),
                company.getCompanyName(),
                industry,
                company.getEdinetCode(),
                company.getCapitalStock(),
                company.getSettlementDate(),
                stockPrices.stream()
                        .filter(stockPrice -> stockPrice.getPer().isPresent())
                        .max(Comparator.comparing(StockPrice::getTargetDate))
                        .flatMap(StockPrice::getPer)
                        .orElseThrow(),
                stockPrices.stream()
                        .filter(stockPrice -> stockPrice.getPbr().isPresent())
                        .max(Comparator.comparing(StockPrice::getTargetDate))
                        .flatMap(StockPrice::getPbr)
                        .orElseThrow(),
                stockPrices.stream()
                        .filter(stockPrice -> stockPrice.getRoe().isPresent())
                        .max(Comparator.comparing(StockPrice::getTargetDate))
                        .flatMap(StockPrice::getRoe)
                        .orElseThrow(),
                stockPrices.stream()
                        .filter(stockPrice -> stockPrice.getNumberOfShares().isPresent())
                        .max(Comparator.comparing(StockPrice::getTargetDate))
                        .flatMap(StockPrice::getNumberOfShares)
                        .orElseThrow(),
                stockPrices.stream()
                        .filter(stockPrice -> stockPrice.getMarketCapitalization().isPresent())
                        .max(Comparator.comparing(StockPrice::getTargetDate))
                        .flatMap(StockPrice::getMarketCapitalization)
                        .orElseThrow(),
                stockPrices.stream()
                        .filter(stockPrice -> stockPrice.getDividendYield().isPresent())
                        .max(Comparator.comparing(StockPrice::getTargetDate))
                        .flatMap(StockPrice::getDividendYield)
                        .orElseThrow(),
                stockPrices.stream()
                        .filter(stockPrice -> stockPrice.getShareholderBenefit().isPresent())
                        .max(Comparator.comparing(StockPrice::getTargetDate))
                        .flatMap(StockPrice::getShareholderBenefit)
                        .orElseThrow()
        );
    }
}
