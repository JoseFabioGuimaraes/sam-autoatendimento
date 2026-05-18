package br.com.sam.consulta.service;

import br.com.sam.consulta.dto.ConsultaRequest;
import br.com.sam.consulta.dto.ConsultaResponse;
import br.com.sam.consulta.model.Consulta;
import br.com.sam.consulta.model.StatusConsulta;
import br.com.sam.consulta.model.TipoConsulta;
import br.com.sam.consulta.repository.ConsultaRepository;
import br.com.sam.disponibilidade.service.DisponibilidadeService;
import br.com.sam.medico.model.Medico;
import br.com.sam.medico.repository.MedicoRepository;
import br.com.sam.shared.exception.*;
import br.com.sam.usuario.model.PerfilUsuario;
import br.com.sam.usuario.model.Usuario;
import br.com.sam.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para ConsultaService — fluxo de retorno.
 * Cobre: T33–T36 das regras de negócio.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConsultaService (Retorno) — Testes Unitários")
class ConsultaServiceRetornoTest {

    @Mock private ConsultaRepository consultaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private MedicoRepository medicoRepository;
    @Mock private DisponibilidadeService disponibilidadeService;

    @InjectMocks
    private ConsultaService consultaService;

    private final LocalDateTime FUTURO = LocalDateTime.now().plusDays(10);

    // ===== T33 =====
    @Test
    @DisplayName("T33 — Paciente solicita retorno de consulta REALIZADA: criada com AGUARDANDO_APROVACAO")
    void t33_retornoDeConsultaRealizada_deveCriarComAguardandoAprovacao() {
        Usuario paciente = paciente("p1", "p@test.com");
        Medico medico = medico("m1");
        Consulta origem = consultaOrigem("c-origem", paciente, medico, StatusConsulta.REALIZADA);
        ConsultaRequest request = new ConsultaRequest("m1", FUTURO);

        when(usuarioRepository.findByEmail("p@test.com")).thenReturn(Optional.of(paciente));
        when(consultaRepository.findById("c-origem")).thenReturn(Optional.of(origem));
        when(consultaRepository.findByConsultaOrigemId("c-origem")).thenReturn(Optional.empty());
        when(medicoRepository.findById("m1")).thenReturn(Optional.of(medico));
        when(consultaRepository.findConflitsDoPaciente(any(), any())).thenReturn(java.util.List.of());
        when(consultaRepository.findConflitosDoMedico(any(), any(), any())).thenReturn(java.util.List.of());
        when(consultaRepository.save(any())).thenAnswer(inv -> {
            Consulta c = inv.getArgument(0);
            c.setId("c-retorno");
            return c;
        });

        ConsultaResponse response = consultaService.solicitarRetorno("p@test.com", "c-origem", request);

        assertThat(response.status()).isEqualTo(StatusConsulta.AGUARDANDO_APROVACAO);
        assertThat(response.tipoConsulta()).isEqualTo(TipoConsulta.RETORNO);
    }

    // ===== T34 =====
    @Test
    @DisplayName("T34 — Retorno de consulta com status != REALIZADA: lança ConsultaNaoRealizadaException")
    void t34_retornoConsultaNaoRealizada_deveLancarConsultaNaoRealizadaException() {
        Usuario paciente = paciente("p1", "p@test.com");
        Medico medico = medico("m1");
        Consulta origem = consultaOrigem("c-origem", paciente, medico, StatusConsulta.APROVADA);
        ConsultaRequest request = new ConsultaRequest("m1", FUTURO);

        when(usuarioRepository.findByEmail("p@test.com")).thenReturn(Optional.of(paciente));
        when(consultaRepository.findById("c-origem")).thenReturn(Optional.of(origem));

        assertThatThrownBy(() -> consultaService.solicitarRetorno("p@test.com", "c-origem", request))
                .isInstanceOf(ConsultaNaoRealizadaException.class);
    }

    // ===== T35 =====
    @Test
    @DisplayName("T35 — Retorno duplicado para mesma consulta: lança RetornoDuplicadoException")
    void t35_retornoDuplicado_deveLancarRetornoDuplicadoException() {
        Usuario paciente = paciente("p1", "p@test.com");
        Medico medico = medico("m1");
        Consulta origem = consultaOrigem("c-origem", paciente, medico, StatusConsulta.REALIZADA);
        ConsultaRequest request = new ConsultaRequest("m1", FUTURO);

        when(usuarioRepository.findByEmail("p@test.com")).thenReturn(Optional.of(paciente));
        when(consultaRepository.findById("c-origem")).thenReturn(Optional.of(origem));
        when(consultaRepository.findByConsultaOrigemId("c-origem"))
                .thenReturn(Optional.of(new Consulta())); // retorno já existe

        assertThatThrownBy(() -> consultaService.solicitarRetorno("p@test.com", "c-origem", request))
                .isInstanceOf(RetornoDuplicadoException.class);
    }

    // ===== T36 =====
    @Test
    @DisplayName("T36 — Paciente cria retorno de consulta de outro paciente: lança AcessoNegadoException")
    void t36_retornoConsultaDeOutroPaciente_deveLancarAcessoNegadoException() {
        Usuario paciente1 = paciente("p1", "p1@test.com");
        Usuario paciente2 = paciente("p2", "p2@test.com");
        Medico medico = medico("m1");
        Consulta origem = consultaOrigem("c-origem", paciente2, medico, StatusConsulta.REALIZADA);
        ConsultaRequest request = new ConsultaRequest("m1", FUTURO);

        when(usuarioRepository.findByEmail("p1@test.com")).thenReturn(Optional.of(paciente1));
        when(consultaRepository.findById("c-origem")).thenReturn(Optional.of(origem));

        assertThatThrownBy(() -> consultaService.solicitarRetorno("p1@test.com", "c-origem", request))
                .isInstanceOf(AcessoNegadoException.class);
    }

    // ===== Helpers =====
    private Usuario paciente(String id, String email) {
        return Usuario.builder().id(id).nome("Paciente").email(email)
                .senha("hash").perfil(PerfilUsuario.PACIENTE).ativo(true).build();
    }

    private Medico medico(String id) {
        Usuario u = Usuario.builder().id(id).nome("Dr").email(id + "@test.com")
                .senha("hash").perfil(PerfilUsuario.MEDICO).ativo(true).build();
        Medico m = new Medico();
        m.setId(id); m.setCrm("CRM"); m.setEspecialidade("Clinica");
        m.setNomeCompleto("Dr. Teste"); m.setUsuario(u);
        return m;
    }

    private Consulta consultaOrigem(String id, Usuario paciente, Medico medico, StatusConsulta status) {
        return Consulta.builder()
                .id(id).paciente(paciente).medico(medico)
                .dataHora(LocalDateTime.now().minusDays(3))
                .status(status).tipoConsulta(TipoConsulta.NORMAL)
                .build();
    }
}
