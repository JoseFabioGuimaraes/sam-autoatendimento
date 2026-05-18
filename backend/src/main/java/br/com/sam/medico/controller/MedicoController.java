package br.com.sam.medico.controller;

import br.com.sam.disponibilidade.dto.DisponibilidadeRequest;
import br.com.sam.disponibilidade.dto.DisponibilidadeResponse;
import br.com.sam.disponibilidade.dto.SlotResponse;
import br.com.sam.disponibilidade.service.DisponibilidadeService;
import br.com.sam.medico.dto.MedicoResponse;
import br.com.sam.medico.service.MedicoService;
import br.com.sam.usuario.repository.UsuarioRepository;
import io.swagger.v3.oas.annotations.Operation;
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

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/medicos")
@RequiredArgsConstructor
@Tag(name = "Médicos", description = "Listagem de médicos e gerenciamento de disponibilidade")
@SecurityRequirement(name = "bearerAuth")
public class MedicoController {

    private final MedicoService medicoService;
    private final DisponibilidadeService disponibilidadeService;
    private final UsuarioRepository usuarioRepository;

    // ===== Endpoints públicos (autenticados) =====

    @GetMapping
    @Operation(summary = "Listar médicos ativos", description = "Retorna lista paginada de médicos ativos.")
    public ResponseEntity<Page<MedicoResponse>> listar(Pageable pageable) {
        return ResponseEntity.ok(medicoService.listarAtivos(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar médico por ID")
    public ResponseEntity<MedicoResponse> buscarPorId(@PathVariable String id) {
        return ResponseEntity.ok(medicoService.buscarPorId(id));
    }

    @GetMapping("/{id}/slots")
    @Operation(summary = "Listar slots disponíveis", description = "Retorna os slots livres do médico para a data informada. (RN-DISP-05)")
    public ResponseEntity<List<SlotResponse>> slots(
            @PathVariable String id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data
    ) {
        return ResponseEntity.ok(disponibilidadeService.calcularSlots(id, data));
    }

    // ===== Endpoints de disponibilidade (MEDICO) =====

    @GetMapping("/disponibilidade")
    @Operation(summary = "Listar minha disponibilidade", description = "Retorna as faixas de disponibilidade do médico autenticado.")
    public ResponseEntity<List<DisponibilidadeResponse>> listarDisponibilidade(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String medicoId = resolverMedicoId(userDetails);
        return ResponseEntity.ok(disponibilidadeService.listarPorMedico(medicoId));
    }

    @PostMapping("/disponibilidade")
    @Operation(summary = "Criar faixa de disponibilidade", description = "Cria nova faixa de horário para o médico. (RN-DISP-01, RN-DISP-07, RN-DISP-08)")
    public ResponseEntity<DisponibilidadeResponse> criarDisponibilidade(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody DisponibilidadeRequest request
    ) {
        String medicoId = resolverMedicoId(userDetails);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(disponibilidadeService.criar(medicoId, request));
    }

    @PutMapping("/disponibilidade/{id}")
    @Operation(summary = "Atualizar faixa de disponibilidade", description = "Atualiza faixa existente do médico autenticado. (RN-DISP-03)")
    public ResponseEntity<DisponibilidadeResponse> atualizarDisponibilidade(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String id,
            @Valid @RequestBody DisponibilidadeRequest request
    ) {
        String medicoId = resolverMedicoId(userDetails);
        return ResponseEntity.ok(disponibilidadeService.atualizar(medicoId, id, request));
    }

    @DeleteMapping("/disponibilidade/{id}")
    @Operation(summary = "Desativar faixa de disponibilidade", description = "Desativa faixa de horário (soft delete). Consultas aprovadas mantidas. (RN-DISP-04)")
    public ResponseEntity<Void> desativarDisponibilidade(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String id
    ) {
        String medicoId = resolverMedicoId(userDetails);
        disponibilidadeService.desativar(medicoId, id);
        return ResponseEntity.noContent().build();
    }

    private String resolverMedicoId(UserDetails userDetails) {
        return usuarioRepository.findByEmail(userDetails.getUsername())
                .orElseThrow()
                .getId();
    }
}
