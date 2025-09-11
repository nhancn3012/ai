package vn.com.vpbank.chatbot.service;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import vn.com.vpbank.chatbot.bean.DocumentChunk;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorStoreService {
    private final VectorStore vectorStore;

    @Async("addStoreExecutor")
    public void addDocuments(List<DocumentChunk> chunks) {
        if (chunks.isEmpty()) {
            return;
        }
        int batchSize = 100;
        for (int i = 0; i < chunks.size(); i += batchSize) {
            int end = Math.min(i + batchSize, chunks.size());
            List<DocumentChunk> batch = chunks.subList(i, end);
            List<Document> documents = batch.parallelStream()
                    .map(this::convertToDocument)
                    .filter(doc -> doc.getFormattedContent().length() < 15000) // Ước lượng < ~8191 token
                    .collect(Collectors.toList());
            addToVectorStore(documents);
            log.info("{} add {} chunk to store", chunks.get(i).getSourceId(), documents.size());
        }
        log.info("Added {} chunks to vector store", chunks.size());
    }

    public List<Document> searchSimilar(String query) {
        return vectorStore.similaritySearch(query);
    }

    private Document convertToDocument(DocumentChunk chunk) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("title", chunk.getTitle());
        metadata.put("author", chunk.getAuthor());
        metadata.put("chunkId", chunk.getChunkId());
        metadata.put("chunkIndex", chunk.getChunkIndex());
        metadata.put("sourceId", chunk.getSourceId());

        return new Document(chunk.getContent(), metadata);
    }

    @Retry(name = "vectorStoreRetry")
    private void addToVectorStore(List<Document> documents) {
        vectorStore.add(documents);
    }
}
