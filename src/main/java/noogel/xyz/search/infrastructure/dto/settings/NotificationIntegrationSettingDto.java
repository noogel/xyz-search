package noogel.xyz.search.infrastructure.dto.settings;

import java.util.ArrayList;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import lombok.Data;
import noogel.xyz.search.infrastructure.config.ConfigProperties;

/**
 * 通知与集成配置DTO
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
     * 从App配置转换为通知与集成设置DTO
     */
    public static NotificationIntegrationSettingDto fromAppConfig(ConfigProperties.App appConfig) {
        if (appConfig == null) {
            return new NotificationIntegrationSettingDto();
        }
        
        NotificationIntegrationSettingDto dto = new NotificationIntegrationSettingDto();
        dto.setNotifyEmail(appConfig.getNotifyEmail());
        dto.setNotify(appConfig.getNotify());
        
        return dto;
    }
    
    /**
     * 更新App配置中的通知与集成相关设置
     */
    public void updateAppConfig(ConfigProperties.App appConfig) {
        if (appConfig == null) {
            return;
        }
        
        appConfig.setNotifyEmail(Optional.ofNullable(this.notifyEmail)
                .orElse(new ConfigProperties.NotifyEmail()));
        
        if (appConfig.getNotifyEmail().getReceivers() == null) {
            appConfig.getNotifyEmail().setReceivers(new ArrayList<>());
        }
        
        appConfig.setNotify(this.notify);
    }
    
    /**
     * 验证通知与集成配置是否有效
     * @return 错误信息，无错误返回null
     */
    public String validate() {
        // 检查邮件通知配置
        if (notifyEmail != null && StringUtils.isNotBlank(notifyEmail.getSenderEmail())) {
            if (StringUtils.isBlank(notifyEmail.getEmailHost())) {
                return "邮件服务器地址不能为空";
            }
            if (notifyEmail.getEmailPort() == null) {
                return "邮件服务器端口不能为空";
            }
            if (StringUtils.isBlank(notifyEmail.getEmailPass())) {
                return "邮件密码不能为空";
            }
            if (CollectionUtils.isEmpty(notifyEmail.getReceivers())) {
                return "邮件接收人不能为空";
            }
        }
        
        return null;
    }
} 