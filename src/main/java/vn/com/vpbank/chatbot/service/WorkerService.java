package vn.com.vpbank.chatbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkerService {
    private final DocumentIngestionService ingestionService;

    @Async("ingestExecutor")
    public void processFileAsync(String htmlContent, String fileName) {
        try {
            ingestionService.ingestDocument(htmlContent, fileName);
        } catch (Exception e) {
            log.error("processFileAsync error: ", e);
        }
    }

    private void processZipFile(MultipartFile file) throws IOException {
        // for none-prod lưu zip tạm vào disk, prod có thể là S3 ...
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
                    String html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    // Đẩy sang worker xử lý từng document
                    ingestionService.ingestDocument(html, fileName);
                }
            }
        }

    }


//    @Async("ingestExecutor")
//    public void processFileAsync(MultipartFile file) {
//        try {
//            String fileName = file.getOriginalFilename();
//            if (Strings.isEmpty(fileName) || (!fileName.endsWith(".html") && !fileName.endsWith(".htm") && !fileName.endsWith(".zip")))
//                return;
//            if (fileName.endsWith(".zip")) {
//                processZipFile(file);
//                return;
//            }
//            String htmlContent = new String(file.getBytes(), StandardCharsets.UTF_8);
//            ingestionService.ingestDocument(htmlContent, fileName);
//        } catch (Exception e) {
//            log.error("processFileAsync error: ", e);
//        }
//    }
//
//    private void processZipFile(MultipartFile file) throws IOException {
//        // for none-prod lưu zip tạm vào disk, prod có thể là S3 ...
//        Path tempFile = Files.createTempFile("ingest-", ".zip");
//        file.transferTo(tempFile.toFile());
//        try (ZipFile zipFile = new ZipFile(tempFile.toFile())) {
//            Enumeration<? extends ZipEntry> entries = zipFile.entries();
//            while (entries.hasMoreElements()) {
//                ZipEntry entry = entries.nextElement();
//                if (!entry.isDirectory() && entry.getName().endsWith(".html")) {
//                    String pathInZip = entry.getName();
//                    String fileName = Paths.get(pathInZip).getFileName().toString();
//                    fileName = fileName.replaceAll("[\\\\/]+", "_");
//                    try (InputStream is = zipFile.getInputStream(entry)) {
//                        String html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
//                        // Đẩy sang worker xử lý từng document
//                        ingestionService.ingestDocument(html, fileName);
//                    }
//                }
//            }
//        } catch (Exception e) {
//            log.error("Error processZipFile : ", e);
//        }
//    }


//    @Async("ingestExecutor")
//    public void processZipAsync(Path zipPath) {
//        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
//            Enumeration<? extends ZipEntry> entries = zipFile.entries();
//            while (entries.hasMoreElements()) {
//                ZipEntry entry = entries.nextElement();
//                if (!entry.isDirectory() && entry.getName().endsWith(".html")) {
//                    String pathInZip = entry.getName(); // ví dụ: "folder/sub/article.html"
//                    String fileName = Paths.get(pathInZip).getFileName().toString();
//                    // String fileName = new File(pathInZip).getName();
//                    fileName = fileName.replaceAll("[\\\\/]+", "_");
//                    try (InputStream is = zipFile.getInputStream(entry)) {
//                        String html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
//                        // Đẩy sang worker xử lý từng document
//                        BookDocument document = ingestionService.ingestDocument(html, fileName);
//                    }
//                }
//            }
//        } catch (Exception e) {
//            System.err.println("Error processing zip: " + e.getMessage());
//        }
//    }
}
