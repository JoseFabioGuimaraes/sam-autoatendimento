package br.com.sam.consulta.service;

import br.com.sam.consulta.dto.ConsultaRequest;
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
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Serviço central de consultas.
 * Regras: RN-CONS-01 a RN-CONS-08, RN-APROV-01 a RN-APROV-06, RN-RET-01 a RN-RET-06, RN-HIST-01 a RN-HIST-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultaService {

    private final ConsultaRepository consultaRepository;
    private final UsuarioRepository usuarioRepository;
    private final MedicoRepository medicoRepository;
    private final DisponibilidadeService disponibilidadeService;

    // ===== PACIENTE =====

    /**
     * Solicita nova consulta (RN-CONS-01 a RN-CONS-06).
     */
    @Transactional
    public ConsultaResponse solicitarConsulta(String pacienteEmail, ConsultaRequest request) {
        Usuario paciente = buscarUsuarioPorEmail(pacienteEmail);

        // RN-CONS-01
        if (paciente.getPerfil() != PerfilUsuario.PACIENTE) {
            throw new PerfilNaoAutorizadoException("Somente pacientes podem solicitar consultas.");
        }

        // RN-CONS-03
        if (!request.dataHora().isAfter(LocalDateTime.now())) {
            throw new HorarioNoPassadoException();
        }

        Medico medico = medicoRepository.findById(request.medicoId())
                .orElseThrow(() -> new EntityNotFoundException("Médico não encontrado: " + request.medicoId()));

        // RN-CONS-02 — revalida slot no backend
        List<?> slotsLivres = disponibilidadeService.calcularSlots(medico.getId(), request.dataHora().toLocalDate());
        boolean slotValido = slotsLivres.stream()
                .anyMatch(s -> s instanceof br.com.sam.disponibilidade.dto.SlotResponse slotResponse
                        && slotResponse.dataHora().equals(request.dataHora()));
        if (!slotValido) {
            throw new SlotIndisponivelException();
        }

        // RN-CONS-04 — conflito do paciente
        if (!consultaRepository.findConflitsDoPaciente(paciente.getId(), request.dataHora()).isEmpty()) {
            throw new ConflitoDeAgendaException("Você já possui uma consulta neste horário.");
        }

        // RN-CONS-05 — conflito do médico
        if (!consultaRepository.findConflitosDoMedico(medico.getId(), request.dataHora(), null).isEmpty()) {
            throw new ConflitoDeAgendaException("O médico já possui uma consulta neste horário.");
        }

        Consulta consulta = Consulta.builder()
                .paciente(paciente)
                .medico(medico)
                .dataHora(request.dataHora())
                .status(StatusConsulta.AGUARDANDO_APROVACAO)
                .tipoConsulta(TipoConsulta.NORMAL)
                .build();

        Consulta salva = consultaRepository.save(consulta);
        log.info("Consulta {} criada pelo paciente {} com médico {}", salva.getId(), paciente.getEmail(), medico.getNomeCompleto());
        return ConsultaResponse.from(salva);
    }

    /**
     * Lista todas as consultas do paciente com filtros e paginação (RN-CONS-07, RN-HIST-01, RN-HIST-03).
     */
    @Transactional(readOnly = true)
    public Page<ConsultaResponse> listarConsultasPaciente(
            String pacienteEmail, StatusConsulta status,
            LocalDateTime inicio, LocalDateTime fim, Pageable pageable
    ) {
        String pacienteId = buscarUsuarioPorEmail(pacienteEmail).getId();
        return consultaRepository.findHistoricoPaciente(pacienteId, status, inicio, fim, pageable)
                .map(ConsultaResponse::from);
    }

    /**
     * Cancela consulta pelo paciente (RN-CONS-08).
     */
    @Transactional
    public ConsultaResponse cancelarConsulta(String pacienteEmail, String consultaId) {
        Consulta consulta = buscarConsulta(consultaId);
        String pacienteId = buscarUsuarioPorEmail(pacienteEmail).getId();

        // RN-CONS-08
        if (!consulta.getPaciente().getId().equals(pacienteId)) {
            throw new AcessoNegadoException("Você não pode cancelar a consulta de outro paciente.");
        }

        if (consulta.getStatus() != StatusConsulta.AGUARDANDO_APROVACAO
                && consulta.getStatus() != StatusConsulta.APROVADA) {
            throw new TransicaoInvalidaException(
                    "Não é possível cancelar consulta com status: " + consulta.getStatus());
        }

        consulta.setStatus(StatusConsulta.CANCELADA_PELO_PACIENTE);
        log.info("Consulta {} cancelada pelo paciente {}", consultaId, pacienteEmail);
        return ConsultaResponse.from(consulta);
    }

    /**
     * Solicita retorno de consulta realizada (RN-RET-01 a RN-RET-06).
     */
    @Transactional
    public ConsultaResponse solicitarRetorno(String pacienteEmail, String consultaOrigemId, ConsultaRequest request) {
        Consulta origem = buscarConsulta(consultaOrigemId);
        String pacienteId = buscarUsuarioPorEmail(pacienteEmail).getId();

        // RN-RET-02
        if (!origem.getPaciente().getId().equals(pacienteId)) {
            throw new AcessoNegadoException("Você não pode solicitar retorno de consulta de outro paciente.");
        }

        // RN-RET-03
        if (origem.getStatus() != StatusConsulta.REALIZADA) {
            throw new ConsultaNaoRealizadaException();
        }

        // RN-RET-04
        if (consultaRepository.findByConsultaOrigemId(consultaOrigemId).isPresent()) {
            throw new RetornoDuplicadoException();
        }

        // RN-CONS-03
        if (!request.dataHora().isAfter(LocalDateTime.now())) {
            throw new HorarioNoPassadoException();
        }

        Medico medico = medicoRepository.findById(request.medicoId())
                .orElseThrow(() -> new EntityNotFoundException("Médico não encontrado"));

        // RN-CONS-04
        if (!consultaRepository.findConflitsDoPaciente(pacienteId, request.dataHora()).isEmpty()) {
            throw new ConflitoDeAgendaException("Você já possui uma consulta neste horário.");
        }

        // RN-CONS-05
        if (!consultaRepository.findConflitosDoMedico(medico.getId(), request.dataHora(), null).isEmpty()) {
            throw new ConflitoDeAgendaException("O médico já possui uma consulta neste horário.");
        }

        Consulta retorno = Consulta.builder()
                .paciente(origem.getPaciente())
                .medico(medico)
                .dataHora(request.dataHora())
                .status(StatusConsulta.AGUARDANDO_APROVACAO)
                .tipoConsulta(TipoConsulta.RETORNO)
                .consultaOrigem(origem)
                .build();

        Consulta salvo = consultaRepository.save(retorno);
        log.info("Retorno {} criado para consulta origem {}", salvo.getId(), consultaOrigemId);
        return ConsultaResponse.from(salvo);
    }

    // ===== MÉDICO =====

    /**
     * Lista consultas do médico com filtros e paginação (RN-APROV-05, RN-HIST-02, RN-HIST-03).
     */
    @Transactional(readOnly = true)
    public Page<ConsultaResponse> listarConsultasMedico(
            String medicoEmail, StatusConsulta status,
            LocalDateTime inicio, LocalDateTime fim, Pageable pageable
    ) {
        String medicoId = buscarUsuarioPorEmail(medicoEmail).getId();
        return consultaRepository.findHistoricoMedico(medicoId, status, inicio, fim, pageable)
                .map(ConsultaResponse::from);
    }

    /**
     * Aprova consulta (RN-APROV-01, RN-APROV-02, RN-APROV-04, RN-APROV-06).
     */
    @Transactional
    public ConsultaResponse aprovar(String medicoEmail, String consultaId) {
        Consulta consulta = buscarConsulta(consultaId);
        String medicoId = buscarUsuarioPorEmail(medicoEmail).getId();

        // RN-APROV-01
        if (!consulta.getMedico().getId().equals(medicoId)) {
            throw new AcessoNegadoException("Você não pode aprovar consultas de outro médico.");
        }

        // RN-APROV-04
        if (consulta.getStatus() != StatusConsulta.AGUARDANDO_APROVACAO) {
            throw new TransicaoInvalidaException(
                    "Apenas consultas com status AGUARDANDO_APROVACAO podem ser aprovadas. Status atual: " + consulta.getStatus());
        }

        consulta.setStatus(StatusConsulta.APROVADA);

        // RN-APROV-06 — cancela consultas concorrentes no mesmo horário
        List<Consulta> concorrentes = consultaRepository
                .findConflitosDoMedico(medicoId, consulta.getDataHora(), consultaId);
        for (Consulta concorrente : concorrentes) {
            if (concorrente.getStatus() == StatusConsulta.AGUARDANDO_APROVACAO) {
                concorrente.setStatus(StatusConsulta.RECUSADA);
                concorrente.setJustificativaRecusa("Horário ocupado por outro agendamento confirmado.");
                log.info("Consulta concorrente {} recusada automaticamente.", concorrente.getId());
            }
        }

        log.info("Consulta {} aprovada pelo médico {}", consultaId, medicoEmail);
        return ConsultaResponse.from(consulta);
    }

    /**
     * Recusa consulta com justificativa obrigatória (RN-APROV-03, RN-APROV-04).
     */
    @Transactional
    public ConsultaResponse recusar(String medicoEmail, String consultaId, RecusaRequest request) {
        Consulta consulta = buscarConsulta(consultaId);
        String medicoId = buscarUsuarioPorEmail(medicoEmail).getId();

        // RN-APROV-01
        if (!consulta.getMedico().getId().equals(medicoId)) {
            throw new AcessoNegadoException("Você não pode recusar consultas de outro médico.");
        }

        // RN-APROV-04
        if (consulta.getStatus() != StatusConsulta.AGUARDANDO_APROVACAO) {
            throw new TransicaoInvalidaException(
                    "Apenas consultas com status AGUARDANDO_APROVACAO podem ser recusadas. Status atual: " + consulta.getStatus());
        }

        // RN-APROV-03
        if (request.justificativa() == null || request.justificativa().isBlank()) {
            throw new JustificativaObrigatoriaException();
        }

        consulta.setStatus(StatusConsulta.RECUSADA);
        consulta.setJustificativaRecusa(request.justificativa());

        log.info("Consulta {} recusada pelo médico {}", consultaId, medicoEmail);
        return ConsultaResponse.from(consulta);
    }

    /**
     * Marca consulta como realizada (RN-RET-06).
     */
    @Transactional
    public ConsultaResponse realizar(String medicoEmail, String consultaId) {
        Consulta consulta = buscarConsulta(consultaId);
        String medicoId = buscarUsuarioPorEmail(medicoEmail).getId();

        // RN-APROV-01
        if (!consulta.getMedico().getId().equals(medicoId)) {
            throw new AcessoNegadoException("Você não pode marcar consultas de outro médico como realizadas.");
        }

        if (consulta.getStatus() != StatusConsulta.APROVADA) {
            throw new TransicaoInvalidaException(
                    "Apenas consultas APROVADAS podem ser marcadas como realizadas. Status atual: " + consulta.getStatus());
        }

        // RN-RET-06 — verifica se data já passou ou é hoje
        LocalDate hoje = LocalDate.now();
        if (consulta.getDataHora().toLocalDate().isAfter(hoje)) {
            throw new ConsultaNaoOcorreuAindaException();
        }

        consulta.setStatus(StatusConsulta.REALIZADA);
        log.info("Consulta {} marcada como REALIZADA pelo médico {}", consultaId, medicoEmail);
        return ConsultaResponse.from(consulta);
    }

    // ===== Helpers =====

    private Consulta buscarConsulta(String id) {
        return consultaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Consulta não encontrada: " + id));
    }

    private Usuario buscarUsuarioPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado: " + email));
    }
}
