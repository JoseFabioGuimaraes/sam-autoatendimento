package br.com.sam.consulta.service;

import br.com.sam.consulta.dto.ConsultaRequest;
import br.com.sam.consulta.dto.ConsultaResponse;
import br.com.sam.consulta.model.Consulta;
import br.com.sam.consulta.model.StatusConsulta;
import br.com.sam.consulta.model.TipoConsulta;
import br.com.sam.consulta.repository.ConsultaRepository;
import br.com.sam.disponibilidade.dto.SlotResponse;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para ConsultaService — fluxo do paciente.
 * Cobre: T16–T24 das regras de negócio.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConsultaService (Paciente) — Testes Unitários")
class ConsultaServicePacienteTest {

    @Mock private ConsultaRepository consultaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private MedicoRepository medicoRepository;
    @Mock private DisponibilidadeService disponibilidadeService;

    @InjectMocks
    private ConsultaService consultaService;

    private final LocalDateTime FUTURO = LocalDateTime.now().plusDays(5);

    // ===== T16 =====
    @Test
    @DisplayName("T16 — Paciente solicita slot livre válido: consulta criada com AGUARDANDO_APROVACAO")
    void t16_slotLivreValido_deveCriarConsultaAguardandoAprovacao() {
        Usuario paciente = paciente("p1", "paciente@test.com");
        Medico medico = medico("m1");
        ConsultaRequest request = new ConsultaRequest("m1", FUTURO);

        when(usuarioRepository.findByEmail("paciente@test.com")).thenReturn(Optional.of(paciente));
        when(medicoRepository.findById("m1")).thenReturn(Optional.of(medico));
        when(disponibilidadeService.calcularSlots(eq("m1"), any()))
                .thenReturn(List.of(new SlotResponse(FUTURO)));
        when(consultaRepository.findConflitsDoPaciente(any(), any())).thenReturn(Collections.emptyList());
        when(consultaRepository.findConflitosDoMedico(any(), any(), any())).thenReturn(Collections.emptyList());
        when(consultaRepository.save(any())).thenAnswer(inv -> {
            Consulta c = inv.getArgument(0);
            c.setId("consulta-1");
            return c;
        });

        ConsultaResponse response = consultaService.solicitarConsulta("paciente@test.com", request);

        assertThat(response.status()).isEqualTo(StatusConsulta.AGUARDANDO_APROVACAO);
        assertThat(response.tipoConsulta()).isEqualTo(TipoConsulta.NORMAL);
    }

    // ===== T17 =====
    @Test
    @DisplayName("T17 — Paciente solicita horário no passado: lança HorarioNoPassadoException")
    void t17_horarioNoPassado_deveLancarHorarioNoPassadoException() {
        Usuario paciente = paciente("p1", "paciente@test.com");
        ConsultaRequest request = new ConsultaRequest("m1", LocalDateTime.now().minusDays(1));

        when(usuarioRepository.findByEmail("paciente@test.com")).thenReturn(Optional.of(paciente));

        assertThatThrownBy(() -> consultaService.solicitarConsulta("paciente@test.com", request))
                .isInstanceOf(HorarioNoPassadoException.class);
    }

