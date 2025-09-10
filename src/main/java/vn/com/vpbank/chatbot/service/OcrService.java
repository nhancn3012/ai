package vn.com.vpbank.chatbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.Media;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.net.URL;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OcrService {
    private final OpenAiChatModel openAiChatModel;

    public String extractTextFromImage(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            MimeType mime = MimeTypeUtils.parseMimeType(detectMimeType(imageUrl));
            Media imageMedia = new Media(mime, url);

            UserMessage userMessage = new UserMessage(
                    "Extract all readable text from this image. Return plain text only.",
                    List.of(imageMedia)
            );
            Prompt prompt = new Prompt(
                    List.of(userMessage),
                    OpenAiChatOptions.builder()
                            .model("gpt-4o")
                            .build()
            );
            ChatResponse response = openAiChatModel.call(prompt);
            return response.getResult().getOutput().getText();
        } catch (Exception e) {
            log.error("Process extractTextFromImage error: ", e);
            return Strings.EMPTY;
        }
    }

    private String detectMimeType(String imageUrl) {
        String u = imageUrl.toLowerCase();
        if (u.endsWith(".png")) return "image/png";
        if (u.endsWith(".jpg") || u.endsWith(".jpeg")) return "image/jpeg";
        if (u.endsWith(".gif")) return "image/gif";
        return "application/octet-stream";
    }
}
