package noogel.xyz.search.infrastructure.repo;

import noogel.xyz.search.infrastructure.dto.dao.FileResContentDto;
import noogel.xyz.search.infrastructure.dto.dao.FileResReadDto;

public interface VectorRepo {
    /**
     * 创建或更新
     * @param res
     * @param content
     */
    void upsert(FileResReadDto res, FileResContentDto content);

    /**
     * 清理 ID
     * @param resId
     */
    void delete(String resId);

    /**
     * 压缩数据
     */
    default void forceMerge() {}

    /**
     * 重置索引
     */
    default void reset() {}

}
