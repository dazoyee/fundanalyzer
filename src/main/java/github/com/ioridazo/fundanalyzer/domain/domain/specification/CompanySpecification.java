package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.client.csv.bean.EdinetCsvResultBean;
import github.com.ioridazo.fundanalyzer.client.edinet.entity.response.Results;
import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.domain.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.CompanyEntity;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class CompanySpecification {

    private static final Logger log = LogManager.getLogger(CompanySpecification.class);

    private final CompanyDao companyDao;
    private final IndustrySpecification industrySpecification;

    public CompanySpecification(
            final CompanyDao companyDao,
            final IndustrySpecification industrySpecification) {
        this.companyDao = companyDao;
        this.industrySpecification = industrySpecification;
    }

    LocalDateTime nowLocalDateTime() {
        return LocalDateTime.now();
    }

    /**
     * 企業情報を取得する
     *
     * @param edinetCode EDINETコード
     * @return 企業情報
     */
    public Optional<Company> findCompanyByEdinetCode(final String edinetCode) {
        return companyDao.selectByEdinetCode(edinetCode)
                .map(entity -> Company.of(entity, industrySpecification.convertFromIdToName(entity.getIndustryId())));
    }

    /**
     * 企業情報を取得する
     *
     * @param code 企業コード
     * @return 企業情報
     */
    public Optional<Company> findCompanyByCode(final String code) {
        return companyDao.selectByCode(code)
                .map(entity -> Company.of(entity, industrySpecification.convertFromIdToName(entity.getIndustryId())));
    }

    /**
     * 企業情報更新日時を取得する
     *
     * @return 企業情報更新日時
     */
    public Optional<String> findLastUpdateDateTime() {
        return companyDao.selectAll().stream()
                .map(CompanyEntity::getUpdatedAt)
                .max(LocalDateTime::compareTo)
                .map(dateTime -> dateTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")));
    }

    /**
     * 処理対象となる企業情報リストを取得する
     *
     * @return 企業情報リスト
     */
    public List<Company> allTargetCompanies() {
        return companyDao.selectAll().stream()
                .map(entity -> Company.of(entity, industrySpecification.convertFromIdToName(entity.getIndustryId())))
                .filter(company -> company.getCode().isPresent())
                .filter(company -> industrySpecification.isTarget(company.getIndustryId()))
                .collect(Collectors.toList());
    }

    /**
     * 企業情報を登録する
     *
     * @param results EDINETレスポンス
     */
    public void insertIfNotExist(final Results results) {
        results.getEdinetCode().ifPresent(edinetCode -> {
            if (companyDao.selectByEdinetCode(edinetCode).isEmpty()) {
                companyDao.insert(CompanyEntity.ofSqlForeignKey(edinetCode, results.getFilerName(), nowLocalDateTime()));

                log.info(FundanalyzerLogClient.toSpecificationLogObject(
                        MessageFormat.format(
                                "企業情報が登録されていないため、仮情報を登録します。\tEDINETコード:{0}\t企業名:{1}",
                                edinetCode,
                                results.getFilerName()
                        ),
                        edinetCode,
                        Category.DOCUMENT,
                        Process.EDINET
                ));
            }
        });
    }

    /**
     * 企業情報を登録・更新する
     *
     * @param resultBeanList CSVリスト
     */
    public void upsert(final List<EdinetCsvResultBean> resultBeanList) {
        final List<CompanyEntity> allCompanies = companyDao.selectAll();
        resultBeanList.parallelStream().forEach(resultBean -> {
                    if (isPresent(resultBean.getEdinetCode(), allCompanies)) {
                        companyDao.update(CompanyEntity.of(
                                industrySpecification.convertFromNameToId(resultBean.getIndustry()),
                                resultBean,
                                nowLocalDateTime()
                        ));
                    } else {
                        companyDao.insert(CompanyEntity.of(
                                industrySpecification.convertFromNameToId(resultBean.getIndustry()),
                                resultBean,
                                nowLocalDateTime()
                        ));
                    }
                }
        );
    }

    /**
     * 企業がデータベースに存在するか
     *
     * @param edinetCode   EDINETコード
     * @param allCompanies 処理対象となる企業情報リスト
     * @return boolean
     */
    private boolean isPresent(final String edinetCode, final List<CompanyEntity> allCompanies) {
        return allCompanies.stream()
                .map(CompanyEntity::getEdinetCode)
                .anyMatch(edinetCode::equals);
    }
}
