package br.com.sam.disponibilidade.repository;

import br.com.sam.disponibilidade.model.DiaSemana;
import br.com.sam.disponibilidade.model.DisponibilidadeMedico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;

public interface DisponibilidadeRepository extends JpaRepository<DisponibilidadeMedico, String> {

    List<DisponibilidadeMedico> findByMedicoIdAndAtivoTrue(String medicoId);

    List<DisponibilidadeMedico> findByMedicoIdAndDiaSemanaAndAtivoTrue(String medicoId, DiaSemana diaSemana);

    /**
     * Verifica sobreposição de faixas: retorna faixas ativas do médico no mesmo dia
     * que se sobrepõem ao intervalo [horaInicio, horaFim).
     * RN-DISP-08
     */
    @Query("""
            SELECT d FROM DisponibilidadeMedico d
            WHERE d.medico.id = :medicoId
              AND d.diaSemana = :dia
              AND d.ativo = true
              AND d.horaInicio < :horaFim
              AND d.horaFim > :horaInicio
              AND (:excludeId IS NULL OR d.id <> :excludeId)
            """)
    List<DisponibilidadeMedico> findSobreposicoes(
            @Param("medicoId") String medicoId,
            @Param("dia") DiaSemana dia,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFim") LocalTime horaFim,
            @Param("excludeId") String excludeId
    );
}
