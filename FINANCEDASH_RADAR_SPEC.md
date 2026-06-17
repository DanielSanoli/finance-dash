# FinanceDash Radar — Spec de Implementação

> **Para o agente de código (Cursor):** este arquivo é a fonte de verdade do produto.
> Leia-o por inteiro antes de gerar código. Implemente fase a fase (ver §9), respeitando
> a **Regra de Ouro** (§1) e os critérios de aceite de cada função (§5).
>
> **Como usar no Cursor:** mantenha este arquivo na raiz do repo e use `@FINANCEDASH_RADAR_SPEC.md`
> no chat. Para regras persistentes, copie o bloco do §11 para `.cursor/rules/radar.mdc`.

---

## 0. Contexto do projeto

- **Repo base:** `finance-dash` (MVP já existente).
- **Stack:** Java 21, Spring Boot 3, Maven, Spring Data JPA, PostgreSQL, Bean Validation, Springdoc/OpenAPI, JUnit 5 + Mockito. Frontend estático servido pelo próprio Spring (`src/main/resources/static`). Para IA: Spring AI.
- **Objetivo:** evoluir o dashboard passivo para um **copiloto financeiro** que projeta o mês, alerta riscos e sugere ações para freelancers/MEIs.
- **Convenções:** API REST versionada em `/api/v1/**`, DTOs separados das entidades, services com lógica de negócio, repositories só para acesso a dados, erros no formato padrão já existente (`VALIDATION_ERROR`, `RESOURCE_NOT_FOUND`, `INTERNAL_SERVER_ERROR`).

---

## 1. Regra de Ouro (NÃO VIOLAR)

**A IA nunca calcula valores monetários.** Todo número (saldo, projeção, preço, gap) é produzido por código Java determinístico e coberto por teste. A camada de IA apenas:

1. interpreta a pergunta em linguagem natural,
2. chama a função correta do motor (function calling),
3. recebe o DTO com números + premissas,
4. compõe a resposta em linguagem humana com **ação sugerida + premissas + disclaimer**.

Qualquer resposta da IA que contenha um valor monetário deve ter esse valor originado de um DTO do motor — nunca gerado pelo modelo.

---

## 2. Arquitetura em 3 camadas

| Camada | Pacote sugerido | Responsabilidade |
|---|---|---|
| Motor de cálculo (determinístico) | `radar.engine` | Fórmulas puras, testáveis. Retorna DTO com número + premissas. |
| Motor de regras / alertas | `radar.rules` | Avalia limiares e gera `Alert`. Roda agendado e a cada lançamento. |
| Camada de IA (copiloto) | `radar.ai` | Spring AI function calling. Interpreta pergunta → chama engine → formata resposta. |

Fluxo: `Pergunta → IA(intenção) → Engine(número + premissas) → IA(resposta natural) → Usuário`.

---

## 3. Modelo de dados

### 3.1 `Transaction` (evoluir entidade existente)

Campos novos a adicionar:

| Campo | Tipo Java | Notas |
|---|---|---|
| `status` | `enum TransactionStatus { PENDING, PAID, OVERDUE }` | Diferencia previsto de realizado. Default `PAID` em migração de dados antigos. |
| `dueDate` | `LocalDate` | Data prevista de recebimento/pagamento. |
| `isRecurring` | `boolean` | Marca receitas/despesas fixas. |
| `recurrenceRule` | `enum RecurrenceRule { NONE, WEEKLY, MONTHLY, YEARLY }` | Default `NONE`. |
| `clientId` | `UUID` (FK → `Client`, nullable) | Para aging por cliente. |
| `essential` | `Boolean` (nullable) | Apoia análise "o que cortar". |

> Migração: lançamentos existentes recebem `status = PAID`, `dueDate = transactionDate`, `isRecurring = false`, `recurrenceRule = NONE`.

### 3.2 `Client` (nova entidade — opcional na v1)

### 3.3 `UserSettings` (nova entidade — perfil financeiro)

### 3.4 `Alert` (nova entidade — gerada pelo Radar)

> **Importante (multi-tenant):** toda query do motor filtra por `userId` do usuário autenticado.

---

## 4. DTOs do motor (contrato com a IA)

Padronize: todo DTO de resultado expõe `List<String> assumptions`.

---

## 5. Motor de cálculo — funções, fórmulas e critérios de aceite

Ver §5.1 a §5.6 no documento original (projetarSaldoMes, safeToSpend, recebiveisAtrasados,
precisaMaisFreela, precoMinimoProjeto, analisarCortes).

---

## 9. Plano de build (executar em ordem)

- [x] **R0 — Modelo de dados.**
- [x] **R1 — Motor de cálculo (5.1, 5.2, 5.3).**
- [x] **R2 — Regras/alertas + job agendado.**
- [ ] **R3 — Copiloto IA (Spring AI function calling).**
- [ ] **R4 — Proatividade (digest por e-mail).**
- [ ] **R5 — Diferenciais (5.4, 5.5, 5.6).**

---

## 10. Critérios globais de qualidade

- Valores monetários: `BigDecimal` com `RoundingMode.HALF_EVEN`, 2 casas. Nunca `double`.
- Datas/timezone: usar timezone do usuário (default `America/Sao_Paulo`).
- Toda função do motor: cobertura de teste para caminho feliz + bordas.
- Multi-tenant: filtrar por `userId` em 100% das queries.

---

_Documento de implementação do FinanceDash Radar — manter sincronizado com o código conforme o produto evolui. A versão integral original permanece no histórico do chat/PR._
