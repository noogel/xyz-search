package noogel.xyz.search.infrastructure.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.CommonsConstConfig;
import noogel.xyz.search.infrastructure.config.SearchPropertyConfig;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

@Slf4j
public class EmailNotifyHelper {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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

    /**
     * 发送
     * @param cfg
     * @param subject
     * @param message
     * @param sendCondition
     * @param successRunnable
     */
    public static void send(SearchPropertyConfig.AppConfig cfg, String subject, String message,
                            Supplier<Boolean> sendCondition, Runnable successRunnable) {
        if (StringUtils.isBlank(cfg.getNotifyEmail().getUrl())) {
            return;
        }
        if (CollectionUtils.isEmpty(cfg.getNotifyEmail().getReceivers())) {
            return;
        }
        CommonsConstConfig.SHORT_EXECUTOR_SERVICE.submit(()-> {
            if (!sendCondition.get()) {
                return;
            }
            NotifyDto dto = NotifyDto.of(cfg.getNotifyEmail().getReceivers(), subject, message);
            try {
                String str = OBJECT_MAPPER.writeValueAsString(dto);
                String s = HttpClient.doPost(cfg.getNotifyEmail().getUrl(), str);
                log.info("sendMail subject:{} message:{} result:{}", subject, message, s);
                if ( Objects.equals("Success", s)) {
                    successRunnable.run();
                }
            } catch (Exception ex) {
                log.error("sendMail failed.", ex);
            }
        });
    }
}
