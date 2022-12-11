package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.domain.domain.dao.view.CorporateViewDao;
import github.com.ioridazo.fundanalyzer.domain.domain.dao.view.EdinetListViewDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.DocumentTypeCode;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.view.CorporateViewBean;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.view.EdinetListViewBean;
import github.com.ioridazo.fundanalyzer.domain.value.AnalysisResult;
import github.com.ioridazo.fundanalyzer.domain.value.AverageInfo;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.CorporateValue;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.domain.value.IndicatorValue;
import github.com.ioridazo.fundanalyzer.domain.value.Stock;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.model.DateInputData;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.CorporateViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.edinet.EdinetListViewModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ViewSpecification {

    private static final int DIGIT_NUMBER_OF_DISCOUNT_VALUE = 6;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final int THIRD_DECIMAL_PLACE = 3;
    private static final int FIFTH_DECIMAL_PLACE = 5;

    private final CorporateViewDao corporateViewDao;
    private final EdinetListViewDao edinetListViewDao;
    private final DocumentSpecification documentSpecification;
    private final StockSpecification stockSpecification;

    @Value("${app.config.view.edinet-list.size}")
    int edinetListSize;

    public ViewSpecification(
            final CorporateViewDao corporateViewDao,
            final EdinetListViewDao edinetListViewDao,
            final DocumentSpecification documentSpecification,
            final StockSpecification stockSpecification) {
        this.corporateViewDao = corporateViewDao;
        this.edinetListViewDao = edinetListViewDao;
        this.documentSpecification = documentSpecification;
        this.stockSpecification = stockSpecification;
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
        return corporateViewDao.selectByCode(inputData.getCode()).stream()
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
                .orElseThrow(() -> {
                    throw new FundanalyzerNotExistException("提出日");
                });
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
                .filter(viewModel -> viewModel.getSubmitDate().isAfter(nowLocalDate().minusDays(edinetListSize)))
                .sorted(Comparator.comparing(EdinetListViewModel::getSubmitDate).reversed())
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
            corporateViewDao.insert(CorporateViewBean.of(viewModel, nowLocalDateTime()));
        }
    }

    /**
     * EDINETリストビューを登録・更新する
     *
     * @param viewModel EDINETリストビュー
     */
    public void upsert(final EdinetListViewModel viewModel) {
        if (isPresent(viewModel.getSubmitDate())) {
            edinetListViewDao.update(EdinetListViewBean.of(viewModel, nowLocalDateTime()));
        } else {
            edinetListViewDao.insert(EdinetListViewBean.of(viewModel, nowLocalDateTime()));
        }
    }

    /**
     * 企業情報ビューを生成する
     *
     * @param company        企業情報
     * @param analysisResult 分析結果
     * @param corporateValue 企業価値
     * @param indicatorValue 投資指標
     * @return 企業情報ビュー
     * @throws FundanalyzerNotExistException 値が存在しないとき
     */
    public CorporateViewModel generateCorporateView(
            final Company company,
            final AnalysisResult analysisResult,
            final CorporateValue corporateValue,
            final IndicatorValue indicatorValue) throws FundanalyzerNotExistException {
        final Stock stock = stockSpecification.findStock(company);
        final Optional<Document> latestDocument = documentSpecification.latestDocument(company);

        return CorporateViewModel.of(
                company.getCode().substring(0, 4),
                company.getCompanyName(),
                latestDocument.map(Document::getSubmitDate).orElse(null),
                latestDocument.map(Document::getDocumentTypeCode).map(DocumentTypeCode::toValue)
                        .orElseThrow(() -> new FundanalyzerNotExistException("書類種別コード")),
                latestDocument.map(Document::getDocumentTypeCode).stream()
                        .anyMatch(dtc -> List.of(DocumentTypeCode.DTC_120, DocumentTypeCode.DTC_130).contains(dtc)),
                corporateValue.getLatestCorporateValue().orElse(null),
                corporateValue.getAverageInfoList().stream()
                        .filter(averageInfo -> averageInfo.getYear().equals(AverageInfo.Year.THREE))
                        .findFirst()
                        .flatMap(AverageInfo::getAverageCorporateValue)
                        .orElse(null),
                corporateValue.getAverageInfoList().stream()
                        .filter(averageInfo -> averageInfo.getYear().equals(AverageInfo.Year.THREE))
                        .findFirst()
                        .flatMap(AverageInfo::getStandardDeviation)
                        .orElse(null),
                corporateValue.getAverageInfoList().stream()
                        .filter(averageInfo -> averageInfo.getYear().equals(AverageInfo.Year.THREE))
                        .findFirst()
                        .flatMap(AverageInfo::getCoefficientOfVariation)
                        .orElse(null),
                corporateValue.getAverageInfoList().stream()
                        .filter(averageInfo -> averageInfo.getYear().equals(AverageInfo.Year.FIVE))
                        .findFirst()
                        .flatMap(AverageInfo::getAverageCorporateValue)
                        .orElse(null),
                corporateValue.getAverageInfoList().stream()
                        .filter(averageInfo -> averageInfo.getYear().equals(AverageInfo.Year.FIVE))
                        .findFirst()
                        .flatMap(AverageInfo::getStandardDeviation)
                        .orElse(null),
                corporateValue.getAverageInfoList().stream()
                        .filter(averageInfo -> averageInfo.getYear().equals(AverageInfo.Year.FIVE))
                        .findFirst()
                        .flatMap(AverageInfo::getCoefficientOfVariation)
                        .orElse(null),
                corporateValue.getAverageInfoList().stream()
                        .filter(averageInfo -> averageInfo.getYear().equals(AverageInfo.Year.TEN))
                        .findFirst()
                        .flatMap(AverageInfo::getAverageCorporateValue)
                        .orElse(null),
                corporateValue.getAverageInfoList().stream()
                        .filter(averageInfo -> averageInfo.getYear().equals(AverageInfo.Year.TEN))
                        .findFirst()
                        .flatMap(AverageInfo::getStandardDeviation)
                        .orElse(null),
                corporateValue.getAverageInfoList().stream()
                        .filter(averageInfo -> averageInfo.getYear().equals(AverageInfo.Year.TEN))
                        .findFirst()
                        .flatMap(AverageInfo::getCoefficientOfVariation)
                        .orElse(null),
                corporateValue.getAverageInfoList().stream()
                        .filter(averageInfo -> averageInfo.getYear().equals(AverageInfo.Year.ALL))
                        .findFirst()
                        .flatMap(AverageInfo::getAverageCorporateValue)
                        .orElse(null),
                corporateValue.getAverageInfoList().stream()
                        .filter(averageInfo -> averageInfo.getYear().equals(AverageInfo.Year.ALL))
                        .findFirst()
                        .flatMap(AverageInfo::getStandardDeviation)
                        .orElse(null),
                corporateValue.getAverageInfoList().stream()
                        .filter(averageInfo -> averageInfo.getYear().equals(AverageInfo.Year.ALL))
                        .findFirst()
                        .flatMap(AverageInfo::getCoefficientOfVariation)
                        .orElse(null),
                stock.getAverageStockPrice().orElse(null),
                stock.getImportDate().orElse(null),
                stock.getLatestStockPrice().orElse(null),
                calculateDiscountValue(corporateValue, stock).getOrDefault(AverageInfo.Year.THREE, Optional.empty()).orElse(null),
                calculateDiscountRate(corporateValue, stock).getOrDefault(AverageInfo.Year.THREE, Optional.empty()).orElse(null),
                calculateDiscountValue(corporateValue, stock).getOrDefault(AverageInfo.Year.FIVE, Optional.empty()).orElse(null),
                calculateDiscountRate(corporateValue, stock).getOrDefault(AverageInfo.Year.FIVE, Optional.empty()).orElse(null),
                calculateDiscountValue(corporateValue, stock).getOrDefault(AverageInfo.Year.TEN, Optional.empty()).orElse(null),
                calculateDiscountRate(corporateValue, stock).getOrDefault(AverageInfo.Year.TEN, Optional.empty()).orElse(null),
                calculateDiscountValue(corporateValue, stock).getOrDefault(AverageInfo.Year.ALL, Optional.empty()).orElse(null),
                calculateDiscountRate(corporateValue, stock).getOrDefault(AverageInfo.Year.ALL, Optional.empty()).orElse(null),
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
}
