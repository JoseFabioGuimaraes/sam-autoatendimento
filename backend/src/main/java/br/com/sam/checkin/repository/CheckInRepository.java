package br.com.sam.checkin.repository;

import br.com.sam.checkin.model.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CheckInRepository extends JpaRepository<CheckIn, String> {

    Optional<CheckIn> findByConsultaId(String consultaId);

    boolean existsByConsultaId(String consultaId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(c) > 0 FROM CheckIn c JOIN c.consulta cons WHERE cons.medico.id = :medicoId AND c.statusCheckin = 'PODE_ENTRAR' AND cons.status = 'APROVADA'")
    boolean existsCheckinAtivoParaMedico(@org.springframework.data.repository.query.Param("medicoId") String medicoId);
}
