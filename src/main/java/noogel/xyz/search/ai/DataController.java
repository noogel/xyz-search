package noogel.xyz.search.ai;

import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai/data")
public class DataController {

    private final DataLoadingService dataLoadingService;

    @Autowired
    private QAService qaService;

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public DataController(DataLoadingService dataLoadingService, JdbcTemplate jdbcTemplate) {
        this.dataLoadingService = dataLoadingService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/load")
    public ResponseEntity<String> load() {
        try {
            this.dataLoadingService.load();
            return ResponseEntity.ok("Data loaded successfully!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while loading data: " + e.getMessage());
        }
    }

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
