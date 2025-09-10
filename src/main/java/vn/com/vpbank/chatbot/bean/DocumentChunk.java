package vn.com.vpbank.chatbot.bean;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
public class DocumentChunk {
    private String content;
    private String title;
    private String author;
    private String chunkId;
    private int chunkIndex;
    private String sourceId;

    public DocumentChunk(String content, String title, String author, String chunkId, int chunkIndex, String sourceId) {
        this.content = content;
        this.title = title;
        this.author = author;
        this.chunkId = chunkId;
        this.chunkIndex = chunkIndex;
        this.sourceId = sourceId;
    }
}
