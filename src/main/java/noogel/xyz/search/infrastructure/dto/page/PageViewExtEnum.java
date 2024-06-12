package noogel.xyz.search.infrastructure.dto.page;

import lombok.AllArgsConstructor;
import lombok.Getter;
import noogel.xyz.search.infrastructure.consts.FileExtEnum;
import noogel.xyz.search.infrastructure.utils.UrlHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Optional;

/**
 * 页面查看扩展配置枚举类
 */
@AllArgsConstructor
@Getter
public enum PageViewExtEnum {
    EPUB(false, "/epub/web/view?book=%s"),
    HTML(false, "/file/view/%s"),
    XHTML(false, "/file/view/%s"),
    XML(false, "/file/view/%s"),
    TXT(false, "/file/view/%s"),
    PDF(false, "/pdf.js/web/view?file=/file/view/%s"),
    MP4(false, "/video/%s"),
    AVI(false, "/video/%s"),
    MKV(false, "/video/%s"),
    JPEG(true, "/file/view/%s"),
    JPG(true, "/file/view/%s"),
    PNG(true, "/file/view/%s"),
    WEBP(true, "/file/view/%s"),
    BMP(true, "/file/view/%s"),
    TIFF(true, "/file/view/%s"),
    HTM(false, "/file/view/%s"),
    HEIF(false, null),
    HEIC(false, null),
    MOBI(false, null),
    AZW3(false, null),
    AZW(false, null),
    CSV(false, null),
    MD(false, null),
    ;

    private static final String THUMBNAIL_URL_TPL = "/file/view/thumbnail/%s";
    private static final String DOWNLOAD_URL_TPL = "/file/%s/download";
    private static final String DIR_VIEW_URL_TPL = "/?resId=%s&relativeResDir=%s";

    /**
     * 是否支持缩略图
     */
    private final boolean thumbnail;

    /**
     * 查看 url
     */
    private final String viewUrl;

    public static String downloadUrl(String resId) {
        return String.format(DOWNLOAD_URL_TPL, resId.trim());
    }

    public static String dirViewUrl(String resId, String relativeResDir) {
        return String.format(DIR_VIEW_URL_TPL, resId.trim(), UrlHelper.ct(relativeResDir));
    }

    public static Optional<PageViewExtEnum> find(FileExtEnum extEnum) {
        return Arrays.stream(values()).filter(t -> t.name().equalsIgnoreCase(extEnum.name())).findFirst();
    }

    public boolean supportView() {
        return StringUtils.isNotBlank(viewUrl);
    }

    public String calViewUrl(String resId) {
        if (supportView()) {
            return String.format(viewUrl, resId.trim());
        }
        return "";
    }

    public String calThumbnailUrl(String resId) {
        if (thumbnail) {
            return String.format(THUMBNAIL_URL_TPL, resId.trim());
        }
        return "#";
    }
}
