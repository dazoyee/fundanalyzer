package github.com.ioridazo.fundanalyzer.mapper;

import github.com.ioridazo.fundanalyzer.domain.csv.bean.EdinetCsvResultBean;
import github.com.ioridazo.fundanalyzer.domain.dao.master.IndustryDao;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Company;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Consolidated;
import github.com.ioridazo.fundanalyzer.domain.entity.master.ListCategories;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

import static github.com.ioridazo.fundanalyzer.domain.entity.master.Consolidated.CONSOLIDATED;
import static github.com.ioridazo.fundanalyzer.domain.entity.master.Consolidated.NO_CONSOLIDATED;
import static github.com.ioridazo.fundanalyzer.domain.entity.master.ListCategories.LISTED;
import static github.com.ioridazo.fundanalyzer.domain.entity.master.ListCategories.UNLISTED;

@Component
public class CsvMapper {

    final private IndustryDao industryDao;

    public CsvMapper(final IndustryDao industryDao) {
        this.industryDao = industryDao;
    }

    public Optional<Company> map(final EdinetCsvResultBean resultBean) {
        if (resultBean.getSecuritiesCode().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Company(
                resultBean.getSecuritiesCode(),
                resultBean.getSubmitterName(),
                String.valueOf(industryDao.selectByName(resultBean.getIndustry()).getId()),
                resultBean.getEdinetCode(),
                mapToListed(resultBean.getListCategories()).toValue(),
                mapToConsolidated(resultBean.getConsolidated()).toValue(),
                resultBean.getCapitalStock(),
                resultBean.getSettlementDate(),
                LocalDateTime.now(),
                LocalDateTime.now()));
    }

    private ListCategories mapToListed(final String value) {
        if (LISTED.toName().equals(value)) return LISTED;
        else if (UNLISTED.toName().equals(value)) return UNLISTED;
        else if (value.isEmpty()) return ListCategories.NULL;
        else throw new RuntimeException("マッピングエラー");
    }

    private Consolidated mapToConsolidated(final String value) {
        if (CONSOLIDATED.toName().equals(value)) return CONSOLIDATED;
        else if (NO_CONSOLIDATED.toName().equals(value)) return NO_CONSOLIDATED;
        else if (value.isEmpty()) return Consolidated.NULL;
        else throw new RuntimeException("マッピングエラー");
    }
}
