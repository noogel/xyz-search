package noogel.xyz.search.infrastructure.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.Resource;

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
