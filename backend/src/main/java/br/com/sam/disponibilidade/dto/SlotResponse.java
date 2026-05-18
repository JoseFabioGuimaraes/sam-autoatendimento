package br.com.sam.disponibilidade.dto;

import java.time.LocalDateTime;

public record SlotResponse(
        LocalDateTime dataHora
) {}
