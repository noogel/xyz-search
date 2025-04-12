package noogel.xyz.search.infrastructure.utils;

import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.ConfigProperties;
import noogel.xyz.search.infrastructure.consts.CommonsConsts;

@Slf4j
public class EmailNotifyHelper {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 发送
     *
     * @param cfg
     * @param subject
     * @param message
     * @param sendCondition
     * @param successRunnable
     */
    public static void send(ConfigProperties.App cfg, String subject, String message,
            Supplier<Boolean> sendCondition, Runnable successRunnable) {
        if (Objects.isNull(cfg.getNotifyEmail())) {
            return;
        }
        if (StringUtils.isBlank(cfg.getNotifyEmail().getEmailHost())) {
            return;
        }
        if (CollectionUtils.isEmpty(cfg.getNotifyEmail().getReceivers())) {
            return;
        }
        CommonsConsts.SHORT_EXECUTOR_SERVICE.submit(() -> {
            if (!sendCondition.get()) {
                return;
            }
            NotifyDto dto = NotifyDto.of(cfg.getNotifyEmail().getReceivers(), subject, message);
            try {
                String str = OBJECT_MAPPER.writeValueAsString(dto);
                String s = HttpClient.doPost(cfg.getNotifyEmail().getEmailHost(), str);
                log.info("sendMail subject:{} message:{} result:{}", subject, message, s);
                if (Objects.equals("Success", s)) {
                    successRunnable.run();
                }
            } catch (Exception ex) {
                log.error("sendMail failed.", ex);
            }
        });
    }

    /**
     * 使用 SMTP 发送邮件
     *
     * @param cfg             配置
     * @param subject         主题
     * @param message         消息内容
     * @param sendCondition   发送条件
     * @param successRunnable 发送成功后的回调
     */
    public static void sendBySmtp(ConfigProperties.App cfg, String subject, String message,
            Supplier<Boolean> sendCondition, Runnable successRunnable) {
        if (Objects.isNull(cfg.getNotifyEmail())) {
            return;
        }
        if (StringUtils.isBlank(cfg.getNotifyEmail().getEmailHost())) {
            return;
        }
        if (CollectionUtils.isEmpty(cfg.getNotifyEmail().getReceivers())) {
            return;
        }
        if (StringUtils.isBlank(cfg.getNotifyEmail().getSenderEmail())) {
            return;
        }
        if (StringUtils.isBlank(cfg.getNotifyEmail().getEmailPass())) {
            return;
        }

        CommonsConsts.SHORT_EXECUTOR_SERVICE.submit(() -> {
            if (!sendCondition.get()) {
                return;
            }

            try {
                // 配置邮件服务器属性
                Properties props = new Properties();
                props.put("mail.smtp.host", cfg.getNotifyEmail().getEmailHost());
                props.put("mail.smtp.port", cfg.getNotifyEmail().getEmailPort());
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.ssl.trust", cfg.getNotifyEmail().getEmailHost());

                // 创建会话
                Session session = Session.getInstance(props, null);

                // 创建邮件消息
                MimeMessage mimeMessage = new MimeMessage(session);
                mimeMessage.setFrom(new InternetAddress(cfg.getNotifyEmail().getSenderEmail()));
                mimeMessage.setSubject(subject);
                mimeMessage.setContent(message, "text/html;charset=UTF-8");

                // 设置收件人
                for (String receiver : cfg.getNotifyEmail().getReceivers()) {
                    mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(receiver));
                }

                // 发送邮件
                Transport transport = session.getTransport("smtp");
                transport.connect(cfg.getNotifyEmail().getEmailHost(),
                        cfg.getNotifyEmail().getSenderEmail(),
                        cfg.getNotifyEmail().getEmailPass());
                transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients());
                transport.close();

                log.info("sendMailBySmtp subject:{} message:{} success", subject, message);
                successRunnable.run();
            } catch (MessagingException e) {
                log.error("sendMailBySmtp failed.", e);
            }
        });
    }

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class NotifyDto {
        private List<String> receivers;
        private String subject;
        private String emailMsg;
        private String fromHeader;

        public static NotifyDto of(List<String> receivers, String subject, String message) {
            NotifyDto d = new NotifyDto();
            d.setReceivers(receivers);
            d.setSubject(subject);
            d.setEmailMsg(message);
            d.setFromHeader("通知");
            return d;
        }
    }
}
