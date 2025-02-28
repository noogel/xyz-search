package noogel.xyz.search.infrastructure.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.ai.embedding.*;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VectorStoreConfiguration {

    @Bean
    public QdrantClient qdrantClient(ConfigProperties configProperties) {
        return new QdrantClient(
                QdrantGrpcClient.newBuilder("localhost", 6334, false).build());
    }

    @Bean
    public VectorStore qdrantVectorStore(QdrantClient qdrantClient, EmbeddingModel embeddingModel) {
        return QdrantVectorStore.builder(qdrantClient, embeddingModel)
                .collectionName("vector_store")
                .initializeSchema(true)
                .batchingStrategy(new TokenCountBatchingStrategy())
                .build();
    }

     @Bean
     @ConditionalOnMissingBean
     public VectorStore simpleVectorStore(EmbeddingModel embeddingModel) {
         return SimpleVectorStore.builder(embeddingModel)
                 .batchingStrategy(new TokenCountBatchingStrategy())
                 .build();
     }

}
