package br.com.sam.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class PerfilNaoAutorizadoException extends RuntimeException {
    public PerfilNaoAutorizadoException(String message) {
        super(message);
    }
}
