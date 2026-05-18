package br.com.sam.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class CheckinJaRespondidoException extends RuntimeException {
    public CheckinJaRespondidoException() {
        super("Este check-in já foi respondido.");
    }
}
