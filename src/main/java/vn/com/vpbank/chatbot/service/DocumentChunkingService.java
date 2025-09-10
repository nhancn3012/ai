package vn.com.vpbank.chatbot.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.com.vpbank.chatbot.bean.DocumentChunk;
import vn.com.vpbank.chatbot.repositories.document.BookDocument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Slf4j
public class DocumentChunkingService {

    @Value("${chunk-size}")
    private int chunkSize;
    @Value("${overlap-size}")
    private int overlapSize;
    @Value("${min-chunk-size}")
    private int minChunkSize;

    // Pre-compile all patterns for better performance
    private static final Pattern HTML_BLOCK_PATTERN =
            Pattern.compile("</?(p|div|article|section|h[1-6])\\s*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern BR_PATTERN =
            Pattern.compile("<br\\s*/?>", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_INLINE_PATTERN =
            Pattern.compile("</?(li|td|tr)\\s*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_LIST_END_PATTERN =
            Pattern.compile("</(ul|ol|table)\\s*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_TAG_PATTERN =
            Pattern.compile("<[^>]+>");
    private static final Pattern HTML_ENTITY_PATTERN =
            Pattern.compile("&[a-zA-Z0-9]+;");
    private static final Pattern CONTROL_CHARS_PATTERN =
            Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]");
    private static final Pattern SENTENCE_PARAGRAPH_PATTERN =
            Pattern.compile("([.!?])\\s+([A-ZÁĐÊÔƠƯ])");
    private static final Pattern LONG_NUMBERS_PATTERN =
            Pattern.compile("\\b\\d{10,}\\b");
    private static final Pattern LONG_UPPERCASE_PATTERN =
            Pattern.compile("\\b[A-Z]{5,}\\b");
    private static final Pattern MULTIPLE_DOTS_PATTERN =
            Pattern.compile("[.]{3,}");
    private static final Pattern PAGE_NUMBERS_PATTERN =
            Pattern.compile("(?i)(page|trang)\\s*\\d+");
    private static final Pattern MULTIPLE_SPACES_PATTERN =
            Pattern.compile("[ \\t]+");
    private static final Pattern CLEAN_PARAGRAPH_PATTERN =
            Pattern.compile("\\n[ \\t]*\\n");
    private static final Pattern MULTIPLE_NEWLINES_PATTERN =
            Pattern.compile("\\n{3,}");


    /**
     * Chunking tối ưu cho vector database
     */
    public List<DocumentChunk> chunkDocument(BookDocument document, String rawText) {
        // 1. Clean text trước khi chunk
        String cleanText = deepCleanText(rawText);
        // 2. Kiểm tra text sau khi clean
        if (cleanText == null || cleanText.trim().length() < minChunkSize) {
            return Collections.emptyList();
        }
        // 3. Chunk theo semantic
        return semanticChunking(document, cleanText);
    }

    /**
     * Deep cleaning - loại bỏ tất cả noise không cần thiết
     */
    private String deepCleanText(String text) {
        if (text == null || text.isEmpty()) return null;

        // Apply transformations in optimal order (most frequent first)
        String result = HTML_TAG_PATTERN.matcher(HTML_ENTITY_PATTERN.matcher(BR_PATTERN.matcher(HTML_BLOCK_PATTERN.matcher(text)
                                        .replaceAll("\n\n"))
                                .replaceAll("\n"))
                        .replaceAll(" "))
                .replaceAll(" ");

        result = CONTROL_CHARS_PATTERN.matcher(result).replaceAll(" ");
        result = LONG_NUMBERS_PATTERN.matcher(result).replaceAll("");
        result = LONG_UPPERCASE_PATTERN.matcher(result).replaceAll("");
        result = MULTIPLE_DOTS_PATTERN.matcher(result).replaceAll("...");
        result = PAGE_NUMBERS_PATTERN.matcher(result).replaceAll("");
        result = SENTENCE_PARAGRAPH_PATTERN.matcher(result).replaceAll("$1\n\n$2");

        // Final whitespace cleanup
        result = MULTIPLE_SPACES_PATTERN.matcher(result).replaceAll(" ");
        result = CLEAN_PARAGRAPH_PATTERN.matcher(result).replaceAll("\n\n");
        result = MULTIPLE_NEWLINES_PATTERN.matcher(result).replaceAll("\n\n");

        return result.trim();
    }

    /**
     * Semantic chunking - chia theo ý nghĩa
     */
    private List<DocumentChunk> semanticChunking(BookDocument document, String cleanText) {
        List<DocumentChunk> chunks = new ArrayList<>();

        // Chia theo paragraph với ngữ nghĩa rõ ràng
        String[] paragraphs = cleanText.split("\\n\\s*\\n");
        log.info("Total paragraphs found: {}", paragraphs.length);

        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = 0;

        for (String paragraph : paragraphs) {
            String cleanParagraph = paragraph.trim();
            if (cleanParagraph.isEmpty()) continue;

            // Kiểm tra nếu thêm paragraph này có vượt quá chunk size không
            int potentialSize = currentChunk.length() + cleanParagraph.length() + 1;

            if (potentialSize > chunkSize && currentChunk.length() > minChunkSize) {
                // Tạo chunk hiện tại
                String chunkContent = currentChunk.toString().trim();
                if (!chunkContent.isEmpty()) {
                    chunks.add(createOptimalChunk(document, chunkContent, chunkIndex++));
                }

                // Bắt đầu chunk mới với overlap
                currentChunk = new StringBuilder();
                if (overlapSize > 0) {
                    String overlap = getLastSentences(chunkContent, overlapSize);
                    currentChunk.append(overlap).append(" ");
                }
            }

            currentChunk.append(cleanParagraph).append("\n");
        }

        // Chunk cuối cùng
        String finalChunk = currentChunk.toString().trim();
        if (finalChunk.length() >= minChunkSize) {
            chunks.add(createOptimalChunk(document, finalChunk, chunkIndex));
        }

        return chunks;
    }

    /**
     * Tạo chunk tối ưu không thêm metadata vào content
     */
    private DocumentChunk createOptimalChunk(BookDocument document, String content, int index) {
        // Content chỉ chứa nội dung thuần túy, không có metadata
        String cleanContent = content
                .replaceAll("\\n+", " ")  // Newlines thành spaces
                .replaceAll("\\s+", " ")   // Multiple spaces
                .trim();

        return new DocumentChunk(
                cleanContent,
                document.getTitle(),
                document.getAuthor(),
                document.getId(),
                index,
                document.getSourceUrl()
        );
    }

    /**
     * Lấy overlap từ cuối chunk trước
     */
    private String getLastSentences(String text, int maxChars) {
        if (text.length() <= maxChars) return text;

        String substring = text.substring(Math.max(0, text.length() - maxChars));

        // Tìm điểm bắt đầu sentence gần nhất
        int sentenceStart = substring.indexOf(". ");
        if (sentenceStart > 0) {
            return substring.substring(sentenceStart + 2);
        }

        // Nếu không có, tìm word boundary
        int spaceIndex = substring.indexOf(' ');
        if (spaceIndex > 0) {
            return substring.substring(spaceIndex + 1);
        }

        return substring;
    }

}
