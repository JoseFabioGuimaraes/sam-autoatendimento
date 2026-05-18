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
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço de Cancelamento de Agenda pelo Médico.
 * Regras: RN-CAGD-01 a RN-CAGD-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CancelamentoAgendaService {

    private final CancelamentoAgendaRepository cancelamentoRepository;
    private final ConsultaRepository consultaRepository;
    private final MedicoRepository medicoRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Registra cancelamento de agenda (RN-CAGD-01 a RN-CAGD-08, RN-CAGD-11).
     */
    @Transactional
    public CancelamentoAgendaResponse registrarCancelamento(String medicoEmail, CancelamentoAgendaRequest request) {
        Usuario usuario = buscarUsuario(medicoEmail);
        Medico medico = medicoRepository.findById(usuario.getId())
                .orElseThrow(() -> new EntityNotFoundException("Médico não encontrado."));

        // RN-CAGD-02: data deve ser estritamente futura
        if (!request.data().isAfter(LocalDate.now())) {
            throw new DataCancelamentoInvalidaException(
                    "A data do cancelamento deve ser futura. Não é permitido cancelar o dia atual nem datas passadas.");
        }

        // RN-CAGD-03: motivo mínimo 10 caracteres (validado pelo DTO, mas double-check)
        if (request.motivoCancelamento() == null || request.motivoCancelamento().trim().length() < 10) {
            throw new JustificativaObrigatoriaException(
                    "O motivo do cancelamento é obrigatório e deve ter no mínimo 10 caracteres.");
        }

        // RN-CAGD-04: TURNO exige horaInicio e horaFim, horaFim > horaInicio
        if (request.tipoCancelamento() == TipoCancelamento.TURNO) {
            if (request.horaInicio() == null || request.horaFim() == null) {
                throw new HorarioInvalidoException("Para cancelamento de turno, horaInicio e horaFim são obrigatórios.");
            }
            if (!request.horaFim().isAfter(request.horaInicio())) {
                throw new HorarioInvalidoException("horaFim deve ser maior que horaInicio.");
            }
        }

        // RN-CAGD-05: HORARIO_ESPECIFICO exige horaInicio
        if (request.tipoCancelamento() == TipoCancelamento.HORARIO_ESPECIFICO) {
            if (request.horaInicio() == null) {
                throw new HorarioInvalidoException("Para cancelamento de horário específico, horaInicio é obrigatório.");
            }
        }

        // RN-CAGD-11: verificar sobreposição
        verificarSobreposicao(medico.getId(), request);

        // Criar o cancelamento
        CancelamentoAgenda cancelamento = CancelamentoAgenda.builder()
                .medico(medico)
                .tipoCancelamento(request.tipoCancelamento())
                .data(request.data())
                .horaInicio(request.horaInicio())
                .horaFim(request.horaFim())
                .motivoCancelamento(request.motivoCancelamento().trim())
                .build();

        CancelamentoAgenda salvo = cancelamentoRepository.save(cancelamento);

        // RN-CAGD-06 + RN-CAGD-07: identificar e cancelar consultas afetadas
        List<Consulta> afetadas = identificarConsultasAfetadas(medico.getId(), request);
        for (Consulta c : afetadas) {
            c.setStatusAnterior(c.getStatus());
            c.setStatus(StatusConsulta.CANCELADA_PELO_MEDICO);
            c.setJustificativaRecusa("Agenda cancelada pelo médico: " + request.motivoCancelamento().trim());
        }

        log.info("Cancelamento de agenda {} registrado pelo médico {}. {} consultas afetadas.",
                salvo.getId(), medicoEmail, afetadas.size());

        return CancelamentoAgendaResponse.from(salvo, afetadas.size());
    }

    /**
     * Lista cancelamentos futuros do médico.
     */
    @Transactional(readOnly = true)
    public List<CancelamentoAgendaResponse> listarCancelamentos(String medicoEmail) {
        Usuario usuario = buscarUsuario(medicoEmail);
        List<CancelamentoAgenda> cancelamentos = cancelamentoRepository
                .findByMedicoIdAndDataGreaterThanEqualOrderByDataAsc(usuario.getId(), LocalDate.now());

        return cancelamentos.stream()
                .map(c -> CancelamentoAgendaResponse.from(c, 0))
                .collect(Collectors.toList());
    }

    /**
     * Reverte cancelamento (RN-CAGD-11 complemento — T64).
     */
    @Transactional
    public void reverterCancelamento(String medicoEmail, String cancelamentoId) {
        Usuario usuario = buscarUsuario(medicoEmail);
        CancelamentoAgenda cancelamento = cancelamentoRepository.findById(cancelamentoId)
                .orElseThrow(() -> new EntityNotFoundException("Cancelamento não encontrado: " + cancelamentoId));

        // RN-CAGD-01
        if (!cancelamento.getMedico().getId().equals(usuario.getId())) {
            throw new AcessoNegadoException("Você só pode reverter seus próprios cancelamentos.");
        }

        // Restaurar consultas canceladas pelo médico nesta data/faixa
        List<Consulta> todasNaData = consultaRepository.findConsultasCanceladasPeloMedicoNaData(usuario.getId(), cancelamento.getData());
        
        for (Consulta c : todasNaData) {
            LocalTime horaConsulta = c.getDataHora().toLocalTime();
            boolean affected = switch (cancelamento.getTipoCancelamento()) {
                case DIA_COMPLETO -> true;
                case TURNO -> !horaConsulta.isBefore(cancelamento.getHoraInicio()) && horaConsulta.isBefore(cancelamento.getHoraFim());
                case HORARIO_ESPECIFICO -> horaConsulta.equals(cancelamento.getHoraInicio());
            };

            if (affected) {
                c.setStatus(c.getStatusAnterior() != null ? c.getStatusAnterior() : StatusConsulta.AGUARDANDO_APROVACAO);
                c.setStatusAnterior(null);
                c.setJustificativaRecusa(null);
            }
        }

        cancelamentoRepository.delete(cancelamento);

        log.info("Cancelamento {} revertido pelo médico {}", cancelamentoId, medicoEmail);
    }

    // === Helpers ===

    private void verificarSobreposicao(String medicoId, CancelamentoAgendaRequest request) {
        List<CancelamentoAgenda> existentes = cancelamentoRepository.findByMedicoIdAndData(medicoId, request.data());

        for (CancelamentoAgenda existente : existentes) {
            if (existente.getTipoCancelamento() == TipoCancelamento.DIA_COMPLETO
                    || request.tipoCancelamento() == TipoCancelamento.DIA_COMPLETO) {
                throw new SobreposicaoDeCancelamentoException(
                        "Já existe um cancelamento para esta data que conflita com a solicitação.");
            }

            if (request.tipoCancelamento() == TipoCancelamento.TURNO
                    && existente.getTipoCancelamento() == TipoCancelamento.TURNO) {
                // Verifica sobreposição de intervalos
                if (request.horaInicio().isBefore(existente.getHoraFim())
                        && request.horaFim().isAfter(existente.getHoraInicio())) {
                    throw new SobreposicaoDeCancelamentoException(
                            "O turno solicitado sobrepõe um cancelamento existente de "
                                    + existente.getHoraInicio() + " a " + existente.getHoraFim() + ".");
                }
            }

            if (request.tipoCancelamento() == TipoCancelamento.HORARIO_ESPECIFICO) {
                if (existente.getTipoCancelamento() == TipoCancelamento.TURNO
                        && !request.horaInicio().isBefore(existente.getHoraInicio())
                        && request.horaInicio().isBefore(existente.getHoraFim())) {
                    throw new SobreposicaoDeCancelamentoException(
                            "O horário solicitado já está coberto por um cancelamento de turno existente.");
                }
                if (existente.getTipoCancelamento() == TipoCancelamento.HORARIO_ESPECIFICO
                        && request.horaInicio().equals(existente.getHoraInicio())) {
                    throw new SobreposicaoDeCancelamentoException(
                            "Já existe um cancelamento para este horário específico.");
                }
            }
        }
    }

    private List<Consulta> identificarConsultasAfetadas(String medicoId, CancelamentoAgendaRequest request) {
        List<Consulta> consultasNaData = consultaRepository.findConsultasAtivasDoMedicoNaData(medicoId, request.data());

        return consultasNaData.stream()
                .filter(c -> {
                    // RN-CAGD-09: apenas AGUARDANDO_APROVACAO e APROVADA (já filtrado pela query)
                    LocalTime horaConsulta = c.getDataHora().toLocalTime();

                    return switch (request.tipoCancelamento()) {
                        case DIA_COMPLETO -> true; // RN-CAGD-06 item 1
                        case TURNO -> // RN-CAGD-06 item 2: [horaInicio, horaFim)
                                !horaConsulta.isBefore(request.horaInicio()) && horaConsulta.isBefore(request.horaFim());
                        case HORARIO_ESPECIFICO -> // RN-CAGD-06 item 3: horário exato
                                horaConsulta.equals(request.horaInicio());
                    };
                })
                .collect(Collectors.toList());
    }

    private Usuario buscarUsuario(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado: " + email));
    }
}
