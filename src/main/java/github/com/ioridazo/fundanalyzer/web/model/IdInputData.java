package github.com.ioridazo.fundanalyzer.web.model;

/**
 * ID 入力データ
 *
 * @param id ドキュメント等の識別子
 */
public record IdInputData(String id) {

    /**
     * 静的ファクトリ
     *
     * @param id 識別子
     * @return IdInputData
     */
    public static IdInputData of(final String id) {
        return new IdInputData(id);
    }

    /**
     * 互換アクセサ（既存呼び出し向け）
     *
     * @return id
     */
    public String getId() {
        return id;
    }
}
