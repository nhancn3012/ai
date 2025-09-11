package vn.com.vpbank.chatbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.com.vpbank.chatbot.repositories.BookDocumentRepository;
import vn.com.vpbank.chatbot.bean.DocumentChunk;
import vn.com.vpbank.chatbot.repositories.document.BookDocument;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentIngestionService {

    private final HtmlParserService htmlParserService;
    private final DocumentChunkingService chunkingService;
    private final VectorStoreService vectorStoreService;
    private final BookDocumentRepository documentRepository;

    public void ingestDocument(String htmlContent, String fileName) {
        try {
            log.info("Starting ingestion for document: {}", fileName);
            // 1. Check if document already exists
            BookDocument existingDoc = documentRepository.findBySourceUrl(fileName);
            if (existingDoc != null) {
                log.info("Document already exists, updating: {}", fileName);
                existingDoc.setHtmlContent(htmlContent);
                existingDoc.setUpdatedAt(LocalDateTime.now());
                // Re-parse to update metadata
                BookDocument parsed = htmlParserService.parseHtmlFile(htmlContent, fileName);
                existingDoc.setTitle(parsed.getTitle());
                existingDoc.setAuthor(parsed.getAuthor());
                existingDoc.setOutline(parsed.getOutline());
                existingDoc = documentRepository.save(existingDoc);

                // Re-process and update vectors
                processDocumentForVectorStore(existingDoc, htmlContent);
                return;
            }

            // 2. Parse HTML and extract metadata
            BookDocument document = htmlParserService.parseHtmlFile(htmlContent, fileName);

            // 3. Save to database first to get ID
            document = documentRepository.save(document);
            log.info("Saved document to database with ID: {}", document.getId());
            // 4. Process for vector store
            processDocumentForVectorStore(document, htmlContent);
            log.info("Successfully ingested document: {} by {}",
                    fileName, document.getAuthor());
        } catch (Exception e) {
            log.error("Error ingesting document from: ", e);
        }
    }

    private void processDocumentForVectorStore(BookDocument document, String htmlContent) {
        // Extract clean text
        String cleanText = htmlParserService.extractCleanText(htmlContent);
        log.info("Extracted clean text length: {} characters", cleanText.length());

        // Chunk document
        List<DocumentChunk> chunks = chunkingService.chunkDocument(document, cleanText);
        log.info("Created {} chunks for document ID: {}", chunks.size(), document.getId());
        if (chunks.isEmpty()) return;

        // Create embeddings and store in vector database
        vectorStoreService.addDocuments(chunks);
    }
}
