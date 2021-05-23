package github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.StockPriceEntity;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.Stock;
import lombok.Value;

import java.util.Comparator;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value
public class CompanyViewModel {
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

    public static CompanyViewModel of(final Company company, final Stock stock) {
        return new CompanyViewModel(
                company.getCode().orElse(null),
                company.getCompanyName(),
                company.getIndustryName(),
                company.getEdinetCode(),
                company.getCapitalStock(),
                company.getSettlementDate(),
                stock.getStockPriceEntityList().stream()
                        .filter(stockPrice -> stockPrice.getPer().isPresent())
                        .max(Comparator.comparing(StockPriceEntity::getTargetDate))
                        .flatMap(StockPriceEntity::getPer)
                        .orElse(null),
                stock.getStockPriceEntityList().stream()
                        .filter(stockPrice -> stockPrice.getPbr().isPresent())
                        .max(Comparator.comparing(StockPriceEntity::getTargetDate))
                        .flatMap(StockPriceEntity::getPbr)
                        .orElse(null),
                stock.getStockPriceEntityList().stream()
                        .filter(stockPrice -> stockPrice.getRoe().isPresent())
                        .max(Comparator.comparing(StockPriceEntity::getTargetDate))
                        .flatMap(StockPriceEntity::getRoe)
                        .orElse(null),
                stock.getStockPriceEntityList().stream()
                        .filter(stockPrice -> stockPrice.getNumberOfShares().isPresent())
                        .max(Comparator.comparing(StockPriceEntity::getTargetDate))
                        .flatMap(StockPriceEntity::getNumberOfShares)
                        .orElse(null),
                stock.getStockPriceEntityList().stream()
                        .filter(stockPrice -> stockPrice.getMarketCapitalization().isPresent())
                        .max(Comparator.comparing(StockPriceEntity::getTargetDate))
                        .flatMap(StockPriceEntity::getMarketCapitalization)
                        .orElse(null),
                stock.getStockPriceEntityList().stream()
                        .filter(stockPrice -> stockPrice.getDividendYield().isPresent())
                        .max(Comparator.comparing(StockPriceEntity::getTargetDate))
                        .flatMap(StockPriceEntity::getDividendYield)
                        .orElse(null),
                stock.getStockPriceEntityList().stream()
                        .filter(stockPrice -> stockPrice.getShareholderBenefit().isPresent())
                        .max(Comparator.comparing(StockPriceEntity::getTargetDate))
                        .flatMap(StockPriceEntity::getShareholderBenefit)
                        .orElse(null)
        );
    }
}
