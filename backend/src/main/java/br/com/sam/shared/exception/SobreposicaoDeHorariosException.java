package br.com.sam.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class SobreposicaoDeHorariosException extends RuntimeException {
    public SobreposicaoDeHorariosException() {
        super("A faixa de horário informada se sobrepõe a uma faixa já existente para este dia.");
    }
}
