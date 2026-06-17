# Architecture Decision Records (ADRs)

Este diretório registra as **decisões de arquitetura** relevantes do FinanceDash — o contexto, a escolha feita e suas consequências. Cada ADR é imutável: se uma decisão muda, cria-se um novo ADR que substitui o anterior.

Formato baseado no modelo de Michael Nygard.

| # | Decisão | Status |
|---|---|---|
| [0001](0001-monolito-modular.md) | Monólito modular em vez de microsserviços | Aceito |
| [0002](0002-regra-de-ouro-ia-nao-calcula.md) | Regra de Ouro: a IA não calcula valores | Aceito |
| [0003](0003-autenticacao-jwt-stateless.md) | Autenticação JWT stateless + refresh tokens | Aceito |
| [0004](0004-multi-tenancy-por-userid.md) | Multi-tenancy por `userId` em coluna | Aceito |
| [0005](0005-cobranca-asaas.md) | Cobrança via Asaas | Aceito |
| [0006](0006-frontend-servido-pelo-spring.md) | Frontend estático servido pelo Spring Boot | Aceito |

## Status possíveis

- **Proposto** — em discussão
- **Aceito** — em vigor
- **Substituído** — trocado por um ADR mais novo (com referência)
- **Descontinuado** — não se aplica mais
