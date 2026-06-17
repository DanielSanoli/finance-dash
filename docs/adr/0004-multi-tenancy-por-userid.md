# ADR 0004 — Multi-tenancy por `userId` em coluna

**Status:** Aceito · **Data:** 2026-06

## Contexto

Sendo um SaaS, cada usuário deve enxergar **apenas os próprios** lançamentos, categorias, metas, configurações e alertas. É preciso garantir isolamento de dados de forma simples e segura, sem a complexidade de bancos/esquemas separados por cliente.

## Decisão

Adotar **multi-tenancy de linha única (shared database, shared schema)**, com uma referência ao usuário (`user`/`userId`) nas entidades de dados e **filtro obrigatório por usuário** em todas as consultas dos repositories.

- O usuário autenticado é obtido via `CurrentUserService`.
- Os repositories expõem métodos escopados (ex.: `findByUserId...`, `findByIdAndUserId`).
- Um teste de integração (`TenantIsolationIntegrationTest`) garante que um usuário não acessa dados de outro.

## Consequências

**Positivas**
- Simples de implementar e operar; um único banco e esquema.
- Fácil de testar o isolamento.

**Negativas / trade-offs**
- O isolamento depende de disciplina: **toda** query precisa filtrar por usuário (risco de vazamento se esquecido). Mitigado por convenção, revisão e teste de isolamento.
- Não há separação física entre clientes (aceitável para o estágio atual).

## Alternativas consideradas

- **Esquema ou banco por tenant:** isolamento mais forte, porém com alto custo operacional — desnecessário no estágio atual.
- **Row-Level Security do PostgreSQL:** poderoso, mas adiciona complexidade; pode ser adotado no futuro como reforço.
