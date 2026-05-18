package br.com.sam.cancelamento.dto;

import br.com.sam.cancelamento.model.TipoCancelamento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record CancelamentoAgendaRequest(
        @NotNull(message = "Tipo de cancelamento é obrigatório")
        TipoCancelamento tipoCancelamento,

        @NotNull(message = "Data é obrigatória")
        LocalDate data,

        LocalTime horaInicio,

        LocalTime horaFim,

        @NotNull(message = "Motivo do cancelamento é obrigatório")
        @Size(min = 10, message = "Motivo deve ter no mínimo 10 caracteres")
        String motivoCancelamento
) {}
