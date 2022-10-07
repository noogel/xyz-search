package noogel.xyz.search.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import noogel.xyz.search.infrastructure.utils.EnvHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import javax.annotation.Nullable;
import java.io.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Configuration
@Slf4j
public class SearchPropertyConfig {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final ObjectMapper YAML_OBJECT_MAPPER = new ObjectMapper(new YAMLFactory());

    @Data
    public static class PropertyConfig {
        /*
        elasticSearch 配置
         */
        private String elasticsearchHost;
        private String elasticsearchUser;
        private String elasticsearchPassword;
        private String elasticsearchCAPath;
        private Integer elasticsearchConnectionTimeout;
        private Integer elasticsearchSocketTimeout;

        /*
        搜索目录
         */
        private List<String> searchDirectories;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class SearchConfig extends PropertyConfig {
        /**
         * 配置文件路径
         */
        private String configFilePath;
        /**
         * 日志文件路径
         */
        private String logFilePath;
        /**
         * 索引名称
         */
        private String ftsIndexName;

        /**
         * 保存到文件
         */
        public void saveToFile() {
            String pCfgPath = propertyConfigPath(configFilePath);
            File pCfgFile = new File(pCfgPath);
            try {
                PropertyConfig propertyConfig = new PropertyConfig();
                BeanUtils.copyProperties(this, propertyConfig);
                OBJECT_MAPPER.writeValue(pCfgFile, propertyConfig);
            } catch (IOException e) {
                log.error("writeConfigErr path: {}", pCfgPath, e);
            }
        }
    }

    /**
     * 获取配置文件，必须
     *
     * @return
     */
    @Bean
    public SearchConfig getSearchConfig() {
        // 先从启动命令中读取配置
        SearchConfig searchConfig = readConfigByProperty();
        if (Objects.isNull(searchConfig)) {
            // 否则从资源文件中读取配置
            searchConfig = readConfigByResource();
        }
        ExceptionCode.CONFIG_ERROR.throwOn(Objects.isNull(searchConfig), "找不到配置文件");
        String pCfgPath = propertyConfigPath(searchConfig.configFilePath);
        PropertyConfig propertyConfig = readConfigByFile(pCfgPath);
        if (Objects.nonNull(propertyConfig)) {
            BeanUtils.copyProperties(propertyConfig, searchConfig);
        }
        return searchConfig;
    }

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
    public static PropertyConfig readConfigByFile(String configPath) {
        if (StringUtils.isBlank(configPath)) {
            return null;
        }
        File file = new File(configPath);
        if (!file.exists()) {
            return null;
        }
        try (InputStream input = new FileInputStream(file)) {
            return YAML_OBJECT_MAPPER.readValue(input, PropertyConfig.class);
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
    public static SearchConfig readConfigByResource() {
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
}
