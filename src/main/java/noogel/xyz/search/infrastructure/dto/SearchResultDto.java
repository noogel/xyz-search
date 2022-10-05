package noogel.xyz.search.infrastructure.dto;

import lombok.Data;
import noogel.xyz.search.infrastructure.model.ResourceModel;

import java.util.List;

@Data
public class SearchResultDto {
    /**
     * 分页数据
     */
    private List<ResourceModel> data;
    /**
     * 数量
     */
    private long size;
    /**
     * 是否准确数量
     */
    private boolean exactSize;
}
