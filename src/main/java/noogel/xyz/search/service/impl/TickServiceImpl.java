package noogel.xyz.search.service.impl;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.CommonsConstConfig;
import noogel.xyz.search.infrastructure.consts.FileStateEnum;
import noogel.xyz.search.infrastructure.dao.elastic.ElasticDao;
import noogel.xyz.search.infrastructure.dto.dao.FileResContentDto;
import noogel.xyz.search.infrastructure.dto.dao.FileResReadDto;
import noogel.xyz.search.infrastructure.model.elastic.FileEsModel;
import noogel.xyz.search.infrastructure.utils.MD5Helper;
import noogel.xyz.search.service.FileDbService;
import noogel.xyz.search.service.TickService;
import noogel.xyz.search.service.extension.ExtensionPointService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class TickServiceImpl implements TickService {

    @Resource
    private FileDbService fileDbService;
    @Resource
    private List<ExtensionPointService> extServices;
    @Resource
    private ElasticDao ftsDao;

    @PostConstruct
    public void init() {
        // 处理文件索引
        Thread indexingThread = new Thread(this::scanNonIndexFiles);
        indexingThread.start();
        // 清理文件索引
        Thread invalidThread = new Thread(this::scanInvalidFiles);
        invalidThread.start();
    }

    /**
     * 扫描未索引的文件
     */
    private void scanNonIndexFiles() {
        while (true) {
            try {
                List<Long> waitIds = fileDbService.scanFileResByState(FileStateEnum.VALID);
                List<CompletableFuture<Void>> data = new ArrayList<>();
                waitIds.forEach(id -> {
                    data.add(CompletableFuture.runAsync(
                            () -> this.indexFileToEs(id), CommonsConstConfig.TICK_SCAN_EXECUTOR_SERVICE));
                });
                CompletableFuture.allOf(data.toArray(new CompletableFuture[0])).join();
                if (waitIds.isEmpty()) {
                    log.info("scanNonIndex item empty.");
                    Thread.sleep(CommonsConstConfig.SLEEP_SEC_MS);
                }
            } catch (Exception ex) {
                if (ex instanceof InterruptedException) {
                    break;
                }
                log.error("scanNonIndex item error.", ex);
            }
        }
    }

    private void indexFileToEs(Long id) {
        fileDbService.findByIdFilterState(id, FileStateEnum.VALID).ifPresent(t -> {
            File file = new File(String.format("%s/%s", t.getDir(), t.getName()));
            log.info("indexFileToEs {}", file.getAbsolutePath());
            // 检查是否文件
            if (!file.isFile()) {
                return;
            }
            try {
                // 解析子文件
                extServices.stream()
                        .filter(l -> l.supportFile(file))
                        .findFirst()
                        .ifPresent(l -> {
                            // 解析文件
                            FileResContentDto contentDto = l.parseFile(t);
                            if (Objects.isNull(contentDto)) {
                                return;
                            }
                            // ES 文件
                            FileEsModel fileEsModel = buildEsModel(t, contentDto);
                            // 同步到 es
                            ftsDao.upsertData(fileEsModel);
                            // 更新状态
                            fileDbService.updateFileState(t.getFieldId(), FileStateEnum.INDEXED);
                        });
            } catch (Exception ex) {
                log.error("indexFileToEs error {}", file.getAbsolutePath(), ex);
                fileDbService.updateFileState(t.getFieldId(), FileStateEnum.ERROR);
            }
        });
    }

    private FileEsModel buildEsModel(FileResReadDto t, FileResContentDto dto) {
        String content = dto.genContent();
        String title = Optional.ofNullable(t.getOptions().get("title"))
                .filter(StringUtils::isNotBlank).orElse(t.getName());
        FileEsModel es = new FileEsModel();
        es.setResId(t.getResId());
        es.setResName(t.getName());
        es.setResTitle(title);
        es.setRank(t.getRank());
        es.setResDir(t.getDir());
        es.setResHash(t.getHash());
        es.setResType(t.getDir());
        es.setResType(t.getType());
        es.setResSize(t.getSize());
        es.setModifiedAt(t.getModifiedAt());
        es.setSearchableText(content);
        es.setTextHash(MD5Helper.getMD5(content));
        es.setTextSize(content.length());
        return es;
    }


    /**
     * 扫描未索引的文件
     */
    private void scanInvalidFiles() {
        while (true) {
            try {
                List<Long> waitIds = fileDbService.scanFileResByState(FileStateEnum.INVALID);
                List<CompletableFuture<Void>> data = new ArrayList<>();
                waitIds.forEach(id -> {
                    data.add(CompletableFuture.runAsync(
                            () -> this.removeEsAndFile(id), CommonsConstConfig.TICK_SCAN_EXECUTOR_SERVICE));
                });
                CompletableFuture.allOf(data.toArray(new CompletableFuture[0])).join();
                if (waitIds.isEmpty()) {
                    log.info("scanInvalidFiles item empty.");
                    Thread.sleep(CommonsConstConfig.SLEEP_SEC_MS);
                }
            } catch (Exception ex) {
                if (ex instanceof InterruptedException) {
                    break;
                }
                log.error("scanInvalidFiles item error.", ex);
            }
        }
    }

    private void removeEsAndFile(Long id) {
        fileDbService.findByIdFilterState(id, FileStateEnum.INVALID).ifPresent(t -> {
            File file = new File(String.format("%s/%s", t.getDir(), t.getName()));
            log.info("removeEsAndFile {}", file.getAbsolutePath());
            try {
                // 清理ES
                ftsDao.deleteByResId(t.getResId());
                // 清理DB
                fileDbService.removeFile(t.getFieldId());
            } catch (Exception ex) {
                log.error("removeEsAndFile error {}", file.getAbsolutePath(), ex);
                fileDbService.updateFileState(t.getFieldId(), FileStateEnum.ERROR);
            }
        });
    }

}
