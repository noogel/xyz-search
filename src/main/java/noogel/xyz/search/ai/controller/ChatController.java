package noogel.xyz.search.ai.controller;

import noogel.xyz.search.ai.service.QuestionAnswerService;
import noogel.xyz.search.ai.dto.ChatRequestDto;
import noogel.xyz.search.ai.dto.ChatResponseDto;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/ai/chat")
public class ChatController {

    @Autowired
    private OllamaChatModel chatModel;
    @Autowired
    private QuestionAnswerService qaService;

    @GetMapping("")
    public ModelAndView chatPage() {
        return new ModelAndView("chat");
    }

    @GetMapping("/2")
    public ModelAndView chatPage2() {
        return new ModelAndView("ai/chat2");
    }

    @PostMapping("/stream")
    public SseEmitter chatStream(@RequestBody ChatRequestDto request) {
        SseEmitter emitter = new SseEmitter();
        Prompt prompt = qaService.getPrompt(request.getMessage(), true);
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

    @GetMapping("/stream")
    public SseEmitter chatStreamGet(@RequestParam String message) {
        SseEmitter emitter = new SseEmitter();
        Prompt prompt = qaService.getPrompt(message, true);
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
}
