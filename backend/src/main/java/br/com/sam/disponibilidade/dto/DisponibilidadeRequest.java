package br.com.sam.disponibilidade.dto;

import br.com.sam.disponibilidade.model.DiaSemana;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record DisponibilidadeRequest(
        @NotNull(message = "Dia da semana é obrigatório")
        DiaSemana diaSemana,

        @NotNull(message = "Hora de início é obrigatória")
        LocalTime horaInicio,

        @NotNull(message = "Hora de fim é obrigatória")
        LocalTime horaFim,

        @Min(value = 10, message = "Duração mínima do slot é 10 minutos")
        Integer duracaoSlotMin
) {}
