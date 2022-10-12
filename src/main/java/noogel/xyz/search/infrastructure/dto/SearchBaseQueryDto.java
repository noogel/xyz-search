package noogel.xyz.search.infrastructure.dto;

import lombok.Data;

@Data
public class SearchBaseQueryDto {
    private String search;
    private String resDirPrefix;
    private String resSize;
    private String modifiedAt;
    private Integer limit = 10;
    private Integer offset = 0;
}
