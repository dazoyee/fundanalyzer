package github.com.ioridazo.fundanalyzer.domain.interactor;

import github.com.ioridazo.fundanalyzer.client.csv.CsvCommander;
import github.com.ioridazo.fundanalyzer.client.csv.bean.EdinetCsvResultBean;
import github.com.ioridazo.fundanalyzer.client.file.FileOperator;
import github.com.ioridazo.fundanalyzer.client.jsoup.JsoupClient;
import github.com.ioridazo.fundanalyzer.client.log.Category;
import github.com.ioridazo.fundanalyzer.client.log.FundanalyzerLogClient;
import github.com.ioridazo.fundanalyzer.client.log.Process;
import github.com.ioridazo.fundanalyzer.client.selenium.SeleniumClient;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.CompanySpecification;
import github.com.ioridazo.fundanalyzer.domain.domain.specification.IndustrySpecification;
import github.com.ioridazo.fundanalyzer.domain.usecase.CompanyUseCase;
import github.com.ioridazo.fundanalyzer.domain.value.Company;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerFileException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerNotExistException;
import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRestClientException;
import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class CompanyInteractor implements CompanyUseCase {

    private static final Logger log = LogManager.getLogger(CompanyInteractor.class);

    private final IndustrySpecification industrySpecification;
    private final CompanySpecification companySpecification;
    private final FileOperator fileOperator;
    private final CsvCommander csvCommander;
    private final SeleniumClient seleniumClient;
    private final JsoupClient jsoupClient;

    @Value("${app.settings.file.path.company.company}")
    String pathCompany;
    @Value("${app.settings.file.path.company.zip}")
    String pathCompanyZip;

    public CompanyInteractor(
            final IndustrySpecification industrySpecification,
            final CompanySpecification companySpecification,
            final FileOperator fileOperator,
            final CsvCommander csvCommander,
            final SeleniumClient seleniumClient,
            final JsoupClient jsoupClient) {
        this.industrySpecification = industrySpecification;
        this.companySpecification = companySpecification;
        this.fileOperator = fileOperator;
        this.csvCommander = csvCommander;
        this.seleniumClient = seleniumClient;
        this.jsoupClient = jsoupClient;
    }

    /**
     * 企業情報の更新日時を取得する
     *
     * @return 企業情報の更新日時
     */
    @Override
    public String getUpdateDate() {
        return companySpecification.findLastUpdateDateTime().orElse("null");
    }

    /**
     * 企業情報ファイルを取得して登録する
     */
    @Override
    public void importCompanyInfo() {
        final String inputFilePath = makeTargetPath(pathCompanyZip, LocalDate.now()).getPath();
        try {
            // ファイルダウンロード
            final String fileName = seleniumClient.edinetCodeList(inputFilePath);

            final File inputFile = new File(String.format("%s/%s", inputFilePath, fileName.replace(".zip", "")));
            final File outputFile = new File(pathCompany);

            // zipファイル解凍
            fileOperator.decodeZipFile(inputFile, outputFile);
        } catch (FundanalyzerRestClientException e) {
            log.warn("Selenium通信が完了しなかったため、ファイルダウンロードをスキップします。", e);
        } catch (IOException e) {
            throw new FundanalyzerFileException("zipファイルの解凍処理に失敗しました。スタックトレースから原因を確認してください。", e);
        }

        // データベースアップデート
        saveCompanyInfo();
    }

    /**
     * 取得済のファイルを登録する
     */
    @Override
    public void saveCompanyInfo() {
        final long startTime = System.currentTimeMillis();
        // ファイル読み取り
        final List<EdinetCsvResultBean> resultBeanList = csvCommander.readCsv(
                Arrays.stream(Objects.requireNonNull(new File(pathCompany).listFiles())).findFirst().orElseThrow(),
                Charset.forName("windows-31j"),
                EdinetCsvResultBean.class
        );

        // Industryの登録
        industrySpecification.insert(resultBeanList);

        // Companyの登録
        companySpecification.upsert(resultBeanList);

        log.info(FundanalyzerLogClient.toInteractorLogObject(
                "CSVファイルから会社情報の登録が完了しました。",
                Category.COMPANY,
                Process.UPDATE,
                System.currentTimeMillis() - startTime
        ));
    }

    /**
     * 企業のお気に入り登録を更新する
     *
     * @param inputData 企業コード
     */
    @Override
    public boolean updateFavoriteCompany(final CodeInputData inputData) {
        final long startTime = System.currentTimeMillis();
        final Optional<Company> company = companySpecification.findCompanyByCode(inputData.getCode());

        if (company.isPresent()) {
            final boolean isFavorite = companySpecification.updateFavorite(company.get());
            if (isFavorite) {
                log.info(FundanalyzerLogClient.toInteractorLogObject(
                        MessageFormat.format("対象の企業をお気に入りに登録しました。\t企業コード:{0}", inputData.getCode()),
                        Category.COMPANY,
                        Process.UPDATE,
                        System.currentTimeMillis() - startTime
                ));
                return true;
            } else {
                log.info(FundanalyzerLogClient.toInteractorLogObject(
                        MessageFormat.format("対象の企業をお気に入りから除外しました。\t企業コード:{0}", inputData.getCode()),
                        Category.COMPANY,
                        Process.UPDATE,
                        System.currentTimeMillis() - startTime
                ));
                return false;
            }
        } else {
            log.info(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format("対象の企業は存在しませんでした。\t企業コード:{0}", inputData.getCode()),
                    Category.COMPANY,
                    Process.UPDATE,
                    System.currentTimeMillis() - startTime
            ));
            throw new FundanalyzerNotExistException();
        }
    }

    /**
     * 上場しているか確認する
     *
     * @param inputData 企業コード
     * @return boolean
     */
    @Override
    public boolean isLived(final CodeInputData inputData) {
        return jsoupClient.isLivedCompanyFromMinkabu(inputData.getCode());
    }

    /**
     * 上場廃止済ならば企業除外する
     *
     * @param inputData 企業コード
     */
    @Override
    public void updateRemovedCompany(final CodeInputData inputData) {
        final long startTime = System.currentTimeMillis();

        final Optional<Company> company = companySpecification.findCompanyByCode(inputData.getCode());
        if (company.isPresent()) {
            companySpecification.updateRemoved(company.get());

            log.info(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format("対象の企業は上場廃止済みのため除外しました。\t企業コード:{0}", inputData.getCode()),
                    companySpecification.findCompanyByCode(inputData.getCode()).map(Company::edinetCode).orElse("null"),
                    Category.COMPANY,
                    Process.UPDATE,
                    System.currentTimeMillis() - startTime
            ));
        } else {
            log.info(FundanalyzerLogClient.toInteractorLogObject(
                    MessageFormat.format("対象の企業は存在しませんでした。\t企業コード:{0}", inputData.getCode()),
                    companySpecification.findCompanyByCode(inputData.getCode()).map(Company::edinetCode).orElse("null"),
                    Category.COMPANY,
                    Process.UPDATE,
                    System.currentTimeMillis() - startTime
            ));
            throw new FundanalyzerNotExistException();
        }
    }

    /**
     * ファイルパスを生成する
     *
     * @param prePath    前パス
     * @param targetDate 対象日
     * @return ファイルパス
     */
    private File makeTargetPath(final String prePath, final LocalDate targetDate) {
        return new File(String.format("%s/%d/%s/%s", prePath, targetDate.getYear(), targetDate.getMonth(), targetDate));
    }
}
