package noogel.xyz.search.ai.chat;

import noogel.xyz.search.ai.QAService;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/ai/chat")
public class ChatController {

    @Autowired
    private OllamaChatModel chatModel;
    @Autowired
    private QAService qaService;

    @GetMapping("")
    public ModelAndView chat() {
        return new ModelAndView("chat");
    }

//    @GetMapping("/generate")
//    public Map<String,String> generate(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
//        return Map.of("generation", this.chatModel.call(message));
//    }

    @GetMapping("/stream")
    public SseEmitter generateStream(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {

        SseEmitter emitter = new SseEmitter();
        Prompt prompt = qaService.getPrompt(message, true);
        chatModel.stream(prompt)
                .subscribe(
                        chatResponse -> {
                            try {
                                emitter.send(chatResponse.getResult().getOutput());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        },
                        emitter::completeWithError,
                        emitter::complete
                );
        return emitter;
//
//
//        Prompt prompt = new Prompt(new UserMessage(message));
//        return this.chatModel.stream(prompt);
    }

    @GetMapping("/query")
    public Map completion(
            @RequestParam(value = "question", defaultValue = "What is the purpose of CVS?") String question,
            @RequestParam(value = "stuffit", defaultValue = "true") boolean stuffit) {
        String answer = this.qaService.generate(question, stuffit);
        Map map = new LinkedHashMap();
        map.put("question", question);
        map.put("answer", answer);
        return map;
    }

}
