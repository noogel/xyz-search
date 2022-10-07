package noogel.xyz.search.service;

import java.util.List;
import java.util.Map;

public interface SynchronizeService {
    /**
     * 异步索引指定目录
     * @param paths
     */
    void async(List<String> paths);

    /**
     * 异步索引全部
     */
    void asyncAll();

    /**
     * 重置索引
     */
    boolean resetIndex();
}
