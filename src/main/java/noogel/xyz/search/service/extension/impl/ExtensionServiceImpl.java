package noogel.xyz.search.service.extension.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.SearchPropertyConfig;
import noogel.xyz.search.infrastructure.consts.FileExtEnum;
import noogel.xyz.search.infrastructure.consts.FileProcessClassEnum;
import noogel.xyz.search.service.extension.ExtensionParserService;
import noogel.xyz.search.service.extension.ExtensionPointService;
import noogel.xyz.search.service.extension.ExtensionService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ExtensionServiceImpl implements ExtensionService {

    @Resource
    private SearchPropertyConfig.SearchConfig searchConfig;

    @Resource
    private List<ExtensionPointService> pointServiceList;

    @Override
    public Map<FileProcessClassEnum, Set<FileExtEnum>> fileExtensions() {
        return pointServiceList.stream().collect(Collectors.toMap(
                ExtensionPointService::getFileClass,
                ExtensionPointService::getSupportParseFileExtension
        ));
    }

    @Override
    public Optional<ExtensionParserService> findParser(String filePath) {
        Set<String> excludeExtensionConfig = excludeFileClassConfig();
        return pointServiceList.stream()
                // 支持的文件类型
                .filter(l -> l.supportFile(filePath))
                // 排除移除的处理类
                .filter(l -> !excludeExtensionConfig.contains(l.getFileClass().name()))
                // 转换类型
                .map(l -> (ExtensionParserService) l)
                .findFirst();
    }

    private Set<String> excludeFileClassConfig() {
        try {
            String excludeExtension = searchConfig.getApp().getExcludeFileProcessClass();
            if (StringUtils.isBlank(excludeExtension)) {
                return Collections.emptySet();
            }
            return Set.of(excludeExtension.split(";")).stream()
                    .map(String::toUpperCase).collect(Collectors.toSet());
        } catch (Exception ex) {
            log.error("excludeExtensionConfig error.", ex);
        }
        return Collections.emptySet();
    }
}
