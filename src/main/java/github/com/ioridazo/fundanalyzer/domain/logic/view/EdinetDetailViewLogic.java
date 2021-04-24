package github.com.ioridazo.fundanalyzer.domain.logic.view;

import github.com.ioridazo.fundanalyzer.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.entity.BsEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.DocumentStatus;
import github.com.ioridazo.fundanalyzer.domain.entity.DocumentTypeCode;
import github.com.ioridazo.fundanalyzer.domain.entity.PlEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Company;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.Document;
import github.com.ioridazo.fundanalyzer.domain.logic.analysis.AnalysisLogic;
import github.com.ioridazo.fundanalyzer.domain.logic.view.bean.EdinetDetailViewBean;
import github.com.ioridazo.fundanalyzer.domain.logic.view.bean.EdinetListViewDao;
import github.com.ioridazo.fundanalyzer.domain.util.Converter;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerCalculateException;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

@Component
public class EdinetDetailViewLogic {

    private final AnalysisLogic analysisLogic;
    private final CompanyDao companyDao;
    private final DocumentDao documentDao;
    private final EdinetListViewDao edinetListViewDao;

    public EdinetDetailViewLogic(
            final AnalysisLogic analysisLogic,
            final CompanyDao companyDao,
            final DocumentDao documentDao,
            final EdinetListViewDao edinetListViewDao) {
        this.analysisLogic = analysisLogic;
        this.companyDao = companyDao;
        this.documentDao = documentDao;
        this.edinetListViewDao = edinetListViewDao;
    }

    /**
     * 対象提出日の未処理書類リストを取得する
     *
     * @param submitDate         対象提出日
     * @param targetTypes        書類種別コード
     * @param allTargetCompanies 処理対象となるすべての会社
     * @return 象提出日の未処理書類情報
     */
    @NewSpan("EdinetDetailViewLogic.edinetDetailView")
    public EdinetDetailViewBean edinetDetailView(
            final LocalDate submitDate,
            final List<DocumentTypeCode> targetTypes,
            final List<Company> allTargetCompanies) {
        final List<String> docTypeCode = targetTypes.stream().map(DocumentTypeCode::toValue).collect(Collectors.toList());
        final var cantScrapedList = documentDao.selectByTypeAndSubmitDate(docTypeCode, submitDate).stream()
                // filter companyCode is present
                .filter(d -> Converter.toCompanyCode(d.getEdinetCode(), allTargetCompanies).isPresent())
                // filter removed
                .filter(Document::getNotRemoved)
                .filter(d -> {
                    if (!DocumentStatus.DONE.toValue().equals(d.getScrapedBs())) {
                        // filter scrapedBs is not done
                        return true;
                    } else if (!DocumentStatus.DONE.toValue().equals(d.getScrapedPl())) {
                        // filter scrapedPl is not done
                        return true;
                    } else {
                        // filter scrapedNumberOfShares is not done
                        return !DocumentStatus.DONE.toValue().equals(d.getScrapedNumberOfShares());
                    }
                })
                .collect(Collectors.toList());

        return new EdinetDetailViewBean(
                // 対象提出日の処理状況
                edinetListViewDao.selectBySubmitDate(submitDate),
                // 提出日に関連する未処理ドキュメントのリスト
                cantScrapedList.stream()
                        .map(document -> new EdinetDetailViewBean.DocumentDetail(
                                companyDao.selectByEdinetCode(document.getEdinetCode()).orElse(Company.ofNull()),
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

        return new EdinetDetailViewBean.ValuesForAnalysis(
                fsValue(company, BsEnum.TOTAL_CURRENT_ASSETS, document, analysisLogic::bsValue),
                fsValue(company, BsEnum.TOTAL_INVESTMENTS_AND_OTHER_ASSETS, document, analysisLogic::bsValue),
                fsValue(company, BsEnum.TOTAL_CURRENT_LIABILITIES, document, analysisLogic::bsValue),
                fsValue(company, BsEnum.TOTAL_FIXED_LIABILITIES, document, analysisLogic::bsValue),
                fsValue(company, PlEnum.OPERATING_PROFIT, document, analysisLogic::plValue),
                nsValue(company, document, analysisLogic::nsValue)
        );
    }

    private <T> Long fsValue(
            final Company company,
            final T t,
            final Document document,
            final BiFunction<T, AnalysisLogic.FsValueParameter, Long> biFunction) {
        try {
            return biFunction.apply(
                    t,
                    AnalysisLogic.FsValueParameter.of(
                            company,
                            document.getDocumentPeriod(),
                            DocumentTypeCode.fromValue(document.getDocumentTypeCode()),
                            document.getSubmitDate()
                    )
            );
        } catch (FundanalyzerCalculateException e) {
            return null;
        }
    }

    private Long nsValue(
            final Company company,
            final Document document,
            final ToLongFunction<AnalysisLogic.FsValueParameter> toLongFunction) {
        try {
            return toLongFunction.applyAsLong(
                    AnalysisLogic.FsValueParameter.of(
                            company,
                            document.getDocumentPeriod(),
                            DocumentTypeCode.fromValue(document.getDocumentTypeCode()),
                            document.getSubmitDate()
                    )
            );
        } catch (FundanalyzerCalculateException e) {
            return null;
        }
    }
}
