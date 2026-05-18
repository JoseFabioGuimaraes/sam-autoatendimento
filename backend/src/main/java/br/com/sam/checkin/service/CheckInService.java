package br.com.sam.checkin.service;

import br.com.sam.checkin.dto.CheckInResponse;
import br.com.sam.checkin.dto.ConfirmarCheckinRequest;
import br.com.sam.checkin.model.CheckIn;
import br.com.sam.checkin.model.StatusCheckin;
import br.com.sam.checkin.repository.CheckInRepository;
import br.com.sam.consulta.model.Consulta;
import br.com.sam.consulta.model.StatusConsulta;
import br.com.sam.consulta.repository.ConsultaRepository;
import br.com.sam.shared.exception.*;
import br.com.sam.usuario.model.PerfilUsuario;
import br.com.sam.usuario.model.Usuario;
import br.com.sam.usuario.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Serviço de Check-in Automático.
 * Regras: RN-CHKIN-01 a RN-CHKIN-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckInService {

    private final CheckInRepository checkInRepository;
    private final ConsultaRepository consultaRepository;
    private final UsuarioRepository usuarioRepository;

    @Value("${sam.checkin.janela-minutos:30}")
    private int janelaMinutos;

    /**
     * Paciente realiza check-in (RN-CHKIN-01 a RN-CHKIN-05).
     */
    @Transactional
    public CheckInResponse realizarCheckin(String pacienteEmail, String consultaId) {
        Usuario paciente = buscarUsuario(pacienteEmail);
        Consulta consulta = buscarConsulta(consultaId);

        // RN-CHKIN-01
        if (!consulta.getPaciente().getId().equals(paciente.getId())) {
            throw new AcessoNegadoException("Você só pode fazer check-in na sua própria consulta.");
        }

        // RN-CHKIN-02
        if (consulta.getStatus() != StatusConsulta.APROVADA) {
            throw new TransicaoInvalidaException(
                    "Check-in só é permitido para consultas com status APROVADA. Status atual: " + consulta.getStatus());
        }

        // RN-CHKIN-05 (Re-checkin se estiver em AGUARDAR)
        CheckIn existing = checkInRepository.findByConsultaId(consultaId).orElse(null);
        if (existing != null) {
            if (existing.getStatusCheckin() == StatusCheckin.AGUARDAR) {
                existing.setStatusCheckin(StatusCheckin.AGUARDANDO_CONFIRMACAO);
                existing.setJustificativaEspera(null);
                existing.setRealizadoEm(LocalDateTime.now());
                existing.setRespondidoEm(null);
                CheckIn salvo = checkInRepository.save(existing);
                log.info("Check-in {} re-solicitado pelo paciente {} na consulta {}", salvo.getId(), pacienteEmail, consultaId);
                return CheckInResponse.from(salvo);
            }
            throw new CheckinDuplicadoException();
        }

        // RN-CHKIN-03
        LocalDate hoje = LocalDate.now();
        if (!consulta.getDataHora().toLocalDate().equals(hoje)) {
            throw new CheckinForaDaJanelaException("O check-in só pode ser realizado no dia da consulta.");
        }

        // RN-CHKIN-04 — janela de 30 min antes até o horário da consulta
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime inicioJanela = consulta.getDataHora().minusMinutes(janelaMinutos);
        LocalDateTime fimJanela = consulta.getDataHora();
        if (agora.isBefore(inicioJanela) || agora.isAfter(fimJanela)) {
            throw new CheckinForaDaJanelaException(
                    "O check-in só pode ser feito entre " + janelaMinutos + " minutos antes e o horário da consulta.");
        }

        CheckIn checkin = CheckIn.builder()
                .consulta(consulta)
                .paciente(paciente)
                .statusCheckin(StatusCheckin.AGUARDANDO_CONFIRMACAO)
                .build();

        CheckIn salvo = checkInRepository.save(checkin);
        log.info("Check-in {} realizado pelo paciente {} na consulta {}", salvo.getId(), pacienteEmail, consultaId);
        return CheckInResponse.from(salvo);
    }

    /**
     * Médico consulta status do check-in (RN-CHKIN-08).
     */
    @Transactional(readOnly = true)
    public CheckInResponse consultarCheckin(String medicoEmail, String consultaId) {
        Consulta consulta = buscarConsulta(consultaId);
        Usuario medico = buscarUsuario(medicoEmail);

        // RN-CHKIN-08
        if (!consulta.getMedico().getId().equals(medico.getId())) {
            throw new AcessoNegadoException("Você só pode visualizar check-ins das suas consultas.");
        }

        CheckIn checkin = checkInRepository.findByConsultaId(consultaId)
                .orElseThrow(() -> new EntityNotFoundException("Nenhum check-in registrado para esta consulta."));

        return CheckInResponse.from(checkin);
    }

    /**
     * Médico confirma check-in (RN-CHKIN-07 a RN-CHKIN-09).
     */
    @Transactional
    public CheckInResponse confirmarCheckin(String medicoEmail, String consultaId, ConfirmarCheckinRequest request) {
        Consulta consulta = buscarConsulta(consultaId);
        Usuario medico = buscarUsuario(medicoEmail);

        // RN-CHKIN-08
        if (!consulta.getMedico().getId().equals(medico.getId())) {
            throw new AcessoNegadoException("Você só pode responder check-ins das suas consultas.");
        }

        CheckIn checkin = checkInRepository.findByConsultaId(consultaId)
                .orElseThrow(() -> new EntityNotFoundException("Nenhum check-in registrado para esta consulta."));

        // RN-CHKIN-09
        if (checkin.getStatusCheckin() != StatusCheckin.AGUARDANDO_CONFIRMACAO) {
            throw new CheckinJaRespondidoException();
        }

        if (request.statusCheckin() == StatusCheckin.PODE_ENTRAR) {
            if (checkInRepository.existsCheckinAtivoParaMedico(medico.getId())) {
                throw new MedicoOcupadoException();
            }
        }

        // RN-CHKIN-07 — validação AGUARDAR exige justificativa
        if (request.statusCheckin() == StatusCheckin.AGUARDAR) {
            if (request.justificativaEspera() == null || request.justificativaEspera().trim().length() < 10) {
                throw new JustificativaObrigatoriaException(
                        "A justificativa de espera é obrigatória e deve ter no mínimo 10 caracteres.");
            }
            checkin.setJustificativaEspera(request.justificativaEspera().trim());
        }

        if (request.statusCheckin() == StatusCheckin.AGUARDANDO_CONFIRMACAO) {
            throw new TransicaoInvalidaException("O status de resposta deve ser PODE_ENTRAR ou AGUARDAR.");
        }

        checkin.setStatusCheckin(request.statusCheckin());
        checkin.setRespondidoEm(LocalDateTime.now());

        log.info("Check-in {} confirmado pelo médico {} com status {}", checkin.getId(), medicoEmail, request.statusCheckin());
        return CheckInResponse.from(checkin);
    }

    // === Helpers ===

    private Consulta buscarConsulta(String id) {
        return consultaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Consulta não encontrada: " + id));
    }

    private Usuario buscarUsuario(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado: " + email));
    }
}
