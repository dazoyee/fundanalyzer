package github.com.ioridazo.fundanalyzer.domain.usecase;

import github.com.ioridazo.fundanalyzer.web.model.CodeInputData;
import io.micrometer.observation.annotation.Observed;

public interface CompanyUseCase {

    /**
     * 企業情報の更新日時を取得する
     *
     * @return 企業情報の更新日時
     */
    @Observed
    String getUpdateDate();

    /**
     * EDINETから企業情報ファイルダウンロード
     * ↓
     * zipファイル解凍
     * ↓
     * ファイル読み取り
     * ↓
     * データベース保存
     */
    @Observed
    void importCompanyInfo();

    /**
     * ファイル読み取り
     * ↓
     * データベース保存
     */
    @Observed
    void saveCompanyInfo();

    /**
     * お気に入り企業の登録
     *
     * @param inputData 企業コード
     */
    @Observed
    boolean updateFavoriteCompany(CodeInputData inputData);

    /**
     * 上場中かどうか
     *
     * @param inputData 企業コード
     * @return boolean
     */
    @Observed
    boolean isLived(CodeInputData inputData);

    /**
     * 企業の除外
     *
     * @param inputData 企業コード
     */
    @Observed
    void updateRemovedCompany(CodeInputData inputData);
}
