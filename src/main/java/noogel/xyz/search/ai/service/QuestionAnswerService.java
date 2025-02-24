package noogel.xyz.search.ai.service;

//import io.qdrant.client.QdrantClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class QuestionAnswerService {

    @Value("classpath:/ai/prompts/system-stuff.st")
    private Resource qaSystemPromptResource;

    @Value("classpath:/ai/prompts/system-generic.st")
    private Resource chatbotSystemPromptResource;

    @Autowired
    VectorStore vectorStore;
//    @Autowired
//    private QdrantClient qdrantClient;
    @Autowired
    OllamaChatModel chatModel;


    @Autowired
    public QuestionAnswerService(OllamaChatModel chatModel, VectorStore vectorStore) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
    }

    public Flux<ChatResponse> generateStream(String message, boolean stuffit) {
        Prompt prompt = getPrompt(message, stuffit);

        log.info("Asking AI model to reply to question.");
        Flux<ChatResponse> chatResponse = chatModel.stream(prompt);
        log.info("AI responded.");
        return chatResponse;
    }

    public Prompt getPrompt(String message, boolean stuffit) {
        return getPrompt(message, stuffit, null);
    }
    public Prompt getPrompt(String message, boolean stuffit, ChatOptions chatOptions) {
        Message systemMessage = getSystemMessage(message, stuffit);
        UserMessage userMessage = new UserMessage(message);
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage), chatOptions);
        return prompt;
    }

    public String generate(String message, boolean stuffit) {
        Prompt prompt = getPrompt(message, stuffit);

        log.info("Asking AI model to reply to question.");
        ChatResponse chatResponse = chatModel.call(prompt);
        log.info("AI responded.");
        return chatResponse.getResult().getOutput().getText();
    }

    public List<Document> test(String query) {
        return vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(10).similarityThreshold(0.1f).build());
    }

    private Message getSystemMessage(String query, boolean stuffit) {
        if (stuffit) {
            log.info("Retrieving relevant documents");
            List<Document> similarDocuments = vectorStore.similaritySearch(
                    SearchRequest.builder().query(query).topK(10).similarityThreshold(0.7f).build());
            log.info("Found {} relevant documents.", similarDocuments.size());

            String context = similarDocuments.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n"));
            SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(this.qaSystemPromptResource);
            return systemPromptTemplate.createMessage(Map.of("context", context));
        } else {
            log.info("Not stuffing the prompt, using generic prompt");
            return new SystemPromptTemplate(this.chatbotSystemPromptResource).createMessage();
        }
    }

}