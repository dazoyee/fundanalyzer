package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.config.AnalysisCoefficient;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.IndustrySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.StockPriceEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.AnalysisResultSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CompanySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.DocumentSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.FinancialStatementSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.InvestmentIndicatorSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.StockSpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.ValuationSpecification;
import github.com.ioridazo.fundanalyzer.domain.usecase.AnalyzeUseCase;
import github.com.ioridazo.fundanalyzer.domain.value.AnalysisResult;
import github.com.ioridazo.fundanalyzer.domain.value.AverageInfo;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.CorporateValue;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.domain.value.FinanceValue;
import github.com.ioridazo.fundanalyzer.domain.value.IndicatorValue;
import github.com.ioridazo.fundanalyzer.domain.value.RecalculationPreview;
import github.com.ioridazo.fundanalyzer.domain.value.RecalculationResult;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.model.IdInputData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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
    private final StockSpecification stockSpecification;
    private final InvestmentIndicatorSpecification investmentIndicatorSpecification;
    private final IndustrySpecification industrySpecification;
    private final CacheManager cacheManager;

    @Value("${app.config.view.document-type-code}")
    List<String> targetTypeCodes;

    public AnalyzeInteractor(
            final CompanySpecification companySpecification,
            final DocumentSpecification documentSpecification,
            final FinancialStatementSpecification financialStatementSpecification,
            final AnalysisResultSpecification analysisResultSpecification,
            final ValuationSpecification valuationSpecification,
            final StockSpecification stockSpecification,
            final InvestmentIndicatorSpecification investmentIndicatorSpecification,
            final IndustrySpecification industrySpecification,
            final CacheManager cacheManager) {
        this.companySpecification = companySpecification;
        this.documentSpecification = documentSpecification;
        this.financialStatementSpecification = financialStatementSpecification;
        this.analysisResultSpecification = analysisResultSpecification;
        this.valuationSpecification = valuationSpecification;
        this.stockSpecification = stockSpecification;
        this.investmentIndicatorSpecification = investmentIndicatorSpecification;
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
            final AnalysisResult analysisResult = new AnalysisResult(financeValue, document, coefficient);

            analysisResultSpecification.insert(document, analysisResult);

            documentSpecification.findLatestDocument(document.getEdinetCode()).ifPresent(ld -> {
                // this document == latest document
                if (Objects.equals(document.getDocumentId(), ld.getDocumentId())) {
                    // indicate（取得済みの財務諸表値を使い回して都度計算する）
                    analysisResultSpecification.findAnalysisResult(document.getDocumentId())
                            .ifPresent(ar -> indicate(ar, financeValue, document));
                }
            });

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
                final AnalysisResult recalculated = new AnalysisResult(financeValue, document, coefficient);

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

    /**
     * 投資指標を算出する
     *
     * @param inputData 企業コード
     */
    @Override
    public void indicate(final CodeInputData inputData) {
        analysisResultSpecification.findLatestAnalysisResult(inputData.getCode()).ifPresent(this::indicate);
    }

    /**
     * 投資指標を算出する
     *
     * <p>財務諸表値・ドキュメントを未取得の呼び出し元向けに解決してから委譲する。
     *
     * @param analysisResult 分析結果
     */
    void indicate(final AnalysisResultEntity analysisResult) {
        if (targetTypeCodes.stream().noneMatch(target -> analysisResult.getDocumentTypeCode().equals(target))) {
            // 120,130 以外は処理対象外
            return;
        }

        final Document document = documentSpecification.findDocument(analysisResult.getDocumentId());
        indicate(analysisResult, financialStatementSpecification.getFinanceValue(document), document);
    }

    /**
     * 投資指標を算出する
     *
     * <p>PER/PBR の入力となる BPS/EPS は永続列ではなく財務諸表値からの都度計算値を用いる。
     *
     * @param analysisResult 分析結果
     * @param financeValue   財務諸表値
     * @param document       ドキュメント
     */
    void indicate(final AnalysisResultEntity analysisResult, final FinanceValue financeValue, final Document document) {
        if (targetTypeCodes.stream().noneMatch(target -> analysisResult.getDocumentTypeCode().equals(target))) {
            // 120,130 以外は処理対象外
            return;
        }

        final AnalysisResult computedResult = AnalysisResult.of(analysisResult, financeValue, document);
        if (computedResult.getBps().isEmpty() && computedResult.getEps().isEmpty()) {
            // 指標に関する値が存在しない場合は対象外
            return;
        }

        final long startTime = System.currentTimeMillis();
        final List<IndicatorValue> indicatorValueList = investmentIndicatorSpecification.findIndicatorValueList(analysisResult.getId());
        final Optional<StockPriceEntity> latestStock = stockSpecification.findLatestStock(analysisResult.getCompanyCode());

        if (latestStock.isPresent()) {
            final LocalDate executedDate = indicatorValueList.stream()
                    // latest indicator
                    .max(Comparator.comparing(IndicatorValue::getTargetDate))
                    .map(IndicatorValue::getTargetDate)
                    // default
                    .orElse(analysisResult.getSubmitDate().minusDays(1));
            final LocalDate latestDate = latestStock.get().getTargetDate();

            // 提出日 <= 株価取得日 && 株価取得日 <= 提出日+1年
            if (
                    (latestDate.isEqual(analysisResult.getSubmitDate())
                     || latestDate.isAfter(analysisResult.getSubmitDate()))
                    &&
                    (latestDate.isEqual(analysisResult.getSubmitDate().plusYears(1))
                     || (latestDate.isBefore(analysisResult.getSubmitDate().plusYears(1))))
            ) {
                // executedDate -> latestDate
                executedDate.plusDays(1).datesUntil(latestDate.plusDays(1)).forEach(date ->
                        // find stock
                        stockSpecification.findStock(analysisResult.getCompanyCode(), date)
                                .ifPresent(spe -> {
                                            // indicate
                                            investmentIndicatorSpecification.insert(analysisResult, computedResult, spe);
                                        }
                                )
                );

                log.trace(FundanalyzerLogClient.toInteractorLogObject(
                        MessageFormat.format(
                                "投資指標を算出しました。\t企業コード:{0}\t処理対象日:{1} -> {2}",
                                analysisResult.getCompanyCode(),
                                executedDate.plusDays(1),
                                latestDate
                        ),
                        analysisResult.getDocumentId(),
                        companySpecification.findCompanyByCode(analysisResult.getCompanyCode()).map(Company::edinetCode).orElse("null"),
                        Category.ANALYSIS,
                        Process.INDICATE,
                        System.currentTimeMillis() - startTime
                ));
            } else {
                log.warn(FundanalyzerLogClient.toInteractorLogObject(
                        MessageFormat.format(
                                "提出日または株価取得日が正しくないため、投資指標を算出しませんでした。" +
                                "\t企業コード:{0}\t提出日:{1}\t株価取得日:{2}",
                                analysisResult.getCompanyCode(),
                                analysisResult.getSubmitDate(),
                                latestDate
                        ),
                        analysisResult.getDocumentId(),
                        companySpecification.findCompanyByCode(analysisResult.getCompanyCode()).map(Company::edinetCode).orElse("null"),
                        Category.ANALYSIS,
                        Process.INDICATE,
                        System.currentTimeMillis() - startTime
                ));
            }
        } else {
            log.warn(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format(
                            "株価が存在しないため、投資指標を算出できませんでした。\t企業コード:{0}",
                            analysisResult.getCompanyCode()
                    ),
                    analysisResult.getDocumentId(),
                    companySpecification.findCompanyByCode(analysisResult.getCompanyCode()).map(Company::edinetCode).orElse("null"),
                    Category.ANALYSIS,
                    Process.INDICATE,
                    System.currentTimeMillis() - startTime
            ));
        }
    }
}
