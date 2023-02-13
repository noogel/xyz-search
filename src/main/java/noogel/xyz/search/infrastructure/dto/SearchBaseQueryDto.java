package noogel.xyz.search.infrastructure.dto;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
public class SearchBaseQueryDto {
    private String search;
    private String resDirPrefix;
    private String resSize;
    private String modifiedAt;
    private Integer limit = 10;
    private Integer offset = 0;

    public boolean emptyQuery() {
        return StringUtils.isEmpty(search) && StringUtils.isEmpty(resDirPrefix)
                && StringUtils.isEmpty(resSize) && StringUtils.isEmpty(modifiedAt);
    }

    public boolean dirQuery() {
        return StringUtils.isEmpty(search) && StringUtils.isNotEmpty(resDirPrefix)
                && StringUtils.isEmpty(resSize) && StringUtils.isEmpty(modifiedAt);
    }
}
