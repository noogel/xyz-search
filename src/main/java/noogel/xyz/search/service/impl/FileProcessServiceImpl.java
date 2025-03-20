package noogel.xyz.search.service.impl;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.ConfigProperties;
import noogel.xyz.search.infrastructure.consts.CommonsConsts;
import noogel.xyz.search.infrastructure.consts.FileStateEnum;
import noogel.xyz.search.infrastructure.dto.dao.FileViewDto;
import noogel.xyz.search.infrastructure.event.ConfigAppUpdateEvent;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import noogel.xyz.search.infrastructure.utils.FileHelper;
import noogel.xyz.search.infrastructure.utils.JsonHelper;
import noogel.xyz.search.infrastructure.utils.MD5Helper;
import noogel.xyz.search.service.FileDbService;
import noogel.xyz.search.service.FileProcessService;
import noogel.xyz.search.service.SynchronizeService;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FileProcessServiceImpl implements FileProcessService {
    private static final DateTimeFormatter AUTO_COLLECT_FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月");
    private static final AtomicInteger TRANSFER_RUNNING_COUNT = new AtomicInteger();
    private static final List<Pattern> PATTERN = new ArrayList<>();

    @Resource
    private FileDbService fileDbService;
    @Resource
    private SynchronizeService synchronizeService;
    @Resource
    private ConfigProperties configProperties;


    @PostConstruct
    public void init() {
        // 初始化正则匹配器
        List<ConfigProperties.CollectItem> itemList = configProperties.getApp().getCollectDirectories();
        if (CollectionUtils.isEmpty(itemList)) {
            return;
        }
        for (ConfigProperties.CollectItem item : itemList) {
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
        List<ConfigProperties.CollectItem> itemList = event.getNewApp().getCollectDirectories();
        if (CollectionUtils.isEmpty(itemList)) {
            return;
        }
        for (ConfigProperties.CollectItem item : itemList) {
            String newRegex = item.getFilterRegex();
            if (StringUtils.isNotBlank(newRegex)) {
                PATTERN.add(Pattern.compile(newRegex));
            } else {
                PATTERN.add(null);
            }
        }
    }


    @Override
    public void uploadFile(MultipartFile file, String fileDirectory) {
        try {
            File upDir = getOrMkCollectToSubDir(fileDirectory);
            if (!upDir.exists()) {
                upDir.mkdirs();
            }
            Path realPath;
            int flag = 0;
            while (true) {
                ExceptionCode.FILE_ACCESS_ERROR.throwOn(flag > 20, "重名文件太多");
                String prefix = flag > 0 ? String.format("(%s)", flag) : "";
                realPath = Paths.get(upDir.getAbsolutePath())
                        .resolve(prefix + Objects.requireNonNull(file.getOriginalFilename()))
                        .normalize().toAbsolutePath();
                ExceptionCode.FILE_ACCESS_ERROR.throwOn(!realPath.startsWith(upDir.getAbsolutePath()), "目录错误");
                if (!realPath.toFile().exists()) {
                    break;
                }
                flag++;
            }
            // 保存文件，并重命名
            Path tmpPath = Paths.get(upDir.getAbsolutePath())
                    .resolve(realPath.getName(realPath.getNameCount() - 1) + CommonsConsts.FILE_SUFFIX)
                    .normalize().toAbsolutePath();
            Files.copy(file.getInputStream(), tmpPath);
            String md5 = MD5Helper.getMD5(tmpPath.toFile());
            // 文件存在则删除文件
            if (fileDbService.findFirstByHash(md5).isPresent()) {
                Files.deleteIfExists(tmpPath);
                throw ExceptionCode.RUNTIME_ERROR.throwExc("文件已经存在");
            }
            // 重命名
            Files.move(tmpPath, realPath);
            // 同步文件
            synchronizeService.appendFiles(Collections.singletonList(realPath.toFile()));
        } catch (IOException e) {
            throw ExceptionCode.RUNTIME_ERROR.throwExc(e.getMessage());
        }
    }


    @Override
    public void syncCollectFileIfNotExist() {
        try {
            if (TRANSFER_RUNNING_COUNT.incrementAndGet() > 1) {
                log.warn("transferFileIfNotExist already running {}.", TRANSFER_RUNNING_COUNT.get());
                return;
            }
            List<ConfigProperties.CollectItem> itemList = configProperties.getApp().getCollectDirectories();
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
                    log.info("文件收集:来源目录为空 或 目标目录为空 或 目录不存在: \n{}", JsonHelper.toJson(itemList.get(i)));
                    return;
                }
                // 获取或创建子目录
                toDir = getOrMkCollectToSubDir(toDirectory);
                // 遍历目录，转移资源
                Optional.of(fromDirectories).orElse(Collections.emptyList()).forEach(from -> {
                    File fromDir;
                    if (!(fromDir = new File(from)).exists()) {
                        log.info("文件收集:原始目录不存在: {}", from);
                    }
                    // 拷贝文件
                    List<File> files = copyFilesFromSource(fromDir, toDir, pattern, autoDelete);
                    // 追加到数据库
                    synchronizeService.appendFiles(files);
                });
                log.info("文件收集:执行完成");
            }
        } finally {
            TRANSFER_RUNNING_COUNT.decrementAndGet();
        }
    }

    @Override
    public void fileMarkDelete(String resId) {
        ExceptionCode.CONFIG_ERROR.throwOn(StringUtils.isBlank(configProperties.getApp().getMarkDeleteDirectory()),
                "需要先配置标记清理目录");
        fileDbService.findByResIdFilterState(resId, FileStateEnum.INDEXED).ifPresent(t -> {
            // 转移文件
            Path sourcePath = Paths.get(t.calFilePath());
            ExceptionCode.FILE_ACCESS_ERROR.throwOn(!sourcePath.toFile().exists(), "文件不存在");
            Path targetPath;
            int flag = 0;
            while (true) {
                ExceptionCode.FILE_ACCESS_ERROR.throwOn(flag > 200, "重名文件太多");
                String prefix = flag > 0 ? String.format("(%s)", flag) : "";
                targetPath = Paths.get(configProperties.getApp().getMarkDeleteDirectory())
                        .resolve(prefix + t.getName())
                        .normalize().toAbsolutePath();
                if (!targetPath.toFile().exists()) {
                    break;
                }
                flag++;
            }
            try {
                Path descPath = Paths.get(configProperties.getApp().getMarkDeleteDirectory())
                        .resolve(targetPath.getFileName() + ".转移说明.txt")
                        .normalize().toAbsolutePath();
                try (BufferedWriter writer = Files.newBufferedWriter(descPath, StandardCharsets.UTF_8)) {
                    writer.write(Objects.requireNonNull(JsonHelper.toJson(t)));
                }
                Files.move(sourcePath, targetPath);
            } catch (IOException e) {
                log.error("fileMarkDelete error.", e);
                throw ExceptionCode.FILE_ACCESS_ERROR.throwExc("标记删除，文件转移异常");
            }
            // 标记失效
            fileDbService.updateFileState(t.getFieldId(), FileStateEnum.INVALID);
        });
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
                log.info("文件收集:创建目标目录成功 {}.", collectToDirectory);
            } else {
                log.error("文件收集:创建目标目录失败 {}.", collectToDirectory);
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
        log.info("计划添加文件记录:\n{}\n计划移除文件记录:\n{}",
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
                    log.warn("文件收集:待转移文件已存在: {} md5:{}", sourceFile.getAbsolutePath(), fromMD5);
                    continue;
                }
                // 拷贝文件
                try {
                    Files.copy(sourceFile.toPath(), targetFile.toPath());
                    log.info("文件收集:完成 {}", sourceFile);
                    // 添加到回填对象
                    realTargetFiles.add(targetFile);
                    // 资源收集后自动删除
                    if (autoDelete) {
                        if (sourceFile.delete()) {
                            log.info("文件收集:原始文件已删除: {}", sourceFile.getAbsolutePath());
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
