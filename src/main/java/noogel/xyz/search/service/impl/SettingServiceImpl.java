package noogel.xyz.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.ElasticsearchConfig;
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
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SettingServiceImpl implements SettingService {

    @Resource
    private SearchPropertyConfig.SearchConfig searchConfig;
    @Resource
    private SynchronizeService synchronizeService;
    @Resource
    private ElasticsearchConfig elasticsearchConfig;

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
        SearchPropertyConfig.SearchConfig sc = validateAndCopyToNewConfig(cfg);
        // XXX 移除 diff 代码
        // 拷贝到全局对象
        BeanUtils.copyProperties(sc, searchConfig);
        // 保存配置
        searchConfig.saveToFile();
        // 更新 es client bean
        elasticsearchConfig.reloadClient();
        // 同步新目录
        synchronizeService.asyncAll();
        return query();
    }

    @Override
    public boolean connectTesting(SearchSettingDto cfg) {
        SearchPropertyConfig.SearchConfig sc = validateAndCopyToNewConfig(cfg);
        try {
            ElasticsearchClient elasticsearchClient = elasticsearchConfig.genClient(sc);
            BooleanResponse ping = elasticsearchClient.ping();
            return ping.value();
        } catch (IOException e) {
            throw ExceptionCode.CONFIG_ERROR.throwExc(e);
        }
    }

    /**
     * 校验并且拷贝到新的配置对象中
     * @param cfg
     * @return
     */
    public SearchPropertyConfig.SearchConfig validateAndCopyToNewConfig(SearchSettingDto cfg) {
        // 校验数据
        ExceptionCode.CONFIG_ERROR.throwOn(StringUtils.isBlank(cfg.getElasticsearchHost()),
                "Elasticsearch host 不能为空");
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
                }).collect(Collectors.toList());

        // 更新数据
        StringBuilder esHost = new StringBuilder("http");
        if (StringUtils.isNotBlank(cfg.getElasticsearchCAPath())) {
            esHost.append("s");
        }
        esHost.append("://").append(cfg.getElasticsearchHost());

        SearchPropertyConfig.SearchConfig sc = new SearchPropertyConfig.SearchConfig();
        BeanUtils.copyProperties(searchConfig, sc);
        sc.setElasticsearchHost(esHost.toString());
        sc.setElasticsearchUser(cfg.getElasticsearchUser());
        sc.setElasticsearchPassword(cfg.getElasticsearchPassword());
        sc.setElasticsearchCAPath(cfg.getElasticsearchCAPath());
        sc.setSearchDirectories(searchDirectories);
        return sc;
    }
}
