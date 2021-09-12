package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.domain.domain.dao.view.CorporateViewDao;
import github.com.ioridazo.fundanalyzer.domain.domain.dao.view.EdinetListViewDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.DocumentTypeCode;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.view.CorporateViewBean;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.view.EdinetListViewBean;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.CorporateValue;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
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
import java.util.List;
import java.util.Objects;
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
     * 企業情報ビューを取得する
     *
     * @param inputData 企業コード
     * @return 企業情報ビュー
     */
    public CorporateViewModel findCorporateView(final CodeInputData inputData) {
        return CorporateViewModel.of(corporateViewDao.selectByCode(inputData.getCode()));
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
                .orElseThrow(FundanalyzerNotExistException::new);
    }

    /**
     * 企業情報ビューを取得する
     *
     * @param inputData 提出日
     * @return 企業情報ビューリスト
     */
    public List<CorporateViewModel> findAllCorporateView(final DateInputData inputData) {
        return corporateViewDao.selectBySubmitDate(inputData.getDate()).stream()
                .map(CorporateViewModel::of)
                .collect(Collectors.toList());
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
                .sorted(Comparator
                        .comparing(CorporateViewModel::getSubmitDate).reversed()
                        .thenComparing(CorporateViewModel::getCode))
                .collect(Collectors.toList());
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
                .collect(Collectors.toList());
    }

    /**
     * 企業情報ビューを登録・更新する
     *
     * @param viewModel 企業情報ビュー
     */
    public void upsert(final CorporateViewModel viewModel) {
        if (isPresent(viewModel.getCode())) {
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
     * @param corporateValue 企業価値
     * @return 企業情報ビュー
     */
    public CorporateViewModel generateCorporateView(final Company company, final CorporateValue corporateValue) {
        final Stock stock = stockSpecification.findStock(company);
        final Optional<Document> latestDocument = documentSpecification.latestDocument(company);

        return CorporateViewModel.of(
                company.getCode().map(code -> code.substring(0, 4)).orElseThrow(FundanalyzerNotExistException::new),
                company.getCompanyName(),
                latestDocument.map(Document::getSubmitDate).orElse(null),
                latestDocument.map(Document::getDocumentTypeCode).map(DocumentTypeCode::toValue).orElse(null),
                latestDocument.map(Document::getDocumentTypeCode).stream()
                        .anyMatch(dtc -> List.of(DocumentTypeCode.DTC_120, DocumentTypeCode.DTC_130).contains(dtc)),
                corporateValue.getLatestCorporateValue().orElse(null),
                corporateValue.getAverageCorporateValue().orElse(null),
                corporateValue.getStandardDeviation().orElse(null),
                corporateValue.getCoefficientOfVariation().orElse(null),
                stock.getAverageStockPrice().orElse(null),
                stock.getImportDate().orElse(null),
                stock.getLatestStockPrice().orElse(null),
                calculateDiscountValue(corporateValue, stock).orElse(null),
                calculateDiscountRate(corporateValue, stock).orElse(null),
                corporateValue.getCountYear().orElse(null),
                stock.getLatestForecastStock().orElse(null)
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
        final List<Document> targetList = documentSpecification.targetList(inputData);
        // 処理済書類/未処理書類
        final Pair<List<Document>, List<Document>> scrapedList = documentSpecification.extractScrapedList(targetList);
        // 分析済書類/未分析書類
        final Pair<List<Document>, List<Document>> analyzedList = documentSpecification.extractAnalyzedList(scrapedList.getFirst());
        return EdinetListViewModel.of(
                inputData.getDate(),
                documentList.size(),
                targetList.size(),
                scrapedList.getFirst().size(),
                analyzedList.getFirst().size(),
                analyzedList.getSecond().stream().map(Document::getDocumentId).collect(Collectors.joining(",")),
                scrapedList.getSecond().stream().map(Document::getDocumentId).collect(Collectors.joining(",")),
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
    private Optional<BigDecimal> calculateDiscountValue(final CorporateValue corporateValue, final Stock stock) {
        if (corporateValue.getAverageCorporateValue().isEmpty() || stock.getLatestStockPrice().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(corporateValue.getAverageCorporateValue().orElseThrow()
                .subtract(stock.getLatestStockPrice().orElseThrow())
                .abs(new MathContext(DIGIT_NUMBER_OF_DISCOUNT_VALUE)));
    }

    /**
     * 割安度を算出する
     *
     * @param corporateValue 企業価値
     * @param stock          株価情報
     * @return 割安度
     */
    private Optional<BigDecimal> calculateDiscountRate(final CorporateValue corporateValue, final Stock stock) {
        if (corporateValue.getAverageCorporateValue().isEmpty() || stock.getLatestStockPrice().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(corporateValue.getAverageCorporateValue().orElseThrow()
                .divide(stock.getLatestStockPrice().orElseThrow(), FIFTH_DECIMAL_PLACE, RoundingMode.HALF_UP)
                .multiply(ONE_HUNDRED).setScale(THIRD_DECIMAL_PLACE, RoundingMode.HALF_UP));
    }

    /**
     * 企業情報ビューがデータベースに存在するか
     *
     * @param code 企業コード
     * @return boolean
     */
    private boolean isPresent(final String code) {
        return Objects.nonNull(corporateViewDao.selectByCode(code));
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
