package br.com.sam.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class CheckinForaDaJanelaException extends RuntimeException {
    public CheckinForaDaJanelaException(String message) {
        super(message);
    }
}
