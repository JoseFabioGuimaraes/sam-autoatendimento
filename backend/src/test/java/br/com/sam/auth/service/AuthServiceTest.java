package br.com.sam.auth.service;

import br.com.sam.auth.dto.LoginRequest;
import br.com.sam.auth.dto.RegisterRequest;
import br.com.sam.auth.dto.TokenResponse;
import br.com.sam.auth.security.JwtService;
import br.com.sam.shared.exception.CredenciaisInvalidasException;
import br.com.sam.shared.exception.EmailJaCadastradoException;
import br.com.sam.usuario.model.PerfilUsuario;
import br.com.sam.usuario.model.Usuario;
import br.com.sam.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para AuthService.
 * Cobre: T01–T06 das regras de negócio.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — Testes Unitários")
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "expirationMs", 86400000L);
    }

    // ===== T01 =====
    @Test
    @DisplayName("T01 — Cadastro com e-mail único e senha válida: usuário criado com BCrypt")
    void t01_cadastroComEmailUnicoESenhaValida_deveSalvarComBCrypt() {
        RegisterRequest request = new RegisterRequest("João Silva", "joao@test.com", "Senha@123");

        when(usuarioRepository.existsByEmail("joao@test.com")).thenReturn(false);
        when(passwordEncoder.encode("Senha@123")).thenReturn("$2a$hash");
        when(jwtService.gerarToken(any())).thenReturn("jwt.token.here");
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TokenResponse response = authService.registrar(request);

        assertThat(response.token()).isEqualTo("jwt.token.here");
        assertThat(response.email()).isEqualTo("joao@test.com");
        assertThat(response.perfil()).isEqualTo("PACIENTE");
        verify(passwordEncoder).encode("Senha@123");
        verify(usuarioRepository).save(argThat(u -> u.getSenha().equals("$2a$hash")));
    }

    // ===== T02 =====
    @Test
    @DisplayName("T02 — Cadastro com e-mail duplicado: lança EmailJaCadastradoException")
    void t02_cadastroComEmailDuplicado_deveLancarEmailJaCadastradoException() {
        RegisterRequest request = new RegisterRequest("João Silva", "joao@test.com", "Senha@123");
        when(usuarioRepository.existsByEmail("joao@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.registrar(request))
                .isInstanceOf(EmailJaCadastradoException.class)
                .hasMessageContaining("joao@test.com");

        verify(usuarioRepository, never()).save(any());
    }

    // ===== T03 — Validação via Bean Validation, testada no controller IT =====

    // ===== T04 =====
    @Test
    @DisplayName("T04 — Login com credenciais corretas: JWT retornado")
    void t04_loginComCredenciaisCorretas_deveRetornarJwt() {
        LoginRequest request = new LoginRequest("joao@test.com", "Senha@123");
        Usuario usuario = criarUsuario("joao@test.com", "$2a$hash", PerfilUsuario.PACIENTE, true);

        when(usuarioRepository.findByEmail("joao@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Senha@123", "$2a$hash")).thenReturn(true);
        when(jwtService.gerarToken(usuario)).thenReturn("jwt.valido");

        TokenResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("jwt.valido");
        assertThat(response.perfil()).isEqualTo("PACIENTE");
    }

    // ===== T05 =====
    @Test
    @DisplayName("T05 — Login com senha incorreta: lança CredenciaisInvalidasException")
    void t05_loginComSenhaIncorreta_deveLancarCredenciaisInvalidasException() {
        LoginRequest request = new LoginRequest("joao@test.com", "senhaErrada");
        Usuario usuario = criarUsuario("joao@test.com", "$2a$hash", PerfilUsuario.PACIENTE, true);

        when(usuarioRepository.findByEmail("joao@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senhaErrada", "$2a$hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(CredenciaisInvalidasException.class);
    }

    // ===== T06 =====
    @Test
    @DisplayName("T06 — Login com e-mail inexistente: lança CredenciaisInvalidasException (sem indicar campo)")
    void t06_loginComEmailInexistente_deveLancarCredenciaisInvalidasException() {
        LoginRequest request = new LoginRequest("inexistente@test.com", "Senha@123");
        when(usuarioRepository.findByEmail("inexistente@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage("Credenciais inválidas."); // mensagem genérica — RN-AUTH-06
    }

    // ===== Helper =====
    private Usuario criarUsuario(String email, String senha, PerfilUsuario perfil, boolean ativo) {
        return Usuario.builder()
                .id("uuid-test-1")
                .nome("Teste")
                .email(email)
                .senha(senha)
                .perfil(perfil)
                .ativo(ativo)
                .build();
    }
}
