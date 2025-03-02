package noogel.xyz.search.infrastructure.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class HttpClient {
    // 设置连接主机服务超时时间
    private static final int CONNECT_TIMEOUT = 6000;
    // 设置连接请求超时时间
    private static final int CONNECT_REQ_TIMEOUT = 6000;
    // 设置读取数据连接超时时间
    private static final int CONNECT_SOCKET_TIMEOUT = 6000;

    private static final Map<Integer, RequestConfig> REQUEST_CONFIG_MAP = new ConcurrentHashMap<>();

    public static String doGet(String url) {
        return doGet(url, CONNECT_SOCKET_TIMEOUT);
    }

    public static String doGet(String url, int socketTimeout) {
        RequestConfig requestConfig = REQUEST_CONFIG_MAP.computeIfAbsent(
                socketTimeout,
                t -> RequestConfig.custom()
                        .setConnectionRequestTimeout(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
                        .setResponseTimeout(Timeout.of(t, TimeUnit.MILLISECONDS))
                        .build());
        // 通过址默认配置创建一个httpClient实例
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // 创建httpGet远程连接实例
            HttpGet httpGet = new HttpGet(url);
            // 为httpGet实例设置配置
            httpGet.setConfig(requestConfig);
            // 执行get请求得到返回对象
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                // 通过返回对象获取返回数据
                HttpEntity entity = response.getEntity();
                // 通过EntityUtils中的toString方法将结果转换为字符串
                return EntityUtils.toString(entity);
            }
        } catch (IOException | ParseException e) {
            log.error("doGet error url: {}", url);
        }
        return "";
    }

    public static String doPost(String url, String jsonString) {
        return doPost(url, jsonString, CONNECT_SOCKET_TIMEOUT);
    }

    public static String doPost(String url, String jsonString, int socketTimeout) {
        RequestConfig requestConfig = REQUEST_CONFIG_MAP.computeIfAbsent(
                socketTimeout,
                t -> RequestConfig.custom()
                        .setConnectionRequestTimeout(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
                        .setResponseTimeout(Timeout.of(t, TimeUnit.MILLISECONDS))
                        .build());
        // 创建httpClient实例
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // 创建httpPost远程连接实例
            HttpPost httpPost = new HttpPost(url);
            // 为httpPost实例设置配置
            httpPost.setConfig(requestConfig);
            // 设置请求头
            httpPost.addHeader("Content-Type", "application/json; charset=utf-8");
            httpPost.addHeader("Accept", "application/json");
            // 封装post请求参数
            httpPost.setEntity(new StringEntity(jsonString, Charset.defaultCharset()));
            // httpClient对象执行post请求,并返回响应参数对象
            try (CloseableHttpResponse httpResponse = httpClient.execute(httpPost)) {
                // 从响应对象中获取响应内容
                HttpEntity entity = httpResponse.getEntity();
                return EntityUtils.toString(entity);
            }
        } catch (IOException | ParseException e) {
            log.error("doPost error url: {}", url);
        }
        return "";
    }
}
