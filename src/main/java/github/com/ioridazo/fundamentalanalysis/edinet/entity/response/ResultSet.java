package github.com.ioridazo.fundamentalanalysis.edinet.entity.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResultSet {

    // 件数
    @JsonProperty("count")
    private int count;
}
