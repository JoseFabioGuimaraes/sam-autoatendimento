package br.com.sam.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class DataCancelamentoInvalidaException extends RuntimeException {
    public DataCancelamentoInvalidaException(String message) {
        super(message);
    }
}
