package github.com.ioridazo.fundanalyzer.proxy.edinet.entity.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Metadata {

    // タイトル
    @JsonProperty("title")
    private String title;

    // パラメータ
    @JsonProperty("parameter")
    private Parameter parameter;

    // 結果セット
    @JsonProperty("resultset")
    private ResultSet resultset;

    // 書類一覧更新日時
    @JsonProperty("processDateTime")
    private String processDateTime;

    // ステータス
    @JsonProperty("status")
    private String status;

    // メッセージ
    @JsonProperty("message")
    private String message;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Parameter {
        // ファイル日付
        @JsonProperty("date")
        private String date;

        // 取得情報
        @JsonProperty("type")
        private String type;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResultSet {
        // 件数
        @JsonProperty("count")
        private String count;
    }
}
