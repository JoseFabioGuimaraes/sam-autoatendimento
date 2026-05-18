-- V3__insert_pacientes_seed.sql
-- Pacientes de demonstração
-- Senhas são BCrypt de "Senha@123"

-- Paciente 1: Maria Santos
INSERT INTO usuario (id, nome, email, senha, perfil, ativo, criado_em) VALUES
    ('00000000-0000-0000-0000-000000000010',
     'Maria Santos',
     'paciente@teste.com',
     '$2a$05$bzvhlBroPOmx09nBSX8GkOgTUV9S5DPzPNxkLDalchA0biGIk8kZ2',
     'PACIENTE', 1, NOW());

-- Paciente 2: João Oliveira
INSERT INTO usuario (id, nome, email, senha, perfil, ativo, criado_em) VALUES
    ('00000000-0000-0000-0000-000000000011',
     'João Oliveira',
     'joao.oliveira@email.com',
     '$2a$05$bzvhlBroPOmx09nBSX8GkOgTUV9S5DPzPNxkLDalchA0biGIk8kZ2',
     'PACIENTE', 1, NOW());

-- Paciente 3: Ana Costa
INSERT INTO usuario (id, nome, email, senha, perfil, ativo, criado_em) VALUES
    ('00000000-0000-0000-0000-000000000012',
     'Ana Costa',
     'ana.costa@email.com',
     '$2a$05$bzvhlBroPOmx09nBSX8GkOgTUV9S5DPzPNxkLDalchA0biGIk8kZ2',
     'PACIENTE', 1, NOW());
