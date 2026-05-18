package br.com.sam.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class RetornoDuplicadoException extends RuntimeException {
    public RetornoDuplicadoException() {
        super("Já existe um retorno cadastrado para esta consulta.");
    }
}
