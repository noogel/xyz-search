package noogel.xyz.search.application.scheduler;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.CommonsConstConfig;
import noogel.xyz.search.infrastructure.config.SearchPropertyConfig;
import noogel.xyz.search.infrastructure.consts.FileStateEnum;
import noogel.xyz.search.infrastructure.dto.dao.FileResReadDto;
import noogel.xyz.search.infrastructure.dto.dao.FileViewDto;
import noogel.xyz.search.infrastructure.event.ConfigAppUpdateEvent;
import noogel.xyz.search.infrastructure.utils.FileHelper;
import noogel.xyz.search.infrastructure.utils.MD5Helper;
import noogel.xyz.search.service.FileDbService;
import noogel.xyz.search.service.SynchronizeService;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CollectServiceScheduler {
    private static final DateTimeFormatter AUTO_COLLECT_FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月");
    private static final AtomicInteger TRANSFER_RUNNING_COUNT = new AtomicInteger();
    private static final List<Pattern> PATTERN = new ArrayList<>();

    @Resource
    private SearchPropertyConfig.SearchConfig searchConfig;
    @Resource
    private SynchronizeService synchronizeService;
    @Resource
    private FileDbService fileDbService;


    @PostConstruct
    public void init() {
        // 初始化正则匹配器
        List<SearchPropertyConfig.CollectItem> itemList = searchConfig.getApp().getCollectDirectories();
        if (CollectionUtils.isEmpty(itemList)) {
            return;
        }
        for (SearchPropertyConfig.CollectItem item : itemList) {
            String filterRegex = item.getFilterRegex();
            if (StringUtils.isNotBlank(filterRegex)) {
                PATTERN.add(Pattern.compile(filterRegex));
            } else {
                PATTERN.add(null);
            }
        }
    }

    @EventListener(ConfigAppUpdateEvent.class)
    public void configAppUpdate(ConfigAppUpdateEvent event) {
        // 更新正则匹配器
        PATTERN.clear();
        List<SearchPropertyConfig.CollectItem> itemList = event.getNewApp().getCollectDirectories();
        if (CollectionUtils.isEmpty(itemList)) {
            return;
        }
        for (SearchPropertyConfig.CollectItem item : itemList) {
            String newRegex = item.getFilterRegex();
            if (StringUtils.isNotBlank(newRegex)) {
                PATTERN.add(Pattern.compile(newRegex));
            } else {
                PATTERN.add(null);
            }
        }
    }

    /**
     * 每天 3 点 diff 一遍文件
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void asyncScanFsFiles() {
        synchronizeService.asyncDirectories();
    }

    /**
     * 每天 8 点 处理异常记录
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void asyncCleanDbErrorFiles() {
        for (int i = 0; i < 1000; i++) {
            List<FileResReadDto> errorRecords = fileDbService.scanFileResByState(FileStateEnum.ERROR);
            if (errorRecords.isEmpty()) {
                break;
            }
            log.info("asyncCleanDbErrorFiles {}", errorRecords.size());
            // 恢复记录状态
            errorRecords.forEach(t -> fileDbService.updateFileState(t.getFieldId(), FileStateEnum.VALID));
        }
    }

    /**
     * 转移收集的文件
     */
    @Scheduled(cron = "0 0 0,6,11,15,20 * * *")
    public void asyncCollectFileIfNotExist() {
        CommonsConstConfig.SHORT_EXECUTOR_SERVICE.submit(this::syncCollectFileIfNotExist);
    }

    public void syncCollectFileIfNotExist() {
        try {
            if (TRANSFER_RUNNING_COUNT.incrementAndGet() > 1) {
                log.warn("transferFileIfNotExist already running {}.", TRANSFER_RUNNING_COUNT.get());
                return;
            }
            List<SearchPropertyConfig.CollectItem> itemList = searchConfig.getApp().getCollectDirectories();
            if (CollectionUtils.isEmpty(itemList)) {
                return;
            }

            for (int i = 0; i < itemList.size(); i++) {
                List<String> fromDirectories = itemList.get(i).getFromList();
                String toDirectory = itemList.get(i).getTo();
                Pattern pattern = PATTERN.get(i);
                boolean autoDelete = BooleanUtils.isTrue(itemList.get(i).getAutoDelete());
                File toDir;
                // 检查配置
                if (CollectionUtils.isEmpty(fromDirectories)
                        || StringUtils.isEmpty(toDirectory)
                        || Objects.isNull(pattern)
                        || !((new File(toDirectory)).exists())) {
                    log.info("transferFileIfNotExist config empty or notExist.");
                    return;
                }
                // 获取或创建子目录
                toDir = getOrMkCollectToSubDir(toDirectory);
                // 遍历目录，转移资源
                Optional.of(fromDirectories).orElse(Collections.emptyList()).forEach(from -> {
                    File fromDir;
                    if (!(fromDir = new File(from)).exists()) {
                        log.info("transferFileIfNotExist fromDir notExist.");
                    }
                    // 拷贝文件
                    List<File> files = copyFilesFromSource(fromDir, toDir, pattern, autoDelete);
                    // 追加到数据库
                    synchronizeService.appendFiles(files);
                });
                log.info("transferFileIfNotExist run complete.");
            }
        } finally {
            TRANSFER_RUNNING_COUNT.decrementAndGet();
        }
    }

    /**
     * 获取或创建目标子目录
     *
     * @param collectToDirectory
     * @return
     */
    private File getOrMkCollectToSubDir(String collectToDirectory) {
        File toDir;
        // 创建子目录
        String subDir = LocalDateTime.now().format(AUTO_COLLECT_FORMATTER);
        if (!collectToDirectory.endsWith("/")) {
            collectToDirectory += "/";
        }
        collectToDirectory += subDir;
        if (!(toDir = new File(collectToDirectory)).exists()) {
            if (toDir.mkdir()) {
                log.info("transferFileIfNotExist mkdir success {}.", collectToDirectory);
            } else {
                log.info("transferFileIfNotExist mkdir fail {}.", collectToDirectory);
            }
        }
        return toDir;
    }

    /**
     * 拷贝文件
     *
     * @param sourceDir
     * @param targetDir
     */
    private List<File> copyFilesFromSource(File sourceDir, File targetDir, Pattern pattern, boolean autoDelete) {
        List<File> sourceFiles = new ArrayList<>();
        List<String> excludeFiles = new ArrayList<>();
        // 遍历文件和文件夹
        // regex filters
        for (File file : FileHelper.parseAllSubFiles(sourceDir)) {
            if (file.exists() && file.isFile()
                    && pattern.matcher(file.getAbsolutePath()).find()) {
                sourceFiles.add(file);
            } else {
                excludeFiles.add(String.format("%s %s %s %s", file, file.exists(),
                        file.isFile(), pattern.matcher(file.getAbsolutePath()).find()));
            }
        }
        // add logs
        log.info("collectNeedFiles sourceFiles:\n{}\ncollectNeedFiles excludeFiles:\n{}",
                sourceFiles.stream().map(String::valueOf).collect(Collectors.joining("\n")),
                excludeFiles.stream().map(String::valueOf).collect(Collectors.joining("\n"))
        );

        List<File> realTargetFiles = new ArrayList<>();
        // 1. 检查文件是否存在 es, md5 去重。
        // 2. 复制文件。
        // 3. 主动添加到 es。
        for (File sourceFile : sourceFiles) {
            String targetPath = targetDir.getAbsolutePath();
            String targetName = sourceFile.getName();
            long sourceFileLength = sourceFile.length();
            File targetFile = new File(String.format("%s/%s", targetPath, targetName));
            int nameFlag = 0;
            // 目标文件名存在
            while (targetFile.exists()) {
                long toFileLength = targetFile.length();
                // 大小一样 则跳过
                if (sourceFileLength == toFileLength) {
                    targetFile = null;
                    break;
                }
                nameFlag++;
                targetFile = new File(String.format("%s/(%s)%s", targetPath, nameFlag, targetName));
            }
            // 目标文件不存在
            if (Objects.nonNull(targetFile)) {
                // 计算原始文件是否存在 ES
                String fromMD5 = MD5Helper.getMD5(sourceFile);
                Optional<FileViewDto> fileDbDto = fileDbService.findFirstByHash(fromMD5);
                if (fileDbDto.isPresent()) {
                    log.info("transferFiles file exist {} md5:{}", sourceFile.getAbsolutePath(), fromMD5);
                    continue;
                }
                // 拷贝文件
                try {
                    Files.copy(sourceFile.toPath(), targetFile.toPath());
                    log.info("transferFile ok {}", sourceFile);
                    // 添加到回填对象
                    realTargetFiles.add(targetFile);
                    // 资源收集后自动删除
                    if (autoDelete) {
                        if (sourceFile.delete()) {
                            log.info("delete collected file: {}", sourceFile.getAbsolutePath());
                        }
                    }
                } catch (IOException e) {
                    log.error("transferFile error {}", sourceFile);
                }
            }
        }
        return realTargetFiles;
    }
}
