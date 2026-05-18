package br.com.sam.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class SlotIndisponivelException extends RuntimeException {
    public SlotIndisponivelException() {
        super("O horário solicitado não está disponível para agendamento.");
    }
}
