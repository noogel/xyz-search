package noogel.xyz.search.service;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

public interface ChatService {

    SseEmitter sseEmitterChatStream(String message);

    Flux<ChatResponse> fluxChatStream(String message);

}