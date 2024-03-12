package noogel.xyz.search.infrastructure.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import noogel.xyz.search.infrastructure.utils.ConfigNote;
import noogel.xyz.search.infrastructure.utils.EnvHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import javax.annotation.Nullable;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Configuration
@Slf4j
public class SearchPropertyConfig {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final ObjectMapper YAML_OBJECT_MAPPER = new ObjectMapper(new YAMLFactory());

    /**
     * 配置文件绝对路径
     *
     * @param configFilePath
     * @return
     */
    public static String propertyConfigPath(String configFilePath) {
        return String.format("%sproperty-config.json", StringUtils.isBlank(configFilePath) ? "" : (configFilePath + "/"));
    }

    /**
     * 自定义配置文件
     *
     * @param configPath
     * @return
     */
    @Nullable
    public static SearchConfig readConfigByFile(String configPath) {
        if (StringUtils.isBlank(configPath)) {
            return null;
        }
        File file = new File(configPath);
        if (!file.exists()) {
            return null;
        }
        try (InputStream input = new FileInputStream(file)) {
            return YAML_OBJECT_MAPPER.readValue(input, SearchConfig.class);
        } catch (Exception e) {
            log.warn("readConfigByProperty error path:{}", configPath, e);
        }
        return null;
    }

    /**
     * 通过参数配置的资源文件
     *
     * @return
     */
    @Nullable
    public static SearchConfig readConfigByProperty() {
        String configPath = System.getProperty("config.path");
        if (StringUtils.isBlank(configPath)) {
            return null;
        }
        try (InputStream input = new FileInputStream(configPath)) {
            return YAML_OBJECT_MAPPER.readValue(input, SearchConfig.class);
        } catch (Exception e) {
            log.warn("readConfigByProperty error path: {}", configPath, e);
        }
        return null;
    }

    /**
     * 从资源文件读取
     *
     * @return
     */
    @Nullable
    public static SearchConfig readBaseConfigByResource() {
        Resource resource = new DefaultResourceLoader().getResource(
                String.format("classpath:xyz-search-%s.yml", EnvHelper.DEPLOY_ENV));
        try (InputStream inputStream = resource.getInputStream()) {
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                byte[] b = new byte[10240];
                int n;
                while ((n = inputStream.read(b)) != -1) {
                    outputStream.write(b, 0, n);
                }
                String readString = outputStream.toString();
                ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
                return objectMapper.readValue(readString, SearchConfig.class);
            }
        } catch (Exception e) {
            log.error("readConfigByResource error", e);
        }
        return null;
    }

    /**
     * 获取配置文件，必须
     *
     * @return
     */
    @Bean
    public SearchConfig getSearchConfig() {
        // 优先从 yml 文件中读取基础配置
        SearchConfig searchConfig = readBaseConfigByResource();
        if (Objects.isNull(searchConfig)) {
            throw ExceptionCode.CONFIG_ERROR.throwExc("基础配置不能为空");
        }

        // 先从启动命令中读取配置
        SearchConfig commandConfig = readConfigByProperty();
        if (Objects.nonNull(commandConfig)) {
            return commandConfig;
        }

        // 否则从配置路径中获取
        String pCfgPath = propertyConfigPath(searchConfig.base.configFilePath);
        SearchConfig propertyConfig = readConfigByFile(pCfgPath);
        if (Objects.nonNull(propertyConfig)) {
            return propertyConfig;
        }

        // 最后填充配置对象返回
        if (Objects.isNull(searchConfig.getApp())) {
            searchConfig.setApp(AppConfig.init());
        }
        if (Objects.isNull(searchConfig.getRuntime())) {
            searchConfig.setRuntime(RuntimeConfig.init());
        }
        searchConfig.saveToFile();
        return searchConfig;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CollectItem {
        @ConfigNote(desc = "collectDirectories:资源收集来源目录")
        private List<String> fromList;
        @ConfigNote(desc = "collectDirectories:资源收集到的目录")
        private String to;
        @ConfigNote(desc = "collectDirectories:资源收集过滤规则（正则）（可选）")
        private String filterRegex;
        @ConfigNote(desc = "collectDirectories:资源收集后自动删除文件")
        private Boolean autoDelete;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NotifyEmail {
        @ConfigNote(desc = "notifyEmail:访问通知链接")
        private String url;
        @ConfigNote(desc = "notifyEmail:访问通知邮件接收人")
        private List<String> receivers;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AppConfig {
        @ConfigNote(desc = "ES 地址，地址格式 http[s]://xxx:9200")
        private String elasticsearchHost;
        @ConfigNote(desc = "ES 用户名（可选）")
        private String elasticsearchUser;
        @ConfigNote(desc = "ES 密码（可选）")
        private String elasticsearchPassword;
        @ConfigNote(desc = "ES 证书位置（可选）")
        private String elasticsearchCAPath;
        @ConfigNote(desc = "ES 连接超时时间")
        private Integer elasticsearchConnectionTimeout;
        @ConfigNote(desc = "ES Socket 超时时间")
        private Integer elasticsearchSocketTimeout;
        @ConfigNote(desc = "索引目录")
        private List<String> searchDirectories;
        @ConfigNote(desc = "OPDS 资源目录，如果存在则开启")
        private String opdsDirectory;
        @ConfigNote(desc = "访问邮件通知")
        private NotifyEmail notifyEmail;
        @ConfigNote(desc = "资源收集目录映射")
        private List<CollectItem> collectDirectories;
        @ConfigNote(desc = "索引限速")
        private Long indexLimitMs;
        @ConfigNote(desc = "上传文件所在目录")
        private String uploadFileDirectory;

        public static AppConfig init() {
            AppConfig appConfig = new AppConfig();
            appConfig.setSearchDirectories(new ArrayList<>());
            appConfig.setNotifyEmail(new NotifyEmail());
            appConfig.getNotifyEmail().setReceivers(new ArrayList<>());
            return appConfig;
        }

        public static List<Pair<String, String>> getNotes() {
            return Arrays.stream(AppConfig.class.getDeclaredFields()).map(t -> {
                String name = "[" + t.getType().getSimpleName() + "] " + t.getName();
                ConfigNote annotation = t.getAnnotation(ConfigNote.class);
                return Pair.of(name, annotation.desc());
            }).collect(Collectors.toList());
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RuntimeConfig {
        /**
         * 是否已经初始化索引
         */
        private boolean initIndex;

        public static RuntimeConfig init() {
            return new RuntimeConfig();
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BaseConfig {
        /**
         * 配置文件路径
         */
        private String configFilePath;
        /**
         * 索引名称
         */
        private String ftsIndexName;
        /**
         * 用户名
         */
        private String username;
        /**
         * 密码
         */
        private String password;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchConfig {
        /**
         * 基本配置，不可直接修改
         */
        private BaseConfig base;
        /**
         * 运行时数据，不可直接修改
         */
        private RuntimeConfig runtime;
        /**
         * 应用数据，用户可直接修改
         */
        private AppConfig app;

        /**
         * 保存到文件
         */
        public void saveToFile() {
            String pCfgPath = propertyConfigPath(base.configFilePath);
            File pCfgFile = new File(pCfgPath);
            try {
                SearchConfig propertyConfig = new SearchConfig();
                BeanUtils.copyProperties(this, propertyConfig);
                OBJECT_MAPPER.writeValue(pCfgFile, propertyConfig);
            } catch (IOException e) {
                log.error("writeConfigErr path: {}", pCfgPath, e);
            }
        }
    }
}