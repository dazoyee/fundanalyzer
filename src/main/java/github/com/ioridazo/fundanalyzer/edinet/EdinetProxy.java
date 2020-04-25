package github.com.ioridazo.fundanalyzer.edinet;

import github.com.ioridazo.fundanalyzer.edinet.entity.request.AcquisitionRequestParameter;
import github.com.ioridazo.fundanalyzer.edinet.entity.request.ListRequestParameter;
import github.com.ioridazo.fundanalyzer.edinet.entity.response.EdinetResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestOperations;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Component
public class EdinetProxy {

    final private RestOperations restOperations;

    public EdinetProxy(final RestOperations restOperations) {
        this.restOperations = restOperations;
    }

    public EdinetResponse documentList(ListRequestParameter parameter) {
        return restOperations.getForObject(
                "/api/v1/documents.json?date={date}&type={type}",
                EdinetResponse.class,
                param(parameter)
        );
    }

    public void documentAcquisition(File storagePath, AcquisitionRequestParameter parameter) {
        if (!storagePath.exists()) //noinspection ResultOfMethodCallIgnored
            storagePath.mkdir();

        RequestCallback requestCallback =
                request -> request
                        .getHeaders()
                        .setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));

        ResponseExtractor<Void> responseExtractor = response -> {
            Path path = Paths.get(storagePath + "/" + parameter.getDocId() + ".zip");
            Files.copy(response.getBody(), path);
            return null;
        };

        restOperations.execute(
                "/api/v1/documents/{docId}?type={type}",
                HttpMethod.GET,
                requestCallback,
                responseExtractor,
                param(parameter)
        );
    }

    private Map<String, String> param(ListRequestParameter parameter) {
        var param = new HashMap<String, String>();
        param.put("date", parameter.getDate());
        param.put("type", parameter.getType().toValue());
        return param;
    }

    private Map<String, String> param(AcquisitionRequestParameter parameter) {
        var param = new HashMap<String, String>();
        param.put("docId", parameter.getDocId());
        param.put("type", parameter.getType().toValue());
        return param;
    }
}
