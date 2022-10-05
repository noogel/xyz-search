package noogel.xyz.search.infrastructure.dto;

import lombok.Data;

import java.util.List;

@Data
public class SearchResultShowDto {
    /**
     * 数据
     */
    private List<ResourceSimpleDto> data;
    /**
     * 分页
     */
    private PagingDto paging;
}
