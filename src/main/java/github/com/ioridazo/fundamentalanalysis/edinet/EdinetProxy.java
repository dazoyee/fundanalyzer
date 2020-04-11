package github.com.ioridazo.fundamentalanalysis.edinet;

import github.com.ioridazo.fundamentalanalysis.edinet.entity.request.AcquisitionRequestParameter;
import github.com.ioridazo.fundamentalanalysis.edinet.entity.request.ListRequestParameter;
import github.com.ioridazo.fundamentalanalysis.edinet.entity.response.Response;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestOperations;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Component
public class EdinetProxy {

    RestOperations restOperations;

    public EdinetProxy(final RestOperations restOperations) {
        this.restOperations = restOperations;
    }

    public Response documentList(ListRequestParameter parameter) {
        return exchange(
                "/api/v1/documents.json?date={date}&type={type}",
                param(parameter)
        );
    }

    public void documentAcquisition(AcquisitionRequestParameter parameter) {

        RequestCallback requestCallback = request -> request.getHeaders()
                .setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));

        ResponseExtractor<Void> responseExtractor = response -> {
            Path path = Paths.get("C:/sps_batch/S100IBHG_" + LocalDateTime.now().getMinute() + LocalDateTime.now().getSecond() + ".zip");
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


    public Response exchange(String uri, Map parameter) {
        return restOperations.getForObject(
                uri,
                Response.class,
                parameter
        );
    }

    private Map param(ListRequestParameter parameter) {
        var param = new HashMap<>();
        param.put("date", parameter.getDate());
        param.put("type", parameter.getType().toValue());
        return param;
    }

    private Map param(AcquisitionRequestParameter parameter) {
        var param = new HashMap<>();
        param.put("docId", parameter.getDocId());
        param.put("type", parameter.getType().toValue());
        return param;
    }
}
