package github.com.ioridazo.fundamentalanalysis.edinet.entity.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Parameter {

    // ファイル日付
    @JsonProperty("data")
    private String date;

    // 取得情報
    @JsonProperty("type")
    private String type;
}
