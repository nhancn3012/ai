package vn.com.vpbank.chatbot.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * @author nhannv7
 */
@Getter
@AllArgsConstructor
public enum BaseResCode implements IResponseCode {

    SUCCESS(HttpStatus.OK, "200", "Success"),
    DATA_NOT_FOUND(HttpStatus.OK, "001", "Data not found"),
    BUSINESS_ERROR_CODE(HttpStatus.UNPROCESSABLE_ENTITY, "422", "Business error"),
    BAD_REQUEST_CODE(HttpStatus.BAD_REQUEST, "400", "Bad request"),
    NOT_FOUND_CODE(HttpStatus.NOT_FOUND, "404", "Not found"),
    TIMEOUT_CODE(HttpStatus.GATEWAY_TIMEOUT, "504", "Timeout"),
    UNAUTHORIZED_CODE(HttpStatus.UNAUTHORIZED, "401", "Unauthorized"),
    INTERNAL_ERROR_CODE(HttpStatus.INTERNAL_SERVER_ERROR, "500", "Internal server error");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
