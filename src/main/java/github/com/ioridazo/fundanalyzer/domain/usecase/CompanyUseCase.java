package github.com.ioridazo.fundanalyzer.domain.usecase;

import org.springframework.cloud.sleuth.annotation.NewSpan;

public interface CompanyUseCase {

    /**
     * 企業情報の更新日時を取得する
     *
     * @return 企業情報の更新日時
     */
    @NewSpan
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
    @NewSpan
    void importCompanyInfo();

    /**
     * ファイル読み取り
     * ↓
     * データベース保存
     */
    @NewSpan
    void saveCompanyInfo();
}