    // ===== T18 =====
    @Test
    @DisplayName("T18 — Paciente solicita slot já ocupado (conflito médico): lança SlotIndisponivelException")
    void t18_slotOcupado_deveLancarSlotIndisponivelException() {
        Usuario paciente = paciente("p1", "paciente@test.com");
        Medico medico = medico("m1");
        ConsultaRequest request = new ConsultaRequest("m1", FUTURO);

        when(usuarioRepository.findByEmail("paciente@test.com")).thenReturn(Optional.of(paciente));
        when(medicoRepository.findById("m1")).thenReturn(Optional.of(medico));
        // Slot não disponível (lista vazia)
        when(disponibilidadeService.calcularSlots(eq("m1"), any())).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> consultaService.solicitarConsulta("paciente@test.com", request))
                .isInstanceOf(SlotIndisponivelException.class);
    }

    // ===== T19 =====
    @Test
    @DisplayName("T19 — Médico tenta solicitar consulta: lança PerfilNaoAutorizadoException")
    void t19_medicoTentaSolicitar_deveLancarPerfilNaoAutorizadoException() {
        Usuario medico = usuario("m1", "medico@test.com", PerfilUsuario.MEDICO);
        ConsultaRequest request = new ConsultaRequest("m1", FUTURO);

        when(usuarioRepository.findByEmail("medico@test.com")).thenReturn(Optional.of(medico));

        assertThatThrownBy(() -> consultaService.solicitarConsulta("medico@test.com", request))
                .isInstanceOf(PerfilNaoAutorizadoException.class);
    }

    // ===== T20 =====
    @Test
    @DisplayName("T20 — Paciente já possui consulta no mesmo horário: lança ConflitoDeAgendaException")
    void t20_conflitoPaciente_deveLancarConflitoDeAgendaException() {
        Usuario paciente = paciente("p1", "paciente@test.com");
        Medico medico = medico("m1");
        ConsultaRequest request = new ConsultaRequest("m1", FUTURO);

        when(usuarioRepository.findByEmail("paciente@test.com")).thenReturn(Optional.of(paciente));
        when(medicoRepository.findById("m1")).thenReturn(Optional.of(medico));
        when(disponibilidadeService.calcularSlots(eq("m1"), any()))
                .thenReturn(List.of(new SlotResponse(FUTURO)));
        when(consultaRepository.findConflitsDoPaciente("p1", FUTURO))
                .thenReturn(List.of(new Consulta()));

        assertThatThrownBy(() -> consultaService.solicitarConsulta("paciente@test.com", request))
                .isInstanceOf(ConflitoDeAgendaException.class);
    }

    // ===== T21 =====
    @Test
    @DisplayName("T21 — Paciente cancela consulta AGUARDANDO_APROVACAO: status muda para CANCELADA_PELO_PACIENTE")
    void t21_cancelarAguardandoAprovacao_deveMudarStatus() {
        Usuario paciente = paciente("p1", "paciente@test.com");
        Consulta consulta = consultaComStatus("c1", paciente, StatusConsulta.AGUARDANDO_APROVACAO);

        when(usuarioRepository.findByEmail("paciente@test.com")).thenReturn(Optional.of(paciente));
        when(consultaRepository.findById("c1")).thenReturn(Optional.of(consulta));

        ConsultaResponse response = consultaService.cancelarConsulta("paciente@test.com", "c1");

        assertThat(response.status()).isEqualTo(StatusConsulta.CANCELADA_PELO_PACIENTE);
    }

    // ===== T22 =====
    @Test
    @DisplayName("T22 — Paciente cancela consulta APROVADA: status muda para CANCELADA_PELO_PACIENTE")
    void t22_cancelarAprovada_deveMudarStatus() {
        Usuario paciente = paciente("p1", "paciente@test.com");
        Consulta consulta = consultaComStatus("c1", paciente, StatusConsulta.APROVADA);

        when(usuarioRepository.findByEmail("paciente@test.com")).thenReturn(Optional.of(paciente));
        when(consultaRepository.findById("c1")).thenReturn(Optional.of(consulta));

        ConsultaResponse response = consultaService.cancelarConsulta("paciente@test.com", "c1");

        assertThat(response.status()).isEqualTo(StatusConsulta.CANCELADA_PELO_PACIENTE);
    }

    // ===== T23 =====
    @Test
    @DisplayName("T23 — Paciente tenta cancelar consulta REALIZADA: lança TransicaoInvalidaException")
    void t23_cancelarRealizada_deveLancarTransicaoInvalidaException() {
        Usuario paciente = paciente("p1", "paciente@test.com");
        Consulta consulta = consultaComStatus("c1", paciente, StatusConsulta.REALIZADA);

        when(usuarioRepository.findByEmail("paciente@test.com")).thenReturn(Optional.of(paciente));
        when(consultaRepository.findById("c1")).thenReturn(Optional.of(consulta));

        assertThatThrownBy(() -> consultaService.cancelarConsulta("paciente@test.com", "c1"))
                .isInstanceOf(TransicaoInvalidaException.class);
    }

    // ===== T24 =====
    @Test
    @DisplayName("T24 — Paciente tenta cancelar consulta de outro paciente: lança AcessoNegadoException")
    void t24_cancelarConsultaDeOutroPaciente_deveLancarAcessoNegadoException() {
        Usuario paciente1 = paciente("p1", "p1@test.com");
        Usuario paciente2 = paciente("p2", "p2@test.com");
        Consulta consulta = consultaComStatus("c1", paciente2, StatusConsulta.AGUARDANDO_APROVACAO);

        when(usuarioRepository.findByEmail("p1@test.com")).thenReturn(Optional.of(paciente1));
        when(consultaRepository.findById("c1")).thenReturn(Optional.of(consulta));

        assertThatThrownBy(() -> consultaService.cancelarConsulta("p1@test.com", "c1"))
                .isInstanceOf(AcessoNegadoException.class);
    }

    // ===== T33 =====
    @Test
    @DisplayName("T33 — Paciente solicita retorno de consulta REALIZADA: retorno criado com AGUARDANDO_APROVACAO")
    void t33_solicitarRetornoConsultaRealizada_deveCriarAguardandoAprovacao() {
        Usuario paciente = paciente("p1", "paciente@test.com");
        Consulta consultaRealizada = consultaComStatus("c1", paciente, StatusConsulta.REALIZADA);

        when(usuarioRepository.findByEmail("paciente@test.com")).thenReturn(Optional.of(paciente));
        when(medicoRepository.findById("m1")).thenReturn(Optional.of(medico("m1")));
        when(consultaRepository.findById("c1")).thenReturn(Optional.of(consultaRealizada));
        when(consultaRepository.findByConsultaOrigemId("c1")).thenReturn(Optional.empty());

        when(consultaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ConsultaRequest request = new ConsultaRequest("m1", FUTURO);
        ConsultaResponse response = consultaService.solicitarRetorno("paciente@test.com", "c1", request);

        assertThat(response.status()).isEqualTo(StatusConsulta.AGUARDANDO_APROVACAO);
        assertThat(response.tipoConsulta()).isEqualTo(TipoConsulta.RETORNO);
    }

    // ===== T34 =====
    @Test
    @DisplayName("T34 — Retorno de consulta com status != REALIZADA: lança ConsultaNaoRealizadaException")
    void t34_solicitarRetornoConsultaNaoRealizada_deveLancarConsultaNaoRealizadaException() {
        Usuario paciente = paciente("p1", "paciente@test.com");
        Consulta consultaAprovada = consultaComStatus("c1", paciente, StatusConsulta.APROVADA);

        when(usuarioRepository.findByEmail("paciente@test.com")).thenReturn(Optional.of(paciente));
        when(consultaRepository.findById("c1")).thenReturn(Optional.of(consultaAprovada));

        ConsultaRequest request = new ConsultaRequest("m1", FUTURO);
        assertThatThrownBy(() -> consultaService.solicitarRetorno("paciente@test.com", "c1", request))
                .isInstanceOf(ConsultaNaoRealizadaException.class);
    }

    // ===== T35 =====
    @Test
    @DisplayName("T35 — Retorno duplicado para a mesma consulta: lança RetornoDuplicadoException")
    void t35_solicitarRetornoDuplicado_deveLancarRetornoDuplicadoException() {
        Usuario paciente = paciente("p1", "paciente@test.com");
        Consulta consultaRealizada = consultaComStatus("c1", paciente, StatusConsulta.REALIZADA);

        when(usuarioRepository.findByEmail("paciente@test.com")).thenReturn(Optional.of(paciente));
        when(consultaRepository.findById("c1")).thenReturn(Optional.of(consultaRealizada));
        when(consultaRepository.findByConsultaOrigemId("c1")).thenReturn(Optional.of(new Consulta()));

        ConsultaRequest request = new ConsultaRequest("m1", FUTURO);
        assertThatThrownBy(() -> consultaService.solicitarRetorno("paciente@test.com", "c1", request))
                .isInstanceOf(RetornoDuplicadoException.class);
    }

    // ===== T36 =====
    @Test
    @DisplayName("T36 — Paciente tenta criar retorno de consulta de outro paciente: lança AcessoNegadoException")
    void t36_solicitarRetornoConsultaOutroPaciente_deveLancarAcessoNegadoException() {
        Usuario paciente1 = paciente("p1", "p1@test.com");
        Usuario paciente2 = paciente("p2", "p2@test.com");
        Consulta consultaOutro = consultaComStatus("c1", paciente2, StatusConsulta.REALIZADA);

        when(usuarioRepository.findByEmail("p1@test.com")).thenReturn(Optional.of(paciente1));
        when(consultaRepository.findById("c1")).thenReturn(Optional.of(consultaOutro));

        ConsultaRequest request = new ConsultaRequest("m1", FUTURO);
        assertThatThrownBy(() -> consultaService.solicitarRetorno("p1@test.com", "c1", request))
                .isInstanceOf(AcessoNegadoException.class);
    }

    // ===== Helpers =====
    private Usuario usuario(String id, String email, PerfilUsuario perfil) {
        return Usuario.builder().id(id).nome("Teste").email(email)
                .senha("hash").perfil(perfil).ativo(true).build();
    }

    private Usuario paciente(String id, String email) {
        return usuario(id, email, PerfilUsuario.PACIENTE);
    }

    private Medico medico(String id) {
        Usuario u = usuario(id, id + "@test.com", PerfilUsuario.MEDICO);
        Medico m = new Medico();
        m.setId(id); m.setCrm("CRM"); m.setEspecialidade("Clinica");
        m.setNomeCompleto("Dr. Teste"); m.setUsuario(u);
        return m;
    }

    private Consulta consultaComStatus(String id, Usuario paciente, StatusConsulta status) {
        Medico m = medico("m1");
        Consulta c = Consulta.builder()
                .id(id).paciente(paciente).medico(m)
                .dataHora(FUTURO).status(status).tipoConsulta(TipoConsulta.NORMAL)
                .build();
        return c;
    }
}
