package br.com.sam.checkin.dto;

import br.com.sam.checkin.model.StatusCheckin;
import jakarta.validation.constraints.NotNull;

public record ConfirmarCheckinRequest(
        @NotNull(message = "Status do check-in é obrigatório")
        StatusCheckin statusCheckin,

        String justificativaEspera
) {}
