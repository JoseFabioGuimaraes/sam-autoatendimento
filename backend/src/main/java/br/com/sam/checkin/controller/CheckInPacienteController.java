package br.com.sam.checkin.controller;

import br.com.sam.checkin.dto.CheckInResponse;
import br.com.sam.checkin.service.CheckInService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pacientes/consultas/{consultaId}/checkin")
@RequiredArgsConstructor
@Tag(name = "Check-in — Paciente", description = "Check-in automático do paciente")
@SecurityRequirement(name = "bearerAuth")
public class CheckInPacienteController {

    private final CheckInService checkInService;

    @PostMapping
    @Operation(summary = "Realizar check-in",
            description = "Paciente realiza check-in na consulta aprovada, dentro da janela de 30 min. (RN-CHKIN-01 a RN-CHKIN-05)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Check-in realizado"),
            @ApiResponse(responseCode = "403", description = "Consulta de outro paciente"),
            @ApiResponse(responseCode = "409", description = "Check-in duplicado ou status inválido"),
            @ApiResponse(responseCode = "422", description = "Fora da janela de check-in")
    })
    public ResponseEntity<CheckInResponse> realizarCheckin(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String consultaId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(checkInService.realizarCheckin(userDetails.getUsername(), consultaId));
    }
}
