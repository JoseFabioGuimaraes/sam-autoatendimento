package br.com.sam.cancelamento.repository;

import br.com.sam.cancelamento.model.CancelamentoAgenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CancelamentoAgendaRepository extends JpaRepository<CancelamentoAgenda, String> {

    List<CancelamentoAgenda> findByMedicoIdAndDataGreaterThanEqualOrderByDataAsc(String medicoId, LocalDate data);

    @Query("""
            SELECT c FROM CancelamentoAgenda c
            WHERE c.medico.id = :medicoId
              AND c.data = :data
            """)
    List<CancelamentoAgenda> findByMedicoIdAndData(
            @Param("medicoId") String medicoId,
            @Param("data") LocalDate data
    );
}
