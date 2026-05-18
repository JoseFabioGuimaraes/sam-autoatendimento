-- V5__add_status_anterior_consulta.sql
-- Adiciona a coluna status_anterior para restaurar consultas quando um cancelamento de agenda for revertido

ALTER TABLE consulta
    ADD COLUMN status_anterior ENUM(
        'AGUARDANDO_APROVACAO',
        'APROVADA',
        'RECUSADA',
        'CANCELADA_PELO_PACIENTE',
        'CANCELADA_PELO_MEDICO',
        'REALIZADA'
    ) NULL;
