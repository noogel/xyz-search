package noogel.xyz.search.infrastructure.filter;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.ConfigProperties;
import noogel.xyz.search.infrastructure.utils.EmailNotifyHelper;
import noogel.xyz.search.infrastructure.utils.IpUtils;
import noogel.xyz.search.infrastructure.utils.JsonHelper;

@Slf4j
@Component
public class RequestFilter implements Filter {
    private static final Map<String, Long> ACCESS_TIME_MAP = new ConcurrentHashMap<>();
    private static final long CLEANUP_INTERVAL = 3600 * 1000L; // 每小时清理一次

    @Resource
    private ConfigProperties configProperties;

    @Scheduled(fixedRate = CLEANUP_INTERVAL)
    public void removeExpiredRecord() {
        long nowTs = Instant.now().toEpochMilli();
        Long notifyInterval = getNotifyInterval();
        if (notifyInterval == null) {
            return;
        }

        ArrayList<String> expiredKeys = new ArrayList<>();
        ACCESS_TIME_MAP.forEach((key, timestamp) -> {
            if (nowTs - timestamp > notifyInterval) {
                expiredKeys.add(key);
            }
        });

        if (!expiredKeys.isEmpty()) {
            expiredKeys.forEach(ACCESS_TIME_MAP::remove);
            log.debug("Cleaned up {} expired access records", expiredKeys.size());
        }
    }

    private Long getNotifyInterval() {
        if (configProperties.getApp().getNotify() == null) {
            return null;
        }
        Integer hours = configProperties.getApp().getNotify().getAccessIntervalHours();
        return hours != null ? hours * 3600 * 1000L : null;
    }

    private boolean shouldNotify(String hashKey, long nowTs) {
        if (getNotifyInterval() == null) {
            return false;
        }
        Long lastAccessTime = ACCESS_TIME_MAP.get(hashKey);
        return lastAccessTime == null || (nowTs - lastAccessTime >= getNotifyInterval());
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) servletRequest;
        long nowTs = Instant.now().toEpochMilli();

        try {
            String remoteIP = IpUtils.getIpAddr(req);
            String serverName = req.getServerName();
            String hashKey = String.format("%s:%s", remoteIP, serverName);

            if (shouldNotify(hashKey, nowTs)) {
                String subject = String.format("畅文: %s -> %s", remoteIP, serverName);
                String msg = String.format("畅文全索请求通知：<br/><br/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
                        "新 IP:%s，访问时间：%s，访问路径：%s %s，访问参数：%s",
                        remoteIP, LocalDateTime.now(), req.getMethod(), req.getRequestURL(),
                        JsonHelper.toJson(req.getParameterMap()));
                EmailNotifyHelper.sendBySmtp(configProperties.getApp(), subject, msg,
                        () -> shouldNotify(hashKey, nowTs),
                        () -> ACCESS_TIME_MAP.put(hashKey, nowTs));
            } else {
                log.debug("Access notification skipped for IP: {}, within notification interval", remoteIP);
            }

            HashMap<String, String> headers = new HashMap<>();
            Enumeration<String> headerNames = req.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = req.getHeader(headerName);
                headers.put(headerName, headerValue);
            }
            log.info("request headers: {}", headers);
        } catch (Exception ex) {
            log.error("畅文全索请求发送通知失败", ex);
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }
}
