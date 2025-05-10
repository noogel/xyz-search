package noogel.xyz.search.infrastructure.dto;

import java.util.List;

import lombok.Data;
import noogel.xyz.search.infrastructure.config.ConfigProperties;

/**
 * 文件与目录管理配置
 */
@Data
public class FileDirectorySettingDto {
    /**
     * 索引目录列表
     */
    private List<ConfigProperties.IndexItem> indexDirectories;
    
    /**
     * 资源收集目录映射
     */
    private List<ConfigProperties.CollectItem> collectDirectories;
    
    /**
     * OPDS 资源目录
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
     * 从App配置转换
     */
    public static FileDirectorySettingDto fromApp(ConfigProperties.App app) {
        FileDirectorySettingDto dto = new FileDirectorySettingDto();
        dto.setIndexDirectories(app.getIndexDirectories());
        dto.setCollectDirectories(app.getCollectDirectories());
        dto.setOpdsDirectory(app.getOpdsDirectory());
        dto.setUploadFileDirectory(app.getUploadFileDirectory());
        dto.setMarkDeleteDirectory(app.getMarkDeleteDirectory());
        return dto;
    }
    
    /**
     * 应用到App配置
     */
    public void applyToApp(ConfigProperties.App app) {
        app.setIndexDirectories(this.indexDirectories);
        app.setCollectDirectories(this.collectDirectories);
        app.setOpdsDirectory(this.opdsDirectory);
        app.setUploadFileDirectory(this.uploadFileDirectory);
        app.setMarkDeleteDirectory(this.markDeleteDirectory);
    }
} 