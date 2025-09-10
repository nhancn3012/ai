package vn.com.vpbank.chatbot.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import vn.com.vpbank.chatbot.repositories.document.BookDocument;

@Service
@Slf4j
@RequiredArgsConstructor
public class HtmlParserService {
    private final OcrService ocrService;

    public BookDocument parseHtmlFile(String htmlContent, String sourceUrl) {
        Document doc = Jsoup.parse(htmlContent);

        BookDocument bookDoc = new BookDocument();
        bookDoc.setSourceUrl(sourceUrl);
        bookDoc.setHtmlContent(htmlContent);

        // Extract title
        String title = extractTitle(doc);
        bookDoc.setTitle(title);

        // Extract author
        String author = extractAuthor(doc);
        bookDoc.setAuthor(author);

        // Extract outline/table of contents
        String outline = extractOutline(doc);
        bookDoc.setOutline(outline);

        return bookDoc;
    }

    private String extractTitle(Document doc) {
        String[] titleSelectors = {
                "h1", ".title", ".book-title", ".main-title",
                "[class*=title]", "title", ".post-title"
        };
        for (String selector : titleSelectors) {
            Element titleElement = doc.selectFirst(selector);
            if (titleElement != null && !titleElement.text().trim().isEmpty()) {
                return titleElement.text().trim();
            }
        }
        // Fallback to document title or source URL
        String docTitle = doc.title();
        return !docTitle.isEmpty() ? docTitle : "Unknown Title";
    }

    private String extractAuthor(Document doc) {
        String[] authorSelectors = {
                ".author", ".book-author", "[class*=author]",
                ".by-author", ".writer", "[rel=author]"
        };
        for (String selector : authorSelectors) {
            Element authorElement = doc.selectFirst(selector);
            if (authorElement != null && !authorElement.text().trim().isEmpty()) {
                return cleanAuthorName(authorElement.text());
            }
        }
        return "Unknown Author";
    }

    private String extractOutline(Document doc) {
        String[] outlineSelectors = {
                ".toc", ".outline", ".table-of-contents",
                ".contents", "[class*=toc]", ".index"
        };
        for (String selector : outlineSelectors) {
            Element outlineElement = doc.selectFirst(selector);
            if (outlineElement != null && !outlineElement.text().trim().isEmpty()) {
                return outlineElement.text().trim();
            }
        }
        return Strings.EMPTY;
    }

    private String cleanAuthorName(String author) {
        // Remove common prefixes like "By:", "Author:", etc.
        return author.replaceAll("^(By|Author|Writer):\\s*", "").trim();
    }

    // transformation text => push to vectorDB
    public String extractCleanText(String htmlContent) {
        Document doc = Jsoup.parse(htmlContent);

        // Remove unwanted elements - phần này có thể đưa ra file config để sau này thêm các tag phụ thuộc vào độ phức tạp của tài liệu HTML
        doc.select("script, style, nav, header, footer, .navigation, aside, form, noscript, iframe, svg, canvas, video, audio, button, input").remove();

        // Get text from body
        String text = doc.body() != null ? doc.body().wholeText() : doc.wholeText();

        // get text from table
        String tableMarkdown = convertTablesToMarkdown(doc);

        // get text from image
        String imageTexts = ocrImage(doc);

        // concat text + table + image
        String combined = text;
        if (!tableMarkdown.isEmpty()) {
            combined += "\nBảng dữ liệu:\n" + tableMarkdown;
        }
        if (!imageTexts.isEmpty()) {
            combined += "\nNội dung từ ảnh:\n" + imageTexts;
        }
        // Clean up extra whitespace and normalize
        return combined.replaceAll("\\s+", " ").trim();
    }

    private String ocrImage(Document doc) {
        try {
            Elements images = doc.select("img");
            if (images.isEmpty()) return Strings.EMPTY;
            StringBuilder imageTexts = new StringBuilder();
            for (Element img : images) {
                String imageUrl = img.absUrl("src");
                if (imageUrl.isBlank()) continue;
                String ocrResult = ocrService.extractTextFromImage(imageUrl);
                if (Strings.isEmpty(ocrResult)) {
                    log.info("Cannot OCR imageUrl: {}", ocrResult);
                    continue;
                }
                imageTexts.append("[Ảnh: ").append(imageUrl).append("]\n")
                        .append(ocrResult).append("\n");
            }
            return imageTexts.toString();
        } catch (Exception e) {
            log.error("Process ocrImage: ", e);
            return Strings.EMPTY;
        }

    }

    private String convertTablesToMarkdown(Document doc) {
        try {
            StringBuilder markdown = new StringBuilder();
            Elements tables = doc.select("table");
            for (Element table : tables) {
                Elements rows = table.select("tr");
                boolean isHeaderDone = false;
                for (Element row : rows) {
                    Elements cells = row.select("th, td");
                    // build row
                    markdown.append("| ");
                    for (Element cell : cells) {
                        markdown.append(cell.text().trim()).append(" | ");
                    }
                    markdown.append("\n");
                    // sau header thì thêm separator
                    if (!isHeaderDone && !row.select("th").isEmpty()) {
                        markdown.append("|");
                        for (int i = 0; i < cells.size(); i++) {
                            markdown.append(" --- |");
                        }
                        markdown.append("\n");
                        isHeaderDone = true;
                    }
                }
                markdown.append("\n"); // ngăn cách các bảng
            }
            return markdown.toString();
        } catch (Exception e) {
            log.error("Process convertTablesToMarkdown error: ", e);
            return Strings.EMPTY;
        }

    }


}
