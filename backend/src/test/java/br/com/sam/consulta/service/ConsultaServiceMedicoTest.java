package br.com.sam.consulta.service;

import br.com.sam.consulta.dto.ConsultaResponse;
import br.com.sam.consulta.dto.RecusaRequest;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para ConsultaService — fluxo do médico.
 * Cobre: T25–T32 das regras de negócio.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConsultaService (Médico) — Testes Unitários")
class ConsultaServiceMedicoTest {

    @Mock private ConsultaRepository consultaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private MedicoRepository medicoRepository;
    @Mock private DisponibilidadeService disponibilidadeService;

    @InjectMocks
    private ConsultaService consultaService;

    private final LocalDateTime PASSADO = LocalDateTime.now().minusDays(1);
    private final LocalDateTime FUTURO = LocalDateTime.now().plusDays(5);

    // ===== T25 =====
    @Test
    @DisplayName("T25 — Médico aprova consulta AGUARDANDO_APROVACAO: status muda para APROVADA")
    void t25_aprovarConsultaAguardando_deveMudarParaAprovada() {
        Medico medico = medico("m1", "medico@test.com");
        Consulta consulta = consultaComStatus("c1", medico, StatusConsulta.AGUARDANDO_APROVACAO, FUTURO);

        when(usuarioRepository.findByEmail("medico@test.com")).thenReturn(Optional.of(medico.getUsuario()));
        when(consultaRepository.findById("c1")).thenReturn(Optional.of(consulta));
        when(consultaRepository.findConflitosDoMedico(any(), any(), any())).thenReturn(List.of());

        ConsultaResponse response = consultaService.aprovar("medico@test.com", "c1");

        assertThat(response.status()).isEqualTo(StatusConsulta.APROVADA);
    }

    // ===== T26 =====
    @Test
    @DisplayName("T26 — Médico tenta aprovar consulta já APROVADA: lança TransicaoInvalidaException")
    void t26_aprovarConsultaJaAprovada_deveLancarTransicaoInvalida() {
        Medico medico = medico("m1", "medico@test.com");
        Consulta consulta = consultaComStatus("c1", medico, StatusConsulta.APROVADA, FUTURO);

        when(usuarioRepository.findByEmail("medico@test.com")).thenReturn(Optional.of(medico.getUsuario()));
        when(consultaRepository.findById("c1")).thenReturn(Optional.of(consulta));

        assertThatThrownBy(() -> consultaService.aprovar("medico@test.com", "c1"))
                .isInstanceOf(TransicaoInvalidaException.class);
    }

    // ===== T27 =====
    @Test
    @DisplayName("T27 — Médico recusa com justificativa: status RECUSADA, justificativa salva")
    void t27_recusarComJustificativa_deveSalvarJustificativa() {
        Medico medico = medico("m1", "medico@test.com");
        Consulta consulta = consultaComStatus("c1", medico, StatusConsulta.AGUARDANDO_APROVACAO, FUTURO);
        RecusaRequest request = new RecusaRequest("Agenda lotada");

        when(usuarioRepository.findByEmail("medico@test.com")).thenReturn(Optional.of(medico.getUsuario()));
        when(consultaRepository.findById("c1")).thenReturn(Optional.of(consulta));

        ConsultaResponse response = consultaService.recusar("medico@test.com", "c1", request);

        assertThat(response.status()).isEqualTo(StatusConsulta.RECUSADA);
        assertThat(response.justificativaRecusa()).isEqualTo("Agenda lotada");
    }

    // ===== T28 =====
    @Test
    @DisplayName("T28 — Médico recusa sem justificativa: lança JustificativaObrigatoriaException")
    void t28_recusarSemJustificativa_deveLancarJustificativaObrigatoria() {
        Medico medico = medico("m1", "medico@test.com");
        Consulta consulta = consultaComStatus("c1", medico, StatusConsulta.AGUARDANDO_APROVACAO, FUTURO);
        RecusaRequest request = new RecusaRequest("");

        when(usuarioRepository.findByEmail("medico@test.com")).thenReturn(Optional.of(medico.getUsuario()));
        when(consultaRepository.findById("c1")).thenReturn(Optional.of(consulta));

        assertThatThrownBy(() -> consultaService.recusar("medico@test.com", "c1", request))
                .isInstanceOf(JustificativaObrigatoriaException.class);
    }

