package noogel.xyz.search.infrastructure.dto;

import java.util.List;

import lombok.Data;
import noogel.xyz.search.infrastructure.config.ConfigProperties;

/**
 * 通知与集成配置
 */
@Data
public class NotificationIntegrationSettingDto {
    /**
     * 邮件通知配置
     */
    private ConfigProperties.NotifyEmail notifyEmail;
    
    /**
     * 通知配置
     */
    private ConfigProperties.Notify notify;
    
    /**
     * 外链配置
     */
    private List<ConfigProperties.LinkItem> linkItems;
    
    /**
     * 从App配置转换
     */
    public static NotificationIntegrationSettingDto fromApp(ConfigProperties.App app) {
        NotificationIntegrationSettingDto dto = new NotificationIntegrationSettingDto();
        dto.setNotifyEmail(app.getNotifyEmail());
        dto.setNotify(app.getNotify());
        dto.setLinkItems(app.getLinkItems());
        return dto;
    }
    
    /**
     * 应用到App配置
     */
    public void applyToApp(ConfigProperties.App app) {
        app.setNotifyEmail(this.notifyEmail);
        app.setNotify(this.notify);
        app.setLinkItems(this.linkItems);
    }
} 