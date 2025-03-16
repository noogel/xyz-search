package noogel.xyz.search.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.dto.LLMSearchResultDto;
import noogel.xyz.search.infrastructure.dto.api.ChatRequestDto;
import noogel.xyz.search.infrastructure.dto.api.ChatResponseDto;
import noogel.xyz.search.infrastructure.dto.repo.LLMSearchDto;
import noogel.xyz.search.infrastructure.repo.FullTextSearchRepo;
import noogel.xyz.search.service.ChatService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
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
    @Value("classpath:/ai/prompts/system-stuff-editor.st")
    private org.springframework.core.io.Resource chatbotEditorSystemPromptResource;

    @Resource
    private ChatModel chatModel;
    @Resource
    private ChatOptions chatOptions;
    @Resource
    private FullTextSearchRepo fullTextSearchRepo;

    @Override
    public SseEmitter sseEmitterChatStream(ChatRequestDto dto) {
        String message = dto.getMessage();
        SseEmitter emitter = new SseEmitter();
        Prompt prompt = this.getRawPrompt(message);
        chatModel.stream(prompt)
                .subscribe(
                        chatResponse -> {
                            try {
                                ChatResponseDto chatResponseDto = new ChatResponseDto(
                                        UUID.randomUUID().toString(), chatResponse.getResult().getOutput().getText());
                                emitter.send(chatResponseDto);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        },
                        emitter::completeWithError,
                        emitter::complete
                );
        return emitter;
    }

    @Override
    public SseEmitter sseEmitterChat(ChatRequestDto dto) {
        String message = dto.getMessage();
        SseEmitter emitter = new SseEmitter();
        Prompt prompt = this.getRawPromptAndEditor(message);
        log.info("fluxChat:\n{}", message);
        ChatResponse call = chatModel.call(prompt);
        try {
            Generation result = call.getResult();
            log.info("fluxChat result:\n{}", result.getOutput().getText());
            ChatResponseDto chatResponseDto = new ChatResponseDto(
                    UUID.randomUUID().toString(), result.getOutput().getText());
            emitter.send(chatResponseDto);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return emitter;
    }


    @Override
    public Flux<ChatResponse> fluxChatStream(ChatRequestDto dto) {
        String message = dto.getMessage();
        Prompt prompt = getPrompt(message);
        return chatModel.stream(prompt);
    }

    private Prompt getRawPrompt(String message) {
        UserMessage userMessage = new UserMessage(message);
        return new Prompt(List.of(userMessage), chatOptions);
    }

    private Prompt getRawPromptAndEditor(String message) {
        UserMessage userMessage = new UserMessage(message);
        SystemMessage systemMessage = new SystemMessage(this.chatbotEditorSystemPromptResource);
        return new Prompt(List.of(systemMessage, userMessage), chatOptions);
    }

    private Prompt getPrompt(String message) {
        Message systemMessage = getSystemMessage(message);
        log.info("getPrompt system:\n{}", systemMessage.getText());
        return new Prompt(List.of(systemMessage), chatOptions);
    }

    private Message getSystemMessage(String query) {
        LLMSearchDto dto = new LLMSearchDto();
        dto.setSearchQuery(query);
        dto.setFragmentSize(1024);
        dto.setMaxNumFragments(10);
        LLMSearchResultDto llmSearchResultDto = fullTextSearchRepo.llmSearch(dto);
        List<String> fixedHighlights = fixedHighlights(llmSearchResultDto.getHighlights());
        String context = String.join("\n\n", fixedHighlights);
        if (StringUtils.isBlank(context)) {
            return new SystemPromptTemplate(this.chatbotSystemPromptResource)
                    .createMessage(Map.of("query", query));
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