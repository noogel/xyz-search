package noogel.xyz.search.infrastructure.dto.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import lombok.Data;
import noogel.xyz.search.infrastructure.config.ConfigProperties;

/**
 * 文件与目录管理配置DTO
 */
@Data
public class FileDirectorySettingDto {
    /**
     * 索引目录
     */
    private List<ConfigProperties.IndexItem> indexDirectories;
    
    /**
     * 资源收集目录映射
     */
    private List<ConfigProperties.CollectItem> collectDirectories;
    
    /**
     * OPDS 资源目录，如果存在则开启
     */
    private String opdsDirectory;
    
    /**
     * 上传文件所在目录
     */
    private String uploadFileDirectory;
    
    /**
     * 标记删除文件转移到的目录
     */
    private String markDeleteDirectory;
    
    /**
     * 从App配置转换为目录设置DTO
     */
    public static FileDirectorySettingDto fromAppConfig(ConfigProperties.App appConfig) {
        if (appConfig == null) {
            return new FileDirectorySettingDto();
        }
        
        FileDirectorySettingDto dto = new FileDirectorySettingDto();
        dto.setIndexDirectories(appConfig.getIndexDirectories());
        dto.setCollectDirectories(appConfig.getCollectDirectories());
        dto.setOpdsDirectory(appConfig.getOpdsDirectory());
        dto.setUploadFileDirectory(appConfig.getUploadFileDirectory());
        dto.setMarkDeleteDirectory(appConfig.getMarkDeleteDirectory());
        
        return dto;
    }
    
    /**
     * 更新App配置中的目录相关设置
     */
    public void updateAppConfig(ConfigProperties.App appConfig) {
        if (appConfig == null) {
            return;
        }
        
        appConfig.setIndexDirectories(Optional.ofNullable(this.indexDirectories)
                .orElse(new ArrayList<>()));
        appConfig.setCollectDirectories(Optional.ofNullable(this.collectDirectories)
                .orElse(new ArrayList<>()));
        appConfig.setOpdsDirectory(this.opdsDirectory);
        appConfig.setUploadFileDirectory(this.uploadFileDirectory);
        appConfig.setMarkDeleteDirectory(this.markDeleteDirectory);
    }
    
    /**
     * 验证目录配置是否有效
     * @return 错误信息，无错误返回null
     */
    public String validate() {
        if (indexDirectories == null || indexDirectories.isEmpty()) {
            return "索引目录不能为空";
        }
        
        for (ConfigProperties.IndexItem item : indexDirectories) {
            if (StringUtils.isBlank(item.getDirectory())) {
                return "索引目录路径不能为空";
            }
        }
        
        return null;
    }
} 