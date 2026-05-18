package br.com.sam.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ConsultaNaoRealizadaException extends RuntimeException {
    public ConsultaNaoRealizadaException() {
        super("O retorno só pode ser solicitado para consultas com status REALIZADA.");
    }
}
