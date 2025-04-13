package noogel.xyz.search.infrastructure.dto;

import lombok.Data;
import noogel.xyz.search.infrastructure.model.FullTextSearchModel;

import java.util.List;

@Data
public class SearchResultDto {
    /**
     * 分页数据
     */
    private List<FullTextSearchModel> data;
    /**
     * 数量
     */
    private long size;
}
