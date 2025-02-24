package noogel.xyz.search.infrastructure.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfiguration {
//
//    @Bean
//    public QdrantClient qdrantClient(ConfigProperties configProperties) {
//        return new QdrantClient(
//                QdrantGrpcClient.newBuilder("localhost", 6334, false).build());
////
////        QdrantClient customClient = new QdrantClient.Builder()
////                .setStorageOptions(
////                        new StorageOptions()
////                                .setCacheSize(512 * 1024 * 1024)  // 内存缓存上限
////                                .setWalBufferSize(64)             // 预写日志缓冲区(MB)
////                )
////                .setConnectionTimeout(30, TimeUnit.SECONDS)
////                .setRetryPolicy(RetryPolicy.fixed(3, 500)) // 重试策略
////                .build();
//    }
//
//    @Bean
//    public VectorStore qdrantVectorStore(QdrantClient qdrantClient, EmbeddingModel embeddingModel) {
//        return QdrantVectorStore.builder(qdrantClient, embeddingModel)
//                .collectionName("vector_store")
//                .initializeSchema(true)
//                .batchingStrategy(new TokenCountBatchingStrategy())
//                .build();
//    }

    @Bean
    @ConditionalOnMissingBean
    public VectorStore simpleVectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel)
                .batchingStrategy(new TokenCountBatchingStrategy())
                .build();
    }

}
