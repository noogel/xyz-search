package noogel.xyz.search.application.controller;

import jakarta.annotation.Resource;
import noogel.xyz.search.infrastructure.dto.api.ChatRequestDto;
import noogel.xyz.search.service.ChatService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/chat")
public class ChatCtrl {

    @Resource
    private ChatService chatService;

    @GetMapping("/page")
    public ModelAndView chatPage() {
        return new ModelAndView("chat");
    }

    @PostMapping("/stream")
    public SseEmitter chatStreamPost(@RequestBody ChatRequestDto request) {
        return chatService.sseEmitterChatStream(request.getMessage());
    }

    @GetMapping("/stream")
    public SseEmitter chatStreamGet(@RequestParam String message) {
        return chatService.sseEmitterChatStream(message);
    }

}
