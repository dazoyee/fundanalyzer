package github.com.ioridazo.fundanalyzer.domain.logic.company;

import github.com.ioridazo.fundanalyzer.csv.CsvCommander;
import github.com.ioridazo.fundanalyzer.domain.dao.master.CompanyDao;
import github.com.ioridazo.fundanalyzer.domain.dao.master.IndustryDao;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Company;
import github.com.ioridazo.fundanalyzer.domain.entity.master.Industry;
import github.com.ioridazo.fundanalyzer.domain.logic.company.bean.EdinetCsvResultBean;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerFileException;
import github.com.ioridazo.fundanalyzer.file.FileOperator;
import lombok.extern.log4j.Log4j2;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
@Component
public class CompanyLogic {

    private final FileOperator fileOperator;
    private final CsvCommander csvCommander;
    private final IndustryDao industryDao;
    private final CompanyDao companyDao;

    public CompanyLogic(
            final FileOperator fileOperator,
            final CsvCommander csvCommander,
            final IndustryDao industryDao,
            final CompanyDao companyDao) {
        this.fileOperator = fileOperator;
        this.csvCommander = csvCommander;
        this.industryDao = industryDao;
        this.companyDao = companyDao;
    }

    LocalDateTime nowLocalDateTime() {
        return LocalDateTime.now();
    }

    /**
     * zipファイルを解凍して会社情報一覧CSVを読み取る
     *
     * @param fileName       zipファイル名
     * @param fileInputPath  zipファイルパス
     * @param fileOutputPath CSVファイルパス
     * @return CSV読み取り結果
     */
    @NewSpan("CompanyLogic.readFile")
    public List<EdinetCsvResultBean> readFile(
            final String fileName, final String fileInputPath, final String fileOutputPath) {
        final File inputFile = new File(fileInputPath + "/" + fileName.replace(".zip", ""));
        final File outputFile = new File(fileOutputPath);

        try {
            // zipファイル解凍
            fileOperator.decodeZipFile(inputFile, outputFile);
            // CSV読み取り
            return readFile(fileOutputPath);
        } catch (IOException e) {
            log.error("zipファイルの解凍処理に失敗しました。スタックトレースから原因を確認してください。" +
                    "\t対象ファイル:{}", fileInputPath, e);
            throw new FundanalyzerFileException(e);
        }
    }

    /**
     * 会社情報一覧CSVを読み取る
     *
     * @param filePath CSVファイルパス
     * @return CSV読み取り結果
     */
    public List<EdinetCsvResultBean> readFile(final String filePath) {
        return csvCommander.readCsv(
                Arrays.stream(Objects.requireNonNull(new File(filePath).listFiles())).findFirst().orElseThrow(),
                Charset.forName("windows-31j"),
                EdinetCsvResultBean.class
        );
    }

    /**
     * 業種をDBに登録する
     *
     * @param resultBeanList CSV読み取り結果
     */
    @NewSpan("CompanyLogic.insertIndustry")
    @Transactional
    public void insertIndustry(final List<EdinetCsvResultBean> resultBeanList) {
        final var dbIndustryList = industryDao.selectAll().stream()
                .map(Industry::getName)
                .collect(Collectors.toList());

        resultBeanList.stream()
                .map(EdinetCsvResultBean::getIndustry)
                .distinct()
                .forEach(resultBeanIndustry -> Stream.of(resultBeanIndustry)
                        .filter(industryName -> dbIndustryList.stream().noneMatch(industryName::equals))
                        .forEach(industryName -> industryDao.insert(new Industry(null, industryName, nowLocalDateTime())))
                );
    }

    /**
     * 会社をDBに登録する
     *
     * @param resultBeanList CSV読み取り結果
     */
    @NewSpan("CompanyLogic.upsertCompany")
    @Transactional
    public void upsertCompany(final List<EdinetCsvResultBean> resultBeanList) {
        final var companyList = companyDao.selectAll();
        final var industryList = industryDao.selectAll();
        resultBeanList.forEach(resultBean -> {
                    final var match = companyList.stream()
                            .map(Company::getEdinetCode)
                            .anyMatch(resultBean.getEdinetCode()::equals);
                    if (match) {
                        companyDao.update(Company.of(industryList, resultBean, nowLocalDateTime()));
                    } else {
                        companyDao.insert(Company.of(industryList, resultBean, nowLocalDateTime()));
                    }
                }
        );
    }
}
