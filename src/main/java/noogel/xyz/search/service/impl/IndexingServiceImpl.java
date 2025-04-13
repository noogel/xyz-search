package noogel.xyz.search.service.impl;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.client.ElasticClient;
import noogel.xyz.search.infrastructure.client.VectorClient;
import noogel.xyz.search.infrastructure.config.ConfigProperties;
import noogel.xyz.search.infrastructure.consts.CommonsConsts;
import noogel.xyz.search.infrastructure.consts.FileStateEnum;
import noogel.xyz.search.infrastructure.dto.IndexedContentDto;
import noogel.xyz.search.infrastructure.dto.dao.FileResContentDto;
import noogel.xyz.search.infrastructure.dto.dao.FileResReadDto;
import noogel.xyz.search.infrastructure.model.FullTextSearchModel;
import noogel.xyz.search.infrastructure.repo.FullTextSearchService;
import noogel.xyz.search.infrastructure.utils.MD5Helper;
import noogel.xyz.search.service.FileDbService;
import noogel.xyz.search.service.IndexingService;
import noogel.xyz.search.service.VectorProcessService;
import noogel.xyz.search.service.extension.ExtensionService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class IndexingServiceImpl implements IndexingService {

    @Resource
    private FileDbService fileDbService;
    @Resource
    private ExtensionService extensionService;
    @Resource
    private FullTextSearchService fullTextSearchService;
    @Resource
    private VectorProcessService vectorProcessService;
    @Resource
    private ConfigProperties configProperties;
    @Resource
    private ElasticClient elasticClient;
    @Resource
    private VectorClient vectorClient;

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

                        // 提取关键数据避免大对象持有
                        String content = contentDto.genContent();
                        String md5 = MD5Helper.getMD5Stream(content);
                        Map<String, String> metadata = contentDto.getMetadata();
                        FullTextSearchModel ftsModel = buildLuceneModel(t, content, md5, metadata);

                        // 更新 fts
                        fullTextSearchService.getBean().upsert(ftsModel,
                                () -> fileDbService.updateFileState(t.getFieldId(), FileStateEnum.INDEXED));

                        // 异步处理向量
                        if (vectorClient.isEnabled()) {
                            IndexedContentDto indexedContentDto = IndexedContentDto.of(t.getResId(), contentDto);
                            vectorProcessService.asyncUpsert(indexedContentDto);
                        }
                    });
        } catch (Exception ex) {
            log.error("indexFileToEs error {}", t.calFilePath(), ex);
            Map<String, String> options = t.getOptions();
            options.put("error", ex.getMessage());
            fileDbService.updateFileState(t.getFieldId(), FileStateEnum.ERROR, options);
        }
    }

    // 修改 buildLuceneModel 方法参数
    private FullTextSearchModel buildLuceneModel(FileResReadDto t, String content, String md5, Map<String, String> metadata) {
        String title = Optional.ofNullable(metadata)
                .map(m -> m.get("title"))
                .filter(StringUtils::isNotBlank)
                .map(Object::toString)
                .orElse(t.getName());

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
        es.setContentHash(md5);
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
            // 清理索引
            fullTextSearchService.getBean().delete(t.getResId(), () -> {
                // 清理DB
                fileDbService.deleteFile(t.getFieldId());
            });
            if (vectorClient.isEnabled()) {
                // 异步处理向量
                vectorProcessService.asyncDelete(t.getResId());
            }
        } catch (Exception ex) {
            log.error("removeEsAndFile error {}", t.calFilePath(), ex);
            Map<String, String> options = t.getOptions();
            options.put("error", ex.getMessage());
            fileDbService.updateFileState(t.getFieldId(), FileStateEnum.ERROR, options);
        }
    }

}
