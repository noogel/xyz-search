package noogel.xyz.search.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.ConfigProperties;
import noogel.xyz.search.infrastructure.consts.CommonsConsts;
import noogel.xyz.search.infrastructure.consts.FileStateEnum;
import noogel.xyz.search.infrastructure.dto.dao.FileResContentDto;
import noogel.xyz.search.infrastructure.dto.dao.FileResReadDto;
import noogel.xyz.search.infrastructure.model.lucene.FullTextSearchModel;
import noogel.xyz.search.infrastructure.repo.FullTextSearchRepo;
import noogel.xyz.search.infrastructure.utils.MD5Helper;
import noogel.xyz.search.service.FileDbService;
import noogel.xyz.search.service.IndexingService;
import noogel.xyz.search.service.extension.ExtensionService;

@Service
@Slf4j
public class IndexingServiceImpl implements IndexingService {

    @Resource
    private FileDbService fileDbService;
    @Resource
    private ExtensionService extensionService;
    @Resource
    private FullTextSearchRepo fullTextSearchRepo;
    @Resource
    private ConfigProperties configProperties;

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
                List<FileResReadDto> waitReadDtoList = fileDbService.scanFileResByState(FileStateEnum.VALID);
                waitReadDtoList.forEach(this::indexFile);
                if (waitReadDtoList.isEmpty()) {
                    Thread.sleep(configProperties.getRuntime().getDefaultSleepMs());
                } else {
                    Thread.sleep(CommonsConsts.DEFAULT_SCAN_FILE_LIMIT_MS);
                }
            } catch (Exception ex) {
                if (ex instanceof InterruptedException) {
                    break;
                }
                try {
                    Thread.sleep(configProperties.getRuntime().getDefaultSleepMs());
                } catch (InterruptedException e) {
                    break;
                }
                log.error("scanNonIndex item error.", ex);
            }
        }
    }

    private void indexFile(FileResReadDto t) {
        try {
            // 解析子文件
            extensionService.findParser(t.calFilePath())
                    .ifPresent(l -> {
                        // 解析文件
                        FileResContentDto contentDto = l.parseFile(t);
                        if (Objects.isNull(contentDto)) {
                            Map<String, String> options = t.getOptions();
                            options.put("error", "parse file error");
                            fileDbService.updateFileState(t.getFieldId(), FileStateEnum.ERROR, options);
                            return;
                        }
                        FullTextSearchModel fullTextSearchModel = buildLuceneModel(t, contentDto);
                        // 更新全文索引
                        fullTextSearchRepo.upsert(fullTextSearchModel, () -> {
                            // 更新状态
                            fileDbService.updateFileState(t.getFieldId(), FileStateEnum.INDEXED);
                        });
                    });
        } catch (Exception ex) {
            log.error("indexFileToEs error {}", t.calFilePath(), ex);
            Map<String, String> options = t.getOptions();
            options.put("error", ex.getMessage());
            fileDbService.updateFileState(t.getFieldId(), FileStateEnum.ERROR, options);
        }
    }

    private FullTextSearchModel buildLuceneModel(FileResReadDto t, FileResContentDto dto) {
        String content = dto.genContent();
        String title = Optional.ofNullable(dto.getMetadata()).map(l -> l.get("metaTitle"))
                .filter(StringUtils::isNotBlank).orElse(t.getName());
        FullTextSearchModel es = new FullTextSearchModel();
        es.setResId(t.getResId());
        es.setResName(t.getName());
        es.setResTitle(title);
        es.setResRank(t.getRank());
        es.setResDir(t.getDir());
        es.setResHash(t.getHash());
        es.setResType(t.getDir());
        es.setResType(t.getType());
        es.setResSize(t.getSize());
        es.setContent(content);
        es.setContentHash(MD5Helper.getMD5Stream(content));
        es.setContentSize(content.length());
        es.setModifiedAt(t.getModifiedAt());
        return es;
    }

    /**
     * 扫描未索引的文件
     */
    private void scanInvalidFiles() {
        while (true) {
            try {
                List<FileResReadDto> waitDtoList = fileDbService.scanFileResByState(FileStateEnum.INVALID);
                waitDtoList.forEach(this::removeIndexAndFile);
                if (waitDtoList.isEmpty()) {
                    Thread.sleep(configProperties.getRuntime().getDefaultSleepMs());
                } else {
                    Thread.sleep(CommonsConsts.DEFAULT_SCAN_FILE_LIMIT_MS);
                }
            } catch (Exception ex) {
                if (ex instanceof InterruptedException) {
                    break;
                }
                try {
                    Thread.sleep(configProperties.getRuntime().getDefaultSleepMs());
                } catch (InterruptedException e) {
                    break;
                }
                log.error("scanInvalidFiles item error.", ex);
            }
        }
    }

    private void removeIndexAndFile(FileResReadDto t) {
        log.info("移除全文索引和 db 记录 {}", t.calFilePath());
        try {
            // 清理ES
            fullTextSearchRepo.delete(t.getResId(), () -> {
                // 清理DB
                fileDbService.deleteFile(t.getFieldId());
            });
        } catch (Exception ex) {
            log.error("removeEsAndFile error {}", t.calFilePath(), ex);
            Map<String, String> options = t.getOptions();
            options.put("error", ex.getMessage());
            fileDbService.updateFileState(t.getFieldId(), FileStateEnum.ERROR, options);
        }
    }

}
