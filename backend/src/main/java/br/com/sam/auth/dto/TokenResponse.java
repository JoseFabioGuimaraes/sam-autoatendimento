package br.com.sam.auth.dto;

public record TokenResponse(
        String token,
        String tipo,
        String email,
        String perfil,
        long expiracaoMs
) {
    public static TokenResponse of(String token, String email, String perfil, long expiracaoMs) {
        return new TokenResponse(token, "Bearer", email, perfil, expiracaoMs);
    }
}
