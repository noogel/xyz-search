package noogel.xyz.search.infrastructure.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

@Configuration
public class ElasticsearchClientConfig {

    @Resource
    private SearchPropertyConfig.SearchConfig searchConfig;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        // Create the low-level client
        final RestClient restClient = RestClient
                .builder(new HttpHost(searchConfig.getElasticsearchHost(), searchConfig.getElasticsearchPort()))
                .setRequestConfigCallback(builder -> builder
                        .setConnectTimeout(searchConfig.getElasticsearchConnectionTimeout())
                        .setSocketTimeout(searchConfig.getElasticsearchSocketTimeout()))
                .build();

        // Create the transport with a Jackson mapper
        final ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper());

        // And create the API client
        final ElasticsearchClient client = new ElasticsearchClient(transport);

        return client;
    }
}
