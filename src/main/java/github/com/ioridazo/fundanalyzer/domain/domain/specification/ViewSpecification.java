package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.domain.domain.dao.view.CorporateViewDao;
import github.com.ioridazo.fundanalyzer.domain.domain.dao.view.EdinetListViewDao;
import github.com.ioridazo.fundanalyzer.domain.domain.dao.view.ValuationViewDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.AnalysisResultEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.DocumentTypeCode;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.StockPriceEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.ValuationEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.view.CorporateViewBean;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.view.EdinetListViewBean;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.view.ValuationViewBean;
import github.com.ioridazo.fundanalyzer.domain.value.AnalysisResult;
import github.com.ioridazo.fundanalyzer.domain.value.AverageInfo;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.CorporateValue;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.domain.value.IndicatorValue;
import github.com.ioridazo.fundanalyzer.domain.value.Stock;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.CorporateViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.edinet.EdinetListViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.valuation.CompanyValuationViewModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seasar.doma.jdbc.UniqueConstraintException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedRuntimeException;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ViewSpecification {

    private static final Logger log = LogManager.getLogger(ViewSpecification.class);

    private static final int DIGIT_NUMBER_OF_DISCOUNT_VALUE = 6;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final int THIRD_DECIMAL_PLACE = 3;
    private static final int FIFTH_DECIMAL_PLACE = 5;

    private final CorporateViewDao corporateViewDao;
    private final EdinetListViewDao edinetListViewDao;
    private final ValuationViewDao valuationViewDao;
    private final CompanySpecification companySpecification;
    private final DocumentSpecification documentSpecification;
    private final AnalysisResultSpecification analysisResultSpecification;
    private final StockSpecification stockSpecification;
    private final ValuationSpecification valuationSpecification;

    @Value("${app.config.view.edinet-list.size}")
    int edinetListSize;

    public ViewSpecification(
            final CorporateViewDao corporateViewDao,
            final EdinetListViewDao edinetListViewDao,
            final ValuationViewDao valuationViewDao,
            final CompanySpecification companySpecification,
            final DocumentSpecification documentSpecification,
            final AnalysisResultSpecification analysisResultSpecification,
            final StockSpecification stockSpecification,
            final ValuationSpecification valuationSpecification) {
        this.corporateViewDao = corporateViewDao;
        this.edinetListViewDao = edinetListViewDao;
        this.valuationViewDao = valuationViewDao;
        this.companySpecification = companySpecification;
        this.documentSpecification = documentSpecification;
        this.analysisResultSpecification = analysisResultSpecification;
        this.stockSpecification = stockSpecification;
        this.valuationSpecification = valuationSpecification;
    }

    LocalDate nowLocalDate() {
        return LocalDate.now();
    }

    LocalDateTime nowLocalDateTime() {
        return LocalDateTime.now();
    }

    /**
     * 最新の企業情報ビューを取得する
     *
     * @param inputData 企業コード
     * @return 企業情報ビュー
     */
    public CorporateViewModel findLatestCorporateView(final CodeInputData inputData) {
        // corporate_view.code は CHAR(4)。URL に 5 桁の company.code が来た場合も 4 桁に正規化する
        return corporateViewDao.selectByCode(inputData.getCode4()).stream()
                .filter(viewBean -> viewBean.getSubmitDate().isPresent())
                .max(Comparator.comparing(viewBean -> viewBean.getSubmitDate().get()))
                .map(CorporateViewModel::of)
                .orElseThrow();
    }

    /**
     * EDINETリストビューを取得する
     *
     * @param inputData 提出日
     * @return EDINETリストビュー
     * @throws FundanalyzerNotExistException EDINETリストビューが存在しないとき
     */
    public EdinetListViewModel findEdinetListView(final DateInputData inputData) throws FundanalyzerNotExistException {
        return edinetListViewDao.selectBySubmitDate(inputData.getDate())
                .map(EdinetListViewModel::of)
                .orElseThrow(() -> new FundanalyzerNotExistException("提出日"));
    }

    /**
     * すべての企業情報ビューを取得する
     *
     * @return 企業情報ビューリスト
     */
    public List<CorporateViewModel> findAllCorporateView() {
        return corporateViewDao.selectAll().stream()
                // 提出日が存在したら表示する
                .filter(corporateViewBean -> corporateViewBean.getSubmitDate().isPresent())
                .map(CorporateViewModel::of)
                .toList();
    }

    /**
     * すべてのEDINETリストビューを取得する
     *
     * @return EDINETリストビュー
     */
    public List<EdinetListViewModel> findAllEdinetListView() {
        return edinetListViewDao.selectAll().stream()
                .map(EdinetListViewModel::of)
                .filter(viewModel -> viewModel.submitDate().isAfter(nowLocalDate().minusDays(edinetListSize)))
                .sorted(Comparator.comparing(EdinetListViewModel::submitDate).reversed())
                .toList();
    }

    /**
     * すべての会社評価ビューを取得する
     *
     * @return 会社評価ビュー
     */
    public List<CompanyValuationViewModel> findAllCompanyValuationView() {
        return valuationViewDao.selectAll().stream()
                .map(CompanyValuationViewModel::of)
                .toList();
    }

    /**
     * 企業情報ビューを登録・更新する
     *
     * @param viewModel 企業情報ビュー
     */
    public void upsert(final CorporateViewModel viewModel) {
        if (isPresent(viewModel.getCode(), viewModel.getLatestDocumentTypeCode())) {
            corporateViewDao.update(CorporateViewBean.of(viewModel, nowLocalDateTime()));
        } else {
            try {
                corporateViewDao.insert(CorporateViewBean.of(viewModel, nowLocalDateTime()));
            } catch (final NestedRuntimeException e) {
                handleDaoError(
                        e,
                        MessageFormat.format(
                                "一意制約違反のため、データベースへの登録をスキップします。" +
                                "\tテーブル名:{0}\t会社コード:{1}\t書類種別コード:{2}\t提出日:{3}",
                                "corporate_view",
                                viewModel.getCode(),
                                viewModel.getLatestDocumentTypeCode(),
                                viewModel.getSubmitDate()
                        )
                );
            }
        }
    }

    /**
     * EDINETリストビューを登録・更新する
     *
     * @param viewModel EDINETリストビュー
     */
    public void upsert(final EdinetListViewModel viewModel) {
        if (isPresent(viewModel.submitDate())) {
            edinetListViewDao.update(EdinetListViewBean.of(viewModel, nowLocalDateTime()));
        } else {
            try {
                edinetListViewDao.insert(EdinetListViewBean.of(viewModel, nowLocalDateTime()));
            } catch (final NestedRuntimeException e) {
                handleDaoError(
                        e,
                        MessageFormat.format(
                                "一意制約違反のため、データベースへの登録をスキップします。" +
                                "\tテーブル名:{0}\t提出日:{1}",
                                "edinet_list_view",
                                viewModel.submitDate()
                        )
                );
            }
        }
    }

    /**
     * 企業評価ビューを登録・更新する
     *
     * @param viewModel 企業評価ビュー
     */
    public void upsert(final CompanyValuationViewModel viewModel) {
        if (isPresent(viewModel.code())) {
            valuationViewDao.update(ValuationViewBean.of(viewModel, nowLocalDateTime()));
        } else {
            try {
                valuationViewDao.insert(ValuationViewBean.of(viewModel, nowLocalDateTime()));
            } catch (final NestedRuntimeException e) {
                handleDaoError(
                        e,
                        MessageFormat.format(
                                "一意制約違反のため、データベースへの登録をスキップします。\tテーブル名:{0}\t会社コード:{1}",
                                "valuation_view",
                                viewModel.code()
                        )
                );
            }
        }
    }

    /**
     * 企業情報ビューを生成する
     *
     * @param company        企業情報
     * @param document       ドキュメント
     * @param analysisResult 分析結果
     * @param corporateValue 企業価値
     * @param indicatorValue 投資指標
     * @return 企業情報ビュー
     * @throws FundanalyzerNotExistException 値が存在しないとき
     */
    public CorporateViewModel generateCorporateView(
            final Company company,
            final Document document,
            final AnalysisResult analysisResult,
            final CorporateValue corporateValue,
            final IndicatorValue indicatorValue) throws FundanalyzerNotExistException {
        final Stock stock = stockSpecification.findStock(company);
        final Map<AverageInfo.Year, Optional<BigDecimal>> discountValues = calculateDiscountValue(corporateValue, stock);
        final Map<AverageInfo.Year, Optional<BigDecimal>> discountRates = calculateDiscountRate(corporateValue, stock);

        return CorporateViewModel.of(
                company.code().substring(0, 4),
                company.companyName(),
                document.getSubmitDate(),
                document.getDocumentTypeCode().toValue(),
                isAnnualReport(document),
                corporateValue.getLatestCorporateValue().orElse(null),
                averageCorporateValueOf(corporateValue, AverageInfo.Year.THREE),
                standardDeviationOf(corporateValue, AverageInfo.Year.THREE),
                coefficientOfVariationOf(corporateValue, AverageInfo.Year.THREE),
                averageCorporateValueOf(corporateValue, AverageInfo.Year.FIVE),
                standardDeviationOf(corporateValue, AverageInfo.Year.FIVE),
                coefficientOfVariationOf(corporateValue, AverageInfo.Year.FIVE),
                averageCorporateValueOf(corporateValue, AverageInfo.Year.TEN),
                standardDeviationOf(corporateValue, AverageInfo.Year.TEN),
                coefficientOfVariationOf(corporateValue, AverageInfo.Year.TEN),
                averageCorporateValueOf(corporateValue, AverageInfo.Year.ALL),
                standardDeviationOf(corporateValue, AverageInfo.Year.ALL),
                coefficientOfVariationOf(corporateValue, AverageInfo.Year.ALL),
                stock.getAverageStockPrice().orElse(null),
                stock.getImportDate().orElse(null),
                stock.getLatestStockPrice().orElse(null),
                discountValueOf(discountValues, AverageInfo.Year.THREE),
                discountValueOf(discountRates, AverageInfo.Year.THREE),
                discountValueOf(discountValues, AverageInfo.Year.FIVE),
                discountValueOf(discountRates, AverageInfo.Year.FIVE),
                discountValueOf(discountValues, AverageInfo.Year.TEN),
                discountValueOf(discountRates, AverageInfo.Year.TEN),
                discountValueOf(discountValues, AverageInfo.Year.ALL),
                discountValueOf(discountRates, AverageInfo.Year.ALL),
                corporateValue.getCountYear().orElse(null),
                stock.getLatestForecastStock().orElse(null),
                indicatorValue.getPriceCorporateValueRatio(),
                indicatorValue.getPer().orElse(null),
                indicatorValue.getPbr().orElse(null),
                analysisResult.getBps().orElse(null),
                analysisResult.getEps().orElse(null),
                analysisResult.getRoe().orElse(null),
                analysisResult.getRoa().orElse(null),
                indicatorValue.getGrahamIndex().orElse(null)
        );
    }

    /**
     * ドキュメント種別が有価証券報告書系（120/130）か判定する。
     *
     * @param document ドキュメント
     * @return DTC_120 または DTC_130 のとき true
     */
    private boolean isAnnualReport(final Document document) {
        return isAnnualReport(document.getDocumentTypeCode().toValue());
    }

    public static boolean isAnnualReport(final String documentTypeCode) {
        return Stream.of(DocumentTypeCode.DTC_120, DocumentTypeCode.DTC_130)
                .map(DocumentTypeCode::toValue)
                .anyMatch(dtc -> dtc.equals(documentTypeCode));
    }

    /**
     * 指定年区分の AverageInfo を取得する
     *
     * @param corporateValue 企業価値
     * @param year           平均値の対象年区分
     * @return 該当する AverageInfo（存在しなければ Optional.empty）
     */
    private Optional<AverageInfo> findAverageInfo(final CorporateValue corporateValue, final AverageInfo.Year year) {
        return corporateValue.getAverageInfoList().stream()
                .filter(averageInfo -> averageInfo.getYear().equals(year))
                .findFirst();
    }

    /**
     * 指定年区分の平均企業価値を取得する
     *
     * @param corporateValue 企業価値
     * @param year           平均値の対象年区分
     * @return 平均企業価値（存在しなければ null）
     */
    private BigDecimal averageCorporateValueOf(final CorporateValue corporateValue, final AverageInfo.Year year) {
        return findAverageInfo(corporateValue, year).flatMap(AverageInfo::getAverageCorporateValue).orElse(null);
    }

    /**
     * 指定年区分の標準偏差を取得する
     *
     * @param corporateValue 企業価値
     * @param year           平均値の対象年区分
     * @return 標準偏差（存在しなければ null）
     */
    private BigDecimal standardDeviationOf(final CorporateValue corporateValue, final AverageInfo.Year year) {
        return findAverageInfo(corporateValue, year).flatMap(AverageInfo::getStandardDeviation).orElse(null);
    }

    /**
     * 指定年区分の変動係数を取得する
     *
     * @param corporateValue 企業価値
     * @param year           平均値の対象年区分
     * @return 変動係数（存在しなければ null）
     */
    private BigDecimal coefficientOfVariationOf(final CorporateValue corporateValue, final AverageInfo.Year year) {
        return findAverageInfo(corporateValue, year).flatMap(AverageInfo::getCoefficientOfVariation).orElse(null);
    }

    /**
     * 割安値・割安度マップから指定年区分の値を取得する
     *
     * @param valueMap 算出済の割安値または割安度マップ
     * @param year     平均値の対象年区分
     * @return 値（存在しなければ null）
     */
    private BigDecimal discountValueOf(
            final Map<AverageInfo.Year, Optional<BigDecimal>> valueMap, final AverageInfo.Year year) {
        return valueMap.getOrDefault(year, Optional.empty()).orElse(null);
    }

    /**
     * EDINETリストビューを生成する
     *
     * @param inputData 提出日
     * @return EDINETリストビュー
     */
    public EdinetListViewModel generateEdinetListView(final DateInputData inputData) {
        // 総書類
        final List<Document> documentList = documentSpecification.documentList(inputData);
        // 処理対象書類
        final List<Document> targetList = documentSpecification.inquiryTargetDocuments(inputData);
        // 処理済書類/未処理書類
        final Pair<List<Document>, List<Document>> scrapedList = documentSpecification.extractScrapedList(targetList);
        // 分析済書類/未分析書類
        final Pair<List<Document>, List<Document>> analyzedList = documentSpecification.extractAnalyzedList(scrapedList.getFirst());

        final String notAnalyzedId = analyzedList.getSecond().stream()
                .map(Document::getDocumentId)
                .collect(Collectors.joining(","));
        final String cantScrapedId = scrapedList.getSecond().stream()
                .map(Document::getDocumentId)
                .collect(Collectors.joining(","));

        return EdinetListViewModel.of(
                inputData.getDate(),
                documentList.size(),
                targetList.size(),
                scrapedList.getFirst().size(),
                analyzedList.getFirst().size(),
                notAnalyzedId.length() > 998 ? notAnalyzedId.substring(0, 998) : notAnalyzedId,
                cantScrapedId.length() > 998 ? cantScrapedId.substring(0, 998) : cantScrapedId,
                scrapedList.getSecond().size()
        );
    }

    /**
     * 株価評価ビューを一括生成する（N+1解消版）。
     * 企業コードに対するすべての評価エンティティを受け取り、DB呼び出しを最小化する。
     *
     * @param allEntities 株価評価エンティティリスト（重複排除なし）
     * @return 株価評価ビューリスト
     */
    public List<CompanyValuationViewModel> generateCompanyValuationViewsBatch(
            final List<ValuationEntity> allEntities) {
        if (allEntities.isEmpty()) {
            return List.of();
        }
        final String companyCode = allEntities.get(0).getCompanyCode();

        // 企業情報を1回取得
        final Optional<Company> company = companySpecification.findCompanyByCode(companyCode);

        // 株価一覧を1回取得 → 配当利回り算出
        final BigDecimal dividendYield = computeDividendYield(
                companyCode, stockSpecification.findEntityList(companyCode));

        // 提出日ごとの最小 daySinceSubmitDate エンティティのマップ構築
        final Map<LocalDate, ValuationEntity> submitDateEntityMap = allEntities.stream()
                .collect(Collectors.toMap(
                        ValuationEntity::getSubmitDate,
                        e -> e,
                        (a, b) -> a.getDaySinceSubmitDate() <= b.getDaySinceSubmitDate() ? a : b
                ));

        // 対象日ごとに最新提出日エンティティを選択（findValuationと同ロジック）
        final List<ValuationEntity> deduplicated = allEntities.stream()
                .collect(Collectors.toMap(
                        ValuationEntity::getTargetDate,
                        e -> e,
                        (a, b) -> a.getSubmitDate().compareTo(b.getSubmitDate()) >= 0 ? a : b
                ))
                .values().stream()
                .toList();

        // 分析結果キャッシュ（ユニークIDのみフェッチ）
        final Map<Integer, Optional<AnalysisResultEntity>> analysisResultCache = new HashMap<>();

        return deduplicated.stream()
                .map(entity -> {
                    final Optional<AnalysisResultEntity> analysisResult =
                            analysisResultCache.computeIfAbsent(
                                    entity.getAnalysisResultId(),
                                    id -> analysisResultSpecification.findAnalysisResult(id));
                    final ValuationEntity submitDateEntity =
                            submitDateEntityMap.get(entity.getSubmitDate());
                    return new CompanyValuationViewModel(
                            entity.getCompanyCode().substring(0, 4),
                            company.map(Company::companyName).orElseThrow(),
                            entity.getTargetDate(),
                            entity.getStockPrice(),
                            entity.getGrahamIndex().orElse(null),
                            entity.getDiscountValue(),
                            entity.getDiscountRate(),
                            entity.getSubmitDate(),
                            submitDateEntity.getStockPrice(),
                            entity.getDaySinceSubmitDate(),
                            entity.getDifferenceFromSubmitDate(),
                            entity.getSubmitDateRatio(),
                            // 提出日のグレアム指数: 同一提出日で最も提出日に近い評価行（submitDateEntity）自身の
                            // グレアム指数を用いる（都度計算値は評価時に valuation.graham_index として保存済み）
                            submitDateEntity.getGrahamIndex().orElse(null),
                            analysisResult.map(AnalysisResultEntity::getCorporateValue).orElseThrow(),
                            dividendYield
                    );
                })
                .toList();
    }

    private BigDecimal computeDividendYield(
            final String companyCode, final List<StockPriceEntity> entityList) {
        return entityList.stream()
                .filter(stockPriceEntity -> stockPriceEntity.getDividendYield().isPresent())
                .max(Comparator.comparing(StockPriceEntity::getTargetDate))
                .flatMap(StockPriceEntity::getDividendYield)
                .filter(dividendYield -> !"N/A".equals(dividendYield))
                .map(v -> {
                    try {
                        return new BigDecimal(v
                                .replace("%", "").replace("\u301c", "")
                                .replace(" ", "").replace("\u3000", "")
                        );
                    } catch (final NumberFormatException e) {
                        log.warn(FundanalyzerLogClient.toSpecificationLogObject(
                                MessageFormat.format(
                                        "\u4e88\u60f3\u914d\u5f53\u5229\u56de\u308a\u3092\u6570\u5024\u306b\u5909\u63db\u3067\u304d\u307e\u305b\u3093\u3067\u3057\u305f\u3002\u5bfe\u8c61\u5024:{0}", v
                                ),
                                companySpecification.findCompanyByCode(companyCode).map(Company::edinetCode).orElse("null"),
                                Category.STOCK,
                                Process.EVALUATE
                        ), e.getCause());
                        return null;
                    }
                })
                .orElse(null);
    }

    /**
     * 株価評価ビューを生成する
     *
     * @param entity 株価評価
     * @return 株価評価ビュー
     */
    public CompanyValuationViewModel generateCompanyValuationView(final ValuationEntity entity) {
        final Optional<Company> company = companySpecification.findCompanyByCode(entity.getCompanyCode());
        // 提出日の評価行（daySinceSubmitDate 最小）を1回取得し、提出日株価・提出日のグレアム指数の両方に使い回す
        final Optional<ValuationEntity> valuationOfSubmitDate =
                valuationSpecification.findValuationOfSubmitDate(entity.getCompanyCode(), entity.getSubmitDate());
        final Optional<AnalysisResultEntity> analysisResult = analysisResultSpecification.findAnalysisResult(entity.getAnalysisResultId());

        return new CompanyValuationViewModel(
                entity.getCompanyCode().substring(0, 4),
                company.map(Company::companyName).orElseThrow(),
                entity.getTargetDate(),
                entity.getStockPrice(),
                entity.getGrahamIndex().orElse(null),
                entity.getDiscountValue(),
                entity.getDiscountRate(),
                entity.getSubmitDate(),
                valuationOfSubmitDate.map(ValuationEntity::getStockPrice).orElseThrow(),
                entity.getDaySinceSubmitDate(),
                entity.getDifferenceFromSubmitDate(),
                entity.getSubmitDateRatio(),
                valuationOfSubmitDate.flatMap(ValuationEntity::getGrahamIndex).orElse(null),
                analysisResult.map(AnalysisResultEntity::getCorporateValue).orElseThrow(),
                stockSpecification.findEntityList(entity.getCompanyCode()).stream()
                        .filter(stockPriceEntity -> stockPriceEntity.getDividendYield().isPresent())
                        .max(Comparator.comparing(StockPriceEntity::getTargetDate))
                        .flatMap(StockPriceEntity::getDividendYield)
                        .filter(dividendYield -> !"N/A".equals(dividendYield))
                        .map(v -> {
                            try {
                                return new BigDecimal(v
                                        .replace("%", "").replace("％", "")
                                        .replace(" ", "").replace("　", "")
                                );
                            } catch (final NumberFormatException e) {
                                log.warn(FundanalyzerLogClient.toSpecificationLogObject(
                                        MessageFormat.format(
                                                "予想配当利回りを数値に変換できませんでした。\t値:{0}", v
                                        ),
                                        companySpecification.findCompanyByCode(entity.getCompanyCode()).map(Company::edinetCode).orElse("null"),
                                        Category.STOCK,
                                        Process.EVALUATE
                                ), e.getCause());
                                return null;
                            }
                        })
                        .orElse(null)
        );
    }

    /**
     * 割安値を算出する
     *
     * @param corporateValue 企業価値
     * @param stock          株価情報
     * @return 割安値
     */
    private Map<AverageInfo.Year, Optional<BigDecimal>> calculateDiscountValue(
            final CorporateValue corporateValue, final Stock stock) {
        final Map<AverageInfo.Year, Optional<BigDecimal>> discountValue = new EnumMap<>(AverageInfo.Year.class);

        corporateValue.getAverageInfoList().forEach(averageInfo -> {
            if (averageInfo.getAverageCorporateValue().isEmpty() || stock.getLatestStockPrice().isEmpty()) {
                // empty
                discountValue.put(averageInfo.getYear(), Optional.empty());
            } else {
                // present
                discountValue.put(
                        averageInfo.getYear(),
                        averageInfo.getAverageCorporateValue()
                                .map(ave -> ave
                                        .subtract(stock.getLatestStockPrice().orElseThrow())
                                        .abs(new MathContext(DIGIT_NUMBER_OF_DISCOUNT_VALUE)))
                );
            }
        });

        return discountValue;
    }

    /**
     * 割安度を算出する
     *
     * @param corporateValue 企業価値
     * @param stock          株価情報
     * @return 割安度
     */
    private Map<AverageInfo.Year, Optional<BigDecimal>> calculateDiscountRate(
            final CorporateValue corporateValue, final Stock stock) {
        final Map<AverageInfo.Year, Optional<BigDecimal>> discountValue = new EnumMap<>(AverageInfo.Year.class);

        corporateValue.getAverageInfoList().forEach(averageInfo -> {
            if (averageInfo.getAverageCorporateValue().isEmpty() || stock.getLatestStockPrice().isEmpty()) {
                // empty
                discountValue.put(averageInfo.getYear(), Optional.empty());
            } else {
                // present
                discountValue.put(
                        averageInfo.getYear(),
                        averageInfo.getAverageCorporateValue()
                                .map(ave -> ave
                                        .divide(stock.getLatestStockPrice().orElseThrow(), FIFTH_DECIMAL_PLACE, RoundingMode.HALF_UP)
                                        .multiply(ONE_HUNDRED).setScale(THIRD_DECIMAL_PLACE, RoundingMode.HALF_UP))
                );
            }
        });

        return discountValue;
    }

    /**
     * 企業情報ビューがデータベースに存在するか
     *
     * @param code             企業コード
     * @param documentTypeCode 書類種別コード
     * @return boolean
     */
    private boolean isPresent(final String code, final String documentTypeCode) {
        return corporateViewDao.selectByCodeAndType(code, documentTypeCode).isPresent();
    }

    /**
     * EDINETリストビューがデータベースに存在するか
     *
     * @param submitDate 提出日
     * @return boolean
     */
    private boolean isPresent(final LocalDate submitDate) {
        return edinetListViewDao.selectBySubmitDate(submitDate).isPresent();
    }

    /**
     * 企業評価ビューがデータベースに存在するか
     *
     * @param code 企業コード
     * @return boolean
     */
    private boolean isPresent(final String code) {
        return valuationViewDao.selectByCode(code).isPresent();
    }

    private void handleDaoError(final NestedRuntimeException e, final String message) {
        if (e.contains(UniqueConstraintException.class)) {
            log.debug(FundanalyzerLogClient.toSpecificationLogObject(
                    message,
                    Category.VIEW,
                    Process.REGISTER
            ));
        } else {
            throw new FundanalyzerRuntimeException("想定外のエラーが発生しました。", e);
        }
    }
}
