package noogel.xyz.search.infrastructure.dto.settings;

import org.apache.commons.lang3.StringUtils;

import lombok.Data;
import noogel.xyz.search.infrastructure.config.ConfigProperties;

/**
 * 数据存储配置DTO
 */
@Data
public class DataStorageSettingDto {
    /**
     * Elastic 全文索引配置
     */
    private ConfigProperties.Elastic elastic;
    
    /**
     * Qdrant 向量数据库配置
     */
    private ConfigProperties.Qdrant qdrant;
    
    /**
     * 从App配置转换为数据存储设置DTO
     */
    public static DataStorageSettingDto fromAppConfig(ConfigProperties.App appConfig) {
        if (appConfig == null || appConfig.getChat() == null) {
            return new DataStorageSettingDto();
        }
        
        DataStorageSettingDto dto = new DataStorageSettingDto();
        dto.setElastic(appConfig.getChat().getElastic());
        dto.setQdrant(appConfig.getChat().getQdrant());
        
        return dto;
    }
    
    /**
     * 更新App配置中的数据存储相关设置
     */
    public void updateAppConfig(ConfigProperties.App appConfig) {
        if (appConfig == null || appConfig.getChat() == null) {
            return;
        }
        
        appConfig.getChat().setElastic(this.elastic);
        appConfig.getChat().setQdrant(this.qdrant);
    }
    
    /**
     * 验证数据存储配置是否有效
     * @return 错误信息，无错误返回null
     */
    public String validate() {
        // 检查Elasticsearch配置
        if (elastic != null && elastic.isEnable()) {
            if (StringUtils.isBlank(elastic.getHost())) {
                return "Elasticsearch主机地址不能为空";
            }
        }
        
        // 检查Qdrant配置
        if (qdrant != null && qdrant.isEnable()) {
            if (StringUtils.isBlank(qdrant.getHost())) {
                return "Qdrant主机地址不能为空";
            }
            if (qdrant.getPort() == null) {
                return "Qdrant端口不能为空";
            }
        }
        
        return null;
    }
}