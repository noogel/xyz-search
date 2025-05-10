package noogel.xyz.search.infrastructure.dto.settings;

import java.util.List;

import lombok.Data;
import noogel.xyz.search.infrastructure.config.ConfigProperties;

/**
 * 搜索与AI服务配置DTO
 */
@Data
public class SearchAiSettingDto {
    /**
     * Chat配置
     */
    private ConfigProperties.Chat chat;
    
    /**
     * PaddleOCR配置
     */
    private ConfigProperties.PaddleOcr paddleOcr;
    
    /**
     * 外链配置
     */
    private List<ConfigProperties.LinkItem> linkItems;
    
    /**
     * 从App配置转换为搜索与AI设置DTO
     */
    public static SearchAiSettingDto fromAppConfig(ConfigProperties.App appConfig) {
        if (appConfig == null) {
            return new SearchAiSettingDto();
        }
        
        SearchAiSettingDto dto = new SearchAiSettingDto();
        dto.setChat(appConfig.getChat());
        dto.setPaddleOcr(appConfig.getPaddleOcr());
        dto.setLinkItems(appConfig.getLinkItems());
        
        return dto;
    }
    
    /**
     * 更新App配置中的搜索与AI相关设置
     */
    public void updateAppConfig(ConfigProperties.App appConfig) {
        if (appConfig == null) {
            return;
        }
        
        appConfig.setChat(this.chat);
        appConfig.setPaddleOcr(this.paddleOcr);
        appConfig.setLinkItems(this.linkItems);
    }
    
    /**
     * 验证搜索与AI配置是否有效
     * @return 错误信息，无错误返回null
     */
    public String validate() {
        // 检查Elasticsearch配置
        if (chat != null && chat.getElastic() != null) {
            ConfigProperties.Elastic elastic = chat.getElastic();
            
            if (elastic.isEnable() && (elastic.getHost() == null || elastic.getHost().isEmpty())) {
                return "Elasticsearch主机地址不能为空";
            }
        }
        
        // 检查向量配置
        if (chat != null && chat.getVector() != null) {
            ConfigProperties.Vector vector = chat.getVector();
            
            if (vector.getSimilarityThreshold() == null || vector.getTopK() == null) {
                return "向量配置参数不完整";
            }
        }
        
        return null;
    }
} 