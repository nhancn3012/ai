package vn.com.vpbank.chatbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "vn.com.vpbank.chatbot"
})
public class ChatBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatBotApplication.class);
    }

}
