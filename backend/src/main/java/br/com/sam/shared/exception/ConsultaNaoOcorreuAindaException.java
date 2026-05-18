package br.com.sam.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ConsultaNaoOcorreuAindaException extends RuntimeException {
    public ConsultaNaoOcorreuAindaException() {
        super("Não é possível marcar como realizada uma consulta cuja data ainda não chegou.");
    }
}
