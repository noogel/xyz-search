package noogel.xyz.search.infrastructure.dto;

import lombok.Data;

@Data
public class SearchSettingDto {
    private String elasticsearchHost;
    private String elasticsearchUser;
    private String elasticsearchPassword;
    private String elasticsearchCAPath;
    private String searchDirectories;
    private String configFilePath;
    private String logFilePath;
    private String ftsIndexName;
    private String username;
    private String password;
}
