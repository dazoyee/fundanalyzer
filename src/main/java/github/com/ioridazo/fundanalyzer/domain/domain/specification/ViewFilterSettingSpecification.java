package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.domain.domain.dao.master.ViewFilterSettingDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.ViewFilterSettingEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class ViewFilterSettingSpecification {

    private static final Integer SETTING_ID = 1;

    private final ViewFilterSettingDao viewFilterSettingDao;

    public ViewFilterSettingSpecification(final ViewFilterSettingDao viewFilterSettingDao) {
        this.viewFilterSettingDao = viewFilterSettingDao;
    }

    public ViewFilterSettingEntity findSetting() {
        return viewFilterSettingDao.selectOne();
    }

    public void updateSetting(
            final BigDecimal discountRate,
            final BigDecimal outlierOfStandardDeviation,
            final BigDecimal coefficientOfVariation,
            final BigDecimal diffForecastStock,
            final Integer corporateSize) {
        viewFilterSettingDao.update(new ViewFilterSettingEntity(
                SETTING_ID,
                discountRate,
                outlierOfStandardDeviation,
                coefficientOfVariation,
                diffForecastStock,
                corporateSize,
                LocalDateTime.now()
        ));
    }
}
