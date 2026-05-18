package br.com.sam.cancelamento.dto;

import br.com.sam.cancelamento.model.CancelamentoAgenda;
import br.com.sam.cancelamento.model.TipoCancelamento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record CancelamentoAgendaResponse(
        String id,
        String medicoId,
        String medicoNome,
        TipoCancelamento tipoCancelamento,
        LocalDate data,
        LocalTime horaInicio,
        LocalTime horaFim,
        String motivoCancelamento,
        LocalDateTime criadoEm,
        int consultasAfetadas
) {
    public static CancelamentoAgendaResponse from(CancelamentoAgenda c, int consultasAfetadas) {
        return new CancelamentoAgendaResponse(
                c.getId(),
                c.getMedico().getId(),
                c.getMedico().getNomeCompleto(),
                c.getTipoCancelamento(),
                c.getData(),
                c.getHoraInicio(),
                c.getHoraFim(),
                c.getMotivoCancelamento(),
                c.getCriadoEm(),
                consultasAfetadas
        );
    }
}
