package noogel.xyz.search.infrastructure.dto.page;

import lombok.Data;
import noogel.xyz.search.infrastructure.utils.UrlHelper;
import org.apache.commons.lang3.StringUtils;

@Data
public class ResourceSimpleDto {
    /**
     * 资源 HASH
     */
    private String resId;
    /**
     * 资源名称
     */
    private String resTitle;
    private String searchableTitle;
    /**
     * 资源大小
     */
    private String resSize;
    /**
     * 更新时间 秒
     */
    private String modifiedAt;

    public void calculateSearchableResTitle() {
        String searchableTitle = resTitle;
        if (resTitle.contains(" - ")) {
            String splitTitle = resTitle.split(" - ")[0];
            if (StringUtils.isNotBlank(splitTitle)) {
                searchableTitle = splitTitle;
            }
        } else if (resTitle.contains(".")) {
            searchableTitle = resTitle.substring(0, resTitle.lastIndexOf("."));
        }
        this.searchableTitle = UrlHelper.ct(searchableTitle);
    }
}
