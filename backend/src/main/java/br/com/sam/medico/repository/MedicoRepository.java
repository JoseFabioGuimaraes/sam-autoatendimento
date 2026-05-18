package br.com.sam.medico.repository;

import br.com.sam.medico.model.Medico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MedicoRepository extends JpaRepository<Medico, String> {

    boolean existsByCrm(String crm);

    @Query("SELECT m FROM Medico m JOIN m.usuario u WHERE u.ativo = true")
    Page<Medico> findAllAtivos(Pageable pageable);
}
