package br.com.sam.consulta.repository;

import br.com.sam.consulta.model.Consulta;
import br.com.sam.consulta.model.StatusConsulta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ConsultaRepository extends JpaRepository<Consulta, String> {

    Page<Consulta> findByPacienteId(String pacienteId, Pageable pageable);

    Page<Consulta> findByMedicoId(String medicoId, Pageable pageable);

    Page<Consulta> findByMedicoIdAndStatus(String medicoId, StatusConsulta status, Pageable pageable);

    /**
     * Verifica conflito de agenda para o médico (RN-CONS-05, RN-APROV-06)
     */
    @Query("""
            SELECT c FROM Consulta c
            WHERE c.medico.id = :medicoId
              AND c.dataHora = :dataHora
              AND c.status IN ('AGUARDANDO_APROVACAO', 'APROVADA')
              AND (:excludeId IS NULL OR c.id <> :excludeId)
            """)
    List<Consulta> findConflitosDoMedico(
            @Param("medicoId") String medicoId,
            @Param("dataHora") LocalDateTime dataHora,
            @Param("excludeId") String excludeId
    );

    /**
     * Verifica conflito de agenda para o paciente (RN-CONS-04)
     */
    @Query("""
            SELECT c FROM Consulta c
            WHERE c.paciente.id = :pacienteId
              AND c.dataHora = :dataHora
              AND c.status IN ('AGUARDANDO_APROVACAO', 'APROVADA')
            """)
    List<Consulta> findConflitsDoPaciente(
            @Param("pacienteId") String pacienteId,
            @Param("dataHora") LocalDateTime dataHora
    );

    /**
     * Busca retorno existente para consulta de origem (RN-RET-04)
     */
    Optional<Consulta> findByConsultaOrigemId(String consultaOrigemId);



    /**
     * Histórico com filtro por status e período (RN-HIST-03)
     */
    @Query("""
            SELECT c FROM Consulta c
            WHERE c.paciente.id = :pacienteId
              AND (:status IS NULL OR c.status = :status)
              AND (:inicio IS NULL OR c.dataHora >= :inicio)
              AND (:fim IS NULL OR c.dataHora <= :fim)
            """)
    Page<Consulta> findHistoricoPaciente(
            @Param("pacienteId") String pacienteId,
            @Param("status") StatusConsulta status,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            Pageable pageable
    );

    /**
     * Histórico do médico com filtro por status e período (RN-HIST-02, RN-HIST-03)
     */
    @Query("""
            SELECT c FROM Consulta c
            WHERE c.medico.id = :medicoId
              AND (:status IS NULL OR c.status = :status)
              AND (:inicio IS NULL OR c.dataHora >= :inicio)
              AND (:fim IS NULL OR c.dataHora <= :fim)
            """)
    Page<Consulta> findHistoricoMedico(
            @Param("medicoId") String medicoId,
            @Param("status") StatusConsulta status,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            Pageable pageable
    );

    /**
     * Consultas ativas do médico em uma data específica (para cancelamento de agenda).
     */
    @Query("""
            SELECT c FROM Consulta c
            WHERE c.medico.id = :medicoId
              AND FUNCTION('DATE', c.dataHora) = :data
              AND c.status IN ('AGUARDANDO_APROVACAO', 'APROVADA')
            """)
    List<Consulta> findConsultasAtivasDoMedicoNaData(
            @Param("medicoId") String medicoId,
            @Param("data") java.time.LocalDate data
    );

    /**
     * Consultas canceladas pelo médico em uma data específica (para reverter cancelamento).
     */
    @Query("""
            SELECT c FROM Consulta c
            WHERE c.medico.id = :medicoId
              AND FUNCTION('DATE', c.dataHora) = :data
              AND c.status = 'CANCELADA_PELO_MEDICO'
            """)
    List<Consulta> findConsultasCanceladasPeloMedicoNaData(
            @Param("medicoId") String medicoId,
            @Param("data") java.time.LocalDate data
    );
}
