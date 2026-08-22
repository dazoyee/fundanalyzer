package github.com.ioridazo.fundanalyzer.web.presenter;

import com.fasterxml.jackson.databind.ObjectMapper;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.SystemEventEntity;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.SystemEventType;
import github.com.ioridazo.fundanalyzer.domain.service.AnalysisService;
import github.com.ioridazo.fundanalyzer.domain.service.ViewService;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.ViewFilterSettingEntity;
import github.com.ioridazo.fundanalyzer.domain.usecase.SystemEventUseCase;
import github.com.ioridazo.fundanalyzer.domain.usecase.ViewFilterSettingUseCase;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerBadDataException;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.CompanyTablePage;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.CompanyTableQuery;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.CorporateViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.index.SystemEventSummaryViewModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("IndexPresenterのテスト")
class IndexPresenterTest {

    private ViewService viewService;
    private AnalysisService analysisService;
    private SystemEventUseCase systemEventUseCase;
    private ViewFilterSettingUseCase viewFilterSettingUseCase;
    private ObjectMapper objectMapper;
    private IndexPresenter presenter;

    @BeforeEach
    void setUp() {
        this.viewService = mock(ViewService.class);
        this.analysisService = mock(AnalysisService.class);
        this.systemEventUseCase = mock(SystemEventUseCase.class);
        this.viewFilterSettingUseCase = mock(ViewFilterSettingUseCase.class);
        this.objectMapper = mock(ObjectMapper.class);
        this.presenter = new IndexPresenter(viewService, analysisService, systemEventUseCase, viewFilterSettingUseCase, objectMapper);
        ReflectionTestUtils.setField(presenter, "targetTypeCodes", List.of("120", "130", "140", "150", "160", "170"));
        ReflectionTestUtils.setField(presenter, "systemEventDays", 7);
        ReflectionTestUtils.setField(presenter, "systemEventMaxCount", 100);
        when(systemEventUseCase.findRecent(7, 100)).thenReturn(List.of());
        when(viewFilterSettingUseCase.getSetting()).thenReturn(new ViewFilterSettingEntity(
                1,
                BigDecimal.valueOf(120),
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(0.5),
                BigDecimal.valueOf(100),
                300,
                LocalDateTime.parse("2026-07-17T00:00:00")
        ));
    }


    @Nested
    @DisplayName("corporateViewV3 メソッド")
    class CorporateViewV3 {

        @Test
        @DisplayName("デフォルトパラメータの場合 → index-v2 view 名を返し table 属性が設定される")
        void defaultParams_returnsIndexV2() {
            final Model model = mock(Model.class);
            final CompanyTablePage page = new CompanyTablePage(List.of(), 0, 0L, 0, 25, Sort.by("code"));
            when(viewService.findCompanyTable(any(CompanyTableQuery.class))).thenReturn(page);

            final String result = presenter.corporateViewV3(null, null, 0, 25, "code,asc", model);

            assertEquals("index-v2", result);
            verify(model).addAttribute("target", (String) null);
            verify(model).addAttribute("keyword", (String) null);
            verify(model).addAttribute("table", page);
            verify(model).addAttribute("systemEventSummary", SystemEventSummaryViewModel.of(List.of()));
            verify(model).addAttribute("sortParam", "code,asc");
        }

        @Test
        @DisplayName("ViewService.findCompanyTable に target / keyword / pageable が渡される")
        void parametersPassedToService() {
            final Model model = mock(Model.class);
            final CompanyTablePage page = new CompanyTablePage(List.of(), 0, 0L, 0, 25, Sort.by("name"));
            when(viewService.findCompanyTable(any(CompanyTableQuery.class))).thenReturn(page);

            presenter.corporateViewV3("quart", "abc", 2, 50, "name,desc", model);

            final ArgumentCaptor<CompanyTableQuery> captor = ArgumentCaptor.forClass(CompanyTableQuery.class);
            verify(viewService).findCompanyTable(captor.capture());
            final CompanyTableQuery query = captor.getValue();
            assertEquals("quart", query.target());
            assertEquals("abc", query.keyword());
            assertEquals(2, query.pageable().getPageNumber());
            assertEquals(50, query.pageable().getPageSize());
            assertEquals(Sort.by(Sort.Direction.DESC, "name"), query.pageable().getSort());
        }

