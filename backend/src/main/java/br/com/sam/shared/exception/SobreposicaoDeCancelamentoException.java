package br.com.sam.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class SobreposicaoDeCancelamentoException extends RuntimeException {
    public SobreposicaoDeCancelamentoException(String message) {
        super(message);
    }
}
