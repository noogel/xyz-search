package noogel.xyz.search.service;

import noogel.xyz.search.infrastructure.dto.IndexedContentDto;

public interface VectorProcessService {

    /**
     * 处理向量
     * 
     * @param indexedContentDto
     */
    void asyncUpsert(IndexedContentDto indexedContentDto);

    /**
     * 删除向量
     * 
     * @param resId
     */
    void asyncDelete(String resId);
}
