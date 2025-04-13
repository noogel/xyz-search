package noogel.xyz.search.infrastructure.utils;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Objects;

public class UrlHelper {
    public static String ct(Object obj) {
        if (Objects.nonNull(obj)) {
            return URLEncoder.encode(obj.toString(), Charset.defaultCharset());
        }
        return "";
    }
}
