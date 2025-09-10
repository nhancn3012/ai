package vn.com.vpbank.chatbot.enums;

import org.springframework.http.HttpStatus;

/**
 * @author nhannv7
 */
public interface IResponseCode {

    String getCode();

    String getMessage();

    HttpStatus getHttpStatus();

    default boolean codeIs(String code) {
        return this.getCode().equals(code);
    }
}
