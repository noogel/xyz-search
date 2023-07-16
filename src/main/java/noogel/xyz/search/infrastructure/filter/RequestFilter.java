package noogel.xyz.search.infrastructure.filter;

import jakarta.annotation.Resource;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.SearchPropertyConfig;
import noogel.xyz.search.infrastructure.utils.EmailNotifyHelper;
import noogel.xyz.search.infrastructure.utils.IpUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RequestFilter implements Filter {
    private static final Map<String, Long> HASH_TIME_MAP = new ConcurrentHashMap<>();
    private static final long TIME_SHIFT = 600 * 1000L; // 10 分钟

    @Resource
    private SearchPropertyConfig.SearchConfig searchConfig;

    @Scheduled(fixedRate = TIME_SHIFT * 144)
    public void removeExpiredRecord() {
        // 移除过期 IP
        long nowTs = Instant.now().toEpochMilli();
        ArrayList<String> expiredKey = new ArrayList<>();
        HASH_TIME_MAP.forEach((key, val) -> {
            if (nowTs - val > TIME_SHIFT) {
                expiredKey.add(key);
            }
        });
        expiredKey.forEach(HASH_TIME_MAP::remove);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) servletRequest;
        long nowTs = Instant.now().toEpochMilli();
        try {
            String remoteIP = IpUtils.getIpAddr(req);
            String serverName = req.getServerName();
            String hashKey = String.format("%s:%s", remoteIP, serverName);
            // 存在 key 并且 时差小于 x，则不发邮件。
            if (HASH_TIME_MAP.containsKey(hashKey) && nowTs - HASH_TIME_MAP.get(hashKey) < TIME_SHIFT) {
                // x 时间内不重复通知，不随访问更新。
                // IP_TIME_MAP.put(remoteIP, nowTs);
                log.info("畅文全索请求更新，ip:{}， 访问路径：{} {} 访问参数：{}", remoteIP, req.getMethod(),
                        req.getRequestURL(), req.getParameterMap());
            } else {
                String subject = String.format("畅文: %s -> %s", remoteIP, serverName);
                String msg = String.format("畅文全索请求通知：<br/><br/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
                                "新 IP:%s，访问时间：%s，访问路径：%s %s，访问参数：%s",
                        remoteIP, LocalDateTime.now(), req.getMethod(), req.getRequestURL(), req.getParameterMap());
                EmailNotifyHelper.send(searchConfig.getApp(), subject, msg,
                        ()-> !HASH_TIME_MAP.containsKey(hashKey),
                        () -> HASH_TIME_MAP.put(hashKey, nowTs));
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
