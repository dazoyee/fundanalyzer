package github.com.ioridazo.fundanalyzer.domain.logic.view.bean;

import github.com.ioridazo.fundanalyzer.domain.entity.master.CompanyEntity;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.StockPriceEntity;
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
            final CompanyEntity companyEntity,
            final String industry,
            final List<StockPriceEntity> stockPriceEntities) {
        return new BrandDetailCorporateViewBean(
                companyEntity.getCode().orElseThrow(),
                companyEntity.getCompanyName(),
                industry,
                companyEntity.getEdinetCode(),
                companyEntity.getCapitalStock(),
                companyEntity.getSettlementDate(),
                stockPriceEntities.stream()
                        .filter(stockPrice -> stockPrice.getPer().isPresent())
                        .max(Comparator.comparing(StockPriceEntity::getTargetDate))
                        .flatMap(StockPriceEntity::getPer)
                        .orElseThrow(),
                stockPriceEntities.stream()
                        .filter(stockPrice -> stockPrice.getPbr().isPresent())
                        .max(Comparator.comparing(StockPriceEntity::getTargetDate))
                        .flatMap(StockPriceEntity::getPbr)
                        .orElseThrow(),
                stockPriceEntities.stream()
                        .filter(stockPrice -> stockPrice.getRoe().isPresent())
                        .max(Comparator.comparing(StockPriceEntity::getTargetDate))
                        .flatMap(StockPriceEntity::getRoe)
                        .orElseThrow(),
                stockPriceEntities.stream()
                        .filter(stockPrice -> stockPrice.getNumberOfShares().isPresent())
                        .max(Comparator.comparing(StockPriceEntity::getTargetDate))
                        .flatMap(StockPriceEntity::getNumberOfShares)
                        .orElseThrow(),
                stockPriceEntities.stream()
                        .filter(stockPrice -> stockPrice.getMarketCapitalization().isPresent())
                        .max(Comparator.comparing(StockPriceEntity::getTargetDate))
                        .flatMap(StockPriceEntity::getMarketCapitalization)
                        .orElseThrow(),
                stockPriceEntities.stream()
                        .filter(stockPrice -> stockPrice.getDividendYield().isPresent())
                        .max(Comparator.comparing(StockPriceEntity::getTargetDate))
                        .flatMap(StockPriceEntity::getDividendYield)
                        .orElseThrow(),
                stockPriceEntities.stream()
                        .filter(stockPrice -> stockPrice.getShareholderBenefit().isPresent())
                        .max(Comparator.comparing(StockPriceEntity::getTargetDate))
                        .flatMap(StockPriceEntity::getShareholderBenefit)
                        .orElseThrow()
        );
    }
}
