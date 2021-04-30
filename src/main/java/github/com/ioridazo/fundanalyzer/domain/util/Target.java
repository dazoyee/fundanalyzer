package github.com.ioridazo.fundanalyzer.domain.util;

import github.com.ioridazo.fundanalyzer.domain.entity.DocumentTypeCode;
import github.com.ioridazo.fundanalyzer.domain.entity.master.CompanyEntity;
import github.com.ioridazo.fundanalyzer.domain.entity.master.IndustryEntity;
import github.com.ioridazo.fundanalyzer.domain.entity.transaction.AnalysisResultEntity;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class Target {

    private Target() {
    }

    /**
     * 処理対象となる会社を抽出する
     * <p/>
     * 銀行業、保険業は対象外とする想定
     *
     * @param companyEntityList          データベースに登録されている会社一覧
     * @param excludedIndustryEntityList 対象外とする業種リスト
     * @return 処理対象の会社一覧
     */
    public static List<CompanyEntity> allCompanies(
            final List<CompanyEntity> companyEntityList,
            final List<IndustryEntity> excludedIndustryEntityList) {
        return companyEntityList.stream()
                .filter(company -> company.getCode().isPresent())
                .filter(company -> excludedIndustryEntityList.stream().noneMatch(industry -> company.getIndustryId().equals(industry.getId())))
                .collect(Collectors.toList());
    }

    /**
     * 処理対象となる会社から特定のEdinetコードが含まれているかどうかを返却する
     *
     * @param edinetCode           EDINETコード
     * @param companyEntityList          データベースに登録されている会社一覧
     * @param excludedIndustryEntityList 対象外とする業種リスト
     * @return boolean
     */
    public static boolean containsEdinetCode(
            final String edinetCode,
            final List<CompanyEntity> companyEntityList,
            final List<IndustryEntity> excludedIndustryEntityList) {
        return allCompanies(companyEntityList, excludedIndustryEntityList).stream()
                .map(CompanyEntity::getEdinetCode)
                .collect(Collectors.toList())
                .contains(edinetCode);
    }

    /**
     * 処理対象とする分析結果を抽出する
     * <p/>
     * documentPeriodが重複するときに、最新提出日を採用する
     *
     * @param analysisResultEntityList 分析結果リスト
     * @return 処理対象の分析結果一覧
     */
    public static List<AnalysisResultEntity> distinctAnalysisResults(final List<AnalysisResultEntity> analysisResultEntityList) {
        final List<LocalDate> periodList = analysisResultEntityList.stream()
                .map(AnalysisResultEntity::getDocumentPeriod)
                // null のときはEPOCHとなるため、除外する
                .filter(period -> !LocalDate.EPOCH.isEqual(period))
                .distinct()
                .collect(Collectors.toList());

        return periodList.stream()
                .map(period -> analysisResultEntityList.stream()
                        // match period
                        .filter(analysisResult -> period.equals(analysisResult.getDocumentPeriod()))
                        // latest submit date
                        .max(Comparator.comparing(AnalysisResultEntity::getSubmitDate))
                        .orElseThrow())
                .collect(Collectors.toList());
    }

    /**
     * 有価証券報告書
     *
     * @return 書類種別コード
     */
    public static List<DocumentTypeCode> annualSecuritiesReport() {
        return List.of(DocumentTypeCode.DTC_120, DocumentTypeCode.DTC_130);
    }
}
