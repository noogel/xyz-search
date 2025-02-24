package noogel.xyz.search.application.controller.ai;

import noogel.xyz.search.service.QuestionAnswerService;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai/data")
public class DataController {

    @Autowired
    private QuestionAnswerService qaService;

    @GetMapping("/vector")
    public List<Document> vector(@RequestParam(required = false, defaultValue = "") String query) {
        return qaService.test(query);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred in the controller: " + e.getMessage());
    }

}
