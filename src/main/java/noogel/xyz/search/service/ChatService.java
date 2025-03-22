package noogel.xyz.search.service;

import noogel.xyz.search.infrastructure.dto.api.ChatRequestDto;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

public interface ChatService {

    SseEmitter sseEmitterChatStream(ChatRequestDto dto);

    SseEmitter sseEmitterChat(ChatRequestDto dto);

    Flux<ChatResponse> fluxChatStream(ChatRequestDto dto);

}