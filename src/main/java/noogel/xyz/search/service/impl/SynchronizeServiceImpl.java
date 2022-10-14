package noogel.xyz.search.service.impl;

import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.CommonsConstConfig;
import noogel.xyz.search.infrastructure.config.SearchPropertyConfig;
import noogel.xyz.search.infrastructure.dao.ElasticSearchFtsDao;
import noogel.xyz.search.infrastructure.dto.SearchResultDto;
import noogel.xyz.search.infrastructure.dto.TaskDto;
import noogel.xyz.search.infrastructure.model.ResourceModel;
import noogel.xyz.search.infrastructure.utils.MD5Helper;
import noogel.xyz.search.service.SynchronizeService;
import noogel.xyz.search.service.extension.ExtensionPointService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SynchronizeServiceImpl implements SynchronizeService {

    @Resource
    private List<ExtensionPointService> extServices;
    @Resource
    private ElasticSearchFtsDao ftsDao;
    @Resource
    private SearchPropertyConfig.SearchConfig searchConfig;

    @Override
    public void async(List<String> paths) {
        CommonsConstConfig.EXECUTOR_SERVICE.submit(() -> syncProcessDirectory(paths));
    }

    @Override
    public void asyncAll() {
        List<String> paths = searchConfig.getSearchDirectories();
        if (CollectionUtils.isEmpty(paths)) {
            return;
        }
        CommonsConstConfig.EXECUTOR_SERVICE.submit(() -> syncProcessDirectory(paths));
    }

    @Override
    public boolean resetIndex() {
        return ftsDao.createIndex(true);
    }


    private void syncProcessDirectory(List<String> paths) {
        TaskDto taskDto = TaskDto.generateTask();
        for (String path : paths) {
            // 只允许索引配置的文件夹或子文件夹
            boolean match = searchConfig.getSearchDirectories().stream().anyMatch(t -> t.startsWith(path));
            if (!match) {
                log.info("asyncPath not Configured: {}", path);
                continue;
            }
            // 检查文件夹是否存在
            File file = new File(path);
            if (!file.exists()) {
                continue;
            }
            processDirectory(file, taskDto);
        }
        CommonsConstConfig.DELAY_EXECUTOR_SERVICE.schedule(
                ()-> delayCleanOldRes("", taskDto.getTaskOpAt()), 60, TimeUnit.SECONDS);
    }

    private void processDirectory(File rootFile, TaskDto taskDto) {
        log.info("processTask will process {} {}", rootFile.getAbsolutePath(), taskDto);
        // 检查文件夹
        if (!rootFile.isDirectory()) {
            return;
        }
        // 遍历文件和文件夹
        File[] subFiles = rootFile.listFiles();
        if (Objects.isNull(subFiles)) {
            return;
        }

        // 处理所有文件和文件夹
        for (File subFile : subFiles) {
            try {
                // 解析子文件
                extServices.stream().filter(t -> t.supportFile(subFile)).findFirst()
                        .ifPresent(t -> {
                            ResourceModel res = ftsDao.findByResId(MD5Helper.getMD5(subFile.getAbsolutePath()));
                            // 优先判断文件更新时间和大小
                            if (Objects.nonNull(res)
                                    && subFile.lastModified() == res.getModifiedAt()
                                    && subFile.length() == res.getResSize()) {
                                res.updateTask(taskDto);
                                log.info("processTask.useExistRes {} {} {}", res.getResId(),
                                        subFile.getAbsolutePath(), taskDto);
                            } else {
                                res = t.parseFile(subFile, taskDto);
                                log.info("processTask.parseFile {} {} {}", res.getResId(),
                                        subFile.getAbsolutePath(), taskDto);
                            }
                            ftsDao.upsertData(res);
                        });
                // 子目录索引
                if (subFile.isDirectory()) {
                    processDirectory(subFile, taskDto);
                }
            } catch (Exception ex) {
                log.error("processTask error {}", subFile.getAbsolutePath(), ex);
            }
        }
        String rootPath = rootFile.getAbsolutePath();
        Long taskOpAt = taskDto.getTaskOpAt();
        CommonsConstConfig.DELAY_EXECUTOR_SERVICE.schedule(
                ()-> delayCleanOldRes(rootPath, taskOpAt), 10, TimeUnit.SECONDS);
    }

    private void delayCleanOldRes(String rootDir, Long taskOpAt) {
        SearchResultDto oldRes = ftsDao.searchOldRes(rootDir, taskOpAt);
        while (!oldRes.getData().isEmpty()) {
            for (ResourceModel res : oldRes.getData()) {
                File file = new File(res.calculateAbsolutePath());
                if (file.exists()) {
                    log.warn("delayCleanOldRes fileExist {}", res.calculateAbsolutePath());
                }
                ftsDao.deleteByResId(res);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            oldRes = ftsDao.searchOldRes(rootDir, taskOpAt);
        }
    }

}
