package noogel.xyz.search.infrastructure.dto;

import lombok.Data;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Objects;

@Data
public class SearchQueryDto {
    private String search;
    private String resHash;
    private String resSize;
    private String modifiedAt;
    private Integer limit = 10;
    private Integer offset = 0;

    public String getUrlQuery(long offset) {
        return String.format("search=%s&resHash=%s&resSize=%s&modifiedAt=%s&limit=%s&offset=%s",
                ct(search), ct(resHash), ct(resSize), ct(modifiedAt), ct(limit), ct(offset));
    }

    private static String ct(Object obj) {
        if (Objects.nonNull(obj)) {
            return URLEncoder.encode(obj.toString(), Charset.defaultCharset());
        }
        return "";
    }
}
