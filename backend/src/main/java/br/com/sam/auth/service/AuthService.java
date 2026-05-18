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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço de autenticação e cadastro de usuários.
 * Regras: RN-AUTH-01 a RN-AUTH-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    /**
     * Cadastra novo paciente. Endpoint público (RN-AUTH-04).
     * Valida email único (RN-AUTH-05) e armazena senha em BCrypt (RN-AUTH-03).
     */
    @Transactional
    public TokenResponse registrar(RegisterRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new EmailJaCadastradoException(request.email());
        }

        Usuario usuario = Usuario.builder()
                .nome(request.nome())
                .email(request.email())
                .senha(passwordEncoder.encode(request.senha()))
                .perfil(PerfilUsuario.PACIENTE)
                .ativo(true)
                .build();

        usuarioRepository.save(usuario);
        log.info("Novo paciente cadastrado: {}", usuario.getEmail());

        String token = jwtService.gerarToken(usuario);
        return TokenResponse.of(token, usuario.getEmail(), usuario.getPerfil().name(), expirationMs);
    }

    /**
     * Autentica usuário e retorna JWT.
     * Em caso de falha, retorna 401 sem indicar qual campo está errado (RN-AUTH-06).
     */
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(CredenciaisInvalidasException::new);

        if (!passwordEncoder.matches(request.senha(), usuario.getSenha())) {
            throw new CredenciaisInvalidasException();
        }

        if (!usuario.getAtivo()) {
            throw new CredenciaisInvalidasException();
        }

        log.info("Login realizado: {}", usuario.getEmail());
        String token = jwtService.gerarToken(usuario);
        return TokenResponse.of(token, usuario.getEmail(), usuario.getPerfil().name(), expirationMs);
    }
}
