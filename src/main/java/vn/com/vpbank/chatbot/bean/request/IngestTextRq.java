package vn.com.vpbank.chatbot.bean.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestTextRq {

    @NotEmpty
    private String content;
    @NotEmpty
    private String name;
}