    // ===== T29 =====
    @Test
    @DisplayName("T29 — Médico tenta operar consulta de outro médico: lança AcessoNegadoException")
    void t29_medicoOperaConsultaDeOutroMedico_deveLancarAcessoNegado() {
        Medico medicoOrigem = medico("m1", "m1@test.com");
        Medico outroMedico = medico("m2", "m2@test.com");
        Consulta consulta = consultaComStatus("c1", medicoOrigem, StatusConsulta.AGUARDANDO_APROVACAO, FUTURO);

        when(usuarioRepository.findByEmail("m2@test.com")).thenReturn(Optional.of(outroMedico.getUsuario()));
        when(consultaRepository.findById("c1")).thenReturn(Optional.of(consulta));

        assertThatThrownBy(() -> consultaService.aprovar("m2@test.com", "c1"))
                .isInstanceOf(AcessoNegadoException.class);
    }

    // ===== T30 =====
    @Test
    @DisplayName("T30 — Aprovação com consulta concorrente: concorrente é recusada automaticamente")
    void t30_aprovacaoComConcorrente_deveCancelarConcorrente() {
        Medico medico = medico("m1", "medico@test.com");
        Consulta consulta = consultaComStatus("c1", medico, StatusConsulta.AGUARDANDO_APROVACAO, FUTURO);
        Consulta concorrente = consultaComStatus("c2", medico, StatusConsulta.AGUARDANDO_APROVACAO, FUTURO);

        when(usuarioRepository.findByEmail("medico@test.com")).thenReturn(Optional.of(medico.getUsuario()));
        when(consultaRepository.findById("c1")).thenReturn(Optional.of(consulta));
        when(consultaRepository.findConflitosDoMedico(any(), any(), eq("c1")))
                .thenReturn(List.of(concorrente));

        consultaService.aprovar("medico@test.com", "c1");

        assertThat(concorrente.getStatus()).isEqualTo(StatusConsulta.RECUSADA);
        assertThat(concorrente.getJustificativaRecusa())
                .isEqualTo("Horário ocupado por outro agendamento confirmado.");
    }

    // ===== T31 =====
    @Test
    @DisplayName("T31 — Médico marca APROVADA como REALIZADA após dataHora: status REALIZADA")
    void t31_realizarConsultaAprovadaAposDataHora_deveRetornarRealizada() {
        Medico medico = medico("m1", "medico@test.com");
        Consulta consulta = consultaComStatus("c1", medico, StatusConsulta.APROVADA, PASSADO);

        when(usuarioRepository.findByEmail("medico@test.com")).thenReturn(Optional.of(medico.getUsuario()));
        when(consultaRepository.findById("c1")).thenReturn(Optional.of(consulta));

        ConsultaResponse response = consultaService.realizar("medico@test.com", "c1");

        assertThat(response.status()).isEqualTo(StatusConsulta.REALIZADA);
    }

    // ===== T32 =====
    @Test
    @DisplayName("T32 — Médico tenta marcar como REALIZADA antes da dataHora: lança ConsultaNaoOcorreuAindaException")
    void t32_realizarAntesDataHora_deveLancarConsultaNaoOcorreuAinda() {
        Medico medico = medico("m1", "medico@test.com");
        Consulta consulta = consultaComStatus("c1", medico, StatusConsulta.APROVADA, FUTURO);

        when(usuarioRepository.findByEmail("medico@test.com")).thenReturn(Optional.of(medico.getUsuario()));
        when(consultaRepository.findById("c1")).thenReturn(Optional.of(consulta));

        assertThatThrownBy(() -> consultaService.realizar("medico@test.com", "c1"))
                .isInstanceOf(ConsultaNaoOcorreuAindaException.class);
    }

    // ===== Helpers =====
    private Medico medico(String id, String email) {
        Usuario u = Usuario.builder().id(id).nome("Dr Teste").email(email)
                .senha("hash").perfil(PerfilUsuario.MEDICO).ativo(true).build();
        Medico m = new Medico();
        m.setId(id); m.setCrm("CRM"); m.setEspecialidade("Clinica");
        m.setNomeCompleto("Dr. Teste"); m.setUsuario(u);
        return m;
    }

    private Consulta consultaComStatus(String id, Medico medico, StatusConsulta status, LocalDateTime dataHora) {
        Usuario paciente = Usuario.builder().id("p1").nome("Paciente").email("pac@test.com")
                .senha("hash").perfil(PerfilUsuario.PACIENTE).ativo(true).build();
        return Consulta.builder()
                .id(id).paciente(paciente).medico(medico)
                .dataHora(dataHora).status(status).tipoConsulta(TipoConsulta.NORMAL)
                .build();
    }
}
