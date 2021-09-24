package github.com.ioridazo.fundanalyzer.domain.domain.cache;

import github.com.ioridazo.fundanalyzer.domain.domain.dao.master.BsSubjectDao;
import github.com.ioridazo.fundanalyzer.domain.domain.dao.master.PlSubjectDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.BsSubjectEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.PlSubjectEntity;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SubjectCache {

    private static final String CACHE_KEY_BS_SUBJECT_LIST = "bsSubjectList";
    private static final String CACHE_KEY_PL_SUBJECT_LIST = "plSubjectList";

    private final BsSubjectDao bsSubjectDao;
    private final PlSubjectDao plSubjectDao;

    public SubjectCache(
            final BsSubjectDao bsSubjectDao,
            final PlSubjectDao plSubjectDao) {
        this.bsSubjectDao = bsSubjectDao;
        this.plSubjectDao = plSubjectDao;
    }

    /**
     * 貸借対照表の科目情報を取得する
     * <ul>
     *    <li>キャッシュがあるときはキャッシュから取得する<li/>
     *    <li>キャッシュがないときはデータベースから取得する<li/>
     * </>
     *
     * @return 貸借対照表の科目情報
     */
    @Cacheable(CACHE_KEY_BS_SUBJECT_LIST)
    public List<BsSubjectEntity> inquiryBsSubjectList() {
        return findBsSubjectList();
    }

    @CachePut(CACHE_KEY_BS_SUBJECT_LIST)
    public List<BsSubjectEntity> findBsSubjectList() {
        return bsSubjectDao.selectAll();
    }

    /**
     * 損益計算書の科目情報を取得する
     * <ul>
     *    <li>キャッシュがあるときはキャッシュから取得する<li/>
     *    <li>キャッシュがないときはデータベースから取得する<li/>
     * </>
     *
     * @return 損益計算書の科目情報
     */
    @Cacheable(CACHE_KEY_PL_SUBJECT_LIST)
    public List<PlSubjectEntity> inquiryPlSubjectList() {
        return findPlSubjectList();
    }

    @CachePut(CACHE_KEY_PL_SUBJECT_LIST)
    public List<PlSubjectEntity> findPlSubjectList() {
        return plSubjectDao.selectAll();
    }
}
