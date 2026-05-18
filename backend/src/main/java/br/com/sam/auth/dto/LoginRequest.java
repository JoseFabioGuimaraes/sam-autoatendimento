package br.com.sam.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail deve ter formato válido")
        String email,

        @NotBlank(message = "Senha é obrigatória")
        String senha
) {}
