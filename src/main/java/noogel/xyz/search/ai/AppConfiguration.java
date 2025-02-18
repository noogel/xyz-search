package noogel.xyz.search.ai;

import io.qdrant.client.QdrantClient;
import noogel.xyz.search.infrastructure.config.ConfigProperties;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfiguration {

    @Bean
    public QdrantClient qdrantClient(ConfigProperties configProperties) {
        return null;
//
//        QdrantClient customClient = new QdrantClient.Builder()
//                .setStorageOptions(
//                        new StorageOptions()
//                                .setCacheSize(512 * 1024 * 1024)  // 内存缓存上限
//                                .setWalBufferSize(64)             // 预写日志缓冲区(MB)
//                )
//                .setConnectionTimeout(30, TimeUnit.SECONDS)
//                .setRetryPolicy(RetryPolicy.fixed(3, 500)) // 重试策略
//                .build();
    }

    @Bean
    public QdrantVectorStore qdrantVectorStore(QdrantClient qdrantClient, EmbeddingModel embeddingModel) {
        return QdrantVectorStore.builder(qdrantClient, embeddingModel).build();
    }
}