        @Test
        @DisplayName("フルページ表示では systemEventSummary をモデルに含める")
        void fullPage_includesSystemEventSummary() {
            final ExtendedModelMap model = new ExtendedModelMap();
            final CompanyTablePage page = new CompanyTablePage(List.of(), 0, 0L, 0, 25, Sort.by("code"));
            final SystemEventEntity entity = SystemEventEntity.of(
                    SystemEventType.ERROR,
                    "AnalysisScheduler",
                    "想定外のエラーが発生しました。 test",
                    LocalDateTime.parse("2026-07-20T12:00:00"));
            when(viewService.findCompanyTable(any(CompanyTableQuery.class))).thenReturn(page);
            when(systemEventUseCase.findRecent(7, 100)).thenReturn(List.of(entity));

            presenter.corporateViewV3(null, null, 0, 25, "code,asc", model);

            assertEquals(SystemEventSummaryViewModel.of(List.of(entity)), model.getAttribute("systemEventSummary"));
        }

        @Test
        @DisplayName("テーブル fragment 取得では systemEventSummary を再取得しない")
        void tableFragment_doesNotFetchSystemEvents() {
            final Model model = mock(Model.class);
            final CompanyTablePage page = new CompanyTablePage(List.of(), 0, 0L, 0, 25, Sort.by("code"));
            when(viewService.findCompanyTable(any(CompanyTableQuery.class))).thenReturn(page);

            presenter.corporateViewV3Table(null, null, 0, 25, "code,asc", model);

            verify(systemEventUseCase, never()).findRecent(7, 100);
        }

        @Test
        @DisplayName("page が負数の場合 → 0 にクランプされる")
        void negativePage_clampedToZero() {
            final Model model = mock(Model.class);
            final CompanyTablePage page = new CompanyTablePage(List.of(), 0, 0L, 0, 25, Sort.by("code"));
            when(viewService.findCompanyTable(any(CompanyTableQuery.class))).thenReturn(page);

            presenter.corporateViewV3(null, null, -5, 25, "code,asc", model);

            final ArgumentCaptor<CompanyTableQuery> captor = ArgumentCaptor.forClass(CompanyTableQuery.class);
            verify(viewService).findCompanyTable(captor.capture());
            assertEquals(0, captor.getValue().pageable().getPageNumber());
        }

        @Test
        @DisplayName("size が 100 を超える場合 → 100 にクランプされる")
        void sizeOver100_clampedTo100() {
            final Model model = mock(Model.class);
            final CompanyTablePage page = new CompanyTablePage(List.of(), 0, 0L, 0, 100, Sort.by("code"));
            when(viewService.findCompanyTable(any(CompanyTableQuery.class))).thenReturn(page);

            presenter.corporateViewV3(null, null, 0, 1000, "code,asc", model);

            final ArgumentCaptor<CompanyTableQuery> captor = ArgumentCaptor.forClass(CompanyTableQuery.class);
            verify(viewService).findCompanyTable(captor.capture());
            assertEquals(100, captor.getValue().pageable().getPageSize());
        }

        @Test
        @DisplayName("size が 0 以下の場合 → 1 にクランプされる")
        void sizeZeroOrNegative_clampedToOne() {
            final Model model = mock(Model.class);
            final CompanyTablePage page = new CompanyTablePage(List.of(), 0, 0L, 0, 1, Sort.by("code"));
            when(viewService.findCompanyTable(any(CompanyTableQuery.class))).thenReturn(page);

            presenter.corporateViewV3(null, null, 0, 0, "code,asc", model);

            final ArgumentCaptor<CompanyTableQuery> captor = ArgumentCaptor.forClass(CompanyTableQuery.class);
            verify(viewService).findCompanyTable(captor.capture());
            assertEquals(1, captor.getValue().pageable().getPageSize());
        }

