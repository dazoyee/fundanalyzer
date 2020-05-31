package github.com.ioridazo.fundanalyzer.mapper;

import github.com.ioridazo.fundanalyzer.domain.csv.bean.EdinetCsvResultBean;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Company;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Consolidated;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Industry;
import github.com.ioridazo.fundanalyzer.domain.entity.master.ListCategories;

import java.time.LocalDateTime;
import java.util.List;

public class CsvMapper {

    public static Company map(final List<Industry> industryList, final EdinetCsvResultBean resultBean) {
        return new Company(
                resultBean.getSecuritiesCode().isBlank() ? null : resultBean.getSecuritiesCode(),
                resultBean.getSubmitterName(),
                mapToIndustryId(industryList, resultBean.getIndustry()),
                resultBean.getEdinetCode(),
                ListCategories.fromName(resultBean.getListCategories()).toValue(),
                Consolidated.fromName(resultBean.getConsolidated()).toValue(),
                resultBean.getCapitalStock(),
                resultBean.getSettlementDate().isBlank() ? null : resultBean.getSettlementDate(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private static String mapToIndustryId(final List<Industry> industryList, final String industryName) {
        return industryList.stream()
                .filter(industry -> industryName.equals(industry.getName()))
                .map(Industry::getId)
                .map(String::valueOf)
                .findAny()
                .orElseThrow();
    }
}
