package noogel.xyz.search.infrastructure.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Optional;

@Configuration
public class DataSourceConfiguration {

    @Bean(name = "EmbeddeddataSource")
    public DataSource dataSource(@Autowired SearchPropertyConfig.SearchConfig searchConfig) {
        String dbPath = Optional.ofNullable(searchConfig.getBase().getConfigFilePath()).orElse("");
        if (StringUtils.isNotBlank(dbPath)) {
            dbPath += "/";
        }
        dbPath += "search.xyz";

        return DataSourceBuilder.create()
                .driverClassName("org.sqlite.JDBC")
                .url("jdbc:sqlite:" + dbPath)
                .build();
    }
}
