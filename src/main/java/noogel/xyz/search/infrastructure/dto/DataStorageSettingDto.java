package noogel.xyz.search.infrastructure.dto;

import lombok.Data;
import noogel.xyz.search.infrastructure.config.ConfigProperties;

/**
 * 数据存储配置
 */
@Data
public class DataStorageSettingDto {
    /**
     * 向量数据库配置 (Qdrant)
     */
    private ConfigProperties.Qdrant qdrant;
    
    /**
     * Elasticsearch存储配置
     */
    private ConfigProperties.Elastic elastic;
    
    /**
     * 从App配置转换
     */
    public static DataStorageSettingDto fromApp(ConfigProperties.App app) {
        DataStorageSettingDto dto = new DataStorageSettingDto();
        if (app.getChat() != null) {
            dto.setQdrant(app.getChat().getQdrant());
            dto.setElastic(app.getChat().getElastic());
        }
        return dto;
    }
    
    /**
     * 应用到App配置
     */
    public void applyToApp(ConfigProperties.App app) {
        if (app.getChat() == null) {
            app.setChat(new ConfigProperties.Chat());
        }
        app.getChat().setQdrant(this.qdrant);
        app.getChat().setElastic(this.elastic);
    }
} 