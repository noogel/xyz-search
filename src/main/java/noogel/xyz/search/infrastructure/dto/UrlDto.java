package noogel.xyz.search.infrastructure.dto;

import lombok.Data;
import noogel.xyz.search.infrastructure.utils.UrlHelper;
import org.springframework.util.CollectionUtils;

import java.util.Map;
import java.util.stream.Collectors;

@Data
public class UrlDto {
    private String requestUrl;
    private String baseUrl;
    private String searchUrl;
    private Map<String, String> parameters;

    public static String buildRequestUrl(String requestUrl, Map<String, String> parameters) {
        if (CollectionUtils.isEmpty(parameters)) {
            return requestUrl;
        }
        return String.format("%s?%s", requestUrl, parameters.entrySet().stream()
                .map(t -> String.format("%s=%s", t.getKey(), UrlHelper.ct(t.getValue())))
                .collect(Collectors.joining("&"))
        );
    }
}
