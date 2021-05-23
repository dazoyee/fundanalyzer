package github.com.ioridazo.fundanalyzer.domain.domain.specification;

import github.com.ioridazo.fundanalyzer.domain.domain.dao.transaction.FinancialStatementDao;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.FinancialStatementEnum;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.master.Subject;
import github.com.ioridazo.fundanalyzer.domain.domain.entity.transaction.FinancialStatementEntity;
import github.com.ioridazo.fundanalyzer.domain.value.BsSubject;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.domain.value.Document;
import github.com.ioridazo.fundanalyzer.domain.value.FinanceValue;
import github.com.ioridazo.fundanalyzer.domain.value.PlSubject;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.FinancialStatementKeyViewModel;
import github.com.ioridazo.fundanalyzer.web.view.model.corporate.detail.FinancialStatementValueViewModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.seasar.doma.jdbc.UniqueConstraintException;
import org.springframework.core.NestedRuntimeException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class FinancialStatementSpecification {

    private static final Logger log = LogManager.getLogger(FinancialStatementSpecification.class);

    private final FinancialStatementDao financialStatementDao;
    private final SubjectSpecification subjectSpecification;

    public FinancialStatementSpecification(
            final FinancialStatementDao financialStatementDao,
            final SubjectSpecification subjectSpecification) {
        this.financialStatementDao = financialStatementDao;
        this.subjectSpecification = subjectSpecification;
    }

    LocalDateTime nowLocalDateTime() {
        return LocalDateTime.now();
    }

    /**
     * 値を取得する
     *
     * @param fs       財務諸表種別
     * @param document ドキュメント
     * @param subject  科目
     * @return 値
     */
    public Optional<Long> findValue(final FinancialStatementEnum fs, final Document document, final Subject subject) {
        return financialStatementDao.selectByUniqueKey(
                document.getEdinetCode(),
                fs.toValue(),
                subject.getId(),
                String.valueOf(document.getPeriodEnd().getYear()),
                document.getDocumentTypeCode().toValue(),
                document.getSubmitDate()
        ).flatMap(FinancialStatementEntity::getValue);
    }

    /**
     * 値を取得する
     *
     * @param fs          財務諸表種別
     * @param document    ドキュメント
     * @param subjectList 科目リスト
     * @return 値
     */
    public Optional<Long> findValue(
            final FinancialStatementEnum fs, final Document document, final List<Subject> subjectList) {
        for (Subject subject : subjectList) {
            final Optional<Long> value = this.findValue(fs, document, subject);

            if (value.isPresent()) {
                return value;
            }
        }
        return Optional.empty();
    }

    /**
     * 株式総数を取得する
     *
     * @param document ドキュメント
     * @return 値
     */
    public Optional<Long> findNsValue(final Document document) {
        return financialStatementDao.selectByUniqueKey(
                document.getEdinetCode(),
                FinancialStatementEnum.TOTAL_NUMBER_OF_SHARES.toValue(),
                "0",
                String.valueOf(document.getPeriodEnd().getYear()),
                document.getDocumentTypeCode().toValue(),
                document.getSubmitDate()
        ).flatMap(FinancialStatementEntity::getValue);
    }

    /**
     * 企業における財務諸表の値リストを取得する
     *
     * @param company 企業情報
     * @return 財務諸表の値リスト
     */
    public List<FinancialStatementEntity> findByCompany(final Company company) {
        return financialStatementDao.selectByCode(company.getEdinetCode());
    }

    /**
     * 期間などのキーによる企業における財務諸表の値リストを取得する
     *
     * @param company 企業情報
     * @param key     期間などのキー
     * @return 財務諸表の値リスト
     */
    public List<FinancialStatementEntity> findByKeyPerCompany(
            final Company company, final FinancialStatementKeyViewModel key) {

        return financialStatementDao.selectByCodeAndPeriod(
                company.getEdinetCode(), key.getPeriodEnd(), key.getDocumentTypeCode(), key.getSubmitDate());
    }

    /**
     * 財務諸表の値を登録する
     *
     * @param company  企業情報
     * @param fs       財務諸表種別
     * @param dId      科目ID
     * @param document ドキュメント
     * @param value    値
     */
    public void insert(
            final Company company,
            final FinancialStatementEnum fs,
            final String dId,
            final Document document,
            final Long value) {
        try {
            financialStatementDao.insert(FinancialStatementEntity.of(
                    company.getCode().orElse(null),
                    company.getEdinetCode(),
                    fs.toValue(),
                    dId,
                    document.getPeriodStart(),
                    document.getPeriodEnd(),
                    value,
                    document.getDocumentTypeCode(),
                    document.getSubmitDate(),
                    document.getDocumentId(),
                    nowLocalDateTime()
            ));
        } catch (NestedRuntimeException e) {
            if (e.contains(UniqueConstraintException.class)) {
                log.debug("一意制約違反のため、データベースへの登録をスキップします。" +
                                "\t企業コード:{}\t財務諸表名:{}\t科目ID:{}\t対象年:{}",
                        company.getCode().orElse(null),
                        fs.getName(),
                        dId,
                        document.getDocumentPeriod().map(LocalDate::getYear).map(String::valueOf).orElse("null")
                );
            } else {
                throw e;
            }
        }
    }

    /**
     * 特定の財務諸表の値を取得する
     *
     * @param document ドキュメント
     * @return 特定の財務諸表の値
     */
    public FinanceValue getFinanceValue(final Document document) {
        return FinanceValue.of(
                // 流動資産合計
                findValue(
                        FinancialStatementEnum.BALANCE_SHEET,
                        document,
                        subjectSpecification.findBsSubjectList(BsSubject.BsEnum.TOTAL_CURRENT_ASSETS)
                ).orElse(null),
                // 投資その他の資産合計
                findValue(
                        FinancialStatementEnum.BALANCE_SHEET,
                        document,
                        subjectSpecification.findBsSubjectList(BsSubject.BsEnum.TOTAL_INVESTMENTS_AND_OTHER_ASSETS)
                ).orElse(null),
                // 流動負債合計
                findValue(
                        FinancialStatementEnum.BALANCE_SHEET,
                        document,
                        subjectSpecification.findBsSubjectList(BsSubject.BsEnum.TOTAL_CURRENT_LIABILITIES)
                ).orElse(null),
                // 固定負債合計
                findValue(
                        FinancialStatementEnum.BALANCE_SHEET,
                        document,
                        subjectSpecification.findBsSubjectList(BsSubject.BsEnum.TOTAL_FIXED_LIABILITIES)
                ).orElse(null),
                // 営業利益
                findValue(
                        FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT,
                        document,
                        subjectSpecification.findPlSubjectList(PlSubject.PlEnum.OPERATING_PROFIT)
                ).orElse(null),
                // 株式総数
                findNsValue(document).orElse(null)
        );
    }

    /**
     * 対象のデータベースリストから貸借対照表関連の値リストを取得する
     *
     * @param entityList データベースリスト
     * @return 貸借対照表関連の値リスト
     */
    public List<FinancialStatementValueViewModel> parseBsSubjectValue(final List<FinancialStatementEntity> entityList) {
        return entityList.stream()
                .filter(entity -> FinancialStatementEnum.BALANCE_SHEET.equals(FinancialStatementEnum.fromValue(entity.getFinancialStatementId())))
                .map(entity -> {
                    final FinancialStatementEnum fs = FinancialStatementEnum.fromValue(entity.getFinancialStatementId());

                    return FinancialStatementValueViewModel.of(
                            subjectSpecification.findSubject(fs, entity.getSubjectId()).getName(),
                            entity.getValue().orElse(null)
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * 対象のデータベースリストから損益計算書関連の値リストを取得する
     *
     * @param entityList データベースリスト
     * @return 損益計算書関連の値リスト
     */
    public List<FinancialStatementValueViewModel> parsePlSubjectValue(final List<FinancialStatementEntity> entityList) {
        return entityList.stream()
                .filter(entity -> FinancialStatementEnum.PROFIT_AND_LESS_STATEMENT.equals(FinancialStatementEnum.fromValue(entity.getFinancialStatementId())))
                .map(entity -> {
                    final FinancialStatementEnum fs = FinancialStatementEnum.fromValue(entity.getFinancialStatementId());

                    return FinancialStatementValueViewModel.of(
                            subjectSpecification.findSubject(fs, entity.getSubjectId()).getName(),
                            entity.getValue().orElse(null)
                    );
                })
                .collect(Collectors.toList());
    }
}
