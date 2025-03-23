package noogel.xyz.search.infrastructure.config;

import java.util.Objects;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.event.ConfigAppUpdateEvent;
import noogel.xyz.search.infrastructure.utils.JsonHelper;

@Component
@Slf4j
public class VectorClient {

    private QdrantClient qdrantClient;
    @Getter
    private VectorStore vectorStore;

    @Resource
    private ConfigProperties configProperties;
    @Resource
    private OllamaClient ollamaClient;

    @PostConstruct
    public void init() {
        try {
            if (configProperties.getApp().getChat().getQdrant() != null
                    && configProperties.getApp().getChat().getQdrant().getHost() != null
                    && configProperties.getApp().getChat().getQdrant().getPort() != null) {
                initClient();
            }
        } catch (Exception e) {
            log.error("qdrant 配置初始化失败", e);
        }
    }

    @EventListener(ConfigAppUpdateEvent.class)
    public void configAppUpdate(ConfigAppUpdateEvent event) {
        try {
            ConfigProperties.App newApp = event.getNewApp();
            ConfigProperties.App oldApp = event.getOldApp();
            if (!Objects.equals(JsonHelper.toJson(newApp.getChat().getQdrant()),
                    JsonHelper.toJson(oldApp.getChat().getQdrant()))) {
                initClient();
            }
        } catch (Exception e) {
            log.error("qdrant 配置更新失败", e);
        }
    }

    /**
     * 初始化 qdrant 客户端
     * 需要先配置好 ollama 客户端 embedding 配置
     */
    private void initClient() {
        try {
            if (configProperties.getApp().getChat().getQdrant() != null
                    && configProperties.getApp().getChat().getQdrant().getHost() != null
                    && configProperties.getApp().getChat().getQdrant().getPort() != null) {
                this.qdrantClient = qdrantClient();
                this.vectorStore = vectorStore(this.qdrantClient, ollamaClient.getEmbeddingModel());
            }
        } catch (Exception e) {
            log.error("qdrant 配置初始化失败", e);
        }
    }

    private QdrantClient qdrantClient() {
        String host = configProperties.getApp().getChat().getQdrant().getHost();
        Integer port = configProperties.getApp().getChat().getQdrant().getPort();
        String apiKey = configProperties.getApp().getChat().getQdrant().getApiKey();
        boolean useTransportLayerSecurity = host.startsWith("https://");
        QdrantGrpcClient.Builder grpcClientBuilder = QdrantGrpcClient.newBuilder(host, port, useTransportLayerSecurity);
        if (apiKey != null && !apiKey.isEmpty()) {
            grpcClientBuilder.withApiKey(apiKey);
        }
        return new QdrantClient(grpcClientBuilder.build());
    }

    private VectorStore vectorStore(QdrantClient qdrantClient, EmbeddingModel embeddingModel) {
        if (embeddingModel == null) {
            throw new RuntimeException("embeddingModel 为空，请先配置好 ollama 客户端 embedding 配置");
        }
        return QdrantVectorStore.builder(qdrantClient, embeddingModel)
                .collectionName("custom-collection") // Optional: defaults to "vector_store"
                .initializeSchema(true) // Optional: defaults to false
                .batchingStrategy(new TokenCountBatchingStrategy()) // Optional: defaults to TokenCountBatchingStrategy
                .build();
    }
}