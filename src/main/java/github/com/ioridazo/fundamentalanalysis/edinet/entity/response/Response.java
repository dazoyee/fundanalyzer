package github.com.ioridazo.fundamentalanalysis.edinet.entity.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Response {

    //メタデータ
    @JsonProperty("metadata")
    private Metadata metadata;

    // 提出書類一覧
    @JsonProperty("results")
    private List<Results> results;
}
