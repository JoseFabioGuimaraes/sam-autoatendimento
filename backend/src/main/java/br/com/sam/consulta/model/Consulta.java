package br.com.sam.consulta.model;

import br.com.sam.medico.model.Medico;
import br.com.sam.usuario.model.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "consulta")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Consulta {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Usuario paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medico_id", nullable = false)
    private Medico medico;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false,
            columnDefinition = "ENUM('AGUARDANDO_APROVACAO','APROVADA','RECUSADA','CANCELADA_PELO_PACIENTE','CANCELADA_PELO_MEDICO','REALIZADA')")
    @Builder.Default
    private StatusConsulta status = StatusConsulta.AGUARDANDO_APROVACAO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior",
            columnDefinition = "ENUM('AGUARDANDO_APROVACAO','APROVADA','RECUSADA','CANCELADA_PELO_PACIENTE','CANCELADA_PELO_MEDICO','REALIZADA')")
    private StatusConsulta statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_consulta", nullable = false,
            columnDefinition = "ENUM('NORMAL','RETORNO')")
    @Builder.Default
    private TipoConsulta tipoConsulta = TipoConsulta.NORMAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consulta_origem_id")
    private Consulta consultaOrigem;

    @Column(name = "justificativa_recusa", columnDefinition = "TEXT")
    private String justificativaRecusa;

    @OneToOne(mappedBy = "consulta", fetch = FetchType.LAZY)
    private br.com.sam.checkin.model.CheckIn checkIn;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @PrePersist
    protected void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        LocalDateTime now = LocalDateTime.now();
        if (this.criadoEm == null) {
            this.criadoEm = now;
        }
        this.atualizadoEm = now;
    }

    @PreUpdate
    protected void preUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }
}
