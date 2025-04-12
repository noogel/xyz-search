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
    public ModelAndView chatPage(@RequestParam(name = "resId", value = "", required = false) String resId) {
        ModelAndView page = new ModelAndView("chat");
        page.addObject("resId", resId);
        return page;
    }

    @PostMapping("/stream")
    public SseEmitter chatStreamPost(@RequestBody ChatRequestDto request) {
        return chatService.sseEmitterChatStream(request);
    }

    @GetMapping("/stream")
    public SseEmitter chatStreamGet(@RequestParam String message,
                                    @RequestParam(name = "resId", required = false) String resId) {
        ChatRequestDto dto = new ChatRequestDto();
        dto.setMessage(message);
        dto.setResId(resId);
        return chatService.sseEmitterChatStream(dto);
    }

}
