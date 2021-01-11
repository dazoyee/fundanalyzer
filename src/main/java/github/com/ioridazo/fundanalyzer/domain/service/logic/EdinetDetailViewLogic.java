package github.com.ioridazo.fundanalyzer.domain.service.logic;

import github.com.ioridazo.fundanalyzer.domain.bean.EdinetDetailViewBean;
import github.com.ioridazo.fundanalyzer.domain.bean.EdinetListViewDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.entity.BsEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.DocumentStatus;
import github.com.ioridazo.fundanalyzer.domain.entity.PlEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Company;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Document;
import github.com.ioridazo.fundanalyzer.domain.service.AnalysisService;
import github.com.ioridazo.fundanalyzer.domain.util.Converter;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class EdinetDetailViewLogic {

    private final CompanyDao companyDao;
    private final DocumentDao documentDao;
    private final EdinetListViewDao edinetListViewDao;
    private final AnalysisService analysisService;

    public EdinetDetailViewLogic(
            final CompanyDao companyDao,
            final DocumentDao documentDao,
            final EdinetListViewDao edinetListViewDao,
            final AnalysisService analysisService) {
        this.companyDao = companyDao;
        this.documentDao = documentDao;
        this.edinetListViewDao = edinetListViewDao;
        this.analysisService = analysisService;
    }

    /**
     * 対象提出日の未処理書類リストを取得する
     *
     * @param documentTypeCode   書類種別コード
     * @param submitDate         対象提出日
     * @param companyAllTargeted 処理対象となるすべての会社
     * @return 象提出日の未処理書類情報
     */
    public EdinetDetailViewBean edinetDetailView(
            final String documentTypeCode,
            final LocalDate submitDate,
            final List<Company> companyAllTargeted) {

        final var cantScrapedList = documentDao.selectByTypeAndSubmitDate(documentTypeCode, submitDate).stream()
                // filter companyCode is present
                .filter(d -> Converter.toCompanyCode(d.getEdinetCode(), companyAllTargeted).isPresent())
                // filter removed
                .filter(Document::getNotRemoved)
                .filter(d -> {
                    if (!DocumentStatus.DONE.toValue().equals(d.getScrapedBs())) {
                        // filter scrapedBs is not done
                        return true;
                    } else if (!DocumentStatus.DONE.toValue().equals(d.getScrapedPl())) {
                        // filter scrapedPl is not done
                        return true;
                        // filter scrapedNumberOfShares is not done
                    } else return !DocumentStatus.DONE.toValue().equals(d.getScrapedNumberOfShares());
                })
                .collect(Collectors.toList());

        return new EdinetDetailViewBean(
                // 対象提出日の処理状況
                edinetListViewDao.selectBySubmitDate(submitDate),
                // 提出日に関連する未処理ドキュメントのリスト
                cantScrapedList.stream()
                        .map(document -> new EdinetDetailViewBean.DocumentDetail(
                                document,
                                valuesForAnalysis(document)
                        )).collect(Collectors.toList())
        );
    }

    /**
     * 対象書類のスクレイピング結果を取得する
     *
     * @param document 対象書類
     * @return スクレイピング結果
     */
    private EdinetDetailViewBean.ValuesForAnalysis valuesForAnalysis(final Document document) {
        final var company = companyDao.selectByEdinetCode(document.getEdinetCode()).orElseThrow();
        final var period = document.getPeriod();

        return new EdinetDetailViewBean.ValuesForAnalysis(
                analysisService.bsValues(company, BsEnum.TOTAL_CURRENT_ASSETS, period),
                analysisService.bsValues(company, BsEnum.TOTAL_INVESTMENTS_AND_OTHER_ASSETS, period),
                analysisService.bsValues(company, BsEnum.TOTAL_CURRENT_LIABILITIES, period),
                analysisService.bsValues(company, BsEnum.TOTAL_FIXED_LIABILITIES, period),
                analysisService.plValues(company, PlEnum.OPERATING_PROFIT, period),
                analysisService.nsValue(company, period)
        );
    }
}
