# FinanceDash

FinanceDash é um MVP de dashboard financeiro para freelancers, MEIs e pequenos negócios controlarem receitas, despesas, saldo mensal, categorias e metas financeiras.

## Objetivo

Oferecer uma API REST simples e extensível para registrar lançamentos financeiros, organizar categorias, definir metas mensais e consultar um resumo financeiro mensal com totais, saldo, contagens e agrupamentos por categoria.

## Stack utilizada

- Java 21
- Spring Boot 3
- Maven
- Spring Web
- Spring Data JPA
- Bean Validation
- PostgreSQL
- Docker Compose
- Swagger/OpenAPI via Springdoc
- JUnit 5
- Mockito

## Funcionalidades

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

## Endpoints

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
cd C:\Users\Daniel\projetos\finance-dash
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
```

## Como rodar com Docker Compose

Pré-requisitos:

- Docker
- Docker Compose

Execute:

```powershell
cd C:\Users\Daniel\projetos\finance-dash
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

## Swagger/OpenAPI

A documentação da API fica disponível em:

```text
http://localhost:8080/swagger-ui.html
```

O JSON OpenAPI fica disponível em:

```text
http://localhost:8080/v3/api-docs
```

## Frontend MVP

O projeto possui um frontend estático inicial servido pelo próprio Spring Boot.

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
    ├── categories.js
    ├── dashboard.js
    ├── goals.js
    └── transactions.js
```

Funcionalidades iniciais:

- cards de receitas, despesas e saldo mensal;
- gráficos com Chart.js para receitas/despesas por categoria;
- formulário para criar e editar lançamentos;
- tabela de lançamentos recentes com filtros por tipo, categoria, tamanho e ordenação;
- CRUD visual de categorias;
- CRUD visual de metas mensais com barra de progresso;
- confirmação visual para exclusões e estados de loading em ações principais.

## Testes

Execute:

```powershell
cd C:\Users\Daniel\projetos\finance-dash
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
cd C:\Users\Daniel\projetos\finance-dash
mvn clean package
```

## Roadmap futuro

- Frontend com dashboard visual
- Chart.js
- Importação CSV
- Exportação PDF
- Autenticação
- Multiusuário
- Deploy em cloud

