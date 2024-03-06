package noogel.xyz.search.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.CommonsConstConfig;
import noogel.xyz.search.infrastructure.config.SearchPropertyConfig;
import noogel.xyz.search.infrastructure.consts.FileStateEnum;
import noogel.xyz.search.infrastructure.dao.ElasticSearchFtsDao;
import noogel.xyz.search.infrastructure.dto.dao.FileDbDto;
import noogel.xyz.search.infrastructure.dto.dao.FileFsDto;
import noogel.xyz.search.service.FileDbService;
import noogel.xyz.search.service.SynchronizeService;
import noogel.xyz.search.service.extension.ExtensionPointService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class SynchronizeServiceImpl implements SynchronizeService {

    @Resource
    private List<ExtensionPointService> extServices;
    @Resource
    private ElasticSearchFtsDao ftsDao;
    @Resource
    private FileDbService fileDbService;
    @Resource
    private SearchPropertyConfig.SearchConfig searchConfig;

    private static List<FileDbDto> calculateRemoveFiles(List<File> fsFiles, List<FileDbDto> dbFiles) {
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

    private static List<File> calculateAppendFiles(List<File> fsFiles, List<FileDbDto> dbFiles) {
        List<File> resp = new ArrayList<>();
        // 添加所有目录
        fsFiles.stream().filter(File::isDirectory).forEach(resp::add);
        // 添加文件
        List<String> existFilesUk = dbFiles.stream().filter(FileDbDto::isFile).map(t -> {
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
        asyncDirectories(searchConfig.getApp().getSearchDirectories(), Collections.emptyList());
    }

    @Override
    public void asyncDirectories(List<String> syncDirectories, List<String> removeDirectories) {
        if (!CollectionUtils.isEmpty(syncDirectories)) {
            for (String path : syncDirectories) {
                // 检查文件夹是否存在
                File file = new File(path);
                if (!file.exists()) {
                    continue;
                }
                if (!file.isDirectory()) {
                    continue;
                }
                CommonsConstConfig.EXECUTOR_SERVICE.submit(() -> processDirectory(file));
            }
        }
        if (!CollectionUtils.isEmpty(removeDirectories)) {
            for (String path : removeDirectories) {
                FileDbDto dbDto = FileDbDto.of(path, true, null, null, null);
                CommonsConstConfig.EXECUTOR_SERVICE.submit(() -> removeDirectory(dbDto));
            }
        }
    }

    @Override
    public void resetIndex() {
        ftsDao.createIndex(true);
    }

    @Override
    public void appendFiles(List<File> files) {
        List<CompletableFuture<Void>> subResFutureList = new ArrayList<>();
        // 添加执行
        for (File file : files) {
            subResFutureList.add(CompletableFuture.runAsync(() -> this.processFile(file),
                    CommonsConstConfig.MULTI_EXECUTOR_SERVICE));
        }
        // 并发阻塞
        CompletableFuture.allOf(subResFutureList.toArray(CompletableFuture[]::new)).join();
    }

    private void processDirectory(File rootDir) {
        log.info("processDirectory will process {}", rootDir.getAbsolutePath());
        // 检查文件夹
        if (!rootDir.isDirectory()) {
            return;
        }
        // 文件系统 遍历文件和文件夹
        List<File> fsFiles = parseValidSubFsFiles(rootDir);
        // 数据库 文件和文件夹
        List<FileDbDto> dbFiles = fileDbService.listFiles(rootDir);
        // 均为空则返回
        if (CollectionUtils.isEmpty(fsFiles) && CollectionUtils.isEmpty(dbFiles)) {
            return;
        }
        // 待操作数据
        List<File> appendFiles = calculateAppendFiles(fsFiles, dbFiles);
        List<FileDbDto> removeFiles = calculateRemoveFiles(fsFiles, dbFiles);
        log.info("processDirectory\n\nappend:\n  {} \nremove:\n  {}",
                String.join("\n  ", appendFiles.stream().map(File::getAbsolutePath).toList()),
                String.join("\n  ", removeFiles.stream().map(FileDbDto::getPath).toList()));
        // 按照文件和目录分别添加
        for (File t : appendFiles) {
            if (t.isFile()) {
                CommonsConstConfig.MULTI_EXECUTOR_SERVICE.submit(() -> this.processFile(t));
            } else if (t.isDirectory()) {
                CommonsConstConfig.MULTI_EXECUTOR_SERVICE.submit(() -> this.processDirectory(t));
            }
        }
        // 按照文件和目录分别删除
        for (FileDbDto t : removeFiles) {
            if (t.isFile()) {
                CommonsConstConfig.MULTI_EXECUTOR_SERVICE.submit(() -> this.removeFile(t));
            } else if (t.isDirectory()) {
                CommonsConstConfig.MULTI_EXECUTOR_SERVICE.submit(() -> this.removeDirectory(t));
            }
        }
    }

    private List<File> parseValidSubFsFiles(File rootDir) {
        List<File> fsFiles = Optional.ofNullable(rootDir.listFiles())
                .map(List::of).orElse(Collections.emptyList());
        return fsFiles.stream().filter(t -> {
            return t.isDirectory() || extServices.stream().anyMatch(l -> l.supportFile(t));
        }).toList();
    }

    private void processFile(File subFile) {
        log.info("processTask will process {}", subFile.getAbsolutePath());
        // 检查文件夹
        if (!subFile.isFile()) {
            return;
        }
        try {
            // 解析子文件
            extServices.stream().filter(t -> t.supportFile(subFile)).findFirst()
                    .ifPresent(t -> {
                        FileFsDto fsDto = t.parseFile(subFile);
                        fileDbService.appendFile(fsDto);
                    });
        } catch (Exception ex) {
            log.error("processTask error {}", subFile.getAbsolutePath(), ex);
        }
    }

    private void removeFile(FileDbDto subFile) {
        fileDbService.updateFileState(subFile, FileStateEnum.INVALID);
    }

    private void removeDirectory(FileDbDto subDir) {
        fileDbService.updateDirectoryState(subDir, FileStateEnum.INVALID);
    }
}
