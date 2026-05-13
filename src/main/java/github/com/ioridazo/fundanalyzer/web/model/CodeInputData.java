package github.com.ioridazo.fundanalyzer.web.model;

import github.com.ioridazo.fundanalyzer.exception.FundanalyzerRuntimeException;

/**
 * 企業コード入力データ
 *
 * @param code 企業コード（4 桁または 5 桁）
 */
public record CodeInputData(String code) {

    /**
     * 静的ファクトリ
     *
     * @param code 企業コード
     * @return CodeInputData
     */
    public static CodeInputData of(final String code) {
        return new CodeInputData(code);
    }

    /**
     * 互換アクセサ（既存呼び出し向け）
     *
     * @return 企業コード
     */
    public String getCode() {
        return code;
    }

    /**
     * 4 桁の企業コードを返す
     *
     * @return 4 桁コード
     * @throws FundanalyzerRuntimeException 4 桁・5 桁以外のとき
     */
    public String getCode4() {
        if (code.length() == 4) {
            return code;
        } else if (code.length() == 5) {
            return code.substring(0, 4);
        } else {
            throw new FundanalyzerRuntimeException();
        }
    }

    /**
     * 5 桁の企業コードを返す
     *
     * @return 5 桁コード
     * @throws FundanalyzerRuntimeException 4 桁・5 桁以外のとき
     */
    public String getCode5() {
        if (code.length() == 4) {
            return code + "0";
        } else if (code.length() == 5) {
            return code;
        } else {
            throw new FundanalyzerRuntimeException();
        }
    }
}
