package noogel.xyz.search.infrastructure.client;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Objects;

import javax.net.ssl.SSLContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClient;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.ConfigProperties;
import noogel.xyz.search.infrastructure.event.ConfigAppUpdateEvent;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import noogel.xyz.search.infrastructure.utils.JsonHelper;

@Component
@Slf4j
public class ElasticClient {

    private ClientHolder holder = null;
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_INTERVAL = 5000; // 5秒

    @Resource
    private ConfigProperties configProperties;

    @PostConstruct
    public void init() {
        for(int i = 0; i < MAX_RETRIES; i++) {
            try {
                if (configProperties.getApp().getChat().getElastic().isEnable()) {
                    log.info("开始初始化 ES 客户端，第{}次尝试", i + 1);
                    loadClient();
                    log.info("ES 客户端初始化成功");
                    break;
                }
            } catch (Exception e) {
                log.error("elastic 配置初始化失败，第{}次重试", i + 1, e);
                if(i < MAX_RETRIES - 1) {
                    try {
                        Thread.sleep(RETRY_INTERVAL);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }

    @EventListener(ConfigAppUpdateEvent.class)
    public void configAppUpdate(ConfigAppUpdateEvent event) {
        try {
            ConfigProperties.App newApp = event.getNewApp();
            ConfigProperties.App oldApp = event.getOldApp();
            if (!Objects.equals(JsonHelper.toJson(newApp.getChat().getElastic()),
                    JsonHelper.toJson(oldApp.getChat().getElastic()))) {
                loadClient();
            }
        } catch (Exception e) {
            log.error("elastic 配置更新失败", e);
        }
    }

    @NoArgsConstructor
    @AllArgsConstructor(staticName = "of")
    static class ClientHolder {
        @Getter
        private ElasticsearchClient client;

        /**
         * 销毁客户端
         */
        public void destroy() {
            if (Objects.nonNull(client)) {
                try {
                    client.close();
                } catch (Exception e) {
                    log.error("elastic 客户端关闭失败", e);
                }
            }
        }
    }

    /**
     * 重新创建客户端
     */
    public void loadClient() {
        if (Objects.nonNull(configProperties.getApp().getChat().getElastic())) {
            ConfigProperties.Elastic elastic = configProperties.getApp().getChat().getElastic();
            log.info("开始创建 ES 客户端, 配置: host={}, timeout={}", elastic.getHost(), elastic.getConnectionTimeout());
            holder = ClientHolder.of(genClient(elastic));
        }
    }

    /**
     * 获取客户端
     * 
     * @return
     */
    public ElasticsearchClient getClient() {
        if (Objects.isNull(holder)) {
            return null;
        }
        return holder.getClient();
    }

    /**
     * 生成客户端
     * 
     * @param sc
     * @return
     */
    @SneakyThrows
    public ElasticsearchClient genClient(ConfigProperties.Elastic sc) {
        try {
            // 设置默认超时时间
            if (sc.getConnectionTimeout() == null) {
                sc.setConnectionTimeout(30000); // 30秒
            }
            if (sc.getSocketTimeout() == null) {
                sc.setSocketTimeout(60000); // 60秒
            }

            final RestClient restClient = RestClient.builder(HttpHost.create(sc.getHost()))
                    .setRequestConfigCallback(builder -> builder
                            .setConnectTimeout(sc.getConnectionTimeout())
                            .setSocketTimeout(sc.getSocketTimeout())
                            .setConnectionRequestTimeout(20000))
                    .setHttpClientConfigCallback(builder -> {
                        if (StringUtils.isNotBlank(sc.getUsername()) && StringUtils.isNotBlank(sc.getPassword())) {
                            log.info("配置 ES 认证信息");
                            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                            credentialsProvider.setCredentials(AuthScope.ANY,
                                    new UsernamePasswordCredentials(sc.getUsername(), sc.getPassword()));
                            builder.setDefaultCredentialsProvider(credentialsProvider);
                        }

                        if (StringUtils.isNotBlank(sc.getCaPath())) {
                            log.info("配置 ES SSL 证书");
                            try {
                                Path caCertificatePath = Paths.get(sc.getCaPath());
                                CertificateFactory factory = CertificateFactory.getInstance("X.509");
                                Certificate trustedCa;
                                try (InputStream is = Files.newInputStream(caCertificatePath)) {
                                    trustedCa = factory.generateCertificate(is);
                                }
                                KeyStore trustStore = KeyStore.getInstance("pkcs12");
                                trustStore.load(null, null);
                                trustStore.setCertificateEntry("ca", trustedCa);
                                SSLContextBuilder sslContextBuilder = SSLContexts.custom()
                                        .loadTrustMaterial(trustStore, null);
                                final SSLContext sslContext = sslContextBuilder.build();
                                builder.setSSLContext(sslContext);
                            } catch (Exception ex) {
                                log.error("配置 SSL 证书失败", ex);
                                throw ExceptionCode.CONFIG_ERROR.throwExc(ex);
                            }
                        }

                        // 设置最大重试次数
                        builder.setMaxConnTotal(50)
                              .setMaxConnPerRoute(10)
                              .disableAuthCaching()
                              .setKeepAliveStrategy((response, context) -> 30000);

                        return builder;
                    }).build();

            log.info("创建 ES transport");
            final ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());

            log.info("创建 ES client");
            final ElasticsearchClient client = new ElasticsearchClient(transport);

            return client;
        } catch (Exception e) {
            log.error("ES 客户端创建失败", e);
            throw e;
        }
    }
}