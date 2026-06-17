# FinanceDash

FinanceDash é uma aplicação de gestão financeira para freelancers, MEIs e pequenos negócios controlarem receitas, despesas, saldo mensal, categorias e metas — com o **FinanceDash Radar**, um copiloto financeiro que projeta o resultado do mês, alerta riscos e responde perguntas em linguagem natural.

> 📖 **Documentação técnica completa:** [`docs/DOCUMENTACAO.md`](docs/DOCUMENTACAO.md) — arquitetura, modelo de dados, referência completa da API, segurança, deploy e diagramas.

## Objetivo

Oferecer uma API REST e um frontend para registrar lançamentos, organizar categorias, definir metas e consultar um resumo mensal — e, sobre essa base, um copiloto (Radar) que transforma os números em projeções, alertas e ações práticas, sempre calculadas de forma determinística no backend.

## Documentação

- 📘 [Documentação técnica](docs/DOCUMENTACAO.md) — arquitetura, modelo de dados, referência da API e diagramas
- 📗 [Manual do usuário](docs/manual-do-usuario.html) — guia do cliente final (abra no navegador)
- 🔒 [Política de Privacidade (LGPD)](docs/PRIVACIDADE.md)
- 🤝 [Guia de contribuição](CONTRIBUTING.md) — setup, padrões e fluxo de trabalho
- 🧭 [Decisões de arquitetura (ADRs)](docs/adr/) — por que o projeto é como é
- 🚀 [Página de vendas (one-pager)](docs/one-pager.html) — material de divulgação

## Stack utilizada

- Java 21
- Spring Boot 3
- Maven
- Spring Web
- Spring Security (JWT, stateless)
- Spring Data JPA
- Bean Validation
- Spring AI (copiloto Radar — starter OpenAI)
- PostgreSQL
- Docker Compose
- Swagger/OpenAPI via Springdoc
- JUnit 5
- Mockito
- Playwright (E2E)

## Funcionalidades

### Autenticação e multiusuário

- Cadastro e login com email e senha
- Senha criptografada com BCrypt
- Sessão via JWT com refresh token rotativo
- Recuperação de senha e verificação de email por link
- Rate limit de login por email
- Dados financeiros isolados por usuário autenticado
- Conta demo criada automaticamente: `demo@financedash.com` / `demo12345`
- Base para plano, trial e status de assinatura
- Área de conta no frontend com plano, status, trial, email verificado e checkout Pro

### Lançamentos financeiros

- Criar receitas e despesas
- Listar lançamentos
- Buscar lançamento por ID
- Atualizar lançamento
- Remover lançamento
- Validações de campos obrigatórios e valor maior que zero

### Dashboard mensal

- Total de receitas
- Total de despesas
- Saldo mensal
- Quantidade de receitas e despesas
- Despesas agrupadas por categoria
- Receitas agrupadas por categoria

### Categorias

- Criar categorias por tipo de lançamento
- Listar categorias

### Metas financeiras

- Criar metas mensais
- Listar metas
- Listar metas por mês e ano

### FinanceDash Radar (copiloto financeiro)

- Projeção do saldo do fim do mês ("vou fechar positivo?")
- Safe-to-spend: quanto é seguro gastar até o fim do mês (total e por dia)
- Recebíveis atrasados e caixa travado por cliente
- Gap de receita/freela para a meta e preço mínimo do próximo projeto
- Análise de cortes para alcançar a meta
- Alertas automáticos (mês negativo, abaixo da meta, cliente atrasado, reserva em risco) com agendamento diário
- Copiloto em linguagem natural (`POST /api/v1/radar/ask`) via Spring AI — **a IA nunca calcula valores; todo número vem do motor determinístico no backend**
- Resumo proativo por e-mail (digest) configurável
- Perfil financeiro do usuário (reserva, custo fixo, horas faturáveis, imposto, margem, meta) que alimenta os cálculos

### Base para assinatura

- Usuários possuem `plan`, `subscriptionStatus`, `trialEndsAt` e `subscriptionEndsAt`
- O MVP cria contas em trial automaticamente
- APIs financeiras retornam `402 SUBSCRIPTION_REQUIRED` quando o trial expira ou a assinatura fica `PAST_DUE`/`CANCELED`
- A tela de conta mostra o bloqueio e mantém o usuário logado para visualizar o status
- Checkout Pro via Asaas (`POST /api/v1/billing/checkout/pro`) com webhook para ativar/cancelar assinatura

### Conformidade (LGPD)

