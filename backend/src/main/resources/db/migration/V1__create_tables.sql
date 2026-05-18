-- V1__create_tables.sql
-- SAM - Sistema de Agendamento Médico
-- Schema inicial

CREATE TABLE IF NOT EXISTS usuario (
    id          CHAR(36)                    NOT NULL PRIMARY KEY,
    nome        VARCHAR(150)                NOT NULL,
    email       VARCHAR(200)                NOT NULL UNIQUE,
    senha       VARCHAR(255)                NOT NULL,
    perfil      ENUM('PACIENTE', 'MEDICO')  NOT NULL,
    ativo       TINYINT(1)                  NOT NULL DEFAULT 1,
    criado_em   DATETIME                    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS medico (
    id             CHAR(36)     NOT NULL PRIMARY KEY,
    crm            VARCHAR(20)  NOT NULL UNIQUE,
    especialidade  VARCHAR(100) NOT NULL,
    nome_completo  VARCHAR(150) NOT NULL,
    CONSTRAINT fk_medico_usuario FOREIGN KEY (id) REFERENCES usuario (id)
);

CREATE TABLE IF NOT EXISTS disponibilidade_medico (
    id               CHAR(36)                                            NOT NULL PRIMARY KEY,
    medico_id        CHAR(36)                                            NOT NULL,
    dia_semana       ENUM('SEG', 'TER', 'QUA', 'QUI', 'SEX', 'SAB', 'DOM') NOT NULL,
    hora_inicio      TIME                                                NOT NULL,
    hora_fim         TIME                                                NOT NULL,
    duracao_slot_min INT                                                 NOT NULL DEFAULT 30,
    ativo            TINYINT(1)                                          NOT NULL DEFAULT 1,
    CONSTRAINT fk_disp_medico FOREIGN KEY (medico_id) REFERENCES medico (id)
);

CREATE TABLE IF NOT EXISTS consulta (
    id                   CHAR(36) NOT NULL PRIMARY KEY,
    paciente_id          CHAR(36) NOT NULL,
    medico_id            CHAR(36) NOT NULL,
    data_hora            DATETIME NOT NULL,
    status               ENUM (
        'AGUARDANDO_APROVACAO',
        'APROVADA',
        'RECUSADA',
        'CANCELADA_PELO_PACIENTE',
        'REALIZADA'
    )                             NOT NULL DEFAULT 'AGUARDANDO_APROVACAO',
    tipo_consulta        ENUM('NORMAL', 'RETORNO') NOT NULL DEFAULT 'NORMAL',
    consulta_origem_id   CHAR(36)  NULL,
    justificativa_recusa TEXT      NULL,
    criado_em            DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em        DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_consulta_paciente FOREIGN KEY (paciente_id) REFERENCES usuario (id),
    CONSTRAINT fk_consulta_medico   FOREIGN KEY (medico_id)   REFERENCES medico (id),
    CONSTRAINT fk_consulta_origem   FOREIGN KEY (consulta_origem_id) REFERENCES consulta (id),
    CONSTRAINT uq_retorno_origem    UNIQUE (consulta_origem_id)
);

-- Índices para performance
CREATE INDEX idx_consulta_medico_datahora  ON consulta (medico_id, data_hora);
CREATE INDEX idx_consulta_paciente_status  ON consulta (paciente_id, status);
CREATE INDEX idx_consulta_status           ON consulta (status);
CREATE INDEX idx_disp_medico_dia           ON disponibilidade_medico (medico_id, dia_semana, ativo);
