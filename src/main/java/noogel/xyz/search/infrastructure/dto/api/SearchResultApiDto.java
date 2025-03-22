package noogel.xyz.search.infrastructure.dto.api;

import lombok.Data;

import java.util.List;

@Data
public class SearchResultApiDto {
    /**
     * 数据
     */
    private String query;
    private List<SearxngResourceApiDto> results;
}
