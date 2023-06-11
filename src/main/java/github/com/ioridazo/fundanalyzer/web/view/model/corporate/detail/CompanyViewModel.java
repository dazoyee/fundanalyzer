package github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.StockPriceEntity;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.Stock;

import java.util.Comparator;

public record CompanyViewModel(
        String code,
        String name,
        String industry,
        String edinetCode,
        Boolean isFavorite,
        Integer capitalStock,
        String settlementDate,
        String per,
        String pbr,
        String roe,
        String numberOfShares,
        String marketCapitalization,
        String dividendYield,
        String shareholderBenefit
) {

    public static CompanyViewModel of(final Company company, final Stock stock) {
        return new CompanyViewModel(
                company.code(),
                company.companyName(),
                company.industryName(),
                company.edinetCode(),
                company.favorite(),
                company.capitalStock(),
                company.settlementDate(),
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
