package github.com.ioridazo.fundanalyzer.mapper;

import github.com.ioridazo.fundanalyzer.domain.csv.bean.EdinetCsvResultBean;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Company;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Consolidated;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Industry;
import github.com.ioridazo.fundanalyzer.domain.entity.master.ListCategories;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static github.com.ioridazo.fundanalyzer.domain.entity.master.Consolidated.CONSOLIDATED;
import static github.com.ioridazo.fundanalyzer.domain.entity.master.Consolidated.NO_CONSOLIDATED;
import static github.com.ioridazo.fundanalyzer.domain.entity.master.ListCategories.LISTED;
import static github.com.ioridazo.fundanalyzer.domain.entity.master.ListCategories.UNLISTED;

public class CsvMapper {

    public static Optional<Company> map(final List<Industry> industryList, final EdinetCsvResultBean resultBean) {
        if (resultBean.getSecuritiesCode().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Company(
                resultBean.getSecuritiesCode(),
                resultBean.getSubmitterName(),
                mapToIndustryId(industryList, resultBean.getIndustry()),
                resultBean.getEdinetCode(),
                mapToListed(resultBean.getListCategories()).toValue(),
                mapToConsolidated(resultBean.getConsolidated()).toValue(),
                resultBean.getCapitalStock(),
                resultBean.getSettlementDate(),
                LocalDateTime.now(),
                LocalDateTime.now()));
    }

    private static String mapToIndustryId(final List<Industry> industryList, final String industryName) {
        return industryList.stream()
                .filter(industry -> industryName.equals(industry.getName()))
                .map(Industry::getId)
                .map(String::valueOf)
                .findAny()
                .orElseThrow();
    }

    private static ListCategories mapToListed(final String value) {
        if (LISTED.toName().equals(value)) return LISTED;
        else if (UNLISTED.toName().equals(value)) return UNLISTED;
        else if (value.isEmpty()) return ListCategories.NULL;
        else throw new FundanalyzerRuntimeException("マッピングエラー");
    }

    private static Consolidated mapToConsolidated(final String value) {
        if (CONSOLIDATED.toName().equals(value)) return CONSOLIDATED;
        else if (NO_CONSOLIDATED.toName().equals(value)) return NO_CONSOLIDATED;
        else if (value.isEmpty()) return Consolidated.NULL;
        else throw new FundanalyzerRuntimeException("マッピングエラー");
    }
}