        @Test
        @DisplayName("sort のフィールドがホワイトリスト外の場合 → submitDate,desc + code,desc 複合ソートにフォールバック")
        void sortFieldNotAllowed_fallsBackToDefaultCompoundSort() {
            final Model model = mock(Model.class);
            final CompanyTablePage page = new CompanyTablePage(List.of(), 0, 0L, 0, 25, Sort.by("submitDate"));
            when(viewService.findCompanyTable(any(CompanyTableQuery.class))).thenReturn(page);

            presenter.corporateViewV3(null, null, 0, 25, "secret,asc", model);

            final ArgumentCaptor<CompanyTableQuery> captor = ArgumentCaptor.forClass(CompanyTableQuery.class);
            verify(viewService).findCompanyTable(captor.capture());
            assertEquals(
                    Sort.by(Sort.Direction.DESC, "submitDate").and(Sort.by(Sort.Direction.DESC, "code")),
                    captor.getValue().pageable().getSort());
        }

        @Test
        @DisplayName("sort が null や空の場合 → submitDate,desc + code,desc 複合ソートにフォールバック")
        void sortNullOrBlank_fallsBackToDefaultCompoundSort() {
            final Model model = mock(Model.class);
            final CompanyTablePage page = new CompanyTablePage(List.of(), 0, 0L, 0, 25, Sort.by("submitDate"));
            when(viewService.findCompanyTable(any(CompanyTableQuery.class))).thenReturn(page);

            presenter.corporateViewV3(null, null, 0, 25, "", model);

            final ArgumentCaptor<CompanyTableQuery> captor = ArgumentCaptor.forClass(CompanyTableQuery.class);
            verify(viewService).findCompanyTable(captor.capture());
            assertEquals(
                    Sort.by(Sort.Direction.DESC, "submitDate").and(Sort.by(Sort.Direction.DESC, "code")),
                    captor.getValue().pageable().getSort());
        }

        @Test
        @DisplayName("sort の direction が指定されていない場合 → asc になる")
        void sortDirectionMissing_defaultsToAsc() {
            final Model model = mock(Model.class);
            final CompanyTablePage page = new CompanyTablePage(List.of(), 0, 0L, 0, 25, Sort.by("name"));
            when(viewService.findCompanyTable(any(CompanyTableQuery.class))).thenReturn(page);

            presenter.corporateViewV3(null, null, 0, 25, "name", model);

            final ArgumentCaptor<CompanyTableQuery> captor = ArgumentCaptor.forClass(CompanyTableQuery.class);
            verify(viewService).findCompanyTable(captor.capture());
            assertEquals(Sort.by(Sort.Direction.ASC, "name"), captor.getValue().pageable().getSort());
        }

        @Test
        @DisplayName("sort=submitDate,desc を明示指定 → submitDate,desc + code,desc 複合ソート")
        void submitDateDesc_addsCodeDescTieBreak() {
            final Model model = mock(Model.class);
            final CompanyTablePage page = new CompanyTablePage(List.of(), 0, 0L, 0, 25, Sort.by("submitDate"));
            when(viewService.findCompanyTable(any(CompanyTableQuery.class))).thenReturn(page);

            presenter.corporateViewV3(null, null, 0, 25, "submitDate,desc", model);

            final ArgumentCaptor<CompanyTableQuery> captor = ArgumentCaptor.forClass(CompanyTableQuery.class);
            verify(viewService).findCompanyTable(captor.capture());
            assertEquals(
                    Sort.by(Sort.Direction.DESC, "submitDate").and(Sort.by(Sort.Direction.DESC, "code")),
                    captor.getValue().pageable().getSort());
        }

        @Test
        @DisplayName("sort=submitDate,asc を明示指定 → submitDate,asc + code,desc 複合ソート (tie-break は方向に関わらず DESC)")
        void submitDateAsc_addsCodeDescTieBreak() {
            final Model model = mock(Model.class);
            final CompanyTablePage page = new CompanyTablePage(List.of(), 0, 0L, 0, 25, Sort.by("submitDate"));
            when(viewService.findCompanyTable(any(CompanyTableQuery.class))).thenReturn(page);

            presenter.corporateViewV3(null, null, 0, 25, "submitDate,asc", model);

            final ArgumentCaptor<CompanyTableQuery> captor = ArgumentCaptor.forClass(CompanyTableQuery.class);
            verify(viewService).findCompanyTable(captor.capture());
            assertEquals(
                    Sort.by(Sort.Direction.ASC, "submitDate").and(Sort.by(Sort.Direction.DESC, "code")),
                    captor.getValue().pageable().getSort());
        }

