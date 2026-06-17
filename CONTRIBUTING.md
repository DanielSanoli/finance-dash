# Guia de Contribuição — FinanceDash

Obrigado pelo interesse em contribuir! Este guia explica como configurar o ambiente, os padrões do projeto e o fluxo de trabalho.

> 📖 Para entender a arquitetura e a API, leia [`docs/DOCUMENTACAO.md`](docs/DOCUMENTACAO.md). Decisões de arquitetura estão em [`docs/adr/`](docs/adr/).

---

## Pré-requisitos

- **Java 21**
- **Maven**
- **PostgreSQL** (local ou via Docker)
- **Node.js** (apenas para os testes E2E com Playwright)

---

## Configuração do ambiente

```bash
git clone https://github.com/DanielSanoli/finance-dash.git
cd finance-dash
```

Crie um banco PostgreSQL local (`database`, `user` e `password` = `financedash`) **ou** suba tudo com Docker:

```bash
docker compose up --build      # app + Postgres
# ou
mvn spring-boot:run            # app (Postgres precisa estar de pé)
```

A aplicação sobe em `http://localhost:8080` · Swagger em `/swagger-ui.html`.

Variáveis úteis em desenvolvimento (com padrões sensatos): `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_SECRET`, `SPRING_AI_CHAT_MODEL` (use `none` para desligar a IA), `OPENAI_API_KEY`. Veja o `README.md` e `application.yml` para a lista completa.

---

## Rodando os testes

```bash
mvn test                                    # unitários e de integração (JUnit 5 + Mockito)
npx playwright install && npm run test:e2e  # E2E do frontend (app rodando)
```

**Toda mudança deve manter os testes verdes.** Funcionalidade nova no motor do Radar exige teste unitário cobrindo caminho feliz e bordas (mês vazio, dia 1, divisão por zero).

---

## Estrutura do projeto

```
src/main/java/com/sanoli/financedash/
├── controller/   # endpoints REST (/api/v1/**)
├── service/      # regras de negócio
├── repository/   # acesso a dados (Spring Data JPA)
├── domain/       # entidades e enums
├── dto/          # objetos de request/response (records)
├── security/     # JWT, filtros, usuário atual
├── billing/      # integração Asaas
├── radar/
│   ├── engine/   # cálculo determinístico
│   ├── rules/    # alertas + agendador
│   ├── ai/       # copiloto (Spring AI function calling)
│   └── digest/   # resumo proativo por e-mail
├── config/       # propriedades, beans, seeding
└── exception/    # handler global e exceções de negócio
src/main/resources/static/   # frontend (HTML/CSS/JS)
docs/                        # documentação
```

---

## Padrões de código

- **Regra de Ouro do Radar:** a IA **nunca** calcula valores monetários. Todo número vem do `radar.engine`. Veja [ADR 0002](docs/adr/0002-regra-de-ouro-ia-nao-calcula.md).
- **Valores monetários** sempre em `BigDecimal` (`RoundingMode.HALF_EVEN`, 2 casas). Nunca `double`.
- **Multi-tenant:** toda consulta filtra pelo usuário autenticado (`CurrentUserService`). Nunca exponha dados de outro usuário.
- **Camadas:** controllers finos → services com a lógica → repositories só para dados. DTOs separados das entidades.
- **API** versionada em `/api/v1/**`. Erros no formato padrão (`VALIDATION_ERROR`, `RESOURCE_NOT_FOUND`, `INTERNAL_SERVER_ERROR`).
- **Frontend:** módulos IIFE (`FinanceDash*`); reutilize `FinanceDashUi` (formatação, toasts, `showModal`/`hideModal`). Sempre use `escapeHtml` em conteúdo vindo da API.
- Há regras persistentes em [`.cursor/rules/radar.mdc`](.cursor/rules/radar.mdc) para quem usa o Cursor.

---

## Fluxo de trabalho (Git)

1. Crie um branch a partir do branch principal:
   - `feature/<descricao>` para novas funcionalidades
   - `fix/<descricao>` para correções
   - `docs/<descricao>` para documentação
2. Faça commits pequenos e descritivos. Sugestão (Conventional Commits):
   - `feat: adiciona endpoint de exportação CSV`
   - `fix: corrige projeção no primeiro dia do mês`
   - `docs: atualiza referência da API`
   - `test: cobre borda de divisão por zero no safe-to-spend`
3. Garanta `mvn test` verde e abra um **Pull Request** descrevendo o que muda e por quê.
4. Atualize a documentação afetada (`README.md`, `docs/`) quando mexer em endpoints ou no modelo de dados.

---

## Checklist do Pull Request

- [ ] Build e testes passam (`mvn test`)
- [ ] Cobertura de teste para a mudança (caminho feliz + bordas)
- [ ] Respeita a Regra de Ouro e o `BigDecimal` para dinheiro
- [ ] Consultas escopadas por usuário (multi-tenant)
- [ ] Documentação atualizada quando aplicável
- [ ] Sem segredos/credenciais no código

---

## Reportando bugs e ideias

Abra uma *issue* descrevendo: o que esperava, o que aconteceu, passos para reproduzir e ambiente. Para sugestões, descreva o problema do usuário antes da solução.

Obrigado por contribuir! 🚀
