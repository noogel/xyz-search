package noogel.xyz.search.service;

import java.io.File;
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

    /**
     * 重置索引
     */
    boolean resetIndex();

    /**
     * 追加文件
     * @param files
     */
    void appendFiles(List<File> files);
}
