package noogel.xyz.search.infrastructure.filter;

import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.utils.EmailNotifyHelper;
import noogel.xyz.search.infrastructure.utils.IpUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RequestFilter implements Filter {
    private static final Map<String, Long> IP_TIME_MAP = new ConcurrentHashMap<>();
    private static final long TIME_SHIFT = 3600 * 1000 * 3L; // 3 小时

    @Scheduled(fixedRate = TIME_SHIFT)
    public void removeExpiredRecord() {
        log.info("remove expired request record.");
        // 移除过期 IP
        long nowTs = Instant.now().toEpochMilli();
        ArrayList<String> ips = new ArrayList<>();
        IP_TIME_MAP.forEach((key, val) -> {
            if (nowTs - val > TIME_SHIFT) {
                ips.add(key);
            }
        });
        ips.forEach(IP_TIME_MAP::remove);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) servletRequest;
        long nowTs = Instant.now().toEpochMilli();
        try {
            String remoteIP = IpUtils.getIpAddr(req);
            // 存在 key 并且 时差小于 3 小时，则不发邮件。
            if (IP_TIME_MAP.containsKey(remoteIP) && nowTs - IP_TIME_MAP.get(remoteIP) < TIME_SHIFT) {
                IP_TIME_MAP.put(remoteIP, nowTs);
                log.info("畅文全索请求更新，ip:{}， 访问路径：{} {}", remoteIP, req.getMethod(), req.getRequestURL());
            } else {
                String subject = "畅文:" + remoteIP;
                String msg = String.format("畅文全索请求通知：<br/><br/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
                                "新 IP:%s，访问时间：%s，访问路径：%s %s",
                        remoteIP, LocalDateTime.now(), req.getMethod(), req.getRequestURL());
                EmailNotifyHelper.send(subject, msg, () -> IP_TIME_MAP.put(remoteIP, nowTs));
            }
        } catch (Exception ex) {
            log.error("畅文全索请求发送通知失败", ex);
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }
}
