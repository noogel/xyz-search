package noogel.xyz.search.service.impl;

import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.SearchPropertyConfig;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import noogel.xyz.search.service.SettingService;
import noogel.xyz.search.service.SynchronizeService;
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
    public boolean update(Map<String, String> cfg) {
        // 旧目录
        List<String> oldDirList = new ArrayList<>(searchConfig.getSearchDirectories());
        // 新增目录
        List<String> newDirList = new ArrayList<>();

        Optional.ofNullable(cfg.get("elasticsearchHost")).ifPresent(searchConfig::setElasticsearchHost);
        Optional.ofNullable(cfg.get("elasticsearchUser")).ifPresent(searchConfig::setElasticsearchUser);
        Optional.ofNullable(cfg.get("elasticsearchPassword")).ifPresent(searchConfig::setElasticsearchPassword);
        Optional.ofNullable(cfg.get("elasticsearchCAPath")).ifPresent(searchConfig::setElasticsearchCAPath);
        Optional.ofNullable(cfg.get("elasticsearchConnectionTimeout")).map(Integer::parseInt)
                .ifPresent(searchConfig::setElasticsearchConnectionTimeout);
        Optional.ofNullable(cfg.get("elasticsearchSocketTimeout")).map(Integer::parseInt)
                .ifPresent(searchConfig::setElasticsearchSocketTimeout);
        Optional.ofNullable(cfg.get("searchDirectories")).map(t-> {
            List<String> searchDirectories = Arrays.asList(t.split("\n")).stream().peek(k-> {
                File file = new File(k);
                ExceptionCode.CONFIG_ERROR.throwOn(!file.exists(), String.format("目录 %s 不存在", k));
                if (!oldDirList.contains(k)) {
                    newDirList.add(k);
                }
            }).collect(Collectors.toList());
            oldDirList.removeAll(searchDirectories);
            return searchDirectories;
        }).ifPresent(searchConfig::setSearchDirectories);
        // 同步新目录
        synchronizeService.async(newDirList);
        // TODO: 2022/10/7 删除被移除的目录

        return true;
    }
}
