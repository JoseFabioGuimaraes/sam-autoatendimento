package br.com.sam.medico.model;

import br.com.sam.usuario.model.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "medico")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Medico {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    @Column(name = "crm", nullable = false, unique = true, length = 20)
    private String crm;

    @Column(name = "especialidade", nullable = false, length = 100)
    private String especialidade;

    @Column(name = "nome_completo", nullable = false, length = 150)
    private String nomeCompleto;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private Usuario usuario;
}
