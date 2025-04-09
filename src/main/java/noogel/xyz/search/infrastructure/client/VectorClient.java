package noogel.xyz.search.infrastructure.client;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.ConfigProperties;
import noogel.xyz.search.infrastructure.event.ConfigAppUpdateEvent;
import noogel.xyz.search.infrastructure.utils.JsonHelper;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStoreOptions;
import org.springframework.ai.vectorstore.elasticsearch.SimilarityFunction;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Slf4j
public class VectorClient {

    @Getter
    private VectorStore vectorStore;

    @Resource
    private ConfigProperties configProperties;
    @Resource
    private OllamaClient ollamaClient;
    @Resource
    private ElasticClient elasticClient;

    @PostConstruct
    public void init() {
        try {
            if (configProperties.getApp().getChat().getElastic().isEnable()) {
                initClient();
            }
        } catch (Exception e) {
            log.error("vector 配置初始化失败", e);
        }
    }

    @EventListener(ConfigAppUpdateEvent.class)
    public void configAppUpdate(ConfigAppUpdateEvent event) {
        try {
            ConfigProperties.App newApp = event.getNewApp();
            ConfigProperties.App oldApp = event.getOldApp();
            if (!Objects.equals(JsonHelper.toJson(newApp.getChat().getElastic()),
                    JsonHelper.toJson(oldApp.getChat().getElastic()))) {
                initClient();
            }
        } catch (Exception e) {
            log.error("vector 配置更新失败", e);
        }
    }

    /**
     * 初始化 qdrant 客户端
     * 需要先配置好 ollama 客户端 embedding 配置
     */
    private void initClient() {
        try {
            ConfigProperties.Chat chat = configProperties.getApp().getChat();
            if (chat.getElastic().isEnable() && chat.getOllama().isEnable()) {
                this.vectorStore = vectorStore();
                log.info("vector 配置初始化成功");
            }
        } catch (Exception e) {
            log.error("vector 配置初始化失败", e);
        }
    }

    private RestClient restClient() {
        ConfigProperties.Elastic elastic = configProperties.getApp().getChat().getElastic();
        RestClient restClient = RestClient.builder(HttpHost.create(elastic.getHost())).build();
        // todo 账户名和鉴权
        return restClient;
    }

    private VectorStore vectorStore() {
        OllamaEmbeddingModel embeddingModel = ollamaClient.getEmbeddingModel();
        if (embeddingModel == null) {
            throw new RuntimeException("embeddingModel 为空，请先配置好 ollama 客户端 embedding 配置");
        }
        ConfigProperties.Elastic elastic = configProperties.getApp().getChat().getElastic();
        ConfigProperties.Runtime runtime = configProperties.getRuntime();
        ElasticsearchVectorStoreOptions options = new ElasticsearchVectorStoreOptions();
        options.setIndexName(runtime.getVectorIndexName());    // Optional: defaults to "spring-ai-document-index"
        options.setSimilarity(similarityFunction(elastic.getSimilarity()));           // Optional: defaults to COSINE
        options.setDimensions(elastic.getDimensions());             // Optional: defaults to model dimensions or 1536

        ElasticsearchVectorStore store = ElasticsearchVectorStore.builder(restClient(), embeddingModel)
                .options(options)                     // Optional: use custom options
                .initializeSchema(true)               // Optional: defaults to false
                .batchingStrategy(new TokenCountBatchingStrategy()) // Optional: defaults to TokenCountBatchingStrategy
                .build();
        store.afterPropertiesSet();
        return store;
    }

    private static SimilarityFunction similarityFunction(String similarity) {
        if (SimilarityFunction.cosine.name().equalsIgnoreCase(similarity)) {
            return SimilarityFunction.cosine;
        } else if (SimilarityFunction.dot_product.name().equalsIgnoreCase(similarity)) {
            return SimilarityFunction.dot_product;
        } else if (SimilarityFunction.l2_norm.name().equalsIgnoreCase(similarity)) {
            return SimilarityFunction.l2_norm;
        } else {
            return SimilarityFunction.cosine;
        }
    }

}