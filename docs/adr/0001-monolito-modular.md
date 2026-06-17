# ADR 0001 — Monólito modular em vez de microsserviços

**Status:** Aceito · **Data:** 2026-06

## Contexto

O FinanceDash é desenvolvido por um time muito pequeno (essencialmente uma pessoa), com tempo limitado, e está em fase de validação de produto. É preciso entregar rápido, manter custo baixo e garantir consistência dos dados financeiros.

Existia a tentação de aplicar uma arquitetura de microsserviços (há, inclusive, experiência prévia com ela em outro projeto).

## Decisão

Adotar um **monólito modular**: uma única aplicação Spring Boot, organizada em pacotes bem delimitados (`controller`, `service`, `repository`, `domain`, `security`, `billing`, `radar.*`, `config`).

As fronteiras entre módulos são mantidas limpas para permitir, no futuro, extrair um serviço caso surja necessidade real (ex.: a camada de IA, que é I/O-bound).

## Consequências

**Positivas**
- Deploy único e barato (um serviço + Postgres).
- Consistência transacional trivial (uma transação de banco cobre várias operações).
- Iteração rápida, menos sobrecarga operacional (sem rede entre serviços, sem orquestração).

**Negativas / trade-offs**
- Escalabilidade é por instância inteira, não por componente.
- Requer disciplina para manter as fronteiras de módulo (mitigado por convenções e revisão).

## Alternativas consideradas

- **Microsserviços:** descartado por adicionar complexidade operacional incompatível com o tamanho do time e o estágio do produto (YAGNI).
