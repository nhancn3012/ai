package vn.com.vpbank.chatbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotService {

    private final ChatModel chatModel;
    private final VectorStoreService vectorStoreService;

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            Bạn là một trợ lý AI thông minh giúp trả lời câu hỏi dựa trên nội dung các cuốn sách.
            
            Hướng dẫn:
            - Sử dụng thông tin từ ngữ cảnh được cung cấp để trả lời câu hỏi một cách chính xác và hữu ích
            - Nếu thông tin trong ngữ cảnh không đủ để trả lời đầy đủ, hãy nói rõ điều đó
            - Luôn trả lời bằng tiếng Việt
            - Trích dẫn tên sách và tác giả khi có thể
            - Nếu có nhiều nguồn thông tin, hãy tổng hợp một cách logic
            
            Ngữ cảnh từ các cuốn sách:
            {context}
            
            Câu hỏi của người dùng: {question}
            
            Trả lời:
            """;

    public String chat(String userQuestion) {
        try {
            log.info("Processing question: {}", userQuestion);

            // 1. Search for relevant documents
            List<Document> relevantDocs = vectorStoreService.searchSimilar(userQuestion);
            log.info("Found {} relevant documents", relevantDocs.size());

            // 2. Build context from relevant documents
            String context = buildContext(relevantDocs);

            // 3. Create prompt with template
            PromptTemplate promptTemplate = new PromptTemplate(SYSTEM_PROMPT_TEMPLATE);
            Prompt prompt = promptTemplate.create(Map.of(
                    "context", context,
                    "question", userQuestion
            ));

            // 4. Get response from ChatModel
            ChatResponse response = chatModel.call(prompt);
            String answer = response.getResult().getOutput().getText();

            log.info("Generated answer length: {} characters", answer.length());
            return answer;

        } catch (Exception e) {
            log.error("Error processing chat question: ", e);
            return "Xin lỗi, tôi gặp lỗi khi xử lý câu hỏi của bạn. Vui lòng thử lại sau.";
        }
    }

    /**
     * Format documents khi trả về cho AI - KHÔNG trùng lặp với chunking
     */
    public String buildContext(List<Document> documents) {
        if (documents.isEmpty()) {
            return "Không có thông tin liên quan.";
        }

        // Group theo source để tránh duplicate
        Map<String, List<Document>> groupedBySource = documents.stream()
                .collect(Collectors.groupingBy(this::getSourceKey
                ));

        return groupedBySource.entrySet().stream()
                .map(this::formatSourceGroup)
                .collect(Collectors.joining("\n---\n"));
    }

    private String getSourceKey(Document doc) {
        String title = (String) doc.getMetadata().get("title");
        String author = (String) doc.getMetadata().get("author");
        return title + "|" + author;
    }

    private String formatSourceGroup(Map.Entry<String, List<Document>> entry) {
        List<Document> docs = entry.getValue();
        Document firstDoc = docs.get(0);

        String title = (String) firstDoc.getMetadata().get("title");
        String author = (String) firstDoc.getMetadata().get("author");

        // Gộp content của các chunks từ cùng source
        String combinedContent = docs.stream()
                .map(Document::getFormattedContent)
                .collect(Collectors.joining(" "));

        // Remove duplicates và clean
        String cleanContent = removeDuplicateSentences(combinedContent);

        StringBuilder result = new StringBuilder();
        if (title != null && !"Unknown Title".equals(title)) {
            result.append("📚 ").append(title);
            if (author != null && !"Unknown Author".equals(author)) {
                result.append(" - ").append(author);
            }
            result.append("\\n ");
        }

        result.append(cleanContent);
        return result.toString();
    }

    /**
     * Loại bỏ câu trùng lặp trong content
     */
    private String removeDuplicateSentences(String content) {
        String[] sentences = content.split("\\. ");
        Set<String> seen = new LinkedHashSet<>();

        for (String sentence : sentences) {
            String normalized = sentence.trim().toLowerCase();
            if (normalized.length() > 20) { // Chỉ giữ câu có ý nghĩa
                seen.add(sentence.trim());
            }
        }

        return String.join(". ", seen);
    }

//    private String buildContext(List<Document> documents) {
//        if (documents.isEmpty()) {
//            return "Không tìm thấy thông tin liên quan trong cơ sở dữ liệu.";
//        }
//
//        return documents.stream()
//                .map(this::formatDocumentForContext)
//                .collect(Collectors.joining("\\n---\\n"));
//
//    }
//
//    private String formatDocumentForContext(Document doc) {
//        String title = (String) doc.getMetadata().get("title");
//        String author = (String) doc.getMetadata().get("author");
//      //  String content = doc.getFormattedContent();
//        String content = cleanContent(doc.getFormattedContent());
//
//        StringBuilder formatted = new StringBuilder();
//
//        if (title != null && !title.equals("Unknown Title")) {
//            formatted.append("Sách: ").append(title).append("  ");
//        }
//
//        if (author != null && !author.equals("Unknown Author")) {
//            formatted.append("Tác giả: ").append(author).append("\\n");
//        }
//
//        formatted.append("Nội dung: ").append(content);
//
//        return formatted.toString();
//    }
//
//    private String cleanContent(String content) {
//        if (content == null) return "";
//        return content
//                .replaceAll("\\\\n", "\n")  // Chuyển \\n thành \n thật
//                .replaceAll("\n+", " ")     // Thay nhiều \n bằng space
//                .replaceAll("\\s+", " ")    // Xóa multiple spaces
//                .trim();
//    }
}
