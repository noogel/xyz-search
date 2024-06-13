package noogel.xyz.search.infrastructure.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
                        .setConnectTimeout(CONNECT_TIMEOUT)
                        .setConnectionRequestTimeout(CONNECT_REQ_TIMEOUT)
                        .setSocketTimeout(t)
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
        } catch (IOException e) {
            e.printStackTrace();
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
                        .setConnectTimeout(CONNECT_TIMEOUT)
                        .setConnectionRequestTimeout(CONNECT_REQ_TIMEOUT)
                        .setSocketTimeout(t)
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
            httpPost.setEntity(new StringEntity(jsonString, "UTF-8"));
            // httpClient对象执行post请求,并返回响应参数对象
            try (CloseableHttpResponse httpResponse = httpClient.execute(httpPost)) {
                // 从响应对象中获取响应内容
                HttpEntity entity = httpResponse.getEntity();
                return EntityUtils.toString(entity);
            }
        } catch (IOException e) {
            log.error("doPost error {} | {}", url, jsonString.length());
        }
        return "";
    }
}
