package github.com.ioridazo.fundamentalanalysis.edinet.entity.response;

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
}
