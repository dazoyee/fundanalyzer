package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.domain.domain.dao.master.IndustryDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.IndustryEntity;
import github.com.ioridazo.fundanalyzer.client.csv.bean.EdinetCsvResultBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class IndustrySpecification {

    private final IndustryDao industryDao;

    @Value("${app.config.target.no-industry}")
    List<String> noTargetList;

    public IndustrySpecification(final IndustryDao industryDao) {
        this.industryDao = industryDao;
    }

    LocalDateTime nowLocalDateTime() {
        return LocalDateTime.now();
    }

    /**
     * 業種名から業種IDに変換する
     *
     * @param industryName 業種名
     * @return 業種ID
     */
    public Integer convertFromNameToId(final String industryName) {
        return industryDao.selectByName(industryName).getId();
    }

    /**
     * 業種IDから業種名に変換する
     *
     * @param id 業種ID
     * @return 業種名
     */
    public String convertFromIdToName(final Integer id) {
        return industryDao.selectById(id).getName();
    }

    /**
     * 業種情報を登録する
     *
     * @param resultBeanList CSVリスト
     */
    public void insert(final List<EdinetCsvResultBean> resultBeanList) {
        resultBeanList.stream()
                .map(EdinetCsvResultBean::getIndustry)
                .distinct()
                .filter(this::isEmpty)
                .forEach(industryName -> industryDao.insert(IndustryEntity.of(industryName, nowLocalDateTime())));
    }

    /**
     * 処理対象かどうか
     *
     * @param id 業種ID
     * @return boolean
     */
    public boolean isTarget(final Integer id) {
        return noTargetList.stream()
                .map(this::convertFromNameToId)
                .filter(Objects::nonNull)
                .noneMatch(id::equals);
    }

    /**
     * 業種がデータベースに存在するか
     *
     * @param name 業種名
     * @return boolean
     */
    private boolean isEmpty(final String name) {
        return Optional.ofNullable(industryDao.selectByName(name)).isEmpty();
    }
}
