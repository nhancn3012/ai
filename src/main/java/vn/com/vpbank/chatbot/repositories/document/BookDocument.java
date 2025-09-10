package vn.com.vpbank.chatbot.repositories.document;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "book_documents")
public class BookDocument {

    @Id
    private String id;
    private String title;
    private String author;
    private String outline;
    private String htmlContent;
    private String sourceUrl;
    private int chunkSize;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