- Página pública em `/privacy.html`
- Logs de erro em endpoints de auth não incluem stack trace completo
- Emails transacionais (verificação e reset) registrados em log sem conteúdo sensível quando `MAIL_ENABLED=false`

## Endpoints

Todos os endpoints financeiros exigem o header:

```text
Authorization: Bearer {token}
```

### Auth

| Método | Endpoint | Descrição |
| --- | --- | --- |
| POST | `/api/v1/auth/register` | Cria uma conta e retorna JWT + refresh token |
| POST | `/api/v1/auth/login` | Autentica e retorna JWT + refresh token |
| POST | `/api/v1/auth/refresh` | Renova JWT usando refresh token |
| POST | `/api/v1/auth/forgot-password` | Solicita link de recuperação de senha |
| POST | `/api/v1/auth/reset-password` | Redefine senha com token |
| GET | `/api/v1/auth/verify-email?token=` | Confirma email |
| GET | `/api/v1/auth/me` | Retorna o usuário autenticado |

### Billing

| Método | Endpoint | Descrição |
| --- | --- | --- |
| POST | `/api/v1/billing/checkout/pro` | Cria checkout/assinatura Pro no Asaas |
| POST | `/api/v1/billing/webhook` | Webhook do Asaas (header `asaas-access-token`) |

### Transactions

| Método | Endpoint | Descrição |
| --- | --- | --- |
| POST | `/api/v1/transactions` | Cria um lançamento financeiro |
| GET | `/api/v1/transactions?month=7&year=2026&type=EXPENSE&categoryId={uuid}&page=0&size=20` | Lista lançamentos financeiros com filtros e paginação |
| GET | `/api/v1/transactions/{id}` | Busca lançamento por ID |
| PUT | `/api/v1/transactions/{id}` | Atualiza lançamento |
| DELETE | `/api/v1/transactions/{id}` | Remove lançamento |

### Dashboard

| Método | Endpoint | Descrição |
| --- | --- | --- |
| GET | `/api/v1/dashboard/monthly?month=7&year=2026` | Retorna dashboard mensal |

### Categories

| Método | Endpoint | Descrição |
| --- | --- | --- |
| POST | `/api/v1/categories` | Cria categoria |
| GET | `/api/v1/categories` | Lista categorias |
| GET | `/api/v1/categories/{id}` | Busca categoria por ID |
| PUT | `/api/v1/categories/{id}` | Atualiza categoria |
| DELETE | `/api/v1/categories/{id}` | Remove categoria com soft delete |

### Goals

| Método | Endpoint | Descrição |
| --- | --- | --- |
| POST | `/api/v1/goals` | Cria meta financeira |
| GET | `/api/v1/goals` | Lista metas financeiras com progresso calculado |
| GET | `/api/v1/goals/{id}` | Busca meta financeira por ID |
| PUT | `/api/v1/goals/{id}` | Atualiza meta financeira |
| DELETE | `/api/v1/goals/{id}` | Remove meta financeira |
| GET | `/api/v1/goals/monthly?month=7&year=2026` | Lista metas por mês e ano |

### User Settings

| Método | Endpoint | Descrição |
| --- | --- | --- |
| GET | `/api/v1/user-settings` | Perfil financeiro (reserva, custo fixo, horas, imposto, margem, meta) |
| PUT | `/api/v1/user-settings` | Atualiza o perfil financeiro |

### Radar

| Método | Endpoint | Descrição |
| --- | --- | --- |
| GET | `/api/v1/radar/month-projection` | Projeção do saldo do mês |
| GET | `/api/v1/radar/safe-to-spend` | Quanto é seguro gastar (total e por dia) |
| GET | `/api/v1/radar/overdue-receivables` | Recebíveis atrasados e caixa travado |
| GET | `/api/v1/radar/freelance-gap` | Falta de receita para a meta e horas extras |
| GET | `/api/v1/radar/minimum-project-price?estimatedHours=20` | Preço mínimo do próximo projeto |
| GET | `/api/v1/radar/cut-analysis` | O que cortar para bater a meta |
| POST | `/api/v1/radar/ask` | Copiloto IA (pergunta em linguagem natural) |
| GET | `/api/v1/radar/alerts?unreadOnly=false` | Lista alertas do Radar |
| POST | `/api/v1/radar/alerts/{id}/read` | Marca alerta como lido |

## Exemplos de payload

### Criar transaction

```json
{
  "description": "Projeto website",
  "amount": 2500.00,
  "type": "INCOME",
  "categoryId": "00000000-0000-0000-0000-000000000001",
  "transactionDate": "2026-07-10",
  "paymentMethod": "PIX",
  "notes": "Cliente Sanoli"
}
```

