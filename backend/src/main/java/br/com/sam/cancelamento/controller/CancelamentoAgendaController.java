package br.com.sam.cancelamento.controller;

import br.com.sam.cancelamento.dto.CancelamentoAgendaRequest;
import br.com.sam.cancelamento.dto.CancelamentoAgendaResponse;
import br.com.sam.cancelamento.service.CancelamentoAgendaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/medicos/agenda/cancelamentos")
@RequiredArgsConstructor
@Tag(name = "Cancelamento de Agenda — Médico", description = "Cancelamento de dia, turno ou slot da agenda médica")
@SecurityRequirement(name = "bearerAuth")
public class CancelamentoAgendaController {

    private final CancelamentoAgendaService cancelamentoService;

    @PostMapping
    @Operation(summary = "Registrar cancelamento de agenda",
            description = "Cancela dia completo, turno ou horário específico. Consultas afetadas mudam para CANCELADA_PELO_MEDICO. (RN-CAGD-01 a RN-CAGD-08)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cancelamento registrado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou data passada"),
            @ApiResponse(responseCode = "409", description = "Sobreposição de cancelamento")
    })
    public ResponseEntity<CancelamentoAgendaResponse> registrar(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CancelamentoAgendaRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cancelamentoService.registrarCancelamento(userDetails.getUsername(), request));
    }

    @GetMapping
    @Operation(summary = "Listar cancelamentos futuros",
            description = "Lista todos os cancelamentos futuros do médico autenticado.")
    public ResponseEntity<List<CancelamentoAgendaResponse>> listar(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(cancelamentoService.listarCancelamentos(userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Reverter cancelamento",
            description = "Remove o cancelamento de agenda. (T64)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Cancelamento revertido"),
            @ApiResponse(responseCode = "403", description = "Cancelamento de outro médico"),
            @ApiResponse(responseCode = "404", description = "Cancelamento não encontrado")
    })
    public ResponseEntity<Void> reverter(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String id
    ) {
        cancelamentoService.reverterCancelamento(userDetails.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}