        @Test
        @DisplayName("sort=name,asc 明示時 → name,asc 単独 (tie-break なし)")
        void nameSort_noTieBreak() {
            final Model model = mock(Model.class);
            final CompanyTablePage page = new CompanyTablePage(List.of(), 0, 0L, 0, 25, Sort.by("name"));
            when(viewService.findCompanyTable(any(CompanyTableQuery.class))).thenReturn(page);

            presenter.corporateViewV3(null, null, 0, 25, "name,asc", model);

            final ArgumentCaptor<CompanyTableQuery> captor = ArgumentCaptor.forClass(CompanyTableQuery.class);
            verify(viewService).findCompanyTable(captor.capture());
            assertEquals(Sort.by(Sort.Direction.ASC, "name"), captor.getValue().pageable().getSort());
        }

        @Test
        @DisplayName("sort=code,asc 明示時 → code,asc 単独 (tie-break なし)")
        void codeSort_noTieBreak() {
            final Model model = mock(Model.class);
            final CompanyTablePage page = new CompanyTablePage(List.of(), 0, 0L, 0, 25, Sort.by("code"));
            when(viewService.findCompanyTable(any(CompanyTableQuery.class))).thenReturn(page);

            presenter.corporateViewV3(null, null, 0, 25, "code,asc", model);

            final ArgumentCaptor<CompanyTableQuery> captor = ArgumentCaptor.forClass(CompanyTableQuery.class);
            verify(viewService).findCompanyTable(captor.capture());
            assertEquals(Sort.by(Sort.Direction.ASC, "code"), captor.getValue().pageable().getSort());
        }
    }

    @Nested
    @DisplayName("corporateViewV3Table メソッド")
    class CorporateViewV3Table {

        @Test
        @DisplayName("呼び出された場合 → fragments/index-table :: table を返す")
        void returnsFragmentName() {
            final Model model = mock(Model.class);
            final CompanyTablePage page = new CompanyTablePage(List.of(), 0, 0L, 0, 25, Sort.by("code"));
            when(viewService.findCompanyTable(any(CompanyTableQuery.class))).thenReturn(page);

            final String result = presenter.corporateViewV3Table(null, null, 0, 25, "code,asc", model);

            assertEquals("fragments/index-table :: table", result);
            verify(viewService, times(1)).findCompanyTable(any(CompanyTableQuery.class));
            verify(model).addAttribute("table", page);
        }

        @Test
        @DisplayName("テーブル fragment 用の共通属性が設定される")
        void sameCommonAttributesAsFullPage() {
            final Model model = mock(Model.class);
            final CompanyTablePage page = new CompanyTablePage(List.of(), 0, 0L, 0, 25, Sort.by("submitDate"));
            when(viewService.findCompanyTable(any(CompanyTableQuery.class))).thenReturn(page);

            presenter.corporateViewV3Table("favorite", "test", 1, 25, "submitDate,desc", model);

            verify(model).addAttribute("target", "favorite");
            verify(model).addAttribute("keyword", "test");
            verify(model).addAttribute("table", page);
            verify(model).addAttribute("sortParam", "submitDate,desc");
            assertNotNull(page);
        }

        @Test
        @DisplayName("systemEventSummary が model に設定される")
        void systemEventSummary_addedToModel() {
            final Model model = mock(Model.class);
            final CompanyTablePage page = new CompanyTablePage(List.of(), 0, 0L, 0, 25, Sort.by("code"));
            final List<SystemEventEntity> events = List.of(
                    SystemEventEntity.of(
                            SystemEventType.ERROR,
                            "AnalysisScheduler",
                            "想定外のエラーが発生しました。 failure",
                            LocalDateTime.parse("2026-07-20T09:00:00")
                    )
            );
            when(viewService.findCompanyTable(any(CompanyTableQuery.class))).thenReturn(page);
            when(systemEventUseCase.findRecent(7, 100)).thenReturn(events);

            presenter.corporateViewV3(null, null, 0, 25, "code,asc", model);

            verify(model).addAttribute("systemEventSummary", SystemEventSummaryViewModel.of(events));
        }
    }

