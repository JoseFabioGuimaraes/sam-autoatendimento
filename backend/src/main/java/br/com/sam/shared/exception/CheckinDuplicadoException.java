package br.com.sam.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class CheckinDuplicadoException extends RuntimeException {
    public CheckinDuplicadoException() {
        super("Já existe um check-in registrado para esta consulta.");
    }
}
