package noogel.xyz.search.service.impl;

import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.SearchPropertyConfig;
import noogel.xyz.search.infrastructure.dto.SearchSettingDto;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import noogel.xyz.search.service.SettingService;
import noogel.xyz.search.service.SynchronizeService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SettingServiceImpl implements SettingService {

    @Resource
    private SearchPropertyConfig.SearchConfig searchConfig;
    @Resource
    private SynchronizeService synchronizeService;

    @Override
    public SearchSettingDto query() {
        SearchSettingDto dto = new SearchSettingDto();
        BeanUtils.copyProperties(searchConfig, dto, "searchDirectories", "elasticsearchHost");
        dto.setElasticsearchHost(searchConfig.getElasticsearchHost().split("://")[1]);
        dto.setSearchDirectories(String.join("\n", Optional.ofNullable(searchConfig.getSearchDirectories())
                .orElse(Collections.emptyList())));
        return dto;
    }

    @Override
    public SearchSettingDto update(SearchSettingDto cfg) {
        // 旧目录
        List<String> oldDirList = new ArrayList<>(searchConfig.getSearchDirectories());
        // 新增目录
        List<String> newDirList = new ArrayList<>();

        // 校验数据
        ExceptionCode.CONFIG_ERROR.throwOn(StringUtils.isBlank(cfg.getElasticsearchHost()), "Elasticsearch host 不能为空");
        // 校验证书
        if (StringUtils.isNotBlank(cfg.getElasticsearchCAPath())) {
            File file = new File(cfg.getElasticsearchCAPath());
            ExceptionCode.CONFIG_ERROR.throwOn(!file.exists() || !file.isFile(),
                    String.format("证书文件 %s 不存在", cfg.getElasticsearchCAPath()));
        }
        // 校验目录
        String dirs = Optional.ofNullable(cfg.getSearchDirectories()).orElse("");
        List<String> searchDirectories = Arrays.stream(dirs.split("[\n, \r]"))
                .filter(StringUtils::isNotBlank)
                .peek(k -> {
                    File file = new File(k);
                    ExceptionCode.CONFIG_ERROR.throwOn(!file.exists() || !file.isDirectory(),
                            String.format("目录 %s 不存在", k));
                    if (!oldDirList.contains(k)) {
                        newDirList.add(k);
                    }
                }).collect(Collectors.toList());
        oldDirList.removeAll(searchDirectories);

        // 更新数据
        String elasticsearchHost = cfg.getElasticsearchHost();
        if (StringUtils.isNotBlank(cfg.getElasticsearchCAPath())) {
            elasticsearchHost = "https://" + elasticsearchHost;
        } else {
            elasticsearchHost = "http://" + elasticsearchHost;
        }
        searchConfig.setElasticsearchHost(elasticsearchHost);
        searchConfig.setElasticsearchUser(cfg.getElasticsearchUser());
        searchConfig.setElasticsearchPassword(cfg.getElasticsearchPassword());
        searchConfig.setElasticsearchCAPath(cfg.getElasticsearchCAPath());
        searchConfig.setSearchDirectories(searchDirectories);
        // 保存配置
        searchConfig.saveToFile();

        // 同步新目录
        synchronizeService.async(newDirList);
        // TODO: 2022/10/7 删除被移除的目录
        // do...
        return query();
    }
}
