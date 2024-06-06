package noogel.xyz.search.service.impl;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.CommonsConsts;
import noogel.xyz.search.infrastructure.config.SearchPropertyConfig;
import noogel.xyz.search.infrastructure.consts.FileStateEnum;
import noogel.xyz.search.infrastructure.dao.elastic.ElasticDao;
import noogel.xyz.search.infrastructure.dto.dao.FileResContentDto;
import noogel.xyz.search.infrastructure.dto.dao.FileResReadDto;
import noogel.xyz.search.infrastructure.model.elastic.FileEsModel;
import noogel.xyz.search.infrastructure.utils.MD5Helper;
import noogel.xyz.search.service.FileDbService;
import noogel.xyz.search.service.TickService;
import noogel.xyz.search.service.extension.ExtensionService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class TickServiceImpl implements TickService {

    @Resource
    private FileDbService fileDbService;
    @Resource
    private ExtensionService extensionService;
    @Resource
    private ElasticDao elasticDao;
    @Resource
    private SearchPropertyConfig.SearchConfig searchConfig;

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
                waitReadDtoList.forEach(this::indexFileToEs);
                if (waitReadDtoList.isEmpty()) {
                    Thread.sleep(CommonsConsts.SLEEP_SEC_MS);
                } else {
                    Long indexLimit = Optional.ofNullable(searchConfig.getApp().getIndexLimitMs()).orElse(10L);
                    Thread.sleep(indexLimit);
                }
            } catch (Exception ex) {
                if (ex instanceof InterruptedException) {
                    break;
                }
                try {
                    Thread.sleep(60_000);
                } catch (InterruptedException e) {
                    break;
                }
                log.error("scanNonIndex item error.", ex);
            }
        }
    }

    private void indexFileToEs(FileResReadDto t) {
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
                        // ES 文件
                        FileEsModel fileEsModel = buildEsModel(t, contentDto);
                        // 同步到 es
                        elasticDao.upsertData(fileEsModel);
                        // 更新状态
                        fileDbService.updateFileState(t.getFieldId(), FileStateEnum.INDEXED);
                    });
        } catch (Exception ex) {
            log.error("indexFileToEs error {}", t.calFilePath(), ex);
            Map<String, String> options = t.getOptions();
            options.put("error", ex.getMessage());
            fileDbService.updateFileState(t.getFieldId(), FileStateEnum.ERROR, options);
        }
    }

    private FileEsModel buildEsModel(FileResReadDto t, FileResContentDto dto) {
        String content = dto.genContent();
        String title = Optional.ofNullable(dto.getMetaTitle())
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
                List<FileResReadDto> waitDtoList = fileDbService.scanFileResByState(FileStateEnum.INVALID);
                waitDtoList.forEach(this::removeEsAndFile);
                if (waitDtoList.isEmpty()) {
                    Thread.sleep(CommonsConsts.SLEEP_SEC_MS);
                } else {
                    Long indexLimit = Optional.ofNullable(searchConfig.getApp().getIndexLimitMs()).orElse(10L);
                    Thread.sleep(indexLimit);
                }
            } catch (Exception ex) {
                if (ex instanceof InterruptedException) {
                    break;
                }
                try {
                    Thread.sleep(60_000);
                } catch (InterruptedException e) {
                    break;
                }
                log.error("scanInvalidFiles item error.", ex);
            }
        }
    }

    private void removeEsAndFile(FileResReadDto t) {
        log.info("removeEsAndFile {}", t.calFilePath());
        try {
            // 清理ES
            if (elasticDao.deleteByResId(t.getResId())) {
                // 清理DB
                fileDbService.deleteFile(t.getFieldId());
            }
        } catch (Exception ex) {
            log.error("removeEsAndFile error {}", t.calFilePath(), ex);
            Map<String, String> options = t.getOptions();
            options.put("error", ex.getMessage());
            fileDbService.updateFileState(t.getFieldId(), FileStateEnum.ERROR, options);
        }
    }

}
