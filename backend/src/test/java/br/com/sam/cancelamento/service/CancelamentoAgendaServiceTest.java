package br.com.sam.cancelamento.service;

import br.com.sam.cancelamento.dto.CancelamentoAgendaRequest;
import br.com.sam.cancelamento.dto.CancelamentoAgendaResponse;
import br.com.sam.cancelamento.model.CancelamentoAgenda;
import br.com.sam.cancelamento.model.TipoCancelamento;
import br.com.sam.cancelamento.repository.CancelamentoAgendaRepository;
import br.com.sam.consulta.model.Consulta;
import br.com.sam.consulta.model.StatusConsulta;
import br.com.sam.consulta.repository.ConsultaRepository;
import br.com.sam.medico.model.Medico;
import br.com.sam.medico.repository.MedicoRepository;
import br.com.sam.shared.exception.*;
import br.com.sam.usuario.model.Usuario;
import br.com.sam.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CancelamentoAgendaService — Testes Unitários")
class CancelamentoAgendaServiceTest {

    @Mock
    private CancelamentoAgendaRepository cancelamentoRepository;

    @Mock
    private ConsultaRepository consultaRepository;

    @Mock
    private MedicoRepository medicoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private CancelamentoAgendaService cancelamentoAgendaService;

    private Usuario criarMedicoUsuario() {
        return Usuario.builder().id("medico-1").email("medico@test.com").build();
    }

    private Medico criarMedico(Usuario u) {
        Medico m = new Medico();
        m.setId(u.getId());
        m.setUsuario(u);
        return m;
    }

    // T56
    @Test
    @DisplayName("T56 — Médico cancela dia completo: consultas APROVADA mudam para CANCELADA_PELO_MEDICO")
    void t56_cancelamentoDiaCompleto() {
        Usuario u = criarMedicoUsuario();
        Medico m = criarMedico(u);

        CancelamentoAgendaRequest request = new CancelamentoAgendaRequest(
                TipoCancelamento.DIA_COMPLETO, LocalDate.now().plusDays(2), null, null, "Motivo longo o suficiente");

        Consulta c1 = new Consulta();
        c1.setStatus(StatusConsulta.APROVADA);
        c1.setDataHora(LocalDateTime.of(LocalDate.now().plusDays(2), LocalTime.of(10, 0)));

        when(usuarioRepository.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
        when(medicoRepository.findById(u.getId())).thenReturn(Optional.of(m));
        when(cancelamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(consultaRepository.findConsultasAtivasDoMedicoNaData(u.getId(), request.data())).thenReturn(List.of(c1));

        CancelamentoAgendaResponse res = cancelamentoAgendaService.registrarCancelamento(u.getEmail(), request);

        assertThat(c1.getStatus()).isEqualTo(StatusConsulta.CANCELADA_PELO_MEDICO);
        assertThat(res.consultasAfetadas()).isEqualTo(1);
    }

    // T57
    @Test
    @DisplayName("T57 — Médico cancela turno: apenas consultas no intervalo são afetadas")
    void t57_cancelamentoTurno() {
        Usuario u = criarMedicoUsuario();
        Medico m = criarMedico(u);

        CancelamentoAgendaRequest request = new CancelamentoAgendaRequest(
                TipoCancelamento.TURNO, LocalDate.now().plusDays(2), LocalTime.of(8, 0), LocalTime.of(12, 0), "Motivo longo o suficiente");

        Consulta cFora = new Consulta();
        cFora.setStatus(StatusConsulta.APROVADA);
        cFora.setDataHora(LocalDateTime.of(request.data(), LocalTime.of(14, 0))); // Fora do turno

        Consulta cDentro = new Consulta();
        cDentro.setStatus(StatusConsulta.AGUARDANDO_APROVACAO);
        cDentro.setDataHora(LocalDateTime.of(request.data(), LocalTime.of(10, 0))); // Dentro

        when(usuarioRepository.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
        when(medicoRepository.findById(u.getId())).thenReturn(Optional.of(m));
        when(cancelamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(consultaRepository.findConsultasAtivasDoMedicoNaData(u.getId(), request.data())).thenReturn(List.of(cFora, cDentro));

        CancelamentoAgendaResponse res = cancelamentoAgendaService.registrarCancelamento(u.getEmail(), request);

        assertThat(cDentro.getStatus()).isEqualTo(StatusConsulta.CANCELADA_PELO_MEDICO);
        assertThat(cFora.getStatus()).isEqualTo(StatusConsulta.APROVADA); // Não afetada
        assertThat(res.consultasAfetadas()).isEqualTo(1);
    }

    // T58
    @Test
    @DisplayName("T58 — Médico cancela slot específico ocupado")
    void t58_cancelamentoSlotEspecifico() {
        Usuario u = criarMedicoUsuario();
        Medico m = criarMedico(u);

        CancelamentoAgendaRequest request = new CancelamentoAgendaRequest(
                TipoCancelamento.HORARIO_ESPECIFICO, LocalDate.now().plusDays(2), LocalTime.of(9, 30), null, "Motivo longo o suficiente");

        Consulta cDentro = new Consulta();
        cDentro.setStatus(StatusConsulta.APROVADA);
        cDentro.setDataHora(LocalDateTime.of(request.data(), LocalTime.of(9, 30))); 

        when(usuarioRepository.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
        when(medicoRepository.findById(u.getId())).thenReturn(Optional.of(m));
        when(cancelamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(consultaRepository.findConsultasAtivasDoMedicoNaData(u.getId(), request.data())).thenReturn(List.of(cDentro));

        CancelamentoAgendaResponse res = cancelamentoAgendaService.registrarCancelamento(u.getEmail(), request);

        assertThat(cDentro.getStatus()).isEqualTo(StatusConsulta.CANCELADA_PELO_MEDICO);
        assertThat(res.consultasAfetadas()).isEqualTo(1);
    }

    // T59
    @Test
    @DisplayName("T59 — Cancelamento para data passada ou dia atual: DataCancelamentoInvalidaException")
    void t59_cancelamentoDataInvalida() {
        Usuario u = criarMedicoUsuario();
        Medico m = criarMedico(u);

        CancelamentoAgendaRequest request = new CancelamentoAgendaRequest(
                TipoCancelamento.DIA_COMPLETO, LocalDate.now(), null, null, "Motivo longo o suficiente");

        when(usuarioRepository.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
        when(medicoRepository.findById(u.getId())).thenReturn(Optional.of(m));

        assertThatThrownBy(() -> cancelamentoAgendaService.registrarCancelamento(u.getEmail(), request))
                .isInstanceOf(DataCancelamentoInvalidaException.class);
    }

    // T60
    @Test
    @DisplayName("T60 — Cancelamento sem motivo ou motivo < 10 chars: JustificativaObrigatoriaException")
    void t60_cancelamentoMotivoInvalido() {
        Usuario u = criarMedicoUsuario();
        Medico m = criarMedico(u);

        CancelamentoAgendaRequest request = new CancelamentoAgendaRequest(
                TipoCancelamento.DIA_COMPLETO, LocalDate.now().plusDays(1), null, null, "Curto");

        when(usuarioRepository.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
        when(medicoRepository.findById(u.getId())).thenReturn(Optional.of(m));

        assertThatThrownBy(() -> cancelamentoAgendaService.registrarCancelamento(u.getEmail(), request))
                .isInstanceOf(JustificativaObrigatoriaException.class);
    }

    // T61
    @Test
    @DisplayName("T61 — Cancelamento de turno com horaFim <= horaInicio: HorarioInvalidoException")
    void t61_cancelamentoTurnoHorarioInvalido() {
        Usuario u = criarMedicoUsuario();
        Medico m = criarMedico(u);

        CancelamentoAgendaRequest request = new CancelamentoAgendaRequest(
                TipoCancelamento.TURNO, LocalDate.now().plusDays(1), LocalTime.of(12, 0), LocalTime.of(10, 0), "Motivo longo o suficiente");

        when(usuarioRepository.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
        when(medicoRepository.findById(u.getId())).thenReturn(Optional.of(m));

        assertThatThrownBy(() -> cancelamentoAgendaService.registrarCancelamento(u.getEmail(), request))
                .isInstanceOf(HorarioInvalidoException.class);
    }

    // T62
    @Test
    @DisplayName("T62 — Cancelamento sobreposto a cancelamento já existente: SobreposicaoDeCancelamentoException")
    void t62_sobreposicaoCancelamento() {
        Usuario u = criarMedicoUsuario();
        Medico m = criarMedico(u);

        CancelamentoAgendaRequest request = new CancelamentoAgendaRequest(
                TipoCancelamento.DIA_COMPLETO, LocalDate.now().plusDays(1), null, null, "Motivo longo o suficiente");

        CancelamentoAgenda existente = CancelamentoAgenda.builder().tipoCancelamento(TipoCancelamento.DIA_COMPLETO).build();

        when(usuarioRepository.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
        when(medicoRepository.findById(u.getId())).thenReturn(Optional.of(m));
        when(cancelamentoRepository.findByMedicoIdAndData(u.getId(), request.data())).thenReturn(List.of(existente));

        assertThatThrownBy(() -> cancelamentoAgendaService.registrarCancelamento(u.getEmail(), request))
                .isInstanceOf(SobreposicaoDeCancelamentoException.class);
    }

    // T63
    @Test
    @DisplayName("T63 — Outro médico tenta registrar cancelamento na agenda alheia: Mapeado para reverterCancelamento onde é validado")
    void t63_reverterOutroMedico() {
        Usuario u = criarMedicoUsuario();
        CancelamentoAgenda cancelamento = CancelamentoAgenda.builder().data(LocalDate.now().plusDays(1)).medico(criarMedico(Usuario.builder().id("outro-medico").build())).build();

        when(usuarioRepository.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
        when(cancelamentoRepository.findById("1")).thenReturn(Optional.of(cancelamento));

        assertThatThrownBy(() -> cancelamentoAgendaService.reverterCancelamento(u.getEmail(), "1"))
                .isInstanceOf(AcessoNegadoException.class);
    }

    // T64
    @Test
    @DisplayName("T64 — Cancelamento revertido via DELETE; consultas voltam ao status anterior")
    void t64_reverterCancelamento() {
        Usuario u = criarMedicoUsuario();
        Medico m = criarMedico(u);

        CancelamentoAgenda cancelamento = CancelamentoAgenda.builder().data(LocalDate.now().plusDays(1)).medico(m).tipoCancelamento(TipoCancelamento.DIA_COMPLETO).build();
        
        Consulta c = new Consulta();
        c.setStatus(StatusConsulta.CANCELADA_PELO_MEDICO);
        c.setStatusAnterior(StatusConsulta.APROVADA);
        c.setDataHora(LocalDateTime.of(cancelamento.getData(), LocalTime.of(10, 0)));

        when(usuarioRepository.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
        when(cancelamentoRepository.findById("1")).thenReturn(Optional.of(cancelamento));
        when(consultaRepository.findConsultasCanceladasPeloMedicoNaData(u.getId(), cancelamento.getData())).thenReturn(List.of(c));

        cancelamentoAgendaService.reverterCancelamento(u.getEmail(), "1");

        assertThat(c.getStatus()).isEqualTo(StatusConsulta.APROVADA);
        assertThat(c.getStatusAnterior()).isNull();
        verify(cancelamentoRepository).delete(cancelamento);
    }

    // T65
    @Test
    @DisplayName("T65 — Cancelamento em dia sem consultas agendadas: salvo sem afetar nenhuma")
    void t65_cancelamentoSemConsultas() {
        Usuario u = criarMedicoUsuario();
        Medico m = criarMedico(u);

        CancelamentoAgendaRequest request = new CancelamentoAgendaRequest(
                TipoCancelamento.DIA_COMPLETO, LocalDate.now().plusDays(1), null, null, "Motivo longo o suficiente");

        when(usuarioRepository.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
        when(medicoRepository.findById(u.getId())).thenReturn(Optional.of(m));
        when(cancelamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(consultaRepository.findConsultasAtivasDoMedicoNaData(u.getId(), request.data())).thenReturn(Collections.emptyList());

        CancelamentoAgendaResponse res = cancelamentoAgendaService.registrarCancelamento(u.getEmail(), request);

        assertThat(res.consultasAfetadas()).isEqualTo(0);
    }
}
