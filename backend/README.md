# SAM — Backend

Backend do **Sistema de Agendamento Médico** construído com Spring Boot 3.4.x + Java 17.

## Pré-requisitos

- Java 17+
- Maven 3.9+
- Docker + Docker Compose

## Executando Localmente

### 1. Subir apenas o banco de dados

```bash
docker compose up db -d
```

### 2. Rodar a aplicação

```bash
./mvnw spring-boot:run
```

A aplicação inicia em `http://localhost:8080`.

### 3. Swagger UI

Acesse: `http://localhost:8080/swagger-ui.html`

## Executando com Docker Compose (completo)

```bash
cp .env.example .env
# Edite .env com suas chaves
docker compose up --build
```

## Executando os Testes

```bash
./mvnw test
```

## Estrutura de Pacotes

```
br.com.sam
├── auth/         # Autenticação JWT, cadastro e login
├── usuario/      # Entidade base de usuário
├── medico/       # Médicos e disponibilidade de agenda
├── disponibilidade/ # Faixas de horário e cálculo de slots
├── consulta/     # Solicitação, aprovação e retorno de consultas
└── shared/       # Exception handler, OpenAPI, configurações
```

## Endpoints Principais

| Método | Endpoint | Perfil |
|--------|----------|--------|
| POST | `/api/v1/auth/register` | Público |
| POST | `/api/v1/auth/login` | Público |
| GET | `/api/v1/medicos` | Autenticado |
| GET | `/api/v1/medicos/{id}/slots?data=YYYY-MM-DD` | Autenticado |
| POST | `/api/v1/medicos/disponibilidade` | MEDICO |
| POST | `/api/v1/pacientes/consultas` | PACIENTE |
| PATCH | `/api/v1/medicos/consultas/{id}/aprovar` | MEDICO |

## Seed de Dados

O Flyway aplica automaticamente:
- `V1__create_tables.sql` — schema completo
- `V2__insert_seed_data.sql` — 2 médicos com disponibilidade

**Credenciais dos médicos seed:** `Senha@123`
