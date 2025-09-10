package vn.com.vpbank.chatbot.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.com.vpbank.chatbot.service.ChatbotService;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
public class ChatBotController {

    private final ChatbotService chatbotService;


    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, Object> request) {
        try {
            String question = (String) request.get("question");
            if (question == null || question.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Câu hỏi không được để trống"));
            }

            long startTime = System.currentTimeMillis();
            String response = chatbotService.chat(question);
            long processingTime = System.currentTimeMillis() - startTime;

            return ResponseEntity.ok(Map.of(
                    "question", question,
                    "response", response,
                    "processingTimeMs", processingTime,
                    "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            log.error("Error processing chat request", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Lỗi xử lý: " + e.getMessage()));
        }
    }


}
