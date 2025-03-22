package noogel.xyz.search.infrastructure.dto.repo;

import lombok.Data;

@Data
public class LLMSearchDto {
    /**
     * 搜索内容
     */
    private String searchQuery;
    /**
     * 目录前缀
     */
    private String dirPrefix;
    /**
     * 分片数量
     */
    private int maxNumFragments;
    /**
     * 每个分片大小
     */
    private int fragmentSize;
}
