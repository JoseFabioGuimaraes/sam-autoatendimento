package br.com.sam.disponibilidade.dto;

import br.com.sam.disponibilidade.model.DiaSemana;
import br.com.sam.disponibilidade.model.DisponibilidadeMedico;

import java.time.LocalTime;

public record DisponibilidadeResponse(
        String id,
        String medicoId,
        DiaSemana diaSemana,
        LocalTime horaInicio,
        LocalTime horaFim,
        int duracaoSlotMin,
        boolean ativo
) {
    public static DisponibilidadeResponse from(DisponibilidadeMedico d) {
        return new DisponibilidadeResponse(
                d.getId(),
                d.getMedico().getId(),
                d.getDiaSemana(),
                d.getHoraInicio(),
                d.getHoraFim(),
                d.getDuracaoSlotMin(),
                d.getAtivo()
        );
    }
}
