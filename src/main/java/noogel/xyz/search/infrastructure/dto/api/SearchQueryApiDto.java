package noogel.xyz.search.infrastructure.dto.api;

import lombok.Data;

@Data
public class SearchQueryApiDto {
    private String search;
    private Integer limit = 10;
    private Integer offset = 0;
}
