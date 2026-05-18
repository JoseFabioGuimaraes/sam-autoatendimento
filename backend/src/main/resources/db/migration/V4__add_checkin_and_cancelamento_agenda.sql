-- V4__add_checkin_and_cancelamento_agenda.sql
-- SAM - Atualizações de Escopo v1.1
-- Check-in Automático + Cancelamento de Agenda + CANCELADA_PELO_MEDICO

-- 1. Adicionar novo status CANCELADA_PELO_MEDICO à consulta (RN-CAGD-07)
ALTER TABLE consulta
    MODIFY COLUMN status ENUM(
        'AGUARDANDO_APROVACAO',
        'APROVADA',
        'RECUSADA',
        'CANCELADA_PELO_PACIENTE',
        'CANCELADA_PELO_MEDICO',
        'REALIZADA'
    ) NOT NULL DEFAULT 'AGUARDANDO_APROVACAO';

-- 2. Tabela check_in (12.1)
CREATE TABLE IF NOT EXISTS check_in (
    id                    CHAR(36)    NOT NULL PRIMARY KEY,
    consulta_id           CHAR(36)    NOT NULL,
    paciente_id           CHAR(36)    NOT NULL,
    realizado_em          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status_checkin        ENUM('AGUARDANDO_CONFIRMACAO', 'PODE_ENTRAR', 'AGUARDAR')
                                      NOT NULL DEFAULT 'AGUARDANDO_CONFIRMACAO',
    justificativa_espera  TEXT        NULL,
    respondido_em         DATETIME    NULL,
    CONSTRAINT fk_checkin_consulta  FOREIGN KEY (consulta_id)  REFERENCES consulta (id),
    CONSTRAINT fk_checkin_paciente  FOREIGN KEY (paciente_id)  REFERENCES usuario (id),
    CONSTRAINT uq_checkin_consulta UNIQUE (consulta_id)
);

CREATE INDEX idx_checkin_paciente ON check_in (paciente_id);
CREATE INDEX idx_checkin_status   ON check_in (status_checkin);

-- 3. Tabela cancelamento_agenda (12.2)
CREATE TABLE IF NOT EXISTS cancelamento_agenda (
    id                    CHAR(36)    NOT NULL PRIMARY KEY,
    medico_id             CHAR(36)    NOT NULL,
    tipo_cancelamento     ENUM('DIA_COMPLETO', 'TURNO', 'HORARIO_ESPECIFICO')
                                      NOT NULL,
    data                  DATE        NOT NULL,
    hora_inicio           TIME        NULL,
    hora_fim              TIME        NULL,
    motivo_cancelamento   TEXT        NOT NULL,
    criado_em             DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cancel_medico FOREIGN KEY (medico_id) REFERENCES medico (id)
);

CREATE INDEX idx_cancel_medico_data ON cancelamento_agenda (medico_id, data);
