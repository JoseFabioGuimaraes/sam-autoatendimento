package br.com.sam.disponibilidade.service;

import br.com.sam.consulta.repository.ConsultaRepository;
import br.com.sam.disponibilidade.dto.DisponibilidadeRequest;
import br.com.sam.disponibilidade.dto.DisponibilidadeResponse;
import br.com.sam.disponibilidade.dto.SlotResponse;
import br.com.sam.disponibilidade.model.DiaSemana;
import br.com.sam.disponibilidade.model.DisponibilidadeMedico;
import br.com.sam.disponibilidade.repository.DisponibilidadeRepository;
import br.com.sam.medico.model.Medico;
import br.com.sam.medico.repository.MedicoRepository;
import br.com.sam.shared.exception.AcessoNegadoException;
import br.com.sam.shared.exception.HorarioInvalidoException;
import br.com.sam.shared.exception.SobreposicaoDeHorariosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para DisponibilidadeService.
 * Cobre: T07–T15 das regras de negócio.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DisponibilidadeService — Testes Unitários")
class DisponibilidadeServiceTest {

    @Mock
    private DisponibilidadeRepository disponibilidadeRepository;

    @Mock
    private MedicoRepository medicoRepository;

    @Mock
    private ConsultaRepository consultaRepository;

    @InjectMocks
    private DisponibilidadeService disponibilidadeService;

