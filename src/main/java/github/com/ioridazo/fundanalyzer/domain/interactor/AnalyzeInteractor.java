package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.config.AnalysisCoefficient;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.IndustrySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.AnalysisResultSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CompanySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.DocumentSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.FinancialStatementSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.ValuationSpecification;
import github.com.ioridazo.fundanalyzer.domain.usecase.AnalyzeUseCase;
import github.com.ioridazo.fundanalyzer.domain.value.AnalysisResult;
import github.com.ioridazo.fundanalyzer.domain.value.AverageInfo;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.CorporateValue;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.domain.value.FinanceValue;
import github.com.ioridazo.fundanalyzer.domain.value.RecalculationPreview;
import github.com.ioridazo.fundanalyzer.domain.value.RecalculationResult;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.model.IdInputData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class AnalyzeInteractor implements AnalyzeUseCase {

    private static final Logger log = LogManager.getLogger(AnalyzeInteractor.class);
    private static final String BACKTEST_CACHE_NAME = "backtest";

    private final CompanySpecification companySpecification;
    private final DocumentSpecification documentSpecification;
    private final FinancialStatementSpecification financialStatementSpecification;
    private final AnalysisResultSpecification analysisResultSpecification;
    private final ValuationSpecification valuationSpecification;
    private final IndustrySpecification industrySpecification;
    private final CacheManager cacheManager;

    public AnalyzeInteractor(
            final CompanySpecification companySpecification,
            final DocumentSpecification documentSpecification,
            final FinancialStatementSpecification financialStatementSpecification,
            final AnalysisResultSpecification analysisResultSpecification,
            final ValuationSpecification valuationSpecification,
            final IndustrySpecification industrySpecification,
            final CacheManager cacheManager) {
        this.companySpecification = companySpecification;
        this.documentSpecification = documentSpecification;
        this.financialStatementSpecification = financialStatementSpecification;
        this.analysisResultSpecification = analysisResultSpecification;
        this.valuationSpecification = valuationSpecification;
        this.industrySpecification = industrySpecification;
        this.cacheManager = cacheManager;
    }

    /**
     * 企業価値を算出する
     *
     * @param inputData 書類ID
     */
    @Override
    public void analyze(final IdInputData inputData) {
        final long startTime = System.currentTimeMillis();
        final Document document = documentSpecification.findDocument(inputData);
        this.analyze(document);

        log.info(FundanalyzerLogClient.toInteractorLogObject(
                MessageFormat.format("書類ID[{0}]の分析が正常に終了しました。", inputData.getId()),
                document.getDocumentId(),
                document.getEdinetCode(),
                Category.ANALYSIS,
                Process.ANALYSIS,
                System.currentTimeMillis() - startTime
        ));
    }

    /**
     * 企業価値を算出する
     *
     * @param inputData 提出日
     */
    @Override
    public void analyze(DateInputData inputData) {
        final long startTime = System.currentTimeMillis();

        try {
            final List<Document> targetList = documentSpecification.analysisTargetList(inputData);
            if (targetList.isEmpty()) {
                log.info(FundanalyzerLogClient.toInteractorLogObject(
                        MessageFormat.format(
                                "次の提出日に関する書類は分析済みかまたはデータベースに存在しませんでした。\t対象提出日:{0}",
                                inputData.getDate()
                        ),
                        Category.ANALYSIS,
                        Process.ANALYSIS,
                        System.currentTimeMillis() - startTime
                ));
            } else {
                if (targetList.size() > 10) {
                    targetList.parallelStream().forEach(this::analyze);
                } else {
                    targetList.forEach(this::analyze);
                }

                log.info(FundanalyzerLogClient.toInteractorLogObject(
                        MessageFormat.format(
                                "次の提出日に関する書類に対して分析を正常に終了しました。\t対象提出日:{0}",
                                inputData.getDate()
                        ),
                        Category.ANALYSIS,
                        Process.ANALYSIS,
                        System.currentTimeMillis() - startTime
                ));
            }
        } catch (final Exception e) {
            log.error(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format(
                            "{0}付のドキュメントに対して想定外のエラーが発生しました。",
                            inputData.getDate()
                    ),
                    Category.DOCUMENT,
                    Process.ANALYSIS
            ), e);
        }
    }

    /**
     * 企業価値を算出する
     *
     * @param document ドキュメント
     */
    void analyze(final Document document) {
        try {
            final FinanceValue financeValue = financialStatementSpecification.getFinanceValue(document);
            final Integer industryId = companySpecification.findCompanyByEdinetCode(document.getEdinetCode())
                    .map(Company::industryId)
                    .orElse(null);
            final AnalysisCoefficient coefficient = industrySpecification.resolveCoefficient(industryId);
            final AnalysisResult analysisResult = new AnalysisResult(
                    financeValue,
                    document,
                    coefficient,
                    resolveCorporateValueModel(industryId)
            );

            analysisResultSpecification.insert(document, analysisResult);

        } catch (final FundanalyzerNotExistException e) {
            if (e.getFs().isEmpty()) {
                // 財務科目以外の欠損（会社マスタ未登録・業種別係数なし等）はこの書類のみスキップし、バッチ全体は継続する
                log.warn(FundanalyzerLogClient.toInteractorLogObject(
                        MessageFormat.format(
                                "会社情報または業種別係数が存在しないため、分析をスキップしました。\t書類ID:{0}\tEDINETコード:{1}",
                                document.getDocumentId(),
                                document.getEdinetCode()
                        ),
                        document,
                        Category.ANALYSIS,
                        Process.ANALYSIS
                ), e);
                return;
            }
            final FinancialStatementEnum fs = e.getFs().get();
            log.warn(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format(
                            "{0}の必要な値がデータベースに存在しないかまたはNULLで登録されているため、分析できませんでした。次の項目を確認してください。" +
                            "\t会社コード:{1}\t書類ID:{2}\t科目名:{3}\t対象年:{4}\n書類パス:{5}",
                            fs.getName(),
                            companySpecification.findCompanyByEdinetCode(document.getEdinetCode()).map(Company::code).orElse("null"),
                            document.getDocumentId(),
                            e.getSubjectName().orElse("null"),
                            document.getDocumentPeriod().map(String::valueOf).orElse("null"),
                            document.getFsDocumentPath(fs).orElse("null")
                    ),
                    document,
                    Category.ANALYSIS,
                    Process.of(fs)
            ));

            // ステータスをHALF_WAY（途中）に更新する
            documentSpecification.updateFsToHalfWay(document, fs);
        }
    }

    /**
     * 企業価値情報を取得する
     *
     * @param company 企業情報
     * @return 企業価値
     */
    @Override
    public CorporateValue calculateCorporateValue(final Company company) {
        final CorporateValue corporateValue = CorporateValue.of();

        // 最新企業価値
        final Optional<BigDecimal> latestCorporateValue =
                analysisResultSpecification.findLatestAnalysisResult(company.code())
                        .map(AnalysisResultEntity::getCorporateValue);
        if (latestCorporateValue.isEmpty()) {
            return corporateValue;
        } else {
            corporateValue.setLatestCorporateValue(latestCorporateValue.get());
        }

        // 平均企業価値
        final List<AverageInfo> averageInfoList = new ArrayList<>();
        List.of(
                AverageInfo.Year.THREE,
                AverageInfo.Year.FIVE,
                AverageInfo.Year.TEN,
                AverageInfo.Year.ALL
        ).forEach(year -> {
            final AverageInfo averageInfo = new AverageInfo();
            averageInfo.setYear(year);

            Optional.of(year)
                    .flatMap(y -> {
                        // 平均企業価値
                        if (AverageInfo.Year.ALL.equals(year)) {
                            return analysisResultSpecification.allYearAverageCorporateValue(company);
                        } else {
                            return analysisResultSpecification.yearAverageCorporateValue(company, AverageInfo.parseYear(y));
                        }
                    }).ifPresent(ave -> {
                        averageInfo.setAverageCorporateValue(ave);

                        // 標準偏差
                        final Optional<BigDecimal> sd = analysisResultSpecification.standardDeviation(company, ave);
                        if (sd.isPresent()) {
                            averageInfo.setStandardDeviation(sd.get());

                            // 変動係数
                            analysisResultSpecification.coefficientOfVariation(sd.get(), ave)
                                    .ifPresent(averageInfo::setCoefficientOfVariation);
                        }
                    });

            averageInfoList.add(averageInfo);
        });

        corporateValue.setAverageInfoList(averageInfoList);

        // 対象年カウント
        final BigDecimal countYear = analysisResultSpecification.countYear(company);
        corporateValue.setCountYear(countYear);

        return corporateValue;
    }

    /**
     * 係数一括再計算バッチの対象件数を事前確認する。
     *
     * @return 対象件数（analysis_result / valuation の全件数）
     */
    @Override
    public RecalculationPreview previewRecalculation() {
        return new RecalculationPreview(analysisResultSpecification.countAll(), valuationSpecification.countAll());
    }

    /**
     * 業種係数変更に伴い、全期間の企業価値・RIM理論株価を現行係数で一括再計算する。
     *
     * <p>処理順序: (1) 業種係数キャッシュを最新化 (2) analysis_result を全件走査し、値が変わる行のみ更新
     * (3) valuation の割引値・割引率を一括更新 (4) backtest キャッシュを evict する。
     * バッチは冪等（同じ係数で再実行しても同じ値に収束する）ため、途中失敗時は再実行で回復できる。
     *
     * @return 再計算結果
     */
    @Override
    public RecalculationResult recalculate() {
        final long startTime = System.currentTimeMillis();
        log.info(FundanalyzerLogClient.toInteractorLogObject(
                "業種係数変更に伴う一括再計算処理を開始します。",
                Category.ANALYSIS,
                Process.ANALYSIS
        ));

        // 業種係数キャッシュを最新化してから係数を解決する
        industrySpecification.findIndustryList();

        final List<AnalysisResultEntity> targetList = analysisResultSpecification.findAll();
        int updatedCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        for (final AnalysisResultEntity entity : targetList) {
            try {
                final Document document = documentSpecification.findDocument(entity.getDocumentId());
                final FinanceValue financeValue = financialStatementSpecification.getFinanceValue(document);
                final Integer industryId = companySpecification.findCompanyByEdinetCode(document.getEdinetCode())
                        .map(Company::industryId)
                        .orElse(null);
                final AnalysisCoefficient coefficient = industrySpecification.resolveCoefficient(industryId);
                final AnalysisResult recalculated = new AnalysisResult(
                        financeValue,
                        document,
                        coefficient,
                        resolveCorporateValueModel(industryId)
                );

                if (hasChanged(entity, recalculated)) {
                    analysisResultSpecification.updateCorporateValueAndRimValue(
                            entity.getId(),
                            recalculated.getCorporateValue(),
                            recalculated.getRimValue().orElse(null)
                    );
                    updatedCount++;
                } else {
                    skippedCount++;
                }
            } catch (final FundanalyzerNotExistException e) {
                failedCount++;
                log.warn(FundanalyzerLogClient.toInteractorLogObject(
                        MessageFormat.format(
                                "入力値が存在しないため、再計算をスキップしました。\t書類ID:{0}\tID:{1}",
                                entity.getDocumentId(),
                                entity.getId()
                        ),
                        Category.ANALYSIS,
                        Process.ANALYSIS
                ), e);
            } catch (final RuntimeException e) {
                // 異常データ1件でバッチ全体（valuation 追随更新・キャッシュ evict を含む）が
                // 中断しないよう、想定外の実行時例外も該当行のみ失敗として継続する
                failedCount++;
                log.warn(FundanalyzerLogClient.toInteractorLogObject(
                        MessageFormat.format(
                                "再計算中に想定外のエラーが発生したため、該当行をスキップしました。\t書類ID:{0}\tID:{1}",
                                entity.getDocumentId(),
                                entity.getId()
                        ),
                        Category.ANALYSIS,
                        Process.ANALYSIS
                ), e);
            }
        }

        final int valuationUpdatedCount = valuationSpecification.updateDerivedValuesFromAnalysisResult();

        Optional.ofNullable(cacheManager.getCache(BACKTEST_CACHE_NAME)).ifPresent(Cache::clear);

        final RecalculationResult result = new RecalculationResult(
                targetList.size(), updatedCount, skippedCount, failedCount, valuationUpdatedCount);

        log.info(FundanalyzerLogClient.toInteractorLogObject(
                MessageFormat.format(
                        "業種係数変更に伴う一括再計算処理が正常に終了しました。" +
                        "\t対象件数:{0}\t更新件数:{1}\tスキップ件数:{2}\t失敗件数:{3}\tvaluation更新件数:{4}",
                        result.targetCount(),
                        result.updatedCount(),
                        result.skippedCount(),
                        result.failedCount(),
                        result.valuationUpdatedCount()
                ),
                Category.ANALYSIS,
                Process.ANALYSIS,
                System.currentTimeMillis() - startTime
        ));

        return result;
    }

    private AnalysisResult.CorporateValueModel resolveCorporateValueModel(final Integer industryId) {
        return industrySpecification.isNetAssetModel(industryId)
                ? AnalysisResult.CorporateValueModel.NET_ASSET
                : AnalysisResult.CorporateValueModel.STANDARD;
    }

    /**
     * 企業価値または RIM 理論株価に変化があるかどうかを判定する。
     *
     * <p>BigDecimal はスケール違いを同値とみなすため compareTo で比較し、rim_value は null 安全に扱う。
     *
     * @param before 再計算前のエンティティ
     * @param after  再計算後の分析結果
     * @return 変化があるとき true
     */
    static boolean hasChanged(final AnalysisResultEntity before, final AnalysisResult after) {
        return bigDecimalChanged(before.getCorporateValue(), after.getCorporateValue())
               || bigDecimalChanged(before.getRimValue().orElse(null), after.getRimValue().orElse(null));
    }

    static boolean bigDecimalChanged(final BigDecimal before, final BigDecimal after) {
        if (before == null && after == null) {
            return false;
        }
        if (before == null || after == null) {
            return true;
        }
        return before.compareTo(after) != 0;
    }
}
