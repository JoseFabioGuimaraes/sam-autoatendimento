package br.com.sam.auth.controller;

import br.com.sam.auth.dto.LoginRequest;
import br.com.sam.auth.dto.RegisterRequest;
import br.com.sam.usuario.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração para AuthController.
 * Cobre: T37–T40 das regras de negócio.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AuthController — Testes de Integração")
class AuthControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void limpar() {
        usuarioRepository.deleteAll();
    }

    // ===== T37 =====
    @Test
    @DisplayName("T37 — POST /auth/register com dados válidos: 201 Created")
    void t37_registrarComDadosValidos_deve201() throws Exception {
        RegisterRequest request = new RegisterRequest("Maria Silva", "maria@test.com", "Senha@123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.perfil").value("PACIENTE"))
                .andExpect(jsonPath("$.email").value("maria@test.com"));
    }

    // ===== T38 =====
    @Test
    @DisplayName("T38 — POST /auth/register com e-mail duplicado: 409 Conflict")
    void t38_registrarComEmailDuplicado_deve409() throws Exception {
        RegisterRequest request = new RegisterRequest("João", "joao@test.com", "Senha@123");

        // Primeiro registro
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Segundo registro com mesmo e-mail
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    // ===== T39 =====
    @Test
    @DisplayName("T39 — POST /auth/login com credenciais corretas: 200 + JWT")
    void t39_loginComCredenciaisCorretas_deve200ComJwt() throws Exception {
        // Cadastra
        RegisterRequest registerRequest = new RegisterRequest("Ana", "ana@test.com", "Senha@123");
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        // Login
        LoginRequest loginRequest = new LoginRequest("ana@test.com", "Senha@123");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"));
    }

    // ===== T40 =====
    @Test
    @DisplayName("T40 — POST /auth/login com senha errada: 401 Unauthorized")
    void t40_loginComSenhaErrada_deve401() throws Exception {
        // Cadastra
        RegisterRequest registerRequest = new RegisterRequest("Carlos", "carlos@test.com", "Senha@123");
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        // Login com senha errada
        LoginRequest loginRequest = new LoginRequest("carlos@test.com", "senhaErrada");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // ===== T41 — GET /medicos/{id}/slots sem token: 401/403 =====
    @Test
    @DisplayName("T41 — GET /medicos/{id}/slots sem token: 401 ou 403 Unauthorized/Forbidden")
    void t41_slotsSeToken_deve401Ou403() throws Exception {
        // Spring Security 6 retorna 403 por padrão sem token (sem anonymous access).
        // RN-AUTH-01: endpoint protegido — sem credencial válida, acesso negado.
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/medicos/qualquer-id/slots?data=2027-01-04"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 401 || status == 403
                            : "Esperado 401 ou 403, mas recebeu: " + status;
                });
    }
}
