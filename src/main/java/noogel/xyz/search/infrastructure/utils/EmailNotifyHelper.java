package noogel.xyz.search.infrastructure.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.CommonsConstConfig;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

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

    public static boolean can() {
        String url = EnvHelper.FuncEnv.NOTIFY.getEnv();
        if (StringUtils.isBlank(url)) {
            return false;
        }
        String[] receivers = EnvHelper.FuncEnv.NOTIFY_RECEIVERS.getEnv().split(",");
        return receivers.length >= 1;
    }

    /**
     * 发送
     * @param subject
     * @param message
     */
    public static void send(String subject, String message, Runnable successRunnable) {
        String url = EnvHelper.FuncEnv.NOTIFY.getEnv();
        if (StringUtils.isBlank(url)) {
            return;
        }
        String[] receivers = EnvHelper.FuncEnv.NOTIFY_RECEIVERS.getEnv().split(",");
        if (receivers.length < 1) {
            return;
        }
        CommonsConstConfig.SHORT_EXECUTOR_SERVICE.submit(()-> {
            NotifyDto dto = NotifyDto.of(List.of(receivers), subject, message);
            try {
                String str = OBJECT_MAPPER.writeValueAsString(dto);
                String s = HttpClient.doPost(url, str);
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
