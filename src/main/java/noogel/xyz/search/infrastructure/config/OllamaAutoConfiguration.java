package noogel.xyz.search.infrastructure.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationConvention;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.ai.ollama.management.PullModelStrategy;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Configuration
public class OllamaAutoConfiguration {

    @Bean
    public OllamaApi ollamaApi(ConfigProperties configProperties,
                               ObjectProvider<RestClient.Builder> restClientBuilderProvider,
                               ObjectProvider<WebClient.Builder> webClientBuilderProvider) {
        String baseUrl = configProperties.getApp().getChat().getOllama().getBaseUrl();
        return new OllamaApi(baseUrl,
                restClientBuilderProvider.getIfAvailable(RestClient::builder),
                webClientBuilderProvider.getIfAvailable(WebClient::builder));
    }

    @Bean
    public OllamaOptions ollamaOptions(ConfigProperties configProperties) {
        ConfigProperties.Ollama ollama = configProperties.getApp().getChat().getOllama();
        OllamaOptions options = OllamaOptions.builder().build();
        options.setModel(ollama.getChatModel());
        options.setTopK(40);
        options.setTopP(0.8);
        options.setNumCtx(4096);
        options.setTemperature(Double.parseDouble(ollama.getChatOptionTemperature()));
        options.setNumPredict(Integer.parseInt(ollama.getChatOptionNumPredict()));
        return options;
    }

    @Bean
    public ModelManagementOptions modelManagementOptions(ConfigProperties configProperties) {
        ConfigProperties.Ollama ollama = configProperties.getApp().getChat().getOllama();
        var modelPullStrategy = PullModelStrategy.valueOf(ollama.getPullModelStrategy().toUpperCase());
        return new ModelManagementOptions(modelPullStrategy, ollama.getEmbeddingAdditionalModels(), Duration.ofMinutes(5), 0);
    }

    @Bean
    public OllamaChatModel ollamaChatModel(OllamaApi ollamaApi,
                                           OllamaOptions ollamaOptions,
                                           ModelManagementOptions modelManagementOptions,
                                           ObjectProvider<ObservationRegistry> observationRegistry,
                                           ObjectProvider<ChatModelObservationConvention> observationConvention) {
        var chatModel = OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(ollamaOptions)
                .observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
                .modelManagementOptions(modelManagementOptions)
                .build();
        observationConvention.ifAvailable(chatModel::setObservationConvention);
        return chatModel;
    }

    @Bean
    public OllamaEmbeddingModel ollamaEmbeddingModel(OllamaApi ollamaApi,
                                                     OllamaOptions ollamaOptions,
                                                     ModelManagementOptions modelManagementOptions,
                                                     ObjectProvider<ObservationRegistry> observationRegistry,
                                                     ObjectProvider<EmbeddingModelObservationConvention> observationConvention) {
        var embeddingModel = OllamaEmbeddingModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(ollamaOptions)
                .observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
                .modelManagementOptions(modelManagementOptions)
                .build();
        observationConvention.ifAvailable(embeddingModel::setObservationConvention);
        return embeddingModel;
    }

}