### Atualizar transaction

```json
{
  "description": "Assinatura ferramenta",
  "amount": 89.90,
  "type": "EXPENSE",
  "categoryId": "00000000-0000-0000-0000-000000000002",
  "transactionDate": "2026-07-11",
  "paymentMethod": "Cartão",
  "notes": "Plano mensal"
}
```

### Resposta do dashboard mensal

```json
{
  "month": 7,
  "year": 2026,
  "startDate": "2026-07-01",
  "endDate": "2026-07-31",
  "totalIncome": 7200.00,
  "totalExpense": 4276.98,
  "balance": 2923.02,
  "transactionCount": 21,
  "incomeCount": 3,
  "expenseCount": 18,
  "expensesByCategory": [
    {
      "categoryId": "00000000-0000-0000-0000-000000000002",
      "categoryName": "Cartao",
      "categoryColor": "#EF4444",
      "amount": 2193.00,
      "percentage": 51.27
    }
  ],
  "incomesByCategory": [
    {
      "categoryId": "00000000-0000-0000-0000-000000000001",
      "categoryName": "Salario",
      "categoryColor": "#16A34A",
      "amount": 7200.00,
      "percentage": 100.00
    }
  ]
}
```

### Criar category

```json
{
  "name": "Cartão",
  "type": "EXPENSE",
  "color": "#EF4444"
}
```

### Criar goal

```json
{
  "title": "Economizar para reserva",
  "month": 7,
  "year": 2026,
  "targetAmount": 3000.00,
  "type": "SAVINGS_TARGET",
  "categoryId": null
}
```

Tipos de meta disponíveis:

- `INCOME_TARGET`: meta de receita. Pode ser geral ou filtrada por uma categoria `INCOME`.
- `EXPENSE_LIMIT`: limite de despesa. Pode ser geral ou filtrada por uma categoria `EXPENSE`.
- `SAVINGS_TARGET`: meta de economia baseada em `receitas - despesas`. Não usa `categoryId`.

A resposta de metas retorna progresso calculado dinamicamente:

```json
{
  "id": "00000000-0000-0000-0000-000000000003",
  "title": "Economizar para reserva",
  "month": 7,
  "year": 2026,
  "targetAmount": 3000.00,
  "type": "SAVINGS_TARGET",
  "categoryId": null,
  "categoryName": null,
  "categoryColor": null,
  "currentAmount": 1500.00,
  "progressPercentage": 50.00,
  "createdAt": "2026-07-01T10:00:00"
}
```

## Tratamento de erros

Os erros seguem o formato padrão:

```json
{
  "timestamp": "2026-07-10T10:00:00",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Mensagem do erro",
  "path": "/api/v1/transactions"
}
```

Tipos tratados:

- `VALIDATION_ERROR`: erro de validação ou corpo inválido
- `RESOURCE_NOT_FOUND`: recurso não encontrado
- `INTERNAL_SERVER_ERROR`: erro inesperado

## Como rodar localmente

Pré-requisitos:

- Java 21
- Maven
- PostgreSQL local rodando na porta `5432`

Crie um banco PostgreSQL com:

- database: `financedash`
- user: `financedash`
- password: `financedash`

Depois execute:

```powershell
cd finance-dash
mvn spring-boot:run
```

A aplicação ficará disponível em:

```text
http://localhost:8080
```

Variáveis de ambiente suportadas:

```powershell
$env:DATABASE_URL="jdbc:postgresql://localhost:5432/financedash"
$env:DATABASE_USERNAME="financedash"
$env:DATABASE_PASSWORD="financedash"
$env:SERVER_PORT="8080"
$env:JWT_SECRET="troque-este-segredo-em-producao"
$env:JWT_EXPIRATION_SECONDS="86400"
$env:PUBLIC_BASE_URL="http://localhost:8080"
$env:ASAAS_ENABLED="false"
$env:ASAAS_API_KEY=""
$env:ASAAS_WEBHOOK_TOKEN=""
$env:MAIL_ENABLED="false"
# IA do Radar (opcional): "none" mantém o modo determinístico; defina o provider e a chave para ativar /radar/ask
$env:SPRING_AI_CHAT_MODEL="none"
$env:OPENAI_API_KEY=""
$env:OPENAI_MODEL="gpt-4o-mini"
```

### Asaas (checkout Pro)

Para habilitar cobrança real no sandbox ou produção:

