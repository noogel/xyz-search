package noogel.xyz.search.infrastructure.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import noogel.xyz.search.infrastructure.utils.EnvHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Objects;

@Slf4j
@Configuration
public class ConfigPropertiesConfiguration {
    private static final ObjectMapper YAML_OBJECT_MAPPER = new ObjectMapper(new YAMLFactory());

    static {
        YAML_OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 自定义配置文件
     *
     * @param configPath
     * @return
     */
    @Nullable
    public static ConfigProperties readConfigByFile(String configPath) {
        if (StringUtils.isBlank(configPath)) {
            return null;
        }
        File file = new File(configPath);
        if (!file.exists()) {
            return null;
        }
        try (InputStream input = new FileInputStream(file)) {
            return YAML_OBJECT_MAPPER.readValue(input, ConfigProperties.class);
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
    public static ConfigProperties readConfigByProperty() {
        String configPath = System.getProperty("config.path");
        if (StringUtils.isBlank(configPath)) {
            return null;
        }
        try (InputStream input = new FileInputStream(configPath)) {
            return YAML_OBJECT_MAPPER.readValue(input, ConfigProperties.class);
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
    public static ConfigProperties readBaseConfigByResource() {
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
                return YAML_OBJECT_MAPPER.readValue(readString, ConfigProperties.class);
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
    @ConditionalOnMissingBean(ConfigProperties.class)
    public ConfigProperties configProperties() {
        // 优先从 yml 文件中读取基础配置
        ConfigProperties configProperties = readBaseConfigByResource();
        if (Objects.isNull(configProperties)) {
            throw ExceptionCode.CONFIG_ERROR.throwExc("基础配置不能为空");
        }

        // 先从启动命令中读取配置
        ConfigProperties commandConfig = readConfigByProperty();
        if (Objects.nonNull(commandConfig)) {
            // 从启动环境变量中覆盖配置
            overrideFromEnv(commandConfig);
            return commandConfig;
        }

        // 否则从配置路径中获取
        String pCfgPath = configProperties.getBase().propertiesConfigPath().toString();
        ConfigProperties propertyConfig = readConfigByFile(pCfgPath);
        if (Objects.nonNull(propertyConfig)) {
            // 从启动环境变量中覆盖配置
            overrideFromEnv(propertyConfig);
            return propertyConfig;
        }

        // 从启动环境变量中覆盖配置
        overrideFromEnv(configProperties);

        // 最后填充配置对象返回
        if (Objects.isNull(configProperties.getApp())) {
            configProperties.setApp(ConfigProperties.App.init());
        }
        if (Objects.isNull(configProperties.getRuntime())) {
            configProperties.setRuntime(ConfigProperties.Runtime.init());
        }
        configProperties.overrideToFile();
        return configProperties;
    }

    private void overrideFromEnv(ConfigProperties configProperties) {
        String esIdxEnv = EnvHelper.FuncEnv.FTS_IDX.getEnv();
        if (StringUtils.isNotBlank(esIdxEnv)) {
            configProperties.getBase().setFtsIndexName(esIdxEnv);
        }
    }
}