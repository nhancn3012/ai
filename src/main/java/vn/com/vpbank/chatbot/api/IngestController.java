package vn.com.vpbank.chatbot.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.com.vpbank.chatbot.bean.request.IngestTextRq;
import vn.com.vpbank.chatbot.service.DocumentIngestionService;
import vn.com.vpbank.chatbot.service.WorkerService;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/ingest")
@Validated
public class IngestController {
    private final DocumentIngestionService ingestionService;
    private final WorkerService workerService;

    @PostMapping("/text")
    public ResponseEntity<Map<String, Object>> ingestTextDocument(
            @Valid @RequestBody IngestTextRq request) {

        try {
            long startTime = System.currentTimeMillis();
            ingestionService.ingestDocument(request.getContent(), request.getName());
            long processingTime = System.currentTimeMillis() - startTime;
            return ResponseEntity.ok(Map.of(
                    "message", "Process ingest data successfully",
                    "processingTimeMs", processingTime
            ));
        } catch (Exception e) {
            log.error("Error ingesting document", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "System error ingest text: " + e.getMessage()));
        }
    }

    @PostMapping("/file")
    public ResponseEntity<Map<String, Object>> ingestZip(@RequestParam("file") MultipartFile file) {
        try {
            long startTime = System.currentTimeMillis();
            String extension = file.getOriginalFilename();
            if (Strings.isEmpty(extension)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "File is required"));
            }

            if (extension.endsWith(".html") || extension.endsWith(".htm")) {
                String htmlContent = new String(file.getBytes(), StandardCharsets.UTF_8);
                workerService.processFileAsync(htmlContent, extension);
            }
            if (extension.endsWith(".zip")) {
                Path tempFile = Files.createTempFile("ingest-", ".zip");
                file.transferTo(tempFile.toFile());
                ZipFile zipFile = new ZipFile(tempFile.toFile());
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (!entry.isDirectory() && entry.getName().endsWith(".html")) {
                        String pathInZip = entry.getName();
                        String fileName = Paths.get(pathInZip).getFileName().toString();
                        fileName = fileName.replaceAll("[\\\\/]+", "_");
                        try (InputStream is = zipFile.getInputStream(entry)) {
                            String htmlContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                            // Đẩy sang worker xử lý document
                            workerService.processFileAsync(htmlContent, fileName);
                        }
                    }
                }
            }
            long processingTime = System.currentTimeMillis() - startTime;
            return ResponseEntity.ok(Map.of(
                    "message", "Process ingest data successfully",
                    "processingTimeMs", processingTime
            ));
        } catch (Exception e) {
            log.error("Error ingesting file", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "System error ingesting file: " + e.getMessage()));
        }
    }

}
