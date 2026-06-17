# ADR 0003 — Autenticação JWT stateless com refresh tokens

**Status:** Aceito · **Data:** 2026-06

## Contexto

A aplicação precisa autenticar usuários e proteger as APIs financeiras. O frontend é estático (servido pelo Spring) e consome a própria API. É desejável escalar horizontalmente sem sessão pegajosa (sticky session) e manter o backend simples.

## Decisão

Usar **Spring Security com JWT stateless**:

- Após login/registro, o cliente recebe um **access token** (JWT, validade padrão 24h) e um **refresh token** persistido no banco (validade 30 dias, revogável).
- O `JwtAuthenticationFilter` valida o JWT a cada requisição e popula o `SecurityContext`; não há sessão de servidor (`SessionCreationPolicy.STATELESS`).
- Senhas são armazenadas com **BCrypt**.
- Há limite de tentativas de login (`LoginRateLimiter`, 5/15min) e tokens de ação para reset de senha e verificação de e-mail.

## Consequências

**Positivas**
- Escala horizontalmente sem estado compartilhado.
- Simplicidade no frontend (envia `Authorization: Bearer` e renova via refresh).
- Revogação possível via refresh token persistido.

**Negativas / trade-offs**
- O access token é válido até expirar; revogação imediata exigiria lista de bloqueio (não implementada — mitigado pela validade curta + refresh revogável).
- Segredo do JWT (`JWT_SECRET`) precisa ser forte e protegido em produção.

## Alternativas consideradas

- **Sessão server-side:** descartada por dificultar escala e adicionar estado.
- **Provedor externo (Auth0/Clerk/Supabase Auth):** acelera, mas adiciona dependência e custo; como o domínio é simples e há expertise em Spring Security, optou-se por construir.
