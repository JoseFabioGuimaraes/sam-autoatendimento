package br.com.sam.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class JustificativaObrigatoriaException extends RuntimeException {
    public JustificativaObrigatoriaException() {
        super("A justificativa é obrigatória ao recusar uma consulta.");
    }
    
    public JustificativaObrigatoriaException(String message) {
        super(message);
    }
}
