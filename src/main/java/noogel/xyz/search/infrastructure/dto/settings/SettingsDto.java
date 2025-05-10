package noogel.xyz.search.infrastructure.dto.settings;

import lombok.Data;
import noogel.xyz.search.infrastructure.config.ConfigProperties;
import noogel.xyz.search.infrastructure.dto.SearchSettingDto;
import noogel.xyz.search.infrastructure.utils.JsonHelper;

/**
 * 系统设置DTO，聚合所有配置标签
 */
@Data
public class SettingsDto {
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码
     */
    private String password;
    
    /**
     * 文件与目录管理设置
     */
    private FileDirectorySettingDto fileDirectorySetting;
    
    /**
     * 搜索与AI服务设置
     */
    private SearchAiSettingDto searchAiSetting;
    
    /**
     * 数据存储配置设置
     */
    private DataStorageSettingDto dataStorageSetting;
    
    /**
     * 通知与集成设置
     */
    private NotificationIntegrationSettingDto notificationIntegrationSetting;
    
    /**
     * 从SearchSettingDto转换为SettingsDto
     */
    public static SettingsDto fromSearchSettingDto(SearchSettingDto searchSettingDto) {
        if (searchSettingDto == null) {
            return new SettingsDto();
        }
        
        ConfigProperties.App appConfig = JsonHelper.fromJson(searchSettingDto.getAppConfig(), ConfigProperties.App.class);
        if (appConfig == null) {
            appConfig = ConfigProperties.App.init();
        }
        
        SettingsDto dto = new SettingsDto();
        dto.setUsername(searchSettingDto.getUsername());
        dto.setPassword(searchSettingDto.getPassword());
        dto.setFileDirectorySetting(FileDirectorySettingDto.fromAppConfig(appConfig));
        dto.setSearchAiSetting(SearchAiSettingDto.fromAppConfig(appConfig));
        dto.setDataStorageSetting(DataStorageSettingDto.fromAppConfig(appConfig));
        dto.setNotificationIntegrationSetting(NotificationIntegrationSettingDto.fromAppConfig(appConfig));
        
        return dto;
    }
    
    /**
     * 转换为SearchSettingDto
     */
    public SearchSettingDto toSearchSettingDto() {
        ConfigProperties.App appConfig = ConfigProperties.App.init();
        
        if (fileDirectorySetting != null) {
            fileDirectorySetting.updateAppConfig(appConfig);
        }
        
        if (searchAiSetting != null) {
            searchAiSetting.updateAppConfig(appConfig);
        }
        
        if (dataStorageSetting != null) {
            dataStorageSetting.updateAppConfig(appConfig);
        }
        
        if (notificationIntegrationSetting != null) {
            notificationIntegrationSetting.updateAppConfig(appConfig);
        }
        
        SearchSettingDto searchSettingDto = new SearchSettingDto();
        searchSettingDto.setUsername(this.username);
        searchSettingDto.setPassword(this.password);
        searchSettingDto.setAppConfig(JsonHelper.toJson(appConfig));
        searchSettingDto.setConfigDesc(ConfigProperties.App.getNotes());
        
        return searchSettingDto;
    }
    
    /**
     * 验证所有配置是否有效
     * @return 错误信息，无错误返回null
     */
    public String validate() {
        // 验证基本信息
        if (this.password == null || this.password.isEmpty()) {
            return "密码不能为空";
        }
        
        // 验证文件与目录设置
        if (fileDirectorySetting != null) {
            String error = fileDirectorySetting.validate();
            if (error != null) {
                return error;
            }
        }
        
        // 验证搜索与AI服务设置
        if (searchAiSetting != null) {
            String error = searchAiSetting.validate();
            if (error != null) {
                return error;
            }
        }
        
        // 验证数据存储配置设置
        if (dataStorageSetting != null) {
            String error = dataStorageSetting.validate();
            if (error != null) {
                return error;
            }
        }
        
        // 验证通知与集成设置
        if (notificationIntegrationSetting != null) {
            String error = notificationIntegrationSetting.validate();
            if (error != null) {
                return error;
            }
        }
        
        return null;
    }
} 