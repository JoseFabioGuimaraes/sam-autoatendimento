package br.com.sam.consulta.dto;

import jakarta.validation.constraints.NotBlank;

public record RecusaRequest(
        @NotBlank(message = "Justificativa é obrigatória ao recusar uma consulta")
        String justificativa
) {}
