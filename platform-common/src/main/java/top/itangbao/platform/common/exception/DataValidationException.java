package top.itangbao.platform.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST) // 返回 400 Bad Request
public class DataValidationException extends RuntimeException {
    public DataValidationException(String message) {
        super(message);
    }
}