```powershell
$env:ASAAS_ENABLED="true"
$env:ASAAS_API_KEY="sua-chave-asaas"
$env:ASAAS_BASE_URL="https://api-sandbox.asaas.com/v3"
$env:ASAAS_WEBHOOK_TOKEN="token-configurado-no-asaas"
$env:PUBLIC_BASE_URL="https://seu-dominio.com"
```

Configure no painel Asaas um webhook apontando para `POST /api/v1/billing/webhook` com o header `asaas-access-token`.

## Como rodar com Docker Compose

Pré-requisitos:

- Docker
- Docker Compose

Execute:

```powershell
cd finance-dash
docker compose up --build
```

Serviços expostos:

- Aplicação: `http://localhost:8080`
- PostgreSQL: `localhost:5432`

Para parar:

```powershell
docker compose down
```

Para parar e remover o volume do banco:

```powershell
docker compose down -v
```

## Deploy no Railway

O projeto pode ser publicado no Railway usando o `Dockerfile` da raiz. Crie um serviço PostgreSQL no Railway e configure as variáveis do serviço da aplicação.

Opção recomendada, usando a URL padrão do Railway:

```text
DATABASE_URL=${{Postgres.DATABASE_URL}}
SERVER_PORT=${{PORT}}
```

O app converte automaticamente URLs `postgres://` ou `postgresql://` para o formato JDBC exigido pelo Spring (`jdbc:postgresql://...`). Se preferir evitar montagem manual de URL, também pode configurar as partes do PostgreSQL:

```text
PGHOST=${{Postgres.PGHOST}}
PGPORT=${{Postgres.PGPORT}}
PGDATABASE=${{Postgres.PGDATABASE}}
PGUSER=${{Postgres.PGUSER}}
PGPASSWORD=${{Postgres.PGPASSWORD}}
SERVER_PORT=${{PORT}}
```

Evite quebrar a URL em múltiplas linhas ou deixar um `$` isolado no final. Se o deploy falhar com `database "$" does not exist`, remova a `DATABASE_URL` manual quebrada e use `DATABASE_URL=${{Postgres.DATABASE_URL}}` ou as variáveis `PG*` acima. O app também tenta corrigir `jdbc:postgresql://.../$` usando o banco padrão `railway`.

Se o deploy falhar com erro de Hibernate `Unable to determine Dialect`, normalmente a variável `DATABASE_URL` está ausente, vazia ou apontando para uma referência de serviço incorreta no Railway.

## Swagger/OpenAPI

A documentação da API fica disponível em:

```text
http://localhost:8080/swagger-ui.html
```

O JSON OpenAPI fica disponível em:

```text
http://localhost:8080/v3/api-docs
```

## Frontend

O projeto possui um frontend estático servido pelo próprio Spring Boot, com as visões Dashboard e Radar.

Com a aplicação rodando, acesse:

```text
http://localhost:8080/
```

Estrutura dos arquivos:

```text
src/main/resources/static/
├── index.html
├── css/
│   └── styles.css
└── js/
    ├── api.js
    ├── app.js
    ├── auth.js
    ├── categories.js
    ├── dashboard.js
    ├── goals.js
    ├── radar.js
    ├── settings.js
    └── transactions.js
```

Funcionalidades iniciais:

- login, cadastro, logout, recuperação de senha e verificação de email;
- refresh token automático no frontend;
- envio automático do token JWT nas chamadas da API;
- isolamento visual e funcional dos dados por usuário autenticado;
- seção de conta/billing com plano, trial, status, checkout Pro e aviso de bloqueio;
- link para política de privacidade (`/privacy.html`);
- cards de receitas, despesas e saldo mensal;
- gráficos com Chart.js para receitas/despesas por categoria;
- formulário para criar e editar lançamentos;
- tabela de lançamentos recentes com filtros por tipo, categoria, tamanho e ordenação;
- CRUD visual de categorias;
- CRUD visual de metas mensais com barra de progresso;
- aba Radar: projeção do mês, safe-to-spend, alertas e chat do copiloto;
- tela de perfil financeiro (configurações do Radar);
- modais (confirmação, edição, gráfico) com fechamento por Esc e restauração de foco;
- confirmação visual para exclusões e estados de loading em ações principais.

## Testes

Execute:

```powershell
cd finance-dash
mvn test
```

Para os testes E2E do frontend com Playwright, instale os navegadores uma vez e rode com a aplicação disponível em `http://localhost:8080`:

```powershell
npx playwright install
npm run test:e2e
```

## Build

Execute:

```powershell
cd finance-dash
mvn clean package
```

## Roadmap futuro

- Importação CSV
- Exportação PDF
- Envio real de email (SMTP) em produção
- Painel admin e métricas de assinatura

