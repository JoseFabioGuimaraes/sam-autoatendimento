# SAM — Sistema de Agendamento Médico

O **SAM** é uma plataforma moderna e completa de agendamento de consultas médicas e gestão de filas de atendimento em tempo real. Desenvolvido com foco em excelente usabilidade, ele conecta pacientes e médicos de maneira fluida e automatizada.

---

## 🚀 Funcionalidades Principais

### 👤 Para Pacientes
- **Busca de Especialistas**: Filtragem dinâmica de médicos por especialidade.
- **Agendamento Prático**: Escolha de slots de horários dinâmicos e solicitação de consultas normais ou retornos.
- **Check-in Automático**: Botão de check-in liberado automaticamente no dia da consulta, entre 30 minutos antes e o horário de início da consulta.
- **Alertas em Tempo Real**: Notificações instantâneas (via polling rápido) assim que a entrada no consultório for autorizada pelo médico.

### 🥼 Para Médicos
- **Painel de Atendimento (Fila)**: Aprovação e recusa (com justificativa obrigatória) de agendamentos pendentes.
- **Autorização de Entrada**: Opção de mandar o paciente aguardar (com justificativa) ou dar permissão de entrada imediata ("Pode Entrar").
- **Calendário de Horários**: Um calendário interativo mensal onde o médico pode navegar pelos dias e gerenciar todos os agendamentos diretamente no calendário.
- **Histórico & Recusas**: Visualização rápida de todas as consultas finalizadas (`REALIZADA`), recusadas (exibindo o respectivo motivo) ou canceladas.
- **Gerenciador de Disponibilidade**: Interface dedicada para configurar slots e horários de trabalho recorrentes por dia da semana.

---

## 🛠️ Arquitetura do Projeto

- **Frontend**: Single Page Application construída com **React**, **Vite** e **Lucide-react** para ícones. Design moderno e responsivo com CSS Custom Properties.
- **Backend**: API RESTful robusta desenvolvida com **Java 17**, **Spring Boot**, **Spring Security (JWT)** e **Spring Data JPA**.
- **Banco de Dados**: **MariaDB 11.3** com migrations gerenciadas pelo **Flyway**.
- **Infraestrutura**: Orquestração completa de containers com **Docker** e **Docker Compose**.

---

## 📦 Como Rodar o Projeto (Recomendado: Docker)

### Pré-requisitos
Certifique-se de ter instalado em sua máquina:
- [Docker](https://docs.docker.com/get-docker/)
- [Docker Compose](https://docs.docker.com/compose/install/)

### Passo a Passo

1. **Clonar ou acessar o diretório do projeto**:
   ```bash
   cd sam/
   ```

2. **Subir os containers do Docker**:
   O comando abaixo irá baixar as imagens base, compilar o backend Spring Boot, buildar o frontend React e subir os serviços de banco de dados, backend e frontend de forma integrada:
   ```bash
   docker compose up -d --build
   ```

3. **Verificar os Status**:
   Certifique-se de que todos os containers estão saudáveis e rodando:
   ```bash
   docker compose ps
   ```

4. **Acessar as Aplicações**:
   - **Frontend (Paciente/Médico)**: [http://localhost:3000](http://localhost:3000)
   - **Backend API**: [http://localhost:8080](http://localhost:8080)

---

## 💻 Desenvolvimento Local (Sem Docker)

Caso prefira rodar os serviços localmente para fins de debug rápido:

### 1. Banco de Dados
Você precisará de uma instância ativa do MariaDB/MySQL rodando na porta `3306` com as seguintes credenciais padrão (configuradas no `.env` do projeto):
- **Database**: `sam_db`
- **User**: `sam_user`
- **Password**: `sam_pass`

### 2. Backend (Java & Maven)
- **Pré-requisitos**: JDK 17 e Maven instalados.
- **Executar**:
  ```bash
  cd backend/
  mvn spring-boot:run
  ```
- **Rodar os Testes**:
  ```bash
  mvn test
  ```

### 3. Frontend (React & Node.js)
- **Pré-requisitos**: Node.js v20+ e npm.
- **Executar**:
  ```bash
  cd frontend/
  npm install
  npm run dev
  ```
- O frontend dev server subirá em `http://localhost:5173`.

---

## 🔒 Regras de Negócio Críticas Implementadas

- **Validação de Conflito de Agenda**: O backend impede estritamente que um mesmo médico receba mais de uma consulta ou que um mesmo paciente marque mais de uma consulta no mesmo dia e horário.
- **Regra de Entrada Única (Médico Ocupado)**: O médico não pode autorizar a entrada de um paciente (`PODE_ENTRAR`) se ele já estiver em atendimento (ou seja, se já houver um paciente com status `PODE_ENTRAR` em uma consulta ativa `APROVADA` no consultório).
