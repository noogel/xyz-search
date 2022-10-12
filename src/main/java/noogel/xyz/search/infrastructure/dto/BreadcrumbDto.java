package noogel.xyz.search.infrastructure.dto;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Data
public class BreadcrumbDto {
    private List<BreadcrumbItemDto> items;

    public static BreadcrumbDto of(String resId, String relativeResDir) {
        if (StringUtils.isBlank(resId) || StringUtils.isBlank(relativeResDir)) {
            return null;
        }
        if (relativeResDir.endsWith("/")) {
            relativeResDir = relativeResDir.substring(0, relativeResDir.length() - 1);
        }
        BreadcrumbDto dto = new BreadcrumbDto();
        dto.setItems(new ArrayList<>());
        dto.getItems().add(0, BreadcrumbItemDto.of(resId, relativeResDir).setHidden());
        int indexOf = relativeResDir.lastIndexOf("/");
        while (indexOf > 0) {
            relativeResDir = relativeResDir.substring(0, indexOf);
            dto.getItems().add(0, BreadcrumbItemDto.of(resId, relativeResDir));
            indexOf = relativeResDir.lastIndexOf("/");
        }
        dto.getItems().add(0, BreadcrumbItemDto.ofRoot());
        return dto;
    }
}
