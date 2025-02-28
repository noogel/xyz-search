package noogel.xyz.search.infrastructure.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationConvention;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaModel;
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
    public OllamaChatModel ollamaChatModel(OllamaApi ollamaApi,
                                           ConfigProperties configProperties,
                                           ObjectProvider<ObservationRegistry> observationRegistry,
                                           ObjectProvider<ChatModelObservationConvention> observationConvention) {
        ConfigProperties.Ollama ollama = configProperties.getApp().getChat().getOllama();
        var chatModelPullStrategy = PullModelStrategy.valueOf(ollama.getPullModelStrategy().toUpperCase());

        OllamaOptions options = OllamaOptions.builder().model(OllamaModel.MXBAI_EMBED_LARGE.id()).build();
        options.setModel(ollama.getChatModel());
        options.setTemperature(Double.parseDouble(ollama.getChatOptionTemperature()));
        options.setNumPredict(Integer.parseInt(ollama.getChatOptionNumPredict()));

        var chatModel = OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(options)
                .observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
                .modelManagementOptions(new ModelManagementOptions(chatModelPullStrategy,
                        ollama.getEmbeddingAdditionalModels(), Duration.ofMinutes(5), 0))
                .build();

        observationConvention.ifAvailable(chatModel::setObservationConvention);

        return chatModel;
    }

    @Bean
    public OllamaEmbeddingModel ollamaEmbeddingModel(OllamaApi ollamaApi, ConfigProperties configProperties,
                                                     ObjectProvider<ObservationRegistry> observationRegistry,
                                                     ObjectProvider<EmbeddingModelObservationConvention> observationConvention) {
        ConfigProperties.Ollama ollama = configProperties.getApp().getChat().getOllama();

        var embeddingModelPullStrategy = PullModelStrategy.valueOf(ollama.getPullModelStrategy().toUpperCase());

        OllamaOptions options = OllamaOptions.builder().model(OllamaModel.MXBAI_EMBED_LARGE.id()).build();
        options.setModel(ollama.getChatModel());
        options.setTemperature(Double.parseDouble(ollama.getChatOptionTemperature()));
        options.setNumPredict(Integer.parseInt(ollama.getChatOptionNumPredict()));

        var embeddingModel = OllamaEmbeddingModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(options)
                .observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
                .modelManagementOptions(new ModelManagementOptions(embeddingModelPullStrategy,
                        ollama.getEmbeddingAdditionalModels(), Duration.ofMinutes(5), 0))
                .build();

        observationConvention.ifAvailable(embeddingModel::setObservationConvention);

        return embeddingModel;
    }

}
