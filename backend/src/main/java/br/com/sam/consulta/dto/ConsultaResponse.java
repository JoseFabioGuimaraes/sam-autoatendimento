package br.com.sam.consulta.dto;

import br.com.sam.consulta.model.Consulta;
import br.com.sam.consulta.model.StatusConsulta;
import br.com.sam.consulta.model.TipoConsulta;

import java.time.LocalDateTime;

public record ConsultaResponse(
        String id,
        String pacienteId,
        String pacienteNome,
        String medicoId,
        String medicoNome,
        LocalDateTime dataHora,
        StatusConsulta status,
        TipoConsulta tipoConsulta,
        String consultaOrigemId,
        String justificativaRecusa,
        String checkinId,
        String checkinStatus,
        String checkinJustificativaEspera,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
    public static ConsultaResponse from(Consulta c) {
        return new ConsultaResponse(
                c.getId(),
                c.getPaciente().getId(),
                c.getPaciente().getNome(),
                c.getMedico().getId(),
                c.getMedico().getNomeCompleto(),
                c.getDataHora(),
                c.getStatus(),
                c.getTipoConsulta(),
                c.getConsultaOrigem() != null ? c.getConsultaOrigem().getId() : null,
                c.getJustificativaRecusa(),
                c.getCheckIn() != null ? c.getCheckIn().getId() : null,
                c.getCheckIn() != null ? c.getCheckIn().getStatusCheckin().name() : null,
                c.getCheckIn() != null ? c.getCheckIn().getJustificativaEspera() : null,
                c.getCriadoEm(),
                c.getAtualizadoEm()
        );
    }
}
