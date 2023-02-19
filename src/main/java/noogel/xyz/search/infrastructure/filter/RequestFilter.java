package noogel.xyz.search.infrastructure.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.utils.EmailNotifyHelper;
import noogel.xyz.search.infrastructure.utils.IpUtils;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class RequestFilter implements Filter {
    private static final Map<String, Long> IP_TIME_MAP = new ConcurrentHashMap<>();
    private static final long TIME_SHIFT = 600 * 1000L; // 10 分钟

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
            // 存在 key 并且 时差小于 x，则不发邮件。
            if (IP_TIME_MAP.containsKey(remoteIP) && nowTs - IP_TIME_MAP.get(remoteIP) < TIME_SHIFT) {
                // x 时间内不重复通知，不随访问更新。
                // IP_TIME_MAP.put(remoteIP, nowTs);
                log.info("畅文全索请求更新，ip:{}， 访问路径：{} {} 访问参数：{}", remoteIP, req.getMethod(),
                        req.getRequestURL(), req.getParameterMap());
            } else {
                String subject = "畅文:" + remoteIP;
                String msg = String.format("畅文全索请求通知：<br/><br/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
                                "新 IP:%s，访问时间：%s，访问路径：%s %s，访问参数：%s",
                        remoteIP, LocalDateTime.now(), req.getMethod(), req.getRequestURL(), req.getParameterMap());
                EmailNotifyHelper.send(subject, msg,
                        ()-> !IP_TIME_MAP.containsKey(remoteIP),
                        () -> IP_TIME_MAP.put(remoteIP, nowTs));
            }
        } catch (Exception ex) {
            log.error("畅文全索请求发送通知失败", ex);
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }
}
