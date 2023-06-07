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

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
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

    @PostConstruct
    public void init() {
        // 执行时间点
        long dailyRunAt = 5 * 60 * 60;
        // 一天时间
        long oneDay = 24 * 60 * 60;
        // 今天已经过去多久
        long todayPast = LocalDateTime.now().getHour() * 3600 + LocalDateTime.now().getMinute() * 60 + LocalDateTime.now().getSecond();
        long initialDelay = todayPast > dailyRunAt ? (oneDay - todayPast + dailyRunAt) : (dailyRunAt - todayPast);
        log.info("auto forceMerge runDelay {}ms", initialDelay);
        // 定时执行
        CommonsConstConfig.COMMON_SCHEDULED_SERVICE.scheduleAtFixedRate(
                ()-> ftsDao.forceMerge(),
                initialDelay,
                oneDay,
                TimeUnit.SECONDS
        );
    }

    @Override
    public void async(List<String> paths) {
        CommonsConstConfig.EXECUTOR_SERVICE.submit(() -> syncProcessDirectory(paths));
    }

    @Override
    public void asyncAll() {
        List<String> paths = searchConfig.getApp().getSearchDirectories();
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
            boolean match = searchConfig.getApp().getSearchDirectories().stream().anyMatch(t -> t.startsWith(path));
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
        List<CompletableFuture<Void>> subResFutureList = new ArrayList<>();
        List<File> subDirList = new ArrayList<>();
        // 分类
        for (File subFile : subFiles) {
            if (subFile.isFile()) {
                subResFutureList.add(CompletableFuture.runAsync(()-> this.processFile(subFile, taskDto),
                        CommonsConstConfig.MULTI_EXECUTOR_SERVICE));
            } else if (subFile.isDirectory()) {
                subDirList.add(subFile);
            }
        }
        // 并发阻塞
        CompletableFuture.allOf(subResFutureList.toArray(CompletableFuture[]::new)).join();

        // 同步
        for (File subDir : subDirList) {
            try{
                // 子目录索引
                processDirectory(subDir, taskDto);
            } catch (Exception ex) {
                log.error("processTask error {}", subDir.getAbsolutePath(), ex);
            }
        }

        String rootPath = rootFile.getAbsolutePath();
        Long taskOpAt = taskDto.getTaskOpAt();
        CommonsConstConfig.DELAY_EXECUTOR_SERVICE.schedule(
                ()-> delayCleanOldRes(rootPath, taskOpAt), 10, TimeUnit.SECONDS);
    }

    private void processFile(File subFile, TaskDto taskDto) {
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
        } catch (Exception ex) {
            log.error("processTask error {}", subFile.getAbsolutePath(), ex);
        }
    }

    private void delayCleanOldRes(String rootDir, Long taskOpAt) {
        SearchResultDto oldRes = ftsDao.searchOldRes(rootDir, taskOpAt);
        log.info("delayCleanOldRes {} {}", rootDir, oldRes.getSize());
        while (!oldRes.getData().isEmpty()) {
            for (ResourceModel res : oldRes.getData()) {
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
