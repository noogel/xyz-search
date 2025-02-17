package noogel.xyz.search.ai;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class AICtrl {
    private final EmbeddingModel embeddingModel;

    @Autowired
    public AICtrl(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @GetMapping("/ai/embedding")
    public Map embed(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        EmbeddingResponse embeddingResponse = this.embeddingModel.embedForResponse(List.of(message));
        return Map.of("embedding", embeddingResponse);
    }
    // --enable-native-access=ALL-UNNAMED
    // https://docs.spring.io/spring-ai/reference/1.0/api/embeddings/ollama-embeddings.html
    // https://github.com/banup-kubeforce/health-rag.git
    // http://192.168.124.13:8099/pdf.js/web/view?file=/file/view/287c00a9b9834d09b53ae51297b6a3f7
}
