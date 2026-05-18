package br.com.sam.checkin.dto;

import br.com.sam.checkin.model.CheckIn;
import br.com.sam.checkin.model.StatusCheckin;

import java.time.LocalDateTime;

public record CheckInResponse(
        String id,
        String consultaId,
        String pacienteId,
        String pacienteNome,
        LocalDateTime realizadoEm,
        StatusCheckin statusCheckin,
        String justificativaEspera,
        LocalDateTime respondidoEm
) {
    public static CheckInResponse from(CheckIn c) {
        return new CheckInResponse(
                c.getId(),
                c.getConsulta().getId(),
                c.getPaciente().getId(),
                c.getPaciente().getNome(),
                c.getRealizadoEm(),
                c.getStatusCheckin(),
                c.getJustificativaEspera(),
                c.getRespondidoEm()
        );
    }
}