    @Nested
    @DisplayName("toggleFavorite メソッド")
    class ToggleFavorite {

        @Test
        @DisplayName("登録された場合 → favorite=true でボタンフラグメントを返す")
        void registered_returnsButtonFragmentWithTrue() {
            final Model model = mock(Model.class);
            when(analysisService.updateFavoriteCompany(any(CodeInputData.class))).thenReturn(true);

            final String result = presenter.toggleFavorite("9999", model);

            assertEquals("fragments/index-table :: favorite-button", result);
            verify(analysisService, times(1)).updateFavoriteCompany(any(CodeInputData.class));
            verify(model).addAttribute("code", "9999");
            verify(model).addAttribute("favorite", true);
        }

        @Test
        @DisplayName("4桁コード入力時 → company マスタ照合用に5桁へ正規化して更新する")
        void fourDigitCode_normalizedToFiveDigits() {
            final Model model = mock(Model.class);
            when(analysisService.updateFavoriteCompany(any(CodeInputData.class))).thenReturn(true);

            presenter.toggleFavorite("9001", model);

            final ArgumentCaptor<CodeInputData> captor = ArgumentCaptor.forClass(CodeInputData.class);
            verify(analysisService).updateFavoriteCompany(captor.capture());
            assertEquals("90010", captor.getValue().getCode());
            // ボタン側の次回トグル用に code 属性は4桁のまま保持する
            verify(model).addAttribute("code", "9001");
        }

        @Test
        @DisplayName("解除された場合 → favorite=false でボタンフラグメントを返す")
        void unregistered_returnsButtonFragmentWithFalse() {
            final Model model = mock(Model.class);
            when(analysisService.updateFavoriteCompany(any(CodeInputData.class))).thenReturn(false);

            final String result = presenter.toggleFavorite("9999", model);

            assertEquals("fragments/index-table :: favorite-button", result);
            verify(model).addAttribute("favorite", false);
        }
    }

    @Nested
    @DisplayName("toggleStar メソッド")
    class ToggleStar {

        @Test
        @DisplayName("登録された場合 → star=true でボタンフラグメントを返す")
        void registered_returnsButtonFragmentWithTrue() {
            final Model model = mock(Model.class);
            when(analysisService.updateStarCompany(any(CodeInputData.class))).thenReturn(true);

            final String result = presenter.toggleStar("9999", model);

            assertEquals("fragments/index-table :: star-button", result);
            verify(analysisService, times(1)).updateStarCompany(any(CodeInputData.class));
            verify(model).addAttribute("code", "9999");
            verify(model).addAttribute("star", true);
        }

        @Test
        @DisplayName("4桁コード入力時 → company マスタ照合用に5桁へ正規化して更新する")
        void fourDigitCode_normalizedToFiveDigits() {
            final Model model = mock(Model.class);
            when(analysisService.updateStarCompany(any(CodeInputData.class))).thenReturn(true);

            presenter.toggleStar("9001", model);

            final ArgumentCaptor<CodeInputData> captor = ArgumentCaptor.forClass(CodeInputData.class);
            verify(analysisService).updateStarCompany(captor.capture());
            assertEquals("90010", captor.getValue().getCode());
            verify(model).addAttribute("code", "9001");
        }

        @Test
        @DisplayName("解除された場合 → star=false でボタンフラグメントを返す")
        void unregistered_returnsButtonFragmentWithFalse() {
            final Model model = mock(Model.class);
            when(analysisService.updateStarCompany(any(CodeInputData.class))).thenReturn(false);

            final String result = presenter.toggleStar("9999", model);

            assertEquals("fragments/index-table :: star-button", result);
            verify(model).addAttribute("star", false);
        }
    }
}
