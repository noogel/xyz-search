package noogel.xyz.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.ConfigProperties;
import noogel.xyz.search.infrastructure.dto.SearchSettingDto;
import noogel.xyz.search.infrastructure.event.ConfigAppUpdateEvent;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import noogel.xyz.search.infrastructure.utils.JsonHelper;
import noogel.xyz.search.service.SettingService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Slf4j
public class SettingServiceImpl implements SettingService {

    @Resource
    private ConfigProperties configProperties;
    @Resource
    private ElasticsearchClient elasticsearchClient;
    @Resource
    private InMemoryUserDetailsManager inMemoryUserDetailsManager;
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private ApplicationEventPublisher publisher;


    @Override
    public SearchSettingDto query() {
        SearchSettingDto dto = new SearchSettingDto();
        dto.setUsername(configProperties.getBase().getUsername());
        dto.setPassword(configProperties.getBase().getPassword());
        dto.setConfigFilePath(configProperties.getBase().getDataPath());
        dto.setFtsIndexName(configProperties.getBase().getFtsIndexName());
        dto.setAppConfig(Optional.ofNullable(configProperties.getApp()).map(JsonHelper::toJson).orElse("{}"));
        dto.setConfigDesc(ConfigProperties.App.getNotes());
        return dto;
    }

    @Override
    public SearchSettingDto update(SearchSettingDto cfg) {
        ConfigProperties.App newApp = validateAndCopyToNewConfig(cfg);
        ConfigProperties.App oldApp = configProperties.getApp();
        // 判断是否需要更新密码
        boolean updateUserPass = !Objects.equals(cfg.getPassword(), configProperties.getBase().getPassword());

        // 设置并保存配置
        configProperties.setApp(newApp);
        configProperties.getBase().setPassword(cfg.getPassword());
        configProperties.overrideToFile();

        // 更新 es client bean
        // elasticsearchConfiguration.reloadClient();

        // 更新 密码
        if (updateUserPass) {
            updateUserPassword();
        }
        // 发布事件
        publisher.publishEvent(ConfigAppUpdateEvent.of(this, oldApp, newApp));
        return query();
    }

    @Override
    public boolean connectTesting(SearchSettingDto cfg) {
        var App = validateAndCopyToNewConfig(cfg);
        try {
            BooleanResponse ping = elasticsearchClient.ping();
            return ping.value();
        } catch (IOException e) {
            throw ExceptionCode.CONFIG_ERROR.throwExc(e);
        }
    }

    public void updateUserPassword() {
        UserDetails user = User.withUsername(configProperties.getBase().getUsername())
                .password(passwordEncoder.encode(configProperties.getBase().getPassword()))
                .roles("USER")
                .build();
        inMemoryUserDetailsManager.updateUser(user);
    }

    /**
     * 校验并且拷贝到新的配置对象中
     *
     * @param scr
     * @return
     */
    public ConfigProperties.App validateAndCopyToNewConfig(SearchSettingDto scr) {
        ConfigProperties.App cfg = JsonHelper.fromJson(scr.getAppConfig(), ConfigProperties.App.class);
        ExceptionCode.CONFIG_ERROR.throwOn(Objects.isNull(cfg), "配置对象不能为空");
        // 校验数据
        ExceptionCode.CONFIG_ERROR.throwOn(StringUtils.isBlank(cfg.getElasticsearchHost()),
                "Elasticsearch host 不能为空");
        ExceptionCode.CONFIG_ERROR.throwOn(StringUtils.isBlank(scr.getPassword()),
                "密码不能为空");
        ExceptionCode.CONFIG_ERROR.throwOn(Objects.nonNull(cfg.getNotifyEmail())
                && StringUtils.isNotBlank(cfg.getNotifyEmail().getSenderEmail())
                && CollectionUtils.isEmpty(cfg.getNotifyEmail().getReceivers()), "邮件通知接收人不能为空");
        // 校验证书
        if (StringUtils.isNotBlank(cfg.getElasticsearchCAPath())) {
            File file = new File(cfg.getElasticsearchCAPath());
            ExceptionCode.CONFIG_ERROR.throwOn(!file.exists() || !file.isFile(),
                    String.format("证书文件 %s 不存在", cfg.getElasticsearchCAPath()));
        }
        // 校验目录
        Optional.ofNullable(cfg.indexDirectories()).orElse(Collections.emptyList()).stream()
                .filter(StringUtils::isNotBlank)
                .forEach(k -> {
                    File file = new File(k);
                    ExceptionCode.CONFIG_ERROR.throwOn(!file.exists() || !file.isDirectory(),
                            String.format("目录 %s 不存在", k));
                });
        // 校验正则
        List<ConfigProperties.CollectItem> itemList = Optional.ofNullable(cfg.getCollectDirectories())
                .orElse(Collections.emptyList());
        for (ConfigProperties.CollectItem collectItem : itemList) {
            if (StringUtils.isNotBlank(collectItem.getFilterRegex())) {
                // 检查是否可编译
                Pattern.compile(collectItem.getFilterRegex());
            }
        }
        // 文件上传目录
        Optional.ofNullable(cfg.indexDirectories()).ifPresent(t -> {
            Optional.ofNullable(cfg.getUploadFileDirectory()).filter(StringUtils::isNotBlank).ifPresent(l -> {
                ExceptionCode.CONFIG_ERROR.throwOn(t.stream().noneMatch(l::startsWith), "上传目录必须为索引资源子目录");
            });
        });
        // 标记删除转移的目录必须在排除的目录下
        Optional.ofNullable(cfg.getMarkDeleteDirectory()).filter(StringUtils::isNotBlank).ifPresent(t -> {
            List<String> excludeDirs = Optional.ofNullable(cfg.excludesDirectories())
                    .orElse(Collections.emptyList());
            ExceptionCode.CONFIG_ERROR.throwOn(
                    CollectionUtils.isEmpty(excludeDirs) || excludeDirs.stream().noneMatch(t::startsWith),
                    "标记删除转移目录必须为排除目录的子目录");
        });
        return cfg;
    }
}
