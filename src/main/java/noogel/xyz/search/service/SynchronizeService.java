package noogel.xyz.search.service;

import java.util.List;

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
}
