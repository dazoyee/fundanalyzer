package github.com.ioridazo.fundanalyzer.domain.logic.view;

import github.com.ioridazo.fundanalyzer.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.dao.transaction.DocumentDao;
import github.com.ioridazo.fundanalyzer.domain.entity.BsEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.DocumentStatus;
import github.com.ioridazo.fundanalyzer.domain.entity.DocumentTypeCode;
import github.com.ioridazo.fundanalyzer.domain.entity.PlEnum;
import github.com.ioridazo.fundanalyzer.domain.entity.master.CompanyEntity;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.DocumentEntity;
import github.com.ioridazo.fundanalyzer.domain.logic.analysis.AnalysisLogic;
import github.com.ioridazo.fundanalyzer.domain.logic.view.bean.EdinetDetailViewBean;
import github.com.ioridazo.fundanalyzer.domain.logic.view.bean.EdinetListViewDao;
import github.com.ioridazo.fundanalyzer.domain.util.Converter;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException;
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
            final List<CompanyEntity> allTargetCompanies) {
        final List<String> docTypeCode = targetTypes.stream().map(DocumentTypeCode::toValue).collect(Collectors.toList());
        final var cantScrapedList = documentDao.selectByTypeAndSubmitDate(docTypeCode, submitDate).stream()
                // filter companyCode is present
                .filter(d -> Converter.toCompanyCode(d.getEdinetCode().orElseThrow(), allTargetCompanies).isPresent())
                // filter removed
//                .filter(DocumentEntity::getNotRemoved)
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
                edinetListViewDao.selectBySubmitDate(submitDate).orElseThrow(),
                // 提出日に関連する未処理ドキュメントのリスト
                cantScrapedList.stream()
                        .map(document -> new EdinetDetailViewBean.DocumentDetail(
                                companyDao.selectByEdinetCode(document.getEdinetCode().orElseThrow()).orElse(CompanyEntity.ofNull()),
                                document,
                                valuesForAnalysis(document)
                        )).collect(Collectors.toList())
        );
    }

    /**
     * 対象書類のスクレイピング結果を取得する
     *
     * @param documentEntity 対象書類
     * @return スクレイピング結果
     */
    private EdinetDetailViewBean.ValuesForAnalysis valuesForAnalysis(final DocumentEntity documentEntity) {
        final var company = companyDao.selectByEdinetCode(documentEntity.getEdinetCode().orElseThrow()).orElseThrow();

        return new EdinetDetailViewBean.ValuesForAnalysis(
                fsValue(company, BsEnum.TOTAL_CURRENT_ASSETS, documentEntity, analysisLogic::bsValue),
                fsValue(company, BsEnum.TOTAL_INVESTMENTS_AND_OTHER_ASSETS, documentEntity, analysisLogic::bsValue),
                fsValue(company, BsEnum.TOTAL_CURRENT_LIABILITIES, documentEntity, analysisLogic::bsValue),
                fsValue(company, BsEnum.TOTAL_FIXED_LIABILITIES, documentEntity, analysisLogic::bsValue),
                fsValue(company, PlEnum.OPERATING_PROFIT, documentEntity, analysisLogic::plValue),
                nsValue(company, documentEntity, analysisLogic::nsValue)
        );
    }

    private <T> Long fsValue(
            final CompanyEntity companyEntity,
            final T t,
            final DocumentEntity documentEntity,
            final BiFunction<T, AnalysisLogic.FsValueParameter, Long> biFunction) {
        try {
            return biFunction.apply(
                    t,
                    AnalysisLogic.FsValueParameter.of(
                            companyEntity,
                            documentEntity.getDocumentPeriod().orElseThrow(() -> new FundanalyzerNotExistException("documentPeriod")),
                            DocumentTypeCode.fromValue(documentEntity.getDocumentTypeCode().orElseThrow()),
                            documentEntity.getSubmitDate()
                    )
            );
        } catch (FundanalyzerNotExistException e) {
            return null;
        }
    }

    private Long nsValue(
            final CompanyEntity companyEntity,
            final DocumentEntity documentEntity,
            final ToLongFunction<AnalysisLogic.FsValueParameter> toLongFunction) {
        try {
            return toLongFunction.applyAsLong(
                    AnalysisLogic.FsValueParameter.of(
                            companyEntity,
                            documentEntity.getDocumentPeriod().orElseThrow(() -> new FundanalyzerNotExistException("documentPeriod")),
                            DocumentTypeCode.fromValue(documentEntity.getDocumentTypeCode().orElseThrow()),
                            documentEntity.getSubmitDate()
                    )
            );
        } catch (FundanalyzerNotExistException e) {
            return null;
        }
    }
}
