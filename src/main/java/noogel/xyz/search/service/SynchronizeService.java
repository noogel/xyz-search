package noogel.xyz.search.service;

import noogel.xyz.search.infrastructure.config.ConfigProperties;

import java.io.File;
import java.util.List;

public interface SynchronizeService {

    /**
     * 异步索引
     */
    void asyncDirectories();

    /**
     * 异步索引
     */
    void asyncDirectories(List<ConfigProperties.IndexItem> syncDirectories, List<ConfigProperties.IndexItem> removeDirectories);

    /**
     * 重置索引
     */
    void resetIndex();

    /**
     * 追加文件
     *
     * @param files
     */
    void appendFiles(List<File> files);
}
