package br.com.sam.shared.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ===== 400 Bad Request =====

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(400, "Dados inválidos", mensagem));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(400, "Dados inválidos", ex.getMessage()));
    }

    @ExceptionHandler(HorarioInvalidoException.class)
    public ResponseEntity<ErrorResponse> handleHorarioInvalido(HorarioInvalidoException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(400, "Horário inválido", ex.getMessage()));
    }

    @ExceptionHandler(HorarioNoPassadoException.class)
    public ResponseEntity<ErrorResponse> handleHorarioPassado(HorarioNoPassadoException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(400, "Horário no passado", ex.getMessage()));
    }

    @ExceptionHandler(JustificativaObrigatoriaException.class)
    public ResponseEntity<ErrorResponse> handleJustificativa(JustificativaObrigatoriaException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(400, "Justificativa obrigatória", ex.getMessage()));
    }

    @ExceptionHandler(ConsultaNaoOcorreuAindaException.class)
    public ResponseEntity<ErrorResponse> handleConsultaFutura(ConsultaNaoOcorreuAindaException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(400, "Consulta não ocorreu", ex.getMessage()));
    }

    @ExceptionHandler(ConsultaNaoRealizadaException.class)
    public ResponseEntity<ErrorResponse> handleNaoRealizada(ConsultaNaoRealizadaException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(400, "Consulta não realizada", ex.getMessage()));
    }

    // ===== 401 Unauthorized =====

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ErrorResponse> handleCredenciais(CredenciaisInvalidasException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(401, "Não autorizado", ex.getMessage()));
    }

    // ===== 403 Forbidden =====

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<ErrorResponse> handleAcessoNegado(AcessoNegadoException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(403, "Acesso negado", ex.getMessage()));
    }

    @ExceptionHandler(PerfilNaoAutorizadoException.class)
    public ResponseEntity<ErrorResponse> handlePerfilNaoAutorizado(PerfilNaoAutorizadoException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(403, "Perfil não autorizado", ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleSpringAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(403, "Acesso negado", "Você não tem permissão para acessar este recurso."));
    }

    // ===== 404 Not Found =====

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Recurso não encontrado", ex.getMessage()));
    }

    // ===== 409 Conflict =====

    @ExceptionHandler(EmailJaCadastradoException.class)
    public ResponseEntity<ErrorResponse> handleEmailDuplicado(EmailJaCadastradoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflito", ex.getMessage()));
    }

    @ExceptionHandler(SobreposicaoDeHorariosException.class)
    public ResponseEntity<ErrorResponse> handleSobreposicao(SobreposicaoDeHorariosException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Sobreposição de horários", ex.getMessage()));
    }

    @ExceptionHandler(SlotIndisponivelException.class)
    public ResponseEntity<ErrorResponse> handleSlotIndisponivel(SlotIndisponivelException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Slot indisponível", ex.getMessage()));
    }

    @ExceptionHandler(ConflitoDeAgendaException.class)
    public ResponseEntity<ErrorResponse> handleConflito(ConflitoDeAgendaException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflito de agenda", ex.getMessage()));
    }

    @ExceptionHandler(TransicaoInvalidaException.class)
    public ResponseEntity<ErrorResponse> handleTransicao(TransicaoInvalidaException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Transição inválida", ex.getMessage()));
    }

    @ExceptionHandler(RetornoDuplicadoException.class)
    public ResponseEntity<ErrorResponse> handleRetornoDuplicado(RetornoDuplicadoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Retorno duplicado", ex.getMessage()));
    }

    @ExceptionHandler(MedicoOcupadoException.class)
    public ResponseEntity<ErrorResponse> handleMedicoOcupado(MedicoOcupadoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Médico ocupado", ex.getMessage()));
    }

    @ExceptionHandler(CheckinDuplicadoException.class)
    public ResponseEntity<ErrorResponse> handleCheckinDuplicado(CheckinDuplicadoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Check-in duplicado", ex.getMessage()));
    }

    @ExceptionHandler(CheckinJaRespondidoException.class)
    public ResponseEntity<ErrorResponse> handleCheckinJaRespondido(CheckinJaRespondidoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Check-in já respondido", ex.getMessage()));
    }

    @ExceptionHandler(SobreposicaoDeCancelamentoException.class)
    public ResponseEntity<ErrorResponse> handleSobreposicaoCancelamento(SobreposicaoDeCancelamentoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Sobreposição de cancelamento", ex.getMessage()));
    }

    // ===== 422 Unprocessable Entity =====

    @ExceptionHandler(CheckinForaDaJanelaException.class)
    public ResponseEntity<ErrorResponse> handleCheckinForaDaJanela(CheckinForaDaJanelaException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of(422, "Fora da janela de check-in", ex.getMessage()));
    }

    @ExceptionHandler(DataCancelamentoInvalidaException.class)
    public ResponseEntity<ErrorResponse> handleDataCancelamento(DataCancelamentoInvalidaException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(400, "Data inválida", ex.getMessage()));
    }

    // ===== 500 Internal Server Error =====

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Erro interno não tratado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(500, "Erro interno", "Ocorreu um erro inesperado. Por favor, tente novamente."));
    }
}
