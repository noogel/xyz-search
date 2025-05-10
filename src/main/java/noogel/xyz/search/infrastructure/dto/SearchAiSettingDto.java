package noogel.xyz.search.infrastructure.dto;

import lombok.Data;
import noogel.xyz.search.infrastructure.config.ConfigProperties;

/**
 * 搜索与AI服务配置
 */
@Data
public class SearchAiSettingDto {
    /**
     * 向量配置
     */
    private ConfigProperties.Vector vector;
    
    /**
     * Ollama配置
     */
    private ConfigProperties.Ollama ollama;
    
    /**
     * OCR服务配置
     */
    private ConfigProperties.PaddleOcr paddleOcr;
    
    /**
     * Elasticsearch全文索引配置
     */
    private ConfigProperties.Elastic elastic;
    
    /**
     * 从App配置转换
     */
    public static SearchAiSettingDto fromApp(ConfigProperties.App app) {
        SearchAiSettingDto dto = new SearchAiSettingDto();
        if (app.getChat() != null) {
            dto.setVector(app.getChat().getVector());
            dto.setOllama(app.getChat().getOllama());
            dto.setElastic(app.getChat().getElastic());
        }
        dto.setPaddleOcr(app.getPaddleOcr());
        return dto;
    }
    
    /**
     * 应用到App配置
     */
    public void applyToApp(ConfigProperties.App app) {
        if (app.getChat() == null) {
            app.setChat(new ConfigProperties.Chat());
        }
        app.getChat().setVector(this.vector);
        app.getChat().setOllama(this.ollama);
        app.getChat().setElastic(this.elastic);
        app.setPaddleOcr(this.paddleOcr);
    }
} 