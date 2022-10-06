package noogel.xyz.search.infrastructure.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

@Configuration
public class ElasticsearchClientConfig {

    @Resource
    private SearchPropertyConfig.SearchConfig searchConfig;

    @Bean
    public ElasticsearchClient elasticsearchClient() {

        // Create the low-level client
        final RestClient restClient = RestClient
                .builder(HttpHost.create(searchConfig.getElasticsearchHost()))
                // 超时设置
                .setRequestConfigCallback(builder -> builder
                        .setConnectTimeout(searchConfig.getElasticsearchConnectionTimeout())
                        .setSocketTimeout(searchConfig.getElasticsearchSocketTimeout()))
                .setHttpClientConfigCallback(builder -> {
                    // 如果开启了认证
                    if (StringUtils.isNotBlank(searchConfig.getElasticsearchUser())
                            && StringUtils.isNotBlank(searchConfig.getElasticsearchPassword())) {
                        // 认证
                        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(
                                searchConfig.getElasticsearchUser(), searchConfig.getElasticsearchPassword()));
                        builder.setDefaultCredentialsProvider(credentialsProvider);
                    }
                    // CA 证书
                    if (StringUtils.isNotBlank(searchConfig.getElasticsearchCAPath())) {
                        try {
                            Path caCertificatePath = Paths.get(searchConfig.getElasticsearchCAPath());
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
