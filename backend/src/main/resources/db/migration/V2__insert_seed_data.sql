-- V2__insert_seed_data.sql
-- Dados iniciais para desenvolvimento e demo
-- Senhas são BCrypt de "Senha@123"

-- Médico 1: Dr. Carlos Souza — Cardiologia
INSERT INTO usuario (id, nome, email, senha, perfil, ativo, criado_em) VALUES
    ('00000000-0000-0000-0000-000000000001',
     'Dr. Carlos Souza',
     'medico@teste.com',
     '$2a$05$bzvhlBroPOmx09nBSX8GkOgTUV9S5DPzPNxkLDalchA0biGIk8kZ2',
     'MEDICO', 1, NOW());

INSERT INTO medico (id, crm, especialidade, nome_completo) VALUES
    ('00000000-0000-0000-0000-000000000001',
     'CRM-SP-12345',
     'Cardiologia',
     'Dr. Carlos Souza');

-- Disponibilidade: Segunda a Sexta 08:00-12:00 (slots de 30min)
INSERT INTO disponibilidade_medico (id, medico_id, dia_semana, hora_inicio, hora_fim, duracao_slot_min, ativo) VALUES
    ('10000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'SEG', '08:00', '12:00', 30, 1),
    ('10000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'TER', '08:00', '12:00', 30, 1),
    ('10000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', 'QUA', '08:00', '12:00', 30, 1),
    ('10000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000001', 'QUI', '08:00', '12:00', 30, 1),
    ('10000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000001', 'SEX', '08:00', '12:00', 30, 1);

-- Médico 2: Dra. Ana Lima — Dermatologia
INSERT INTO usuario (id, nome, email, senha, perfil, ativo, criado_em) VALUES
    ('00000000-0000-0000-0000-000000000002',
     'Dra. Ana Lima',
     'ana.lima@sam.com',
     '$2a$05$bzvhlBroPOmx09nBSX8GkOgTUV9S5DPzPNxkLDalchA0biGIk8kZ2',
     'MEDICO', 1, NOW());

INSERT INTO medico (id, crm, especialidade, nome_completo) VALUES
    ('00000000-0000-0000-0000-000000000002',
     'CRM-RJ-54321',
     'Dermatologia',
     'Dra. Ana Lima');

-- Disponibilidade: Terça e Quinta 14:00-18:00 (slots de 30min)
INSERT INTO disponibilidade_medico (id, medico_id, dia_semana, hora_inicio, hora_fim, duracao_slot_min, ativo) VALUES
    ('10000000-0000-0000-0000-000000000006', '00000000-0000-0000-0000-000000000002', 'TER', '14:00', '18:00', 30, 1),
    ('10000000-0000-0000-0000-000000000007', '00000000-0000-0000-0000-000000000002', 'QUI', '14:00', '18:00', 30, 1);
