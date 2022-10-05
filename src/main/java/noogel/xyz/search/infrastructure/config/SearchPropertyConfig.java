package noogel.xyz.search.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Data;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.*;
import java.util.List;

@Configuration
public class SearchPropertyConfig {

    @Data
    public static class SearchConfig {
        /*
        elasticSearch 配置
         */
        private String elasticsearchHost;
        private Integer elasticsearchPort;
        private Integer elasticsearchConnectionTimeout;
        private Integer elasticsearchSocketTimeout;

        /*
        搜索目录
         */
        private List<String> searchDirectories;

    }

    /**
     * 获取配置文件，必须
     * @return
     */
    @Bean
    public SearchConfig getSearchConfig() {
        String configPath = System.getProperty("configPath");
        ExceptionCode.CONFIG_ERROR.throwOn(StringUtils.isBlank(configPath), "缺少配置文件");
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        try {
            InputStream input = new FileInputStream(configPath);
            return objectMapper.readValue(input, SearchConfig.class);
        } catch (Exception e) {
            throw ExceptionCode.CONFIG_ERROR.throwExc(e);
        }
    }
}
