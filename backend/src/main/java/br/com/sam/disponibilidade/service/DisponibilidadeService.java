package br.com.sam.disponibilidade.service;

import br.com.sam.consulta.model.StatusConsulta;
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
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviço de disponibilidade médica.
 * Regras: RN-DISP-01 a RN-DISP-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DisponibilidadeService {

    private final DisponibilidadeRepository disponibilidadeRepository;
    private final MedicoRepository medicoRepository;
    private final ConsultaRepository consultaRepository;

    @Transactional(readOnly = true)
    public List<DisponibilidadeResponse> listarPorMedico(String medicoId) {
        return disponibilidadeRepository.findByMedicoIdAndAtivoTrue(medicoId)
                .stream()
                .map(DisponibilidadeResponse::from)
                .toList();
    }

    /**
     * Cria nova faixa de disponibilidade.
     * Valida horaFim > horaInicio (RN-DISP-07) e sobreposição (RN-DISP-08).
     */
    @Transactional
    public DisponibilidadeResponse criar(String medicoId, DisponibilidadeRequest request) {
        validarHorario(request.horaInicio(), request.horaFim());
        verificarSobreposicao(medicoId, request.diaSemana(), request.horaInicio(), request.horaFim(), null);

        Medico medico = medicoRepository.findById(medicoId)
                .orElseThrow(() -> new EntityNotFoundException("Médico não encontrado"));

        int duracao = request.duracaoSlotMin() != null ? request.duracaoSlotMin() : 30;

        DisponibilidadeMedico disponibilidade = DisponibilidadeMedico.builder()
                .medico(medico)
                .diaSemana(request.diaSemana())
                .horaInicio(request.horaInicio())
                .horaFim(request.horaFim())
                .duracaoSlotMin(duracao)
                .ativo(true)
                .build();

        DisponibilidadeMedico salvo = disponibilidadeRepository.save(disponibilidade);
        log.info("Disponibilidade criada para médico {} — {}", medicoId, request.diaSemana());
        return DisponibilidadeResponse.from(salvo);
    }

    /**
     * Atualiza faixa existente.
     * Verifica que o médico autenticado é o dono (RN-DISP-03).
     */
    @Transactional
    public DisponibilidadeResponse atualizar(String medicoId, String disponibilidadeId, DisponibilidadeRequest request) {
        DisponibilidadeMedico disponibilidade = buscarComValidacaoAcesso(disponibilidadeId, medicoId);

        validarHorario(request.horaInicio(), request.horaFim());
        verificarSobreposicao(medicoId, request.diaSemana(), request.horaInicio(), request.horaFim(), disponibilidadeId);

        disponibilidade.setDiaSemana(request.diaSemana());
        disponibilidade.setHoraInicio(request.horaInicio());
        disponibilidade.setHoraFim(request.horaFim());
        if (request.duracaoSlotMin() != null) {
            disponibilidade.setDuracaoSlotMin(request.duracaoSlotMin());
        }

        log.info("Disponibilidade {} atualizada pelo médico {}", disponibilidadeId, medicoId);
        return DisponibilidadeResponse.from(disponibilidade);
    }

    /**
     * Desativa faixa (soft delete).
     * Consultas já aprovadas NÃO são canceladas (RN-DISP-04).
     */
    @Transactional
    public void desativar(String medicoId, String disponibilidadeId) {
        DisponibilidadeMedico disponibilidade = buscarComValidacaoAcesso(disponibilidadeId, medicoId);
        disponibilidade.setAtivo(false);
        log.info("Disponibilidade {} desativada pelo médico {}", disponibilidadeId, medicoId);
    }

    /**
     * Calcula slots disponíveis para um médico em uma data específica.
     * Regras: RN-DISP-02, RN-DISP-05, RN-DISP-06
     */
    @Transactional(readOnly = true)
    public List<SlotResponse> calcularSlots(String medicoId, LocalDate data) {
        DiaSemana diaSemana = converterDayOfWeek(data.getDayOfWeek());

        List<DisponibilidadeMedico> faixas =
                disponibilidadeRepository.findByMedicoIdAndDiaSemanaAndAtivoTrue(medicoId, diaSemana);

        List<SlotResponse> slots = new ArrayList<>();

        for (DisponibilidadeMedico faixa : faixas) {
            LocalTime cursor = faixa.getHoraInicio();
            while (cursor.plusMinutes(faixa.getDuracaoSlotMin()).compareTo(faixa.getHoraFim()) <= 0) {
                LocalDateTime dataHora = LocalDateTime.of(data, cursor);

                // Verifica se o slot está livre (RN-DISP-05)
                List<StatusConsulta> blockedStatuses = List.of(
                        StatusConsulta.APROVADA,
                        StatusConsulta.AGUARDANDO_APROVACAO
                );

                boolean ocupado = consultaRepository
                        .findConflitosDoMedico(medicoId, dataHora, null)
                        .stream()
                        .anyMatch(c -> blockedStatuses.contains(c.getStatus()));

                if (!ocupado && dataHora.isAfter(LocalDateTime.now())) {
                    slots.add(new SlotResponse(dataHora));
                }

                cursor = cursor.plusMinutes(faixa.getDuracaoSlotMin());
            }
        }

        return slots;
    }

    // ===== Helpers =====

    private DisponibilidadeMedico buscarComValidacaoAcesso(String disponibilidadeId, String medicoId) {
        DisponibilidadeMedico d = disponibilidadeRepository.findById(disponibilidadeId)
                .orElseThrow(() -> new EntityNotFoundException("Disponibilidade não encontrada: " + disponibilidadeId));

        if (!d.getMedico().getId().equals(medicoId)) {
            throw new AcessoNegadoException("Você não tem permissão para modificar esta disponibilidade.");
        }
        return d;
    }

    private void validarHorario(LocalTime inicio, LocalTime fim) {
        if (!fim.isAfter(inicio)) {
            throw new HorarioInvalidoException("A hora de fim deve ser posterior à hora de início.");
        }
    }

    private void verificarSobreposicao(String medicoId, DiaSemana dia,
                                        LocalTime inicio, LocalTime fim, String excludeId) {
        List<DisponibilidadeMedico> sobreposicoes =
                disponibilidadeRepository.findSobreposicoes(medicoId, dia, inicio, fim, excludeId);
        if (!sobreposicoes.isEmpty()) {
            throw new SobreposicaoDeHorariosException();
        }
    }

    private DiaSemana converterDayOfWeek(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> DiaSemana.SEG;
            case TUESDAY -> DiaSemana.TER;
            case WEDNESDAY -> DiaSemana.QUA;
            case THURSDAY -> DiaSemana.QUI;
            case FRIDAY -> DiaSemana.SEX;
            case SATURDAY -> DiaSemana.SAB;
            case SUNDAY -> DiaSemana.DOM;
        };
    }
}
