package github.com.ioridazo.fundanalyzer.domain.usecase;

import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.ViewFilterSettingEntity;
import github.com.ioridazo.fundanalyzer.web.model.ViewFilterSettingInputData;
import io.micrometer.observation.annotation.Observed;

public interface ViewFilterSettingUseCase {

    @Observed
    ViewFilterSettingEntity getSetting();

    @Observed
    void updateSetting(ViewFilterSettingInputData inputData);
}
