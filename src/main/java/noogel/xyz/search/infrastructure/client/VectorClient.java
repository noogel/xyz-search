package noogel.xyz.search.infrastructure.client;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.ConfigProperties;
import noogel.xyz.search.infrastructure.event.ConfigAppUpdateEvent;
import noogel.xyz.search.infrastructure.utils.JsonHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Slf4j
public class VectorClient {

    @Getter
    private VectorStore vectorStore;
    @Getter
    private QdrantClient qdrantClient;

    @Resource
    private ConfigProperties configProperties;
    @Resource
    private OllamaClient ollamaClient;

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
     * 初始化 qdrant 客户端 需要先配置好 ollama 客户端 embedding 配置
     */
    private void initClient() {
        try {
            ConfigProperties.Chat chat = configProperties.getApp().getChat();
            if (chat.getQdrant().isEnable() && chat.getOllama().isEnable()) {
                this.vectorStore = vectorStore();
                log.info("vector 配置初始化成功");
            } else {
                this.vectorStore = null;
            }
        } catch (Exception e) {
            log.error("vector 配置初始化失败", e);
        }
    }

    public boolean isEnabled() {
        return Objects.nonNull(this.vectorStore);
    }

    public void reset() {
        try {
            if (Objects.nonNull(this.vectorStore)) {
                ConfigProperties.Runtime runtime = configProperties.getRuntime();
                this.qdrantClient.deleteCollectionAsync(runtime.getVectorIndexName()).get();
                ((QdrantVectorStore) this.vectorStore).afterPropertiesSet();
            }
        } catch (Exception e) {
            log.error("vector 配置初始化失败", e);
        }
    }

    /**
     * 初始化 qdrant 客户端 需要先配置好 ollama 客户端 embedding 配置
     */
    private VectorStore vectorStore() {
        OllamaEmbeddingModel embeddingModel = ollamaClient.getEmbeddingModel();
        if (embeddingModel == null) {
            throw new RuntimeException("embeddingModel 为空，请先配置好 ollama 客户端 embedding 配置");
        }
        ConfigProperties.Qdrant qdrant = configProperties.getApp().getChat().getQdrant();
        ConfigProperties.Runtime runtime = configProperties.getRuntime();

        QdrantGrpcClient.Builder grpcClientBuilder = QdrantGrpcClient.newBuilder(qdrant.getHost(), qdrant.getPort(), false);
        if (StringUtils.isNotBlank(qdrant.getApiKey())) {
            grpcClientBuilder.withApiKey(qdrant.getApiKey());
        }

        this.qdrantClient = new QdrantClient(grpcClientBuilder.build());

        QdrantVectorStore qdrantVectorStore = QdrantVectorStore.builder(this.qdrantClient, embeddingModel)
                .collectionName(runtime.getVectorIndexName()) // Optional: defaults to "vector_store"
                .initializeSchema(true) // Optional: defaults to false
                .batchingStrategy(new TokenCountBatchingStrategy()) // Optional: defaults to TokenCountBatchingStrategy
                .build();
        try {
            qdrantVectorStore.afterPropertiesSet();
        } catch (Exception e) {
            log.error("vector 配置初始化失败", e);
            throw new RuntimeException("qdrantClient 初始化失败", e);
        }
        return qdrantVectorStore;
    }

}