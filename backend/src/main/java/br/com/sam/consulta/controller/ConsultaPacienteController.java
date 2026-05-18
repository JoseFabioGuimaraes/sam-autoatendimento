package br.com.sam.consulta.controller;

import br.com.sam.consulta.dto.ConsultaRequest;
import br.com.sam.consulta.dto.ConsultaResponse;
import br.com.sam.consulta.model.StatusConsulta;
import br.com.sam.consulta.service.ConsultaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/pacientes/consultas")
@RequiredArgsConstructor
@Tag(name = "Consultas — Paciente", description = "Solicitação, listagem e cancelamento de consultas pelo paciente")
@SecurityRequirement(name = "bearerAuth")
public class ConsultaPacienteController {

    private final ConsultaService consultaService;

    @PostMapping
    @Operation(summary = "Solicitar consulta", description = "Paciente solicita consulta com médico. (RN-CONS-01 a RN-CONS-06)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Consulta criada com AGUARDANDO_APROVACAO"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou horário no passado"),
            @ApiResponse(responseCode = "403", description = "Perfil não autorizado (apenas PACIENTE)"),
            @ApiResponse(responseCode = "409", description = "Slot indisponível ou conflito de agenda")
    })
    public ResponseEntity<ConsultaResponse> solicitar(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ConsultaRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(consultaService.solicitarConsulta(userDetails.getUsername(), request));
    }

    @GetMapping
    @Operation(summary = "Listar minhas consultas", description = "Retorna histórico paginado das consultas do paciente com filtros. (RN-CONS-07, RN-HIST-01, RN-HIST-03)")
    public ResponseEntity<Page<ConsultaResponse>> listar(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) StatusConsulta status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                consultaService.listarConsultasPaciente(userDetails.getUsername(), status, inicio, fim, pageable)
        );
    }

    @PatchMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar consulta", description = "Cancela consulta AGUARDANDO_APROVACAO ou APROVADA. (RN-CONS-08)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta cancelada"),
            @ApiResponse(responseCode = "403", description = "Consulta pertence a outro paciente"),
            @ApiResponse(responseCode = "409", description = "Status não permite cancelamento")
    })
    public ResponseEntity<ConsultaResponse> cancelar(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String id
    ) {
        return ResponseEntity.ok(consultaService.cancelarConsulta(userDetails.getUsername(), id));
    }

    @PostMapping("/{id}/retorno")
    @Operation(summary = "Solicitar retorno", description = "Solicita consulta de retorno a partir de consulta REALIZADA. (RN-RET-01 a RN-RET-05)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Retorno criado com AGUARDANDO_APROVACAO"),
            @ApiResponse(responseCode = "400", description = "Consulta não realizada ou horário no passado"),
            @ApiResponse(responseCode = "409", description = "Retorno duplicado ou conflito de agenda")
    })
    public ResponseEntity<ConsultaResponse> retorno(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String id,
            @Valid @RequestBody ConsultaRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(consultaService.solicitarRetorno(userDetails.getUsername(), id, request));
    }
}
