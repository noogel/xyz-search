package noogel.xyz.search.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import noogel.xyz.search.infrastructure.dto.LLMSearchResultDto;
import noogel.xyz.search.infrastructure.dto.api.ChatResponseDto;
import noogel.xyz.search.infrastructure.dto.repo.LLMSearchDto;
import noogel.xyz.search.infrastructure.lucene.LuceneAnalyzer;
import noogel.xyz.search.infrastructure.repo.FullTextSearchRepo;
import noogel.xyz.search.service.ChatService;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    @Value("classpath:/ai/prompts/system-stuff.st")
    private org.springframework.core.io.Resource qaSystemPromptResource;
    @Value("classpath:/ai/prompts/system-generic.st")
    private org.springframework.core.io.Resource chatbotSystemPromptResource;

    @Resource
    private OllamaChatModel chatModel;
    @Resource
    private OllamaOptions ollamaOptions;
    @Resource
    private FullTextSearchRepo fullTextSearchRepo;

    @Override
    public SseEmitter sseEmitterChatStream(String message) {
        SseEmitter emitter = new SseEmitter();
        Prompt prompt = this.getPrompt(message);
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
    public Flux<ChatResponse> fluxChatStream(String message) {
        Prompt prompt = getPrompt(message);
        return chatModel.stream(prompt);
    }

    private Prompt getRawPrompt(String message) {
        // ollama 参数含义
        //使用大模型优化搜索词。
        //基于 lucene 全文搜索
        //把结果输出给大模型总结。
        UserMessage userMessage = new UserMessage(message);
        return new Prompt(List.of(userMessage), ollamaOptions);
    }

    private Prompt getPrompt(String message) {
        // ollama 参数含义
        //使用大模型优化搜索词。
        //基于 lucene 全文搜索
        //把结果输出给大模型总结。
//        UserMessage userMessage = new UserMessage(message);
        Message systemMessage = getSystemMessage(message);
        log.info("getPrompt system:\n{}", systemMessage.getText());
        return new Prompt(List.of(systemMessage), ollamaOptions);
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
            return new SystemPromptTemplate(this.chatbotSystemPromptResource).createMessage();
        }
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(this.qaSystemPromptResource);
        return systemPromptTemplate.createMessage(Map.of("context", context, "query", query));
    }

    private List<String> fixedHighlights(List<String> highlights) {
        return highlights.stream().map(t-> {
            List<String> temps = Arrays.stream(t.split("。")).toList();
            return String.join("。", temps.subList(1, temps.size() - 1)) + "。";
        }).collect(Collectors.toList());
    }
}