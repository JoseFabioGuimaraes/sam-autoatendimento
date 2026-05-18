package br.com.sam.cancelamento.model;

import br.com.sam.medico.model.Medico;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "cancelamento_agenda")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelamentoAgenda {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medico_id", nullable = false)
    private Medico medico;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_cancelamento", nullable = false,
            columnDefinition = "ENUM('DIA_COMPLETO','TURNO','HORARIO_ESPECIFICO')")
    private TipoCancelamento tipoCancelamento;

    @Column(name = "data", nullable = false)
    private LocalDate data;

    @Column(name = "hora_inicio")
    private LocalTime horaInicio;

    @Column(name = "hora_fim")
    private LocalTime horaFim;

    @Column(name = "motivo_cancelamento", nullable = false, columnDefinition = "TEXT")
    private String motivoCancelamento;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    protected void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.criadoEm == null) {
            this.criadoEm = LocalDateTime.now();
        }
    }
}
