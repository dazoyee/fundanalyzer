package github.com.ioridazo.fundanalyzer.domain.domain.cache;

import github.com.ioridazo.fundanalyzer.domain.domain.dao.master.IndustryDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.IndustryEntity;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class IndustryCache {

    private static final String CACHE_KEY_INDUSTRY_LIST = "industryList";

    private final IndustryDao industryDao;

    public IndustryCache(final IndustryDao industryDao) {
        this.industryDao = industryDao;
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
}
