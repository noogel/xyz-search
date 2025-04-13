package noogel.xyz.search.infrastructure.dto;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import static noogel.xyz.search.infrastructure.utils.UrlHelper.ct;

@Data
public class BreadcrumbItemDto {

    private String text;
    private String relativeResDir;
    private String resId;

    private boolean show;

    public static BreadcrumbItemDto of(String resId, String relativeResDir) {
        BreadcrumbItemDto dto = new BreadcrumbItemDto();
        dto.resId = resId;
        dto.relativeResDir = relativeResDir;
        dto.text = relativeResDir.substring(relativeResDir.lastIndexOf("/") + 1);
        dto.show = true;
        return dto;
    }

    public static BreadcrumbItemDto ofRoot() {
        BreadcrumbItemDto dto = new BreadcrumbItemDto();
        dto.setText("根");
        dto.setShow(true);
        return dto;
    }

    public String getUrlQuery() {
        if (StringUtils.isBlank(resId) || StringUtils.isBlank(relativeResDir)) {
            return "";
        }
        return String.format("resId=%s&relativeResDir=%s", ct(resId), ct(relativeResDir));
    }

    public BreadcrumbItemDto setHidden() {
        show = false;
        return this;
    }

}
