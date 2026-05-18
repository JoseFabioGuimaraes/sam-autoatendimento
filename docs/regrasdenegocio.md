# SAM — Sistema de Agendamento Médico
## Documento de Regras de Negócio · MVP v1.0

> **Stack definida:** Java 17 · Spring Boot · MariaDB · Docker Compose · JUnit 5 + Mockito (TDD)

---

## Sumário

1. [Visão Geral](#1-visão-geral)
2. [Atores e Perfis](#2-atores-e-perfis)
3. [Domínio e Entidades](#3-domínio-e-entidades)
4. [Regras de Negócio por Módulo](#4-regras-de-negócio-por-módulo)
   - 4.1 Autenticação e Cadastro
   - 4.2 Disponibilidade do Médico
   - 4.3 Solicitação de Consulta (Paciente)
   - 4.4 Aprovação de Consulta (Médico)
   - 4.5 Retorno
   - 4.6 Histórico
5. [Máquina de Estados da Consulta](#5-máquina-de-estados-da-consulta)
6. [Contratos de API (endpoints esperados)](#6-contratos-de-api-endpoints-esperados)
7. [Casos de Teste (TDD)](#7-casos-de-teste-tdd)
8. [Modelo de Dados](#8-modelo-de-dados)
9. [Docker Compose](#9-docker-compose)
10. [Restrições Técnicas e Não-Funcionais](#10-restrições-técnicas-e-não-funcionais)
11. [Fora do Escopo do MVP](#11-fora-do-escopo-do-mvp)
12. [att — Atualizações de Escopo](#12-att--atualizações-de-escopo)

---

## 1. Visão Geral

O SAM é uma plataforma de conexão direta entre pacientes e médicos. Seu objetivo no MVP é:

- Permitir que pacientes encontrem médicos disponíveis, visualizem horários e solicitem consultas.
- Permitir que médicos gerenciem sua própria agenda (dias e horários) e decidam manualmente se aprovam ou recusam cada solicitação.
- Suportar o ciclo completo de atendimento: solicitação → aprovação/recusa → consulta → retorno (quando necessário).

---

## 2. Atores e Perfis

| Perfil | Descrição |
|--------|-----------|
| `PACIENTE` | Usuário que busca e agenda consultas médicas. |
| `MEDICO` | Profissional de saúde que gerencia agenda e aprova/recusa consultas. |
| `ADMIN` *(pós-MVP)* | Gerencia médicos, usuários, clínica e relatórios. |

> No MVP, o cadastro de médicos e especialidades é feito diretamente via seed/script ou endpoint admin protegido. O foco está no fluxo Paciente ↔ Médico.

---

## 3. Domínio e Entidades

### 3.1 `Usuario`
Entidade base para autenticação. Todo ator do sistema é um `Usuario`.

| Campo | Tipo | Regra |
|-------|------|-------|
| `id` | UUID | PK gerado automaticamente |
| `nome` | String | Obrigatório, mín. 3 chars |
| `email` | String | Único, formato válido, obrigatório |
| `senha` | String | Hash BCrypt, mín. 8 chars |
| `perfil` | Enum(`PACIENTE`, `MEDICO`) | Obrigatório |
| `ativo` | Boolean | Default `true` |
| `criadoEm` | LocalDateTime | Gerado automaticamente |

### 3.2 `Medico`
Extensão de `Usuario` com dados profissionais.

| Campo | Tipo | Regra |
|-------|------|-------|
| `id` | UUID | PK / FK → `Usuario` |
| `crm` | String | Único, obrigatório |
| `especialidade` | String | Obrigatório |
| `nomeCompleto` | String | Obrigatório |

### 3.3 `DisponibilidadeMedico`
Define quais dias da semana e horários o médico atende.

| Campo | Tipo | Regra |
|-------|------|-------|
| `id` | UUID | PK |
| `medicoId` | UUID | FK → `Medico` |
| `diaSemana` | Enum(`SEG`,`TER`,`QUA`,`QUI`,`SEX`,`SAB`,`DOM`) | Obrigatório |
| `horaInicio` | LocalTime | Obrigatório |
| `horaFim` | LocalTime | Obrigatório, deve ser > `horaInicio` |
| `duracaoSlotMin` | Integer | Default 30 minutos, mín. 10 |
| `ativo` | Boolean | Default `true` |

**RN-DISP-01:** Um médico pode ter múltiplas faixas por dia (ex.: manhã e tarde), mas faixas do mesmo dia não podem se sobrepor.

**RN-DISP-02:** A partir da disponibilidade cadastrada, o sistema calcula os *slots* disponíveis dinamicamente (não armazena cada slot individualmente, exceto quando uma consulta ocupa um slot).

### 3.4 `Consulta`
Núcleo do sistema. Representa uma solicitação de atendimento.

| Campo | Tipo | Regra |
|-------|------|-------|
| `id` | UUID | PK |
| `pacienteId` | UUID | FK → `Usuario` (perfil PACIENTE) |
| `medicoId` | UUID | FK → `Medico` |
| `dataHora` | LocalDateTime | Obrigatório, deve ser futura |
| `status` | Enum | Ver seção 5 |
| `tipoConsulta` | Enum(`NORMAL`, `RETORNO`) | Default `NORMAL` |
| `consultaOrigemId` | UUID | Nullable; preenchido se `tipoConsulta = RETORNO` |
| `justificativaRecusa` | String | Obrigatório se status = `RECUSADA` |
| `criadoEm` | LocalDateTime | Automático |
| `atualizadoEm` | LocalDateTime | Automático |

---

## 4. Regras de Negócio por Módulo

### 4.1 Autenticação e Cadastro

**RN-AUTH-01:** O sistema usa autenticação stateless via JWT (Bearer token).

**RN-AUTH-02:** O token JWT deve conter: `userId`, `perfil`, `email` e `exp` (expiração).

**RN-AUTH-03:** Senha deve ser armazenada exclusivamente como hash BCrypt — nunca em texto puro.

**RN-AUTH-04:** O endpoint de cadastro de paciente é público (`POST /auth/register`). O de médico, no MVP, exige autenticação com perfil `ADMIN` ou é feito via seed.

**RN-AUTH-05:** E-mail duplicado retorna erro `409 Conflict` com mensagem descritiva.

**RN-AUTH-06:** Login com credenciais inválidas retorna `401 Unauthorized` — sem indicar qual campo está errado (segurança).

---

### 4.2 Disponibilidade do Médico

**RN-DISP-03:** Somente o próprio médico pode cadastrar, editar ou remover sua disponibilidade.

**RN-DISP-04:** Ao desativar uma faixa de disponibilidade (`ativo = false`), consultas já aprovadas para aquele período **não são canceladas automaticamente** — o médico deve gerenciá-las manualmente.

**RN-DISP-05:** O paciente consulta os *slots* livres de um médico via `GET /medicos/{id}/slots?data=YYYY-MM-DD`. O sistema retorna apenas slots que:
  - Estão dentro de uma faixa ativa do médico para aquele dia da semana.
  - Não possuem uma `Consulta` com status `APROVADA` ou `AGUARDANDO_APROVACAO` no mesmo horário.

**RN-DISP-06:** O sistema deve considerar o dia da semana correspondente à `data` informada para calcular os slots — não a data literal cadastrada.

**RN-DISP-07:** Não é possível cadastrar disponibilidade com `horaFim <= horaInicio`.

**RN-DISP-08:** Não é possível cadastrar disponibilidade que se sobreponha a uma faixa já ativa do mesmo médico no mesmo dia.

---

### 4.3 Solicitação de Consulta (Paciente)

**RN-CONS-01:** Apenas usuários com perfil `PACIENTE` podem solicitar consultas.

**RN-CONS-02:** O paciente só pode solicitar um horário que esteja listado como livre (retornado por `GET /medicos/{id}/slots`). O backend revalida no momento da criação.

**RN-CONS-03:** Não é permitido criar uma consulta para um horário no passado.

**RN-CONS-04:** Um paciente não pode ter duas consultas com status `AGUARDANDO_APROVACAO` ou `APROVADA` no mesmo horário.

**RN-CONS-05:** Um médico não pode ter duas consultas com status `AGUARDANDO_APROVACAO` ou `APROVADA` no mesmo horário (conflito de agenda).

**RN-CONS-06:** Ao criar a solicitação com sucesso, o status inicial é `AGUARDANDO_APROVACAO`.

**RN-CONS-07:** O paciente pode visualizar todas as suas consultas e seus status via `GET /pacientes/consultas`.

**RN-CONS-08:** O paciente pode cancelar uma consulta com status `AGUARDANDO_APROVACAO` ou `APROVADA`. O status passa para `CANCELADA_PELO_PACIENTE`. Consultas já `REALIZADA` ou `RECUSADA` não podem ser canceladas.

---

### 4.4 Aprovação de Consulta (Médico)

**RN-APROV-01:** Apenas o próprio médico destinatário pode aprovar ou recusar a consulta.

**RN-APROV-02:** Ao aprovar (`PATCH /medicos/consultas/{id}/aprovar`), o status muda para `APROVADA`.

**RN-APROV-03:** Ao recusar (`PATCH /medicos/consultas/{id}/recusar`), o campo `justificativaRecusa` é **obrigatório** e o status muda para `RECUSADA`.

**RN-APROV-04:** Somente consultas com status `AGUARDANDO_APROVACAO` podem ser aprovadas ou recusadas. Qualquer outro status retorna `409 Conflict`.

**RN-APROV-05:** O médico pode visualizar apenas as consultas destinadas a ele.

**RN-APROV-06:** Ao aprovar uma consulta, se houver outra consulta do mesmo paciente (ou de qualquer paciente) com `AGUARDANDO_APROVACAO` para o mesmo horário e médico, aquela consulta concorrente passa automaticamente para `RECUSADA` com justificativa padrão: `"Horário ocupado por outro agendamento confirmado."`.

---

### 4.5 Retorno

**RN-RET-01:** Um retorno é uma consulta do tipo `RETORNO` vinculada a uma consulta anterior do tipo `NORMAL` com status `REALIZADA`.

**RN-RET-02:** Somente o paciente que realizou a consulta original pode solicitar o retorno.

**RN-RET-03:** Não é permitido criar retorno de uma consulta que não esteja com status `REALIZADA`.

**RN-RET-04:** Não é permitido criar mais de um retorno para a mesma consulta de origem (campo `consultaOrigemId` deve ser único para retornos).

**RN-RET-05:** O retorno segue o mesmo fluxo de aprovação de uma consulta normal (status inicial: `AGUARDANDO_APROVACAO`).

**RN-RET-06:** O médico marca uma consulta como `REALIZADA` via `PATCH /medicos/consultas/{id}/realizar`. Só é possível para consultas `APROVADAS` cuja `dataHora` já tenha passado ou seja o dia atual.

---

### 4.6 Histórico

**RN-HIST-01:** O paciente pode visualizar todo seu histórico de consultas (todos os status) com paginação.

**RN-HIST-02:** O médico pode visualizar todo o histórico de consultas destinadas a ele (todos os status) com paginação.

**RN-HIST-03:** O histórico deve permitir filtro por status e por período de datas.

---

## 5. Máquina de Estados da Consulta

```
                      ┌─────────────────────────┐
                      │   AGUARDANDO_APROVACAO   │ ◄── Estado inicial
                      └───────────┬─────────────┘
                                  │
              ┌───────────────────┼───────────────────┐
              ▼                   ▼                   ▼
          APROVADA            RECUSADA      CANCELADA_PELO_PACIENTE
              │
     ┌────────┴────────┐
     ▼                 ▼
 REALIZADA     CANCELADA_PELO_PACIENTE
     │
     ▼
 (pode gerar RETORNO → novo ciclo)
```

| Status | Transições permitidas | Quem pode transicionar |
|--------|-----------------------|------------------------|
| `AGUARDANDO_APROVACAO` | → `APROVADA`, `RECUSADA`, `CANCELADA_PELO_PACIENTE` | Médico (aprovar/recusar), Paciente (cancelar) |
| `APROVADA` | → `REALIZADA`, `CANCELADA_PELO_PACIENTE` | Médico (realizar), Paciente (cancelar) |
| `RECUSADA` | — (terminal) | — |
| `CANCELADA_PELO_PACIENTE` | — (terminal) | — |
| `REALIZADA` | — (terminal, exceto gerar retorno) | — |

---

## 6. Contratos de API (endpoints esperados)

> Prefixo base: `/api/v1`

### Auth
| Método | Endpoint | Perfil | Descrição |
|--------|----------|--------|-----------|
| POST | `/auth/register` | Público | Cadastro de paciente |
| POST | `/auth/login` | Público | Login, retorna JWT |

### Médicos (consulta pública)
| Método | Endpoint | Perfil | Descrição |
|--------|----------|--------|-----------|
| GET | `/medicos` | Autenticado | Lista médicos ativos com especialidade |
| GET | `/medicos/{id}` | Autenticado | Detalhe do médico |
| GET | `/medicos/{id}/slots?data=YYYY-MM-DD` | Autenticado | Slots livres em uma data |

### Disponibilidade (médico gerencia a própria agenda)
| Método | Endpoint | Perfil | Descrição |
|--------|----------|--------|-----------|
| GET | `/medicos/disponibilidade` | MEDICO | Lista própria disponibilidade |
| POST | `/medicos/disponibilidade` | MEDICO | Cria faixa de disponibilidade |
| PUT | `/medicos/disponibilidade/{id}` | MEDICO | Atualiza faixa |
| DELETE | `/medicos/disponibilidade/{id}` | MEDICO | Desativa faixa |

### Consultas — Paciente
| Método | Endpoint | Perfil | Descrição |
|--------|----------|--------|-----------|
| POST | `/pacientes/consultas` | PACIENTE | Solicita consulta |
| GET | `/pacientes/consultas` | PACIENTE | Lista consultas do paciente |
| PATCH | `/pacientes/consultas/{id}/cancelar` | PACIENTE | Cancela consulta |
| POST | `/pacientes/consultas/{id}/retorno` | PACIENTE | Solicita retorno de consulta realizada |

### Consultas — Médico
| Método | Endpoint | Perfil | Descrição |
|--------|----------|--------|-----------|
| GET | `/medicos/consultas` | MEDICO | Lista consultas do médico (com filtros) |
| GET | `/medicos/consultas/pendentes` | MEDICO | Apenas `AGUARDANDO_APROVACAO` |
| PATCH | `/medicos/consultas/{id}/aprovar` | MEDICO | Aprova consulta |
| PATCH | `/medicos/consultas/{id}/recusar` | MEDICO | Recusa consulta (body: justificativa) |
| PATCH | `/medicos/consultas/{id}/realizar` | MEDICO | Marca como realizada |

---

## 7. Casos de Teste (TDD)

> Organize os testes por camada: **Unit** (service/domain) e **Integration** (repository + controller).

### 7.1 AutenticacaoService

| # | Cenário | Tipo | Resultado esperado |
|---|---------|------|--------------------|
| T01 | Cadastro com e-mail único e senha válida | Unit | Usuario criado, senha em BCrypt |
| T02 | Cadastro com e-mail duplicado | Unit | `EmailJaCadastradoException` |
| T03 | Cadastro com senha < 8 chars | Unit | `ValidationException` |
| T04 | Login com credenciais corretas | Unit | JWT retornado |
| T05 | Login com senha incorreta | Unit | `CredenciaisInvalidasException` |
| T06 | Login com e-mail inexistente | Unit | `CredenciaisInvalidasException` |

### 7.2 DisponibilidadeService

| # | Cenário | Tipo | Resultado esperado |
|---|---------|------|--------------------|
| T07 | Criar faixa válida (seg 08:00–12:00, 30min) | Unit | Faixa salva, ativo=true |
| T08 | Criar faixa com horaFim <= horaInicio | Unit | `HorarioInvalidoException` |
| T09 | Criar faixa que sobrepõe faixa existente do mesmo dia | Unit | `SobreposicaoDeHorariosException` |
| T10 | Desativar faixa existente | Unit | ativo=false, sem cancelar consultas |
| T11 | Outro médico tenta editar faixa alheia | Unit | `AcessoNegadoException` |
| T12 | Calcular slots livres para data sem consultas marcadas | Unit | Retorna todos os slots da faixa |
| T13 | Calcular slots com consulta APROVADA ocupando horário | Unit | Slot ocupado não aparece na lista |
| T14 | Calcular slots com consulta AGUARDANDO_APROVACAO | Unit | Slot bloqueado não aparece na lista |
| T15 | Data informada em `/slots` não possui disponibilidade para aquele dia da semana | Unit | Lista vazia |

### 7.3 ConsultaService — Paciente

| # | Cenário | Tipo | Resultado esperado |
|---|---------|------|--------------------|
| T16 | Paciente solicita slot livre válido | Unit | Consulta criada com `AGUARDANDO_APROVACAO` |
| T17 | Paciente solicita horário no passado | Unit | `HorarioNoPassadoException` |
| T18 | Paciente solicita slot já ocupado (conflito de médico) | Unit | `SlotIndisponivelException` |
| T19 | Paciente tenta solicitar sendo MEDICO | Unit | `PerfilNaoAutorizadoException` |
| T20 | Paciente já possui consulta no mesmo horário (conflito do paciente) | Unit | `ConflitoDeAgendaException` |
| T21 | Paciente cancela consulta AGUARDANDO_APROVACAO | Unit | Status → `CANCELADA_PELO_PACIENTE` |
| T22 | Paciente cancela consulta APROVADA | Unit | Status → `CANCELADA_PELO_PACIENTE` |
| T23 | Paciente tenta cancelar consulta REALIZADA | Unit | `TransicaoInvalidaException` |
| T24 | Paciente tenta cancelar consulta de outro paciente | Unit | `AcessoNegadoException` |

### 7.4 ConsultaService — Médico

| # | Cenário | Tipo | Resultado esperado |
|---|---------|------|--------------------|
| T25 | Médico aprova consulta AGUARDANDO_APROVACAO | Unit | Status → `APROVADA` |
| T26 | Médico tenta aprovar consulta já APROVADA | Unit | `TransicaoInvalidaException` |
| T27 | Médico recusa com justificativa | Unit | Status → `RECUSADA`, justificativa salva |
| T28 | Médico recusa sem justificativa | Unit | `JustificativaObrigatoriaException` |
| T29 | Médico tenta operar consulta de outro médico | Unit | `AcessoNegadoException` |
| T30 | Aprovação com consulta concorrente no mesmo horário | Unit | Concorrente → `RECUSADA` automaticamente |
| T31 | Médico marca APROVADA como REALIZADA após dataHora | Unit | Status → `REALIZADA` |
| T32 | Médico tenta marcar como REALIZADA antes da dataHora | Unit | `ConsultaNaoOcorreuAindaException` |

### 7.5 RetornoService

| # | Cenário | Tipo | Resultado esperado |
|---|---------|------|--------------------|
| T33 | Paciente solicita retorno de consulta REALIZADA | Unit | Retorno criado com `AGUARDANDO_APROVACAO` |
| T34 | Retorno de consulta com status != REALIZADA | Unit | `ConsultaNaoRealizadaException` |
| T35 | Retorno duplicado para a mesma consulta | Unit | `RetornoDuplicadoException` |
| T36 | Paciente tenta criar retorno de consulta de outro paciente | Unit | `AcessoNegadoException` |

### 7.6 Testes de Integração (Controller)

| # | Endpoint | Cenário | HTTP esperado |
|---|----------|---------|---------------|
| T37 | POST `/auth/register` | Dados válidos | 201 Created |
| T38 | POST `/auth/register` | E-mail duplicado | 409 Conflict |
| T39 | POST `/auth/login` | Credenciais corretas | 200 + JWT |
| T40 | POST `/auth/login` | Senha errada | 401 Unauthorized |
| T41 | GET `/medicos/{id}/slots` | Sem token | 401 Unauthorized |
| T42 | POST `/pacientes/consultas` | Slot livre, token PACIENTE | 201 Created |
| T43 | POST `/pacientes/consultas` | Token MEDICO | 403 Forbidden |
| T44 | PATCH `/medicos/consultas/{id}/aprovar` | Token PACIENTE | 403 Forbidden |
| T45 | PATCH `/medicos/consultas/{id}/recusar` | Sem justificativa | 400 Bad Request |

---

## 8. Modelo de Dados

```sql
-- Tabelas principais (MariaDB)

CREATE TABLE usuario (
    id          CHAR(36)     NOT NULL PRIMARY KEY,
    nome        VARCHAR(150) NOT NULL,
    email       VARCHAR(200) NOT NULL UNIQUE,
    senha       VARCHAR(255) NOT NULL,  -- BCrypt hash
    perfil      ENUM('PACIENTE','MEDICO') NOT NULL,
    ativo       TINYINT(1)   NOT NULL DEFAULT 1,
    criado_em   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE medico (
    id           CHAR(36)    NOT NULL PRIMARY KEY,
    crm          VARCHAR(20) NOT NULL UNIQUE,
    especialidade VARCHAR(100) NOT NULL,
    nome_completo VARCHAR(150) NOT NULL,
    CONSTRAINT fk_medico_usuario FOREIGN KEY (id) REFERENCES usuario(id)
);

CREATE TABLE disponibilidade_medico (
    id               CHAR(36)  NOT NULL PRIMARY KEY,
    medico_id        CHAR(36)  NOT NULL,
    dia_semana       ENUM('SEG','TER','QUA','QUI','SEX','SAB','DOM') NOT NULL,
    hora_inicio      TIME      NOT NULL,
    hora_fim         TIME      NOT NULL,
    duracao_slot_min INT       NOT NULL DEFAULT 30,
    ativo            TINYINT(1) NOT NULL DEFAULT 1,
    CONSTRAINT fk_disp_medico FOREIGN KEY (medico_id) REFERENCES medico(id)
);

CREATE TABLE consulta (
    id                    CHAR(36)     NOT NULL PRIMARY KEY,
    paciente_id           CHAR(36)     NOT NULL,
    medico_id             CHAR(36)     NOT NULL,
    data_hora             DATETIME     NOT NULL,
    status                ENUM(
        'AGUARDANDO_APROVACAO',
        'APROVADA',
        'RECUSADA',
        'CANCELADA_PELO_PACIENTE',
        'REALIZADA'
    ) NOT NULL DEFAULT 'AGUARDANDO_APROVACAO',
    tipo_consulta         ENUM('NORMAL','RETORNO') NOT NULL DEFAULT 'NORMAL',
    consulta_origem_id    CHAR(36)     NULL,
    justificativa_recusa  TEXT         NULL,
    criado_em             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                                                ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_consulta_paciente FOREIGN KEY (paciente_id) REFERENCES usuario(id),
    CONSTRAINT fk_consulta_medico   FOREIGN KEY (medico_id)   REFERENCES medico(id),
    CONSTRAINT fk_consulta_origem   FOREIGN KEY (consulta_origem_id) REFERENCES consulta(id),
    CONSTRAINT uq_retorno_origem    UNIQUE (consulta_origem_id)  -- um retorno por consulta
);

-- Índices para performance
CREATE INDEX idx_consulta_medico_datahora  ON consulta (medico_id, data_hora);
CREATE INDEX idx_consulta_paciente_status  ON consulta (paciente_id, status);
CREATE INDEX idx_consulta_status           ON consulta (status);
CREATE INDEX idx_disp_medico_dia           ON disponibilidade_medico (medico_id, dia_semana, ativo);
```

---

## 9. Docker Compose

```yaml
# docker-compose.yml
version: '3.9'

services:

  db:
    image: mariadb:11.3
    container_name: sam-db
    restart: unless-stopped
    environment:
      MARIADB_ROOT_PASSWORD: rootpass
      MARIADB_DATABASE: sam_db
      MARIADB_USER: sam_user
      MARIADB_PASSWORD: sam_pass
    ports:
      - "3306:3306"
    volumes:
      - sam_db_data:/var/lib/mysql
      - ./src/main/resources/db/init:/docker-entrypoint-initdb.d  # scripts SQL iniciais
    healthcheck:
      test: ["CMD", "healthcheck.sh", "--connect", "--innodb_initialized"]
      interval: 10s
      timeout: 5s
      retries: 5

  app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: sam-app
    restart: unless-stopped
    depends_on:
      db:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:mariadb://db:3306/sam_db
      SPRING_DATASOURCE_USERNAME: sam_user
      SPRING_DATASOURCE_PASSWORD: sam_pass
      SPRING_JPA_HIBERNATE_DDL_AUTO: validate        # Flyway cuida do schema
      SPRING_FLYWAY_ENABLED: "true"
      JWT_SECRET: ${JWT_SECRET:-troque_em_producao_por_uma_chave_forte}
      JWT_EXPIRATION_MS: 86400000                   # 24 horas
    ports:
      - "8080:8080"

volumes:
  sam_db_data:
```

```dockerfile
# Dockerfile (multi-stage)
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 10. Restrições Técnicas e Não-Funcionais

| Item | Decisão |
|------|---------|
| **Java** | 17 (LTS) |
| **Framework** | Spring Boot 3.x |
| **Persistência** | Spring Data JPA + Hibernate |
| **Banco** | MariaDB 11.x |
| **Migrations** | Flyway |
| **Autenticação** | Spring Security + JWT (jjwt) |
| **Testes** | JUnit 5, Mockito, Spring Boot Test, Testcontainers (integração) |
| **Build** | Maven |
| **Containers** | Docker + Docker Compose |
| **Validação** | Bean Validation (jakarta.validation) em todos os DTOs |
| **Paginação** | Todos os endpoints de listagem usam `Pageable` |
| **Erros** | `@ControllerAdvice` global com respostas padronizadas `{ timestamp, status, error, message }` |
| **UUIDs** | Todas as PKs são UUID v4 gerados pela aplicação |
| **Timezone** | Toda `dataHora` armazenada em UTC; conversão feita no frontend |

---

## 11. Fora do Escopo do MVP

Os itens abaixo foram identificados no fluxograma mas **não serão implementados no MVP**:

- Módulo ADMIN completo (cadastro de clínica, relatórios, usuários e perfis via interface)
- Notificações push/e-mail ao paciente sobre aprovação/recusa
- Recepcionista como ator com fila de atendimento presencial
- Cadastro de prioridade de atendimento (urgência, emergência, consulta comum)
- Pagamento (dinheiro, PIX, cartão, plano de saúde)
- Encaixe manual de pacientes pela recepção
- Multi-tenancy (múltiplas clínicas)
- Upload de documentos ou prescrições

---

## 12. att — Atualizações de Escopo

> Esta seção registra as regras de negócio acrescentadas após a definição inicial do MVP v1.0.

---

### 12.1 Check-in Automático

#### Visão geral
O check-in automático permite que o paciente informe ao médico sua presença no dia e horário do atendimento. O médico, por sua vez, confirma se o paciente pode ser atendido imediatamente ou justifica a espera.

#### Entidade `CheckIn`

| Campo | Tipo | Regra |
|-------|------|-------|
| `id` | UUID | PK gerado automaticamente |
| `consultaId` | UUID | FK → `Consulta`; único (um check-in por consulta) |
| `pacienteId` | UUID | FK → `Usuario` (perfil PACIENTE) |
| `realizadoEm` | LocalDateTime | Gerado automaticamente no momento do check-in |
| `statusCheckin` | Enum(`AGUARDANDO_CONFIRMACAO`, `PODE_ENTRAR`, `AGUARDAR`) | Default `AGUARDANDO_CONFIRMACAO` |
| `justificativaEspera` | String | Obrigatório se `statusCheckin = AGUARDAR`; null nos demais |
| `respondidoEm` | LocalDateTime | Preenchido quando o médico responde |

#### Regras de Negócio

**RN-CHKIN-01:** O check-in só pode ser realizado pelo paciente titular da consulta.

**RN-CHKIN-02:** O check-in só é permitido para consultas com status `APROVADA`.

**RN-CHKIN-03:** O check-in só pode ser realizado no **dia** da consulta. Tentativas em datas anteriores ou posteriores retornam `422 Unprocessable Entity`.

**RN-CHKIN-04:** A janela de check-in é de **30 minutos antes** até o **horário exato** da consulta. Fora dessa janela o sistema rejeita a operação.
> *Valor configurável via propriedade `sam.checkin.janela-minutos` (default: 30).*

**RN-CHKIN-05:** Cada consulta admite **no máximo um** check-in. Tentativa de segundo check-in retorna `409 Conflict`.

**RN-CHKIN-06:** Após o check-in do paciente, o médico recebe uma notificação em tempo real (polling ou WebSocket) indicando que o paciente está presente.

**RN-CHKIN-07:** O médico responde ao check-in com uma das ações:
- **`PODE_ENTRAR`** — paciente autorizado a entrar; nenhum campo adicional obrigatório.
- **`AGUARDAR`** — paciente deve aguardar; o campo `justificativaEspera` é **obrigatório** e deve ter no mínimo 10 caracteres.

**RN-CHKIN-08:** Apenas o médico destinatário da consulta pode responder ao check-in.

**RN-CHKIN-09:** O médico só pode responder a um check-in com status `AGUARDANDO_CONFIRMACAO`. Tentativa de resposta a check-in já respondido retorna `409 Conflict`.

**RN-CHKIN-10:** Consultas que chegam ao horário agendado sem check-in registrado não são afetadas automaticamente — o fluxo de realização segue as regras existentes (RN-RET-06).

#### Endpoints — Check-in

> Prefixo base: `/api/v1`

| Método | Endpoint | Perfil | Descrição |
|--------|----------|--------|-----------|
| POST | `/pacientes/consultas/{id}/checkin` | PACIENTE | Realiza check-in na consulta |
| GET | `/medicos/consultas/{id}/checkin` | MEDICO | Consulta o status do check-in |
| PATCH | `/medicos/consultas/{id}/checkin/confirmar` | MEDICO | Responde ao check-in (`PODE_ENTRAR` ou `AGUARDAR`) |

#### Casos de Teste — CheckInService

| # | Cenário | Tipo | Resultado esperado |
|---|---------|------|--------------------|
| T46 | Paciente faz check-in dentro da janela permitida | Unit | `CheckIn` criado com `AGUARDANDO_CONFIRMACAO` |
| T47 | Paciente faz check-in fora da janela (muito cedo) | Unit | `CheckinForaDaJanelaException` |
| T48 | Paciente faz check-in em consulta não `APROVADA` | Unit | `StatusConsultaInvalidoException` |
| T49 | Paciente faz check-in duplicado | Unit | `CheckinDuplicadoException` |
| T50 | Paciente faz check-in em consulta de outro paciente | Unit | `AcessoNegadoException` |
| T51 | Médico responde `PODE_ENTRAR` a check-in pendente | Unit | `statusCheckin` → `PODE_ENTRAR`, `respondidoEm` preenchido |
| T52 | Médico responde `AGUARDAR` com justificativa válida | Unit | `statusCheckin` → `AGUARDAR`, justificativa salva |
| T53 | Médico responde `AGUARDAR` sem justificativa | Unit | `JustificativaObrigatoriaException` |
| T54 | Médico tenta responder a check-in já respondido | Unit | `CheckinJaRespondidoException` |
| T55 | Médico tenta responder check-in de consulta de outro médico | Unit | `AcessoNegadoException` |

---

### 12.2 Cancelamento de Agenda pelo Médico

#### Visão geral
O médico pode cancelar antecipadamente um bloco de atendimentos — um dia completo, um turno (faixa de horário) ou um slot específico — e o sistema notifica automaticamente todos os pacientes impactados.

#### Entidade `CancelamentoAgenda`

| Campo | Tipo | Regra |
|-------|------|-------|
| `id` | UUID | PK gerado automaticamente |
| `medicoId` | UUID | FK → `Medico` |
| `tipoCancelamento` | Enum(`DIA_COMPLETO`, `TURNO`, `HORARIO_ESPECIFICO`) | Obrigatório |
| `data` | LocalDate | Obrigatório; deve ser futura |
| `horaInicio` | LocalTime | Obrigatório se `tipoCancelamento` = `TURNO` ou `HORARIO_ESPECIFICO` |
| `horaFim` | LocalTime | Obrigatório se `tipoCancelamento` = `TURNO`; deve ser > `horaInicio` |
| `motivoCancelamento` | String | Obrigatório, mín. 10 caracteres |
| `criadoEm` | LocalDateTime | Gerado automaticamente |

#### Regras de Negócio

**RN-CAGD-01:** Somente o próprio médico pode registrar um cancelamento de agenda.

**RN-CAGD-02:** A `data` do cancelamento deve ser **estritamente futura** (não é permitido cancelar o dia atual nem datas passadas).

**RN-CAGD-03:** O `motivoCancelamento` é obrigatório e deve ter no mínimo 10 caracteres.

**RN-CAGD-04:** Para `tipoCancelamento = TURNO`, os campos `horaInicio` e `horaFim` são obrigatórios e `horaFim` deve ser maior que `horaInicio`.

**RN-CAGD-05:** Para `tipoCancelamento = HORARIO_ESPECIFICO`, apenas `horaInicio` é obrigatório (representa o horário exato do slot a cancelar).

**RN-CAGD-06:** O sistema identifica todas as consultas do médico com status `APROVADA` ou `AGUARDANDO_APROVACAO` que se enquadram no escopo do cancelamento:
- `DIA_COMPLETO` — qualquer consulta na `data` informada.
- `TURNO` — consultas na `data` cuja `dataHora` esteja dentro do intervalo `[horaInicio, horaFim)`.
- `HORARIO_ESPECIFICO` — consultas na `data` cujo horário corresponda exatamente ao slot indicado.

**RN-CAGD-07:** Todas as consultas identificadas pela regra anterior têm seu status alterado para `CANCELADA_PELO_MEDICO`.
> **Atenção:** adicionar `CANCELADA_PELO_MEDICO` à Enum `status` da entidade `Consulta` e atualizar a máquina de estados (seção 5) na próxima revisão do documento principal.

**RN-CAGD-08:** Após o cancelamento em massa, o sistema dispara uma notificação para cada paciente impactado contendo:
- Nome do médico.
- Data e horário original da consulta cancelada.
- Motivo do cancelamento informado pelo médico.
- Orientação para reagendar.

**RN-CAGD-09:** Consultas já com status `REALIZADA`, `RECUSADA` ou `CANCELADA_PELO_PACIENTE` **não são afetadas** pelo cancelamento de agenda.

**RN-CAGD-10:** O cancelamento de agenda **não desativa** a disponibilidade do médico (`DisponibilidadeMedico`). Os slots continuarão a ser exibidos para futuras datas, a não ser que o médico desative a faixa manualmente.

**RN-CAGD-11:** Não é possível registrar dois cancelamentos do mesmo médico que se sobreponham em escopo (mesma data + intervalo de horário já coberto). O sistema retorna `409 Conflict` com indicação do conflito.

#### Endpoints — Cancelamento de Agenda

> Prefixo base: `/api/v1`

| Método | Endpoint | Perfil | Descrição |
|--------|----------|--------|-----------|
| POST | `/medicos/agenda/cancelamentos` | MEDICO | Registra cancelamento (dia, turno ou slot) |
| GET | `/medicos/agenda/cancelamentos` | MEDICO | Lista cancelamentos futuros do médico |
| DELETE | `/medicos/agenda/cancelamentos/{id}` | MEDICO | Reverte um cancelamento (reativa consultas que estavam `CANCELADA_PELO_MEDICO` e reenvia notificação aos pacientes) |

#### Casos de Teste — CancelamentoAgendaService

| # | Cenário | Tipo | Resultado esperado |
|---|---------|------|--------------------|
| T56 | Médico cancela dia completo com consultas `APROVADA` e `AGUARDANDO_APROVACAO` | Unit | Todas migram para `CANCELADA_PELO_MEDICO`; notificação disparada para cada paciente |
| T57 | Médico cancela turno; consultas fora do turno não são afetadas | Unit | Apenas consultas no intervalo mudam de status |
| T58 | Médico cancela slot específico ocupado | Unit | Consulta do slot → `CANCELADA_PELO_MEDICO`; notificação disparada |
| T59 | Cancelamento para data passada ou dia atual | Unit | `DataCancelamentoInvalidaException` |
| T60 | Cancelamento sem motivo ou motivo < 10 chars | Unit | `ValidationException` |
| T61 | Cancelamento de turno com `horaFim <= horaInicio` | Unit | `HorarioInvalidoException` |
| T62 | Cancelamento sobreposto a cancelamento já existente | Unit | `SobreposicaoDeCancelamentoException` |
| T63 | Outro médico tenta registrar cancelamento na agenda alheia | Unit | `AcessoNegadoException` |
| T64 | Cancelamento revertido via DELETE; consultas voltam ao status anterior | Unit | Status restaurado; notificação de reagendamento enviada |
| T65 | Cancelamento em dia sem consultas agendadas | Unit | `CancelamentoAgenda` salvo; nenhuma notificação disparada |

---

*Documento elaborado como base para desenvolvimento TDD — deve ser atualizado a cada incremento de escopo.*