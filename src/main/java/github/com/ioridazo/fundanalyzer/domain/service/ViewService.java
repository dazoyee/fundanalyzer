package github.com.ioridazo.fundanalyzer.domain.service;

import github.com.ioridazo.fundanalyzer.domain.bean.CorporateViewBean;
import github.com.ioridazo.fundanalyzer.domain.bean.CorporateViewDao;
import github.com.ioridazo.fundanalyzer.domain.bean.EdinetListViewBean;
import github.com.ioridazo.fundanalyzer.domain.bean.EdinetListViewDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.IndustryDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Company;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Document;
import github.com.ioridazo.fundanalyzer.domain.service.logic.CorporateViewLogic;
import github.com.ioridazo.fundanalyzer.domain.service.logic.EdinetListViewLogic;
import github.com.ioridazo.fundanalyzer.slack.SlackProxy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ViewService {

    private final SlackProxy slackProxy;
    private final CorporateViewLogic corporateViewLogic;
    private final EdinetListViewLogic edinetListViewLogic;
    private final IndustryDao industryDao;
    private final CompanyDao companyDao;
    private final DocumentDao documentDao;
    private final CorporateViewDao corporateViewDao;
    private final EdinetListViewDao edinetListViewDao;

    public ViewService(
            final SlackProxy slackProxy,
            final CorporateViewLogic corporateViewLogic,
            final EdinetListViewLogic edinetListViewLogic, final IndustryDao industryDao,
            final CompanyDao companyDao,
            final DocumentDao documentDao,
            final CorporateViewDao corporateViewDao,
            final EdinetListViewDao edinetListViewDao) {
        this.slackProxy = slackProxy;
        this.corporateViewLogic = corporateViewLogic;
        this.edinetListViewLogic = edinetListViewLogic;
        this.industryDao = industryDao;
        this.companyDao = companyDao;
        this.documentDao = documentDao;
        this.corporateViewDao = corporateViewDao;
        this.edinetListViewDao = edinetListViewDao;
    }

    /**
     * 企業価値等を算出して一定以上を表示する
     *
     * @return 会社一覧
     */
    public List<CorporateViewBean> corporateView() {
        return sortedCompanyList(getCorporateViewBeanList().stream()
                // not null
                .filter(cvb -> cvb.getDiscountRate() != null)
                // 100%以上を表示
                .filter(cvb -> cvb.getDiscountRate().compareTo(BigDecimal.valueOf(100)) > 0)
                .collect(Collectors.toList()));
    }

    /**
     * 企業価値等を算出してすべてを表示する
     *
     * @return 会社一覧
     */
    public List<CorporateViewBean> corporateViewAll() {
        return sortedCompanyList(getCorporateViewBeanList());
    }

    /**
     * 企業価値等を割安度でソートする
     *
     * @return ソート後のリスト
     */
    public List<CorporateViewBean> sortByDiscountRate() {
        return getCorporateViewBeanList().stream()
                // not null
                .filter(cvb -> cvb.getDiscountRate() != null)
                // 100%以上を表示
                .filter(cvb -> cvb.getDiscountRate().compareTo(BigDecimal.valueOf(100)) > 0)
                .sorted(Comparator.comparing(CorporateViewBean::getDiscountRate).reversed())
                .collect(Collectors.toList());
    }

    private List<CorporateViewBean> getCorporateViewBeanList() {
        return corporateViewDao.selectAll().stream()
                // 提出日が存在したら表示する
                .filter(corporateViewBean -> corporateViewBean.getSubmitDate() != null)
                .collect(Collectors.toList());
    }

    /**
     * 非同期で表示するリストをアップデートする
     */
    @Async
    @Transactional
    public void updateCorporateView() {
        final var beanAllList = corporateViewDao.selectAll();
        companyAllTargeted().stream()
                .map(corporateViewLogic::corporateViewOf)
                .forEach(corporateViewBean -> {
                    final var match = beanAllList.stream()
                            .map(CorporateViewBean::getCode)
                            .anyMatch(corporateViewBean.getCode()::equals);
                    if (match) {
                        corporateViewDao.update(corporateViewBean);
                    } else {
                        corporateViewDao.insert(corporateViewBean);
                    }
                });
        slackProxy.sendMessage("g.c.i.f.domain.service.ViewService.display.update.complete.corporate");
        log.info("表示アップデートが正常に終了しました。");
    }

    // ----------

    /**
     * 会社情報の更新日を取得する
     *
     * @return 最新更新日
     */
    public String companyUpdated() {
        return companyDao.selectAll().stream()
                .map(Company::getUpdatedAt)
                .max(LocalDateTime::compareTo)
                .map(dateTime -> dateTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")))
                .orElse("null");
    }

    // ----------

    /**
     * 処理結果をSlackに通知する
     *
     * @param submitDate 対象提出日
     */
    public CompletableFuture<Void> notice(final LocalDate submitDate) {
        final var documentList = documentDao.selectByTypeAndSubmitDate("120", submitDate);
        groupBySubmitDate(documentList).forEach(el -> {
            if (el.getCountTarget().equals(el.getCountScraped()) &&
                    el.getCountTarget().equals(el.getCountAnalyzed())) {
                // info message
                slackProxy.sendMessage(
                        "g.c.i.f.domain.service.ViewService.processing.notice.info",
                        el.getSubmitDate(), el.getCountTarget());
            } else {
                // warn message
                slackProxy.sendMessage(
                        "g.c.i.f.domain.service.ViewService.processing.notice.warn",
                        el.getSubmitDate(), el.getCountTarget(), el.getCountNotScraped());
            }
        });
        return null;
    }

    // ----------

    /**
     * 処理状況を表示するためのリストを取得する
     *
     * @return 処理状況リスト
     */
    public List<EdinetListViewBean> edinetListview() {
        final var viewBeanList = edinetListViewDao.selectAll();
        viewBeanList.removeIf(
                el -> el.getCountTarget().equals(el.getCountScraped()) &&
                        el.getCountTarget().equals(el.getCountAnalyzed())
        );
        return sortedEdinetList(viewBeanList);
    }

    /**
     * すべての処理状況を表示するためのリストを取得する
     *
     * @return 処理状況リスト
     */
    public List<EdinetListViewBean> edinetListViewAll() {
        return sortedEdinetList(edinetListViewDao.selectAll());
    }

    /**
     * 非同期で表示する処理状況リストをアップデートする
     *
     * @param documentTypeCode 書類種別コード
     */
    @Async
    @Transactional
    public void updateEdinetListView(final String documentTypeCode) {
        final var beanAllList = edinetListViewDao.selectAll();
        final var documentList = documentDao.selectByDocumentTypeCode(documentTypeCode);
        groupBySubmitDate(documentList)
                .forEach(edinetListViewBean -> {
                    final var match = beanAllList.stream()
                            .map(EdinetListViewBean::getSubmitDate)
                            .anyMatch(edinetListViewBean.getSubmitDate()::equals);
                    if (match) {
                        edinetListViewDao.update(edinetListViewBean);
                    } else {
                        edinetListViewDao.insert(edinetListViewBean);
                    }
                });
        slackProxy.sendMessage("g.c.i.f.domain.service.ViewService.display.update.complete.edinet.list");
        log.info("処理状況アップデートが正常に終了しました。");
    }

    // ----------

    private List<EdinetListViewBean> groupBySubmitDate(final List<Document> documentList) {
        return documentList.stream()
                // 提出日ごとに件数をカウントする
                .collect(Collectors.groupingBy(Document::getSubmitDate, Collectors.counting()))
                // map -> stream
                .entrySet()
                .stream().map(submitDateCountAllEntry -> edinetListViewLogic.counter(
                        submitDateCountAllEntry.getKey(),
                        submitDateCountAllEntry.getValue(),
                        documentList,
                        companyAllTargeted()
                )).collect(Collectors.toList());
    }

    private List<Company> companyAllTargeted() {
        final var companyList = companyDao.selectAll();
        final var bank = industryDao.selectByName("銀行業");
        final var insurance = industryDao.selectByName("保険業");

        return companyList.stream()
                .filter(company -> company.getCode().isPresent())
                // 銀行業、保険業は対象外とする
                .filter(company -> !bank.getId().equals(company.getIndustryId()))
                .filter(company -> !insurance.getId().equals(company.getIndustryId()))
                .collect(Collectors.toList());
    }

    private List<CorporateViewBean> sortedCompanyList(final List<CorporateViewBean> viewBeanList) {
        return viewBeanList.stream()
                .sorted(Comparator
                        .comparing(CorporateViewBean::getSubmitDate).reversed()
                        .thenComparing(CorporateViewBean::getCode))
                .collect(Collectors.toList());
    }

    private List<EdinetListViewBean> sortedEdinetList(final List<EdinetListViewBean> viewBeanList) {
        return viewBeanList.stream()
                .sorted(Comparator.comparing(EdinetListViewBean::getSubmitDate).reversed())
                .collect(Collectors.toList());
    }
}
