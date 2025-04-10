package noogel.xyz.search.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.client.OllamaClient;
import noogel.xyz.search.infrastructure.client.VectorClient;
import noogel.xyz.search.infrastructure.config.ConfigProperties;
import noogel.xyz.search.infrastructure.dto.LLMSearchResultDto;
import noogel.xyz.search.infrastructure.dto.api.ChatRequestDto;
import noogel.xyz.search.infrastructure.dto.api.ChatResponseDto;
import noogel.xyz.search.infrastructure.dto.repo.LLMSearchDto;
import noogel.xyz.search.infrastructure.repo.FullTextSearchService;
import noogel.xyz.search.service.ChatService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    @Value("classpath:/ai/prompts/system-stuff.st")
    private org.springframework.core.io.Resource qaSystemPromptResource;
    @Value("classpath:/ai/prompts/system-generic.st")
    private org.springframework.core.io.Resource chatbotSystemPromptResource;

    @Resource
    private OllamaClient ollamaClient;
    @Resource
    private VectorClient vectorClient;
    @Resource
    private FullTextSearchService fullTextSearchService;
    @Resource
    private ConfigProperties configProperties;

    @Override
    public SseEmitter sseEmitterChatStream(ChatRequestDto dto) {
        SseEmitter emitter = new SseEmitter();

        // 检查 Ollama 服务是否开启
        if (!configProperties.getApp().getChat().getOllama().isEnable()) {
            try {
                emitter.send(new ChatResponseDto(UUID.randomUUID().toString(), "ollama 未开启"));
                emitter.send(new ChatResponseDto(UUID.randomUUID().toString(), ""));
                emitter.complete();
                return emitter;
            } catch (IOException e) {
                emitter.completeWithError(e);
                return emitter;
            }
        }

        // 检查 Elastic 服务是否开启
        if (!configProperties.getApp().getChat().getQdrant().isEnable()) {
            try {
                emitter.send(new ChatResponseDto(UUID.randomUUID().toString(), "qdrant 未开启"));
                emitter.send(new ChatResponseDto(UUID.randomUUID().toString(), ""));
                emitter.complete();
                return emitter;
            } catch (IOException e) {
                emitter.completeWithError(e);
                return emitter;
            }
        }

        // https://docs.spring.io/spring-ai/reference/1.0/api/retrieval-augmented-generation.html
        ChatClient.Builder chatClientBuilder = ChatClient.builder(ollamaClient.getChatModel());
        ChatClient chatClient = chatClientBuilder.build();
        
        // 构建查询转换器
        RewriteQueryTransformer queryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder.build().mutate())
                .build();

        // 构建文档检索器
        VectorStoreDocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .similarityThreshold(0.50)
                .topK(5) // 限制返回的文档数量
                .vectorStore(vectorClient.getVectorStore())
                .build();
                
        // 构建RAG顾问
        Advisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                .queryTransformers(queryTransformer)
                .documentRetriever(documentRetriever)
                .build();

        // 添加错误处理
        chatClient.prompt()
                .advisors(retrievalAugmentationAdvisor)
                .user(dto.getMessage())
                .stream()
                .chatResponse()
                .doOnError(error -> {
                    log.error("RAG处理过程中发生错误", error);
                    try {
                        emitter.send(new ChatResponseDto(UUID.randomUUID().toString(), 
                            "抱歉，处理您的请求时发生错误，请稍后重试。"));
                        emitter.complete();
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                })
                .subscribe(chatResponse -> {
                    try {
                        ChatResponseDto chatResponseDto = new ChatResponseDto(
                            UUID.randomUUID().toString(),
                            chatResponse.getResult().getOutput().getText()
                        );
                        emitter.send(chatResponseDto);
                    } catch (IOException e) {
                        log.error("发送SSE消息时发生错误", e);
                        emitter.completeWithError(e);
                    }
                }, 
                error -> emitter.completeWithError(error),
                emitter::complete);
        return emitter;
    }

    @Override
    public SseEmitter sseEmitterChat(ChatRequestDto dto) {
        SseEmitter emitter = new SseEmitter();

        // 检查 Ollama 服务是否开启
        if (!configProperties.getApp().getChat().getOllama().isEnable()) {
            try {
                emitter.send(new ChatResponseDto(UUID.randomUUID().toString(), "ollama 未开启"));
                emitter.send(new ChatResponseDto(UUID.randomUUID().toString(), ""));
                emitter.complete();
                return emitter;
            } catch (IOException e) {
                emitter.completeWithError(e);
                return emitter;
            }
        }

        String message = dto.getMessage();
        Prompt prompt = this.getRawPromptAndEditor(message);
        // log.info("fluxChat:\n{}", message);
        ChatResponse call = ollamaClient.getChatModel().call(prompt);
        try {
            Generation result = call.getResult();
            // log.info("fluxChat result:\n{}", result.getOutput().getText());
            ChatResponseDto chatResponseDto = new ChatResponseDto(UUID.randomUUID().toString(),
                    result.getOutput().getText());
            emitter.send(chatResponseDto);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return emitter;
    }

    @Override
    public Flux<ChatResponse> fluxChatStream(ChatRequestDto dto) {
        // 检查 Ollama 服务是否开启
        if (!configProperties.getApp().getChat().getOllama().isEnable()) {
            return Flux.error(new RuntimeException("ollama 未开启"));
        }

        String message = dto.getMessage();
        Prompt prompt = getPrompt(message);
        return ollamaClient.getChatModel().stream(prompt);
    }

    private Prompt getRawPrompt(String message) {
        UserMessage userMessage = new UserMessage(message);
        return new Prompt(List.of(userMessage), ollamaClient.getOllamaOptions());
    }

    private Prompt getRawPromptAndEditor(String message) {
        UserMessage userMessage = new UserMessage(message);
        SystemMessage systemMessage = new SystemMessage(this.qaSystemPromptResource);
        return new Prompt(List.of(systemMessage, userMessage), ollamaClient.getOllamaOptions());
    }

    private Prompt getPrompt(String message) {
        Message systemMessage = getSystemMessage(message);
        // log.info("getPrompt system:\n{}", systemMessage.getText());
        return new Prompt(List.of(systemMessage), ollamaClient.getOllamaOptions());
    }

    private Message getSystemMessage(String query) {
        LLMSearchDto dto = new LLMSearchDto();
        dto.setSearchQuery(query);
        dto.setFragmentSize(1024);
        dto.setMaxNumFragments(10);
        LLMSearchResultDto llmSearchResultDto = fullTextSearchService.getBean().llmSearch(dto);
        List<String> fixedHighlights = fixedHighlights(llmSearchResultDto.getHighlights());
        String context = String.join("\n\n", fixedHighlights);
        if (StringUtils.isBlank(context)) {
            return new SystemPromptTemplate(this.chatbotSystemPromptResource).createMessage(Map.of("query", query));
        }
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(this.qaSystemPromptResource);
        return systemPromptTemplate.createMessage(Map.of("context", context, "query", query));
    }

    private List<String> fixedHighlights(List<String> highlights) {
        return highlights.stream().map(t -> {
            List<String> temps = Arrays.stream(t.split("。")).toList();
            return String.join("。", temps.subList(1, temps.size() - 1)) + "。";
        }).collect(Collectors.toList());
    }
}