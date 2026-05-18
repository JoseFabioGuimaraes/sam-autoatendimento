package br.com.sam.consulta.controller;

import br.com.sam.consulta.dto.ConsultaResponse;
import br.com.sam.consulta.dto.RecusaRequest;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/medicos/consultas")
@RequiredArgsConstructor
@Tag(name = "Consultas — Médico", description = "Gerenciamento de consultas pelo médico: aprovação, recusa e realização")
@SecurityRequirement(name = "bearerAuth")
public class ConsultaMedicoController {

    private final ConsultaService consultaService;

    @GetMapping
    @Operation(summary = "Listar minhas consultas", description = "Retorna histórico paginado das consultas do médico com filtros. (RN-APROV-05, RN-HIST-02, RN-HIST-03)")
    public ResponseEntity<Page<ConsultaResponse>> listar(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) StatusConsulta status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                consultaService.listarConsultasMedico(userDetails.getUsername(), status, inicio, fim, pageable)
        );
    }

    @GetMapping("/pendentes")
    @Operation(summary = "Listar consultas pendentes", description = "Retorna apenas consultas com status AGUARDANDO_APROVACAO.")
    public ResponseEntity<Page<ConsultaResponse>> pendentes(
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                consultaService.listarConsultasMedico(
                        userDetails.getUsername(),
                        StatusConsulta.AGUARDANDO_APROVACAO,
                        null, null, pageable
                )
        );
    }

    @PatchMapping("/{id}/aprovar")
    @Operation(summary = "Aprovar consulta", description = "Aprova consulta AGUARDANDO_APROVACAO. Cancela concorrentes automaticamente. (RN-APROV-02, RN-APROV-06)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta aprovada"),
            @ApiResponse(responseCode = "403", description = "Consulta pertence a outro médico"),
            @ApiResponse(responseCode = "409", description = "Status não permite aprovação")
    })
    public ResponseEntity<ConsultaResponse> aprovar(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String id
    ) {
        return ResponseEntity.ok(consultaService.aprovar(userDetails.getUsername(), id));
    }

    @PatchMapping("/{id}/recusar")
    @Operation(summary = "Recusar consulta", description = "Recusa consulta com justificativa obrigatória. (RN-APROV-03)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta recusada"),
            @ApiResponse(responseCode = "400", description = "Justificativa ausente"),
            @ApiResponse(responseCode = "409", description = "Status não permite recusa")
    })
    public ResponseEntity<ConsultaResponse> recusar(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String id,
            @Valid @RequestBody RecusaRequest request
    ) {
        return ResponseEntity.ok(consultaService.recusar(userDetails.getUsername(), id, request));
    }

    @PatchMapping("/{id}/realizar")
    @Operation(summary = "Marcar como realizada", description = "Marca consulta APROVADA como REALIZADA. Só possível se dataHora já passou. (RN-RET-06)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta marcada como REALIZADA"),
            @ApiResponse(responseCode = "400", description = "Consulta ainda não ocorreu"),
            @ApiResponse(responseCode = "409", description = "Status inválido para transição")
    })
    public ResponseEntity<ConsultaResponse> realizar(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String id
    ) {
        return ResponseEntity.ok(consultaService.realizar(userDetails.getUsername(), id));
    }
}
