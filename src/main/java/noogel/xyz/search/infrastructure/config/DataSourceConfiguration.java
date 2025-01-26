package noogel.xyz.search.infrastructure.config;

import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.io.File;
import java.nio.file.Paths;
import java.util.Optional;

@Configuration
public class DataSourceConfiguration {

    @Resource
    private ConfigProperties configProperties;

    @Bean(name = "EmbeddeddataSource")
    public DataSource dataSource(@Autowired ConfigProperties configProperties) {
        String dbPath = configProperties.getBase().dbPath().toString();
        return DataSourceBuilder.create()
                .driverClassName("org.sqlite.JDBC")
                .url("jdbc:sqlite:" + dbPath)
                .build();
    }
}
