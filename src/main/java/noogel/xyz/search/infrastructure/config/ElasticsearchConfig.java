package noogel.xyz.search.infrastructure.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.exception.ExceptionCode;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClient;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Objects;

@Component
@Slf4j
public class ElasticsearchConfig {

    private ElasticsearchClient client = null;

    @Resource
    private SearchPropertyConfig.SearchConfig searchConfig;

    /**
     * 重新创建客户端
     */
    public void reloadClient() {
        client = genClient(searchConfig.getApp());
    }

    /**
     * 获取客户端
     *
     * @return
     */
    public ElasticsearchClient getClient() {
        if (Objects.isNull(client)) {
            client = genClient(searchConfig.getApp());
        }
        return client;
    }

    /**
     * 生成客户端
     *
     * @param sc
     * @return
     */
    public ElasticsearchClient genClient(SearchPropertyConfig.AppConfig sc) {

        // Create the low-level client
        final RestClient restClient = RestClient
                .builder(HttpHost.create(sc.getElasticsearchHost()))
                // 超时设置
                .setRequestConfigCallback(builder -> builder
                        .setConnectTimeout(sc.getElasticsearchConnectionTimeout())
                        .setSocketTimeout(sc.getElasticsearchSocketTimeout()))
                .setHttpClientConfigCallback(builder -> {
                    // 如果开启了认证
                    if (StringUtils.isNotBlank(sc.getElasticsearchUser())
                            && StringUtils.isNotBlank(sc.getElasticsearchPassword())) {
                        // 认证
                        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(
                                sc.getElasticsearchUser(), sc.getElasticsearchPassword()));
                        builder.setDefaultCredentialsProvider(credentialsProvider);
                    }
                    // CA 证书
                    if (StringUtils.isNotBlank(sc.getElasticsearchCAPath())) {
                        try {
                            Path caCertificatePath = Paths.get(sc.getElasticsearchCAPath());
                            CertificateFactory factory = CertificateFactory.getInstance("X.509");
                            Certificate trustedCa;
                            try (InputStream is = Files.newInputStream(caCertificatePath)) {
                                trustedCa = factory.generateCertificate(is);
                            }
                            KeyStore trustStore = KeyStore.getInstance("pkcs12");
                            trustStore.load(null, null);
                            trustStore.setCertificateEntry("ca", trustedCa);
                            SSLContextBuilder sslContextBuilder = SSLContexts.custom().loadTrustMaterial(trustStore, null);
                            final SSLContext sslContext = sslContextBuilder.build();
                            builder.setSSLContext(sslContext);
                        } catch (Exception ex) {
                            throw ExceptionCode.CONFIG_ERROR.throwExc(ex);
                        }
                    }
                    return builder;
                })
                .build();

        // Create the transport with a Jackson mapper
        final ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper());

        // And create the API client
        final ElasticsearchClient client = new ElasticsearchClient(transport);
        return client;
    }
}
