package noogel.xyz.search.infrastructure.client;

import java.time.Duration;
import java.util.Objects;

import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationConvention;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.ai.ollama.management.PullModelStrategy;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import io.micrometer.observation.ObservationRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.config.ConfigProperties;
import noogel.xyz.search.infrastructure.event.ConfigAppUpdateEvent;
import noogel.xyz.search.infrastructure.utils.JsonHelper;

@Component
@Slf4j
public class OllamaClient {

    @Getter
    private OllamaApi ollamaApi;
    @Getter
    private OllamaOptions ollamaOptions;
    @Getter
    private ModelManagementOptions modelManagementOptions;
    @Getter
    private OllamaChatModel chatModel;
    @Getter
    private OllamaEmbeddingModel embeddingModel;

    @Resource
    private ConfigProperties configProperties;
    @Resource
    private ObjectProvider<RestClient.Builder> restClientBuilderProvider;
    @Resource
    private ObjectProvider<WebClient.Builder> webClientBuilderProvider;
    @Resource
    private ObjectProvider<ObservationRegistry> observationRegistry;
    @Resource
    private ObjectProvider<ChatModelObservationConvention> chatModelObservationConvention;
    @Resource
    private ObjectProvider<EmbeddingModelObservationConvention> embeddingModelObservationConvention;

    @PostConstruct
    public void init() {
        try {
            if (configProperties.getApp().getChat().getOllama().isEnable()) {
                initOllama();
            }
        } catch (Exception e) {
            log.error("ollama 配置初始化失败", e);
        }
    }

    @EventListener(ConfigAppUpdateEvent.class)
    public void configAppUpdate(ConfigAppUpdateEvent event) {
        try {
            ConfigProperties.App newApp = event.getNewApp();
            ConfigProperties.App oldApp = event.getOldApp();
            if (!Objects.equals(JsonHelper.toJson(newApp.getChat().getOllama()),
                        JsonHelper.toJson(oldApp.getChat().getOllama()))) {
                initOllama();
            }
        } catch (Exception e) {
            log.error("ollama 配置更新失败", e);
        }
    }

    /**
     * 是否开启
     * 
     * @return
     */
    public boolean isEnabled() {
        return configProperties.getApp().getChat().getOllama().isEnable() && Objects.nonNull(chatModel);
    }

    /**
     * 初始化 ollama
     */
    private void initOllama() {
        this.ollamaApi = ollamaApi(configProperties, restClientBuilderProvider, webClientBuilderProvider);
        this.ollamaOptions = ollamaOptions(configProperties);
        this.modelManagementOptions = modelManagementOptions(configProperties);
        this.chatModel = ollamaChatModel(ollamaApi, ollamaOptions, modelManagementOptions, observationRegistry,
                chatModelObservationConvention);
        this.embeddingModel = ollamaEmbeddingModel(ollamaApi, ollamaOptions, modelManagementOptions,
                observationRegistry, embeddingModelObservationConvention);
        log.info("ollama 初始化完成");
    }

    private OllamaApi ollamaApi(ConfigProperties configProperties,
            ObjectProvider<RestClient.Builder> restClientBuilderProvider,
            ObjectProvider<WebClient.Builder> webClientBuilderProvider) {
        String baseUrl = configProperties.getApp().getChat().getOllama().getBaseUrl();
        return new OllamaApi(baseUrl,
                restClientBuilderProvider.getIfAvailable(RestClient::builder),
                webClientBuilderProvider.getIfAvailable(WebClient::builder));
    }

    private OllamaOptions ollamaOptions(ConfigProperties configProperties) {
        ConfigProperties.Ollama ollama = configProperties.getApp().getChat().getOllama();
        OllamaOptions options = OllamaOptions.builder().build();
        options.setModel(ollama.getChatModel());
        options.setTopK(40);
        options.setTopP(0.8);
        options.setNumCtx(Integer.parseInt(ollama.getChatOptionNumCtx()));
        options.setTemperature(Double.parseDouble(ollama.getChatOptionTemperature()));
        options.setNumPredict(Integer.parseInt(ollama.getChatOptionNumPredict()));
        return options;
    }

    private ModelManagementOptions modelManagementOptions(ConfigProperties configProperties) {
        ConfigProperties.Ollama ollama = configProperties.getApp().getChat().getOllama();
        var modelPullStrategy = PullModelStrategy.valueOf(ollama.getPullModelStrategy().toUpperCase());
        return new ModelManagementOptions(modelPullStrategy, ollama.getEmbeddingAdditionalModels(),
                Duration.ofMinutes(5), 0);
    }

    private OllamaChatModel ollamaChatModel(OllamaApi ollamaApi,
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

    private OllamaEmbeddingModel ollamaEmbeddingModel(OllamaApi ollamaApi,
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
