package br.com.sam.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class HorarioNoPassadoException extends RuntimeException {
    public HorarioNoPassadoException() {
        super("Não é possível agendar uma consulta para um horário no passado.");
    }
}
