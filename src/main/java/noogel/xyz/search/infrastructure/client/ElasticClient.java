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

    @Resource
    private ConfigProperties configProperties;

    @PostConstruct
    public void init() {
        try {
            if (configProperties.getApp().getChat().isEnable()) {
                loadClient();
            }
        } catch (Exception e) {
            log.error("elastic 配置初始化失败", e);
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
            holder = ClientHolder.of(genClient(configProperties.getApp().getChat().getElastic()));
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

        // Create the low-level client
        final RestClient restClient = RestClient.builder(HttpHost.create(sc.getHost()))
                // 超时设置
                .setRequestConfigCallback(builder -> builder.setConnectTimeout(sc.getConnectionTimeout())
                        .setSocketTimeout(sc.getSocketTimeout()))
                .setHttpClientConfigCallback(builder -> {
                    // 如果开启了认证
                    if (StringUtils.isNotBlank(sc.getUser()) && StringUtils.isNotBlank(sc.getPassword())) {
                        // 认证
                        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                        credentialsProvider.setCredentials(AuthScope.ANY,
                                new UsernamePasswordCredentials(sc.getUser(), sc.getPassword()));
                        builder.setDefaultCredentialsProvider(credentialsProvider);
                    }
                    // CA 证书
                    if (StringUtils.isNotBlank(sc.getCaPath())) {
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
                            SSLContextBuilder sslContextBuilder = SSLContexts.custom().loadTrustMaterial(trustStore,
                                    null);
                            final SSLContext sslContext = sslContextBuilder.build();
                            builder.setSSLContext(sslContext);
                        } catch (Exception ex) {
                            throw ExceptionCode.CONFIG_ERROR.throwExc(ex);
                        }
                    }
                    return builder;
                }).build();

        // Create the transport with a Jackson mapper
        final ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());

        // And create the API client
        final ElasticsearchClient client = new ElasticsearchClient(transport);

        return client;
    }
}