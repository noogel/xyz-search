package noogel.xyz.search.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.client.OllamaClient;
import noogel.xyz.search.infrastructure.client.VectorClient;
import noogel.xyz.search.infrastructure.config.ConfigProperties;
import noogel.xyz.search.infrastructure.consts.PromptConsts;
import noogel.xyz.search.infrastructure.dto.api.ChatRequestDto;
import noogel.xyz.search.infrastructure.dto.api.ChatResponseDto;
import noogel.xyz.search.service.ChatService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    @Resource
    private OllamaClient ollamaClient;
    @Resource
    private VectorClient vectorClient;
    @Resource
    private ConfigProperties configProperties;
    @Resource
    private ChatMemory chatMemory;

    private ChatClient chatClient;

    private ChatClient getChatClient() {
        if (chatClient == null) {
            ConfigProperties.Chat chat = configProperties.getApp().getChat();
            var qaAdvisor = new QuestionAnswerAdvisor(vectorClient.getVectorStore(),
                    SearchRequest.builder()
                            .similarityThreshold(Optional.ofNullable(chat.getVector())
                                    .map(ConfigProperties.Vector::getSimilarityThreshold).orElse(0.50))
                            .topK(Optional.ofNullable(chat.getVector())
                                    .map(ConfigProperties.Vector::getTopK).orElse(10)) // 限制返回的文档数量
                            .build());
            var mcmAdvisor = new MessageChatMemoryAdvisor(chatMemory);
            chatClient = ChatClient.builder(ollamaClient.getChatModel())
                    .defaultSystem(PromptConsts.RAG_SYSTEM_PROMPT)
                    .defaultAdvisors(
                            mcmAdvisor, // CHAT MEMORY
                            qaAdvisor, // RAG
                            new SimpleLoggerAdvisor())
                    .build();
        }
        return chatClient;
    }


    @Override
    public SseEmitter sseEmitterChatStream(ChatRequestDto dto) {
        SseEmitter emitter = new SseEmitter();

        ConfigProperties.Chat chat = configProperties.getApp().getChat();
        // 检查 Ollama 服务是否开启
        if (!chat.getOllama().isEnable()) {
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

        // 检查 qdrant 服务是否开启
        if (!chat.getQdrant().isEnable()) {
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

        ChatClient chatClient = getChatClient();

        // 添加错误处理
        chatClient.prompt()
                .advisors(a -> a
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, dto.getChatId())
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100))
                .user(dto.getMessage())
                .stream()
                .chatResponse()
                .doOnError(error -> {
                    log.error("RAG处理过程中发生错误", error);
                    try {
                        emitter.send(new ChatResponseDto(UUID.randomUUID().toString(),
                                "抱歉，处理您的请求时发生错误，请稍后重试。"));
                        emitter.send(new ChatResponseDto(UUID.randomUUID().toString(), ""));
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
                        emitter::completeWithError,
                        emitter::complete);
        return emitter;
    }

    @Override
    public Flux<ChatResponse> fluxChatStream(ChatRequestDto dto) {
        // 检查 Ollama 服务是否开启
        if (!configProperties.getApp().getChat().getOllama().isEnable()) {
            return Flux.error(new RuntimeException("ollama 未开启"));
        }
        Prompt prompt = new Prompt(List.of(new UserMessage(dto.getMessage())), ollamaClient.getOllamaOptions());
        return ollamaClient.getChatModel().stream(prompt);
    }

}