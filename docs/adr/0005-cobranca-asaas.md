# ADR 0005 — Cobrança via Asaas

**Status:** Aceito · **Data:** 2026-06

## Contexto

O produto é um SaaS por assinatura (trial → plano Pro) voltado ao mercado **brasileiro**. É preciso cobrar de forma recorrente, com meios de pagamento locais (Pix, boleto, cartão), sem armazenar dados de cartão.

## Decisão

Integrar com a **Asaas** como gateway de pagamento/assinatura:

- `BillingService` cria cliente e assinatura via `AsaasClient` e devolve a `checkoutUrl`.
- Um **webhook** (`POST /api/v1/billing/webhook`, público e validado por token) atualiza `plan` e `subscriptionStatus` do usuário conforme os eventos de pagamento.
- O `SubscriptionAccessFilter` libera ou bloqueia (HTTP 402) o acesso às APIs financeiras conforme o status (`TRIALING`, `ACTIVE`, `PAST_DUE`, `CANCELED`).
- A aplicação **não** armazena dados de cartão.

## Consequências

**Positivas**
- Meios de pagamento brasileiros nativos e mais simples que alternativas internacionais.
- Sem PCI/armazenamento de cartão do nosso lado.
- Controle de acesso desacoplado em um filtro dedicado.

**Negativas / trade-offs**
- Acoplamento a um provedor específico; trocar exigiria adaptar `AsaasClient`/webhook.
- Depende da confiabilidade e do formato de eventos do provedor.

## Alternativas consideradas

- **Stripe:** excelente DX, mas menos conveniente para Pix/boleto no Brasil no momento da decisão.
- **Pagar.me/Mercado Pago:** opções válidas; a Asaas foi escolhida pela facilidade de assinaturas e cobrança recorrente local.
