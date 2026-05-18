package br.com.sam.consulta.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ConsultaRequest(
        @NotBlank(message = "ID do médico é obrigatório")
        String medicoId,

        @NotNull(message = "Data e hora são obrigatórias")
        @Future(message = "A data da consulta deve ser futura")
        LocalDateTime dataHora
) {}
