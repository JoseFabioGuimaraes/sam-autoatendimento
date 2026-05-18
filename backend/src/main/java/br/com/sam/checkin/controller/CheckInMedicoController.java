package br.com.sam.checkin.controller;

import br.com.sam.checkin.dto.CheckInResponse;
import br.com.sam.checkin.dto.ConfirmarCheckinRequest;
import br.com.sam.checkin.service.CheckInService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/medicos/consultas/{consultaId}/checkin")
@RequiredArgsConstructor
@Tag(name = "Check-in — Médico", description = "Consulta e confirmação de check-in pelo médico")
@SecurityRequirement(name = "bearerAuth")
public class CheckInMedicoController {

    private final CheckInService checkInService;

    @GetMapping
    @Operation(summary = "Consultar check-in",
            description = "Médico consulta o status do check-in de uma consulta. (RN-CHKIN-06, RN-CHKIN-08)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Check-in encontrado"),
            @ApiResponse(responseCode = "403", description = "Consulta de outro médico"),
            @ApiResponse(responseCode = "404", description = "Nenhum check-in registrado")
    })
    public ResponseEntity<CheckInResponse> consultar(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String consultaId
    ) {
        return ResponseEntity.ok(checkInService.consultarCheckin(userDetails.getUsername(), consultaId));
    }

    @PatchMapping("/confirmar")
    @Operation(summary = "Confirmar check-in",
            description = "Médico responde ao check-in com PODE_ENTRAR ou AGUARDAR. (RN-CHKIN-07 a RN-CHKIN-09)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Check-in confirmado"),
            @ApiResponse(responseCode = "400", description = "Justificativa ausente para AGUARDAR"),
            @ApiResponse(responseCode = "403", description = "Consulta de outro médico"),
            @ApiResponse(responseCode = "409", description = "Check-in já respondido")
    })
    public ResponseEntity<CheckInResponse> confirmar(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String consultaId,
            @Valid @RequestBody ConfirmarCheckinRequest request
    ) {
        return ResponseEntity.ok(checkInService.confirmarCheckin(userDetails.getUsername(), consultaId, request));
    }
}
