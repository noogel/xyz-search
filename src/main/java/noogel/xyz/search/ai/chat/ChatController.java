package noogel.xyz.search.ai.chat;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/chat")
public class ChatController {

    @Resource
    private OllamaChatModel ollamaChatModel;


    @PostMapping("/query")
    public ResponseEntity<String> load() {
        ChatResponse response = ollamaChatModel.call(
                new Prompt(
                        "Generate the names of 5 famous pirates.",
                        OllamaOptions.builder()
                                .model(OllamaModel.QWEN_2_5_7B)
                                .temperature(0.4)
                                .build()
                ));
        return null;
    }
}
