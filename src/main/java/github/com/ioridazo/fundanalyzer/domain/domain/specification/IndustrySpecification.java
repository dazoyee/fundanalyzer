package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.client.csv.bean.EdinetCsvResultBean;
import github.com.ioridazo.fundanalyzer.config.AnalysisCoefficient;
import github.com.ioridazo.fundanalyzer.domain.domain.dao.master.IndustryDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.IndustryEntity;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Component
public class IndustrySpecification {

    private static final String CACHE_KEY_INDUSTRY_LIST = "industryList";

    private final IndustryDao industryDao;

    @Value("${app.config.scraping.no-industry}")
    List<String> noTargetList;

    @Value("${app.config.analysis.net-asset-model-industry}")
    List<String> netAssetModelIndustryList;

    public IndustrySpecification(
            final IndustryDao industryDao) {
        this.industryDao = industryDao;
    }

    LocalDateTime nowLocalDateTime() {
        return LocalDateTime.now();
    }

    /**
     * 業種情報を取得する
     * <ul>
     *    <li>キャッシュがあるときはキャッシュから取得する<li/>
     *    <li>キャッシュがないときはデータベースから取得する<li/>
     * </>
     *
     * @return 業種情報
     */
    @Cacheable(CACHE_KEY_INDUSTRY_LIST)
    public List<IndustryEntity> inquiryIndustryList() {
        return findIndustryList();
    }

    @CachePut(CACHE_KEY_INDUSTRY_LIST)
    public List<IndustryEntity> findIndustryList() {
        return industryDao.selectAll();
    }

    /**
     * 業種名から業種IDに変換する
     *
     * @param industryName 業種名
     * @return 業種ID
     */
    public Integer convertFromNameToId(final String industryName) {
        return inquiryIndustryList().stream()
                .filter(industry -> Objects.equals(industryName, industry.name()))
                .map(IndustryEntity::id)
                .findFirst()
                .orElseThrow(() -> new FundanalyzerRuntimeException("業種IDが存在しません。"));
    }

    /**
     * 業種IDから業種名に変換する
     *
     * @param id 業種ID
     * @return 業種名
     */
    public String convertFromIdToName(final Integer id) {
        return inquiryIndustryList().stream()
                .filter(industryEntity -> Objects.equals(id, industryEntity.id()))
                .map(IndustryEntity::name)
                .findFirst()
                .orElseThrow(() -> new FundanalyzerRuntimeException("業種名が存在しません。"));
    }

    /**
     * 業種別の企業価値算出係数を取得する。
     *
     * @param industryId 業種ID
     * @return 業種行の営業利益倍率・流動負債調整係数を持つ係数
     * @throws FundanalyzerNotExistException 業種IDが null または該当業種が存在しないとき
     */
    public AnalysisCoefficient resolveCoefficient(final Integer industryId) {
        return inquiryIndustryList().stream()
                .filter(industry -> Objects.equals(industryId, industry.id()))
                // 係数列が NULL の行（未マイグレーション・簡易生成等）は対象外とし NPE を防ぐ
                .filter(industry -> industry.operatingProfitWeight() != null
                                    && industry.currentLiabilitiesRatio() != null
                                    && industry.costOfEquity() != null)
                .findFirst()
                .map(industry -> new AnalysisCoefficient(
                        industry.operatingProfitWeight(),
                        industry.currentLiabilitiesRatio(),
                        industry.costOfEquity()))
                .orElseThrow(() -> new FundanalyzerNotExistException("業種別係数が存在しません。"));
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
     * 純資産ベース企業価値モデル適用対象かどうか
     *
     * @param id 業種ID
     * @return boolean
     */
    public boolean isNetAssetModel(final Integer id) {
        return netAssetModelIndustryList.stream()
                .map(this::convertFromNameToId)
                .filter(Objects::nonNull)
                .anyMatch(id::equals);
    }

    /**
     * 業種がデータベースに存在するか
     *
     * @param name 業種名
     * @return boolean
     */
    private boolean isEmpty(final String name) {
        return inquiryIndustryList().stream()
                .filter(industryEntity -> Objects.equals(name, industryEntity.name()))
                .findFirst()
                .isEmpty();
    }
}
