package github.com.ioridazo.fundamentalanalysis.edinet;

import github.com.ioridazo.fundamentalanalysis.edinet.entity.request.RequestParameter;
import github.com.ioridazo.fundamentalanalysis.edinet.entity.response.Response;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;

import java.util.HashMap;
import java.util.Map;

@Component
public class EdinetProxy {

    RestOperations restOperations;

    public EdinetProxy(final RestOperations restOperations) {
        this.restOperations = restOperations;
    }

    public Response documentList(RequestParameter parameter) {
        return exchange(
                "/api/v1/documents.json?date={date}&type={type}",
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

    private Map param(RequestParameter parameter) {
        var param = new HashMap<>();
        param.put("date", parameter.getDate());
        param.put("type", parameter.getType().toValue());
        return param;
    }
}
