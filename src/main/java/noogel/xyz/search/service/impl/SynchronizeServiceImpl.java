package noogel.xyz.search.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.client.VectorClient;
import noogel.xyz.search.infrastructure.config.ConfigProperties;
import noogel.xyz.search.infrastructure.consts.CommonsConsts;
import noogel.xyz.search.infrastructure.consts.FileStateEnum;
import noogel.xyz.search.infrastructure.consts.JobTypeEnum;
import noogel.xyz.search.infrastructure.dto.dao.FileResWriteDto;
import noogel.xyz.search.infrastructure.dto.dao.FileViewDto;
import noogel.xyz.search.infrastructure.queue.WorkQueueService;
import noogel.xyz.search.infrastructure.repo.FullTextSearchService;
import noogel.xyz.search.infrastructure.utils.FileResHelper;
import noogel.xyz.search.service.FileDbService;
import noogel.xyz.search.service.SynchronizeService;
import noogel.xyz.search.service.extension.ExtensionService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class SynchronizeServiceImpl implements SynchronizeService {

    @Resource
    private ExtensionService extensionService;
    @Resource
    private FullTextSearchService fullTextSearchService;
    @Resource
    private FileDbService fileDbService;
    @Resource
    private VectorClient vectorClient;
    @Resource
    private WorkQueueService workQueueService;
    @Resource
    private ConfigProperties configProperties;

    private static List<FileViewDto> calculateRemoveFiles(List<File> fsFiles, List<FileViewDto> dbFiles) {
        List<String> fsFilesUk = fsFiles.stream().map(t -> {
            if (t.isDirectory()) {
                return t.getAbsolutePath();
            } else if (t.isFile()) {
                return String.format("%s_%s_%s", t.getAbsolutePath(), t.length(), t.lastModified());
            }
            return "";
        }).filter(StringUtils::isNotBlank).toList();
        return dbFiles.stream().filter(t -> {
            String uk = "";
            if (t.isDirectory()) {
                uk = t.getPath();
            } else if (t.isFile()) {
                uk = String.format("%s_%s_%s", t.getPath(), t.getSize(), t.getModifiedAt());
            }
            return !fsFilesUk.contains(uk);
        }).toList();
    }

    private static List<File> calculateAppendFiles(List<File> fsFiles, List<FileViewDto> dbFiles) {
        List<File> resp = new ArrayList<>();
        // 添加所有目录
        fsFiles.stream().filter(File::isDirectory).forEach(resp::add);
        // 添加文件
        List<String> existFilesUk = dbFiles.stream().filter(FileViewDto::isFile).map(t -> {
            return String.format("%s_%s_%s", t.getPath(), t.getSize(), t.getModifiedAt());
        }).toList();
        fsFiles.stream().filter(File::isFile).forEach(t -> {
            String uk = String.format("%s_%s_%s", t.getAbsolutePath(), t.length(), t.lastModified());
            if (!existFilesUk.contains(uk)) {
                resp.add(t);
            }
        });
        return resp;
    }

    @Override
    public void asyncDirectories() {
        asyncDirectories(configProperties.getApp().getIndexDirectories(), Collections.emptyList());
    }

    @Override
    public void asyncDirectories(List<ConfigProperties.IndexItem> syncDirectories,
                                 List<ConfigProperties.IndexItem> removeDirectories) {
        if (!CollectionUtils.isEmpty(removeDirectories)) {
            for (ConfigProperties.IndexItem indexItem : removeDirectories) {
                String path = indexItem.getDirectory();
                FileViewDto dbDto = FileViewDto.of(path, true, null, null, null);
                this.removeDirectory(dbDto);
            }
        }
        if (!CollectionUtils.isEmpty(syncDirectories)) {
            for (ConfigProperties.IndexItem indexItem : syncDirectories) {
                File directory = new File(indexItem.getDirectory());
                // 检查文件夹是否存在
                if (!directory.exists()) {
                    continue;
                }
                if (!directory.isDirectory()) {
                    continue;
                }
                CommonsConsts.SYNC_EXECUTOR_SERVICE.submit(() -> processDirectory(directory, indexItem.getExcludesDirectories()));
            }
        }
    }

    @Override
    public void resetIndex() {
        log.info("重置索引 start.");
        // 队列重置
        workQueueService.resetJobs(JobTypeEnum.INDEXED_CONTENT.name());
        // 向量重置
        vectorClient.reset();
        // 索引重置
        fullTextSearchService.getBean().reset();
        configProperties.getApp().getIndexDirectories().forEach(t -> {
            // DB 重置
            int updateCount = fileDbService.updateDirectoryState(t.getDirectory(), FileStateEnum.VALID);
            log.info("重新索引 目录: {} 数量 {}", t, updateCount);
        });
        log.info("重置索引 end.");
    }

    @Override
    public void appendFiles(List<File> files) {
        // 添加执行
        for (File file : files) {
            if (extensionService.findParser(file.getAbsolutePath()).isPresent()) {
                CommonsConsts.SHORT_EXECUTOR_SERVICE.submit(() -> this.processFile(file));
            }
        }
    }

    private void processDirectory(File rootDir, List<String> excludeDirectories) {
        // 检查文件夹
        if (!rootDir.isDirectory()) {
            return;
        }
        // 记录目录
        fileDbService.upsertPath(rootDir.getAbsolutePath());
        // 文件系统 遍历文件和文件夹
        List<File> fsFiles = parseValidSubFsFiles(rootDir, excludeDirectories);
        // 数据库 文件和文件夹
        List<FileViewDto> dbFiles = fileDbService.listFiles(rootDir);
        // 均为空则返回
        if (CollectionUtils.isEmpty(fsFiles) && CollectionUtils.isEmpty(dbFiles)) {
            return;
        }
        // 待操作数据
        List<File> appendFiles = calculateAppendFiles(fsFiles, dbFiles);
        List<FileViewDto> removeFiles = calculateRemoveFiles(fsFiles, dbFiles);
        if (!(CollectionUtils.isEmpty(appendFiles) && CollectionUtils.isEmpty(removeFiles))) {
            // 打印日志
            recordLog(rootDir, removeFiles, appendFiles);
        }
        // 按照文件和目录分别删除
        for (FileViewDto t : removeFiles) {
            try {
                if (t.isFile()) {
                    this.removeFile(t);
                } else if (t.isDirectory()) {
                    this.removeDirectory(t);
                }
            } catch (Exception ex) {
                log.error("process remove error {}", t.getPath(), ex);
            }
        }
        // 按照文件和目录分别添加
        for (File t : appendFiles) {
            if (t.isFile()) {
                CommonsConsts.SYNC_EXECUTOR_SERVICE.submit(() -> this.processFile(t));
            } else if (t.isDirectory()) {
                CommonsConsts.SYNC_EXECUTOR_SERVICE.submit(() -> this.processDirectory(t, excludeDirectories));
            }
        }
    }

    /**
     * 记录日志
     *
     * @param rootDir
     * @param removeFiles
     * @param appendFiles
     */
    private static void recordLog(File rootDir, List<FileViewDto> removeFiles, List<File> appendFiles) {
        String removeDirs = String.join("\n  ", removeFiles.stream().filter(FileViewDto::isDirectory)
                .map(FileViewDto::getPath).toList());
        if (StringUtils.isEmpty(removeDirs)) {
            removeDirs = "无";
        } else {
            removeDirs = "\n" + removeDirs;
        }
        String appendDirs = String.join("\n  ", appendFiles.stream().filter(File::isDirectory)
                .map(File::getAbsolutePath).toList());
        if (StringUtils.isEmpty(appendDirs)) {
            appendDirs = "无";
        } else {
            appendDirs = "\n" + appendDirs;
        }
        log.info("计划处理目录: {}\n添加目录:{}\n移除目录:{}", rootDir.getAbsolutePath(), appendDirs, removeDirs);
    }

    private List<File> parseValidSubFsFiles(File rootDir, List<String> excludeDirectories) {
        // 被排除的目录不会索引
        excludeDirectories = Optional.ofNullable(excludeDirectories).orElse(Collections.emptyList());
        if (excludeDirectories.contains(rootDir.getAbsolutePath())) {
            return Collections.emptyList();
        }
        List<File> fsFiles = Optional.ofNullable(rootDir.listFiles())
                .map(List::of).orElse(Collections.emptyList());
        return fsFiles.stream().filter(t -> {
            return t.isDirectory() || extensionService.findParser(t.getAbsolutePath()).isPresent();
        }).toList();
    }

    private void processFile(File subFile) {
        log.info("追加文件到 db: {}", subFile.getAbsolutePath());
        // 检查文件夹
        if (!subFile.isFile()) {
            return;
        }
        // 读取文件
        FileResWriteDto fsDto = FileResHelper.genFileFsDto(subFile);
        // 追加到DB
        fileDbService.appendFile(fsDto);
    }

    private void removeFile(FileViewDto subFile) {
        log.info("即将从 db 移除文件: {}", subFile.getPath());
        fileDbService.updateFileState(subFile.getFileId(), FileStateEnum.INVALID);
    }

    private void removeDirectory(FileViewDto subDir) {
        log.info("即将从 db 移除目录: {}", subDir.getPath());
        fileDbService.updateDirectoryState(subDir.getPath(), FileStateEnum.INVALID);
    }

}