    // ===== T07 =====
    @Test
    @DisplayName("T07 — Criar faixa válida (seg 08:00-12:00, 30min): faixa salva com ativo=true")
    void t07_criarFaixaValida_deveSalvarComAtivoTrue() {
        String medicoId = "medico-1";
        Medico medico = criarMedico(medicoId);
        DisponibilidadeRequest request = new DisponibilidadeRequest(
                DiaSemana.SEG, LocalTime.of(8, 0), LocalTime.of(12, 0), 30);

        when(medicoRepository.findById(medicoId)).thenReturn(Optional.of(medico));
        when(disponibilidadeRepository.findSobreposicoes(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(disponibilidadeRepository.save(any())).thenAnswer(inv -> {
            DisponibilidadeMedico d = inv.getArgument(0);
            d.setId("disp-1");
            return d;
        });

        DisponibilidadeResponse response = disponibilidadeService.criar(medicoId, request);

        assertThat(response.ativo()).isTrue();
        assertThat(response.diaSemana()).isEqualTo(DiaSemana.SEG);
        assertThat(response.horaInicio()).isEqualTo(LocalTime.of(8, 0));
        assertThat(response.horaFim()).isEqualTo(LocalTime.of(12, 0));
    }

    // ===== T08 =====
    @Test
    @DisplayName("T08 — Criar faixa com horaFim <= horaInicio: lança HorarioInvalidoException")
    void t08_horaFimMenorOuIgualHoraInicio_deveLancarHorarioInvalidoException() {
        String medicoId = "medico-1";
        DisponibilidadeRequest request = new DisponibilidadeRequest(
                DiaSemana.SEG, LocalTime.of(12, 0), LocalTime.of(8, 0), 30);

        assertThatThrownBy(() -> disponibilidadeService.criar(medicoId, request))
                .isInstanceOf(HorarioInvalidoException.class);

        verify(disponibilidadeRepository, never()).save(any());
    }

    // ===== T09 =====
    @Test
    @DisplayName("T09 — Criar faixa que sobrepõe faixa existente: lança SobreposicaoDeHorariosException")
    void t09_faixaSobrepostaExistente_deveLancarSobreposicaoDeHorariosException() {
        String medicoId = "medico-1";
        DisponibilidadeRequest request = new DisponibilidadeRequest(
                DiaSemana.SEG, LocalTime.of(10, 0), LocalTime.of(14, 0), 30);

        when(disponibilidadeRepository.findSobreposicoes(any(), any(), any(), any(), any()))
                .thenReturn(List.of(criarDisponibilidade(medicoId)));

        assertThatThrownBy(() -> disponibilidadeService.criar(medicoId, request))
                .isInstanceOf(SobreposicaoDeHorariosException.class);
    }

    // ===== T10 =====
    @Test
    @DisplayName("T10 — Desativar faixa existente: ativo=false, sem cancelar consultas")
    void t10_desativarFaixaExistente_deveSetarAtivoFalse() {
        String medicoId = "medico-1";
        String dispId = "disp-1";
        DisponibilidadeMedico disponibilidade = criarDisponibilidade(medicoId);
        disponibilidade.setId(dispId);

        when(disponibilidadeRepository.findById(dispId)).thenReturn(Optional.of(disponibilidade));

        disponibilidadeService.desativar(medicoId, dispId);

        assertThat(disponibilidade.getAtivo()).isFalse();
        verifyNoInteractions(consultaRepository);
    }

    // ===== T11 =====
    @Test
    @DisplayName("T11 — Outro médico tenta editar faixa alheia: lança AcessoNegadoException")
    void t11_outroMedicoEditaFaixaAlheia_deveLancarAcessoNegadoException() {
        String medicoId = "medico-1";
        String outroMedicoId = "medico-2";
        String dispId = "disp-1";
        DisponibilidadeMedico disponibilidade = criarDisponibilidade(medicoId);
        disponibilidade.setId(dispId);

        when(disponibilidadeRepository.findById(dispId)).thenReturn(Optional.of(disponibilidade));

        assertThatThrownBy(() -> disponibilidadeService.desativar(outroMedicoId, dispId))
                .isInstanceOf(AcessoNegadoException.class);
    }

    // ===== T12 =====
    @Test
    @DisplayName("T12 — Calcular slots livres para data sem consultas: retorna todos os slots")
    void t12_calcularSlotsLivresSemConsultas_deveRetornarTodosSlots() {
        String medicoId = "medico-1";
        // Segunda-feira de 2027
        LocalDate data = LocalDate.of(2027, 1, 4); // Segunda-feira

        DisponibilidadeMedico faixa = criarDisponibilidade(medicoId);
        faixa.setHoraInicio(LocalTime.of(8, 0));
        faixa.setHoraFim(LocalTime.of(10, 0));
        faixa.setDuracaoSlotMin(60);

        when(disponibilidadeRepository.findByMedicoIdAndDiaSemanaAndAtivoTrue(medicoId, DiaSemana.SEG))
                .thenReturn(List.of(faixa));
        when(consultaRepository.findConflitosDoMedico(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        List<SlotResponse> slots = disponibilidadeService.calcularSlots(medicoId, data);

        assertThat(slots).hasSize(2); // 08:00 e 09:00
    }

    // ===== T13 =====
    @Test
    @DisplayName("T13 — Calcular slots com consulta APROVADA ocupando horário: slot não aparece")
    void t13_slotComConsultaAprovada_naoDeveAparecer() {
        String medicoId = "medico-1";
        LocalDate data = LocalDate.of(2027, 1, 4); // Segunda-feira

        DisponibilidadeMedico faixa = criarDisponibilidade(medicoId);
        faixa.setHoraInicio(LocalTime.of(8, 0));
        faixa.setHoraFim(LocalTime.of(9, 0));
        faixa.setDuracaoSlotMin(60);

        when(disponibilidadeRepository.findByMedicoIdAndDiaSemanaAndAtivoTrue(medicoId, DiaSemana.SEG))
                .thenReturn(List.of(faixa));

        // Simula que o slot das 08:00 está ocupado
        br.com.sam.consulta.model.Consulta consultaAprovada = new br.com.sam.consulta.model.Consulta();
        consultaAprovada.setStatus(br.com.sam.consulta.model.StatusConsulta.APROVADA);
        when(consultaRepository.findConflitosDoMedico(any(), any(), any()))
                .thenReturn(List.of(consultaAprovada));

        List<SlotResponse> slots = disponibilidadeService.calcularSlots(medicoId, data);

        assertThat(slots).isEmpty();
    }

    // ===== T15 =====
    @Test
    @DisplayName("T15 — Data sem disponibilidade para aquele dia da semana: retorna lista vazia")
    void t15_semDisponibilidadeParaODia_deveRetornarListaVazia() {
        String medicoId = "medico-1";
        LocalDate domingo = LocalDate.of(2027, 1, 3); // Domingo

        when(disponibilidadeRepository.findByMedicoIdAndDiaSemanaAndAtivoTrue(medicoId, DiaSemana.DOM))
                .thenReturn(Collections.emptyList());

        List<SlotResponse> slots = disponibilidadeService.calcularSlots(medicoId, domingo);

        assertThat(slots).isEmpty();
    }

    // ===== Helpers =====
    private Medico criarMedico(String id) {
        br.com.sam.usuario.model.Usuario u = br.com.sam.usuario.model.Usuario.builder()
                .id(id).nome("Dr. Teste").email("dr@test.com")
                .senha("hash").perfil(br.com.sam.usuario.model.PerfilUsuario.MEDICO).ativo(true)
                .build();
        Medico m = new Medico();
        m.setId(id);
        m.setCrm("CRM-1");
        m.setEspecialidade("Clinica");
        m.setNomeCompleto("Dr. Teste");
        m.setUsuario(u);
        return m;
    }

    private DisponibilidadeMedico criarDisponibilidade(String medicoId) {
        Medico medico = criarMedico(medicoId);
        return DisponibilidadeMedico.builder()
                .id("disp-" + medicoId)
                .medico(medico)
                .diaSemana(DiaSemana.SEG)
                .horaInicio(LocalTime.of(8, 0))
                .horaFim(LocalTime.of(12, 0))
                .duracaoSlotMin(30)
                .ativo(true)
                .build();
    }
}
