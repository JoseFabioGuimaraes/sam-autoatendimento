package br.com.sam.checkin.model;

import br.com.sam.consulta.model.Consulta;
import br.com.sam.usuario.model.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "check_in")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckIn {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consulta_id", nullable = false, unique = true)
    private Consulta consulta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Usuario paciente;

    @Column(name = "realizado_em", nullable = false, updatable = false)
    private LocalDateTime realizadoEm;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_checkin", nullable = false,
            columnDefinition = "ENUM('AGUARDANDO_CONFIRMACAO','PODE_ENTRAR','AGUARDAR')")
    @Builder.Default
    private StatusCheckin statusCheckin = StatusCheckin.AGUARDANDO_CONFIRMACAO;

    @Column(name = "justificativa_espera", columnDefinition = "TEXT")
    private String justificativaEspera;

    @Column(name = "respondido_em")
    private LocalDateTime respondidoEm;

    @PrePersist
    protected void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.realizadoEm == null) {
            this.realizadoEm = LocalDateTime.now();
        }
    }
}
