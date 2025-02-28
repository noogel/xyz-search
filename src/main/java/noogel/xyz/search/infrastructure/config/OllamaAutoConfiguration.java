/*
 * Copyright 2023-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package noogel.xyz.search.infrastructure.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationConvention;
import org.springframework.ai.model.function.DefaultFunctionCallbackResolver;
import org.springframework.ai.model.function.FunctionCallbackResolver;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.ai.ollama.management.PullModelStrategy;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * {@link AutoConfiguration Auto-configuration} for Ollama Chat Client.
 *
 * @author Christian Tzolov
 * @author Eddú Meléndez
 * @author Thomas Vitale
 * @since 0.8.0
 */
//@AutoConfiguration(after = {RestClientAutoConfiguration.class})
//@ConditionalOnClass(OllamaApi.class)
//@ImportAutoConfiguration(classes = {RestClientAutoConfiguration.class, WebClientAutoConfiguration.class})
@Configuration
public class OllamaAutoConfiguration {

    @Bean
//    @ConditionalOnMissingBean
    public OllamaApi ollamaApi(ConfigProperties configProperties,
                               ObjectProvider<RestClient.Builder> restClientBuilderProvider,
                               ObjectProvider<WebClient.Builder> webClientBuilderProvider) {
        String baseUrl = configProperties.getApp().getChat().getOllama().getBaseUrl();
        return new OllamaApi(baseUrl,
                restClientBuilderProvider.getIfAvailable(RestClient::builder),
                webClientBuilderProvider.getIfAvailable(WebClient::builder));
    }

    @Bean
//    @ConditionalOnMissingBean
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
//    @ConditionalOnMissingBean
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

    @Bean
    @ConditionalOnMissingBean
    public FunctionCallbackResolver springAiFunctionManager(ApplicationContext context) {
        DefaultFunctionCallbackResolver manager = new DefaultFunctionCallbackResolver();
        manager.setApplicationContext(context);
        return manager;
    }

}
