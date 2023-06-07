package noogel.xyz.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.ElasticsearchConfig;
import noogel.xyz.search.infrastructure.config.SearchPropertyConfig;
import noogel.xyz.search.infrastructure.dto.SearchSettingDto;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import noogel.xyz.search.infrastructure.utils.JsonHelper;
import noogel.xyz.search.service.SettingService;
import noogel.xyz.search.service.SynchronizeService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.*;

@Service
@Slf4j
public class SettingServiceImpl implements SettingService {

    @Resource
    private SearchPropertyConfig.SearchConfig searchConfig;
    @Resource
    private SynchronizeService synchronizeService;
    @Resource
    private ElasticsearchConfig elasticsearchConfig;
    @Resource
    private InMemoryUserDetailsManager inMemoryUserDetailsManager;
    @Resource
    private PasswordEncoder passwordEncoder;

    @Override
    public SearchSettingDto query() {
        SearchSettingDto dto = new SearchSettingDto();
        dto.setUsername(searchConfig.getBase().getUsername());
        dto.setPassword(searchConfig.getBase().getPassword());
        dto.setAppConfig(Optional.ofNullable(searchConfig.getApp()).map(JsonHelper::toJson).orElse("{}"));
        dto.setConfigDesc(SearchPropertyConfig.AppConfig.getNotes());
        return dto;
    }

    @Override
    public SearchSettingDto update(SearchSettingDto cfg) {
        SearchPropertyConfig.AppConfig newApp = validateAndCopyToNewConfig(cfg);
        SearchPropertyConfig.AppConfig oldApp = searchConfig.getApp();
        // 判断是否需要更新密码
        boolean updateUserPass = !Objects.equals(cfg.getPassword(), searchConfig.getBase().getPassword());

        // 设置并保存配置
        searchConfig.setApp(newApp);
        searchConfig.getBase().setPassword(cfg.getPassword());
        searchConfig.saveToFile();

        // 更新 es client bean
        elasticsearchConfig.reloadClient();

        // 计算并更新目录
        List<String> oldDirList = new ArrayList<>(oldApp.getSearchDirectories());
        // 新增目录
        List<String> newDirList = new ArrayList<>(newApp.getSearchDirectories());
        // 把新目录移除 = 剩下旧目录
        oldDirList.removeAll(newApp.getSearchDirectories());
        // 把旧目录移除 = 剩下新增的目录
        newDirList.removeAll(oldApp.getSearchDirectories());
        // 如果有变化则更新
        if (!CollectionUtils.isEmpty(oldDirList) || !CollectionUtils.isEmpty(newDirList)) {
            // 同步新目录
            synchronizeService.asyncAll();
        }
        // 更新 密码
        if (updateUserPass) {
            updateUserPassword();
        }
        return query();
    }

    @Override
    public boolean connectTesting(SearchSettingDto cfg) {
        SearchPropertyConfig.AppConfig appConfig = validateAndCopyToNewConfig(cfg);
        try {
            ElasticsearchClient elasticsearchClient = elasticsearchConfig.genClient(appConfig);
            BooleanResponse ping = elasticsearchClient.ping();
            return ping.value();
        } catch (IOException e) {
            throw ExceptionCode.CONFIG_ERROR.throwExc(e);
        }
    }

    public void updateUserPassword() {
        UserDetails user = User.withUsername(searchConfig.getBase().getUsername())
                .password(passwordEncoder.encode(searchConfig.getBase().getPassword()))
                .roles("USER")
                .build();
        inMemoryUserDetailsManager.updateUser(user);
    }
    /**
     * 校验并且拷贝到新的配置对象中
     * @param scr
     * @return
     */
    public SearchPropertyConfig.AppConfig validateAndCopyToNewConfig(SearchSettingDto scr) {
        SearchPropertyConfig.AppConfig cfg = JsonHelper.fromJson(scr.getAppConfig(),
                SearchPropertyConfig.AppConfig.class);
        ExceptionCode.CONFIG_ERROR.throwOn(Objects.isNull(cfg), "配置对象不能为空");
        // 校验数据
        ExceptionCode.CONFIG_ERROR.throwOn(StringUtils.isBlank(cfg.getElasticsearchHost()),
                "Elasticsearch host 不能为空");
        ExceptionCode.CONFIG_ERROR.throwOn(StringUtils.isBlank(scr.getPassword()),
                "密码不能为空");
        ExceptionCode.CONFIG_ERROR.throwOn(StringUtils.isNotBlank(cfg.getNotifyUrl())
                && CollectionUtils.isEmpty(cfg.getNotifyReceivers()), "邮件通知接收人不能为空");
        // 校验证书
        if (StringUtils.isNotBlank(cfg.getElasticsearchCAPath())) {
            File file = new File(cfg.getElasticsearchCAPath());
            ExceptionCode.CONFIG_ERROR.throwOn(!file.exists() || !file.isFile(),
                    String.format("证书文件 %s 不存在", cfg.getElasticsearchCAPath()));
        }
        // 校验目录
        Optional.ofNullable(cfg.getSearchDirectories()).orElse(Collections.emptyList()).stream()
                .filter(StringUtils::isNotBlank)
                .forEach(k -> {
                    File file = new File(k);
                    ExceptionCode.CONFIG_ERROR.throwOn(!file.exists() || !file.isDirectory(),
                            String.format("目录 %s 不存在", k));
                });

        return cfg;
    }
}
