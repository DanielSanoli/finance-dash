# ADR 0006 — Frontend estático servido pelo Spring Boot

**Status:** Aceito · **Data:** 2026-06

## Contexto

O produto precisa de uma interface web, mas o time é pequeno e o objetivo é entregar rápido com baixo custo de operação e deploy. Um framework SPA com build (React/Angular) traria toolchain, etapa de build e, possivelmente, um segundo serviço para hospedar.

## Decisão

Servir um **frontend estático** (HTML/CSS/JavaScript "vanilla", organizado em módulos IIFE) diretamente pelo Spring Boot, a partir de `src/main/resources/static`. Gráficos com Chart.js via CDN.

Consequência prática importante para deploy: como o app é um servidor Java que serve o próprio frontend, ele é publicado **inteiro** em um PaaS que roda contêiner (Railway/Render) — **não** em plataformas só de frontend como o Vercel.

## Consequências

**Positivas**
- Sem etapa de build no frontend; um único artefato e um único deploy.
- Mesma origem para API e UI → sem complicações de CORS.
- Simples de manter para o estágio atual.

**Negativas / trade-offs**
- Menos estrutura que um framework SPA (estado, componentização, rotas) — mitigado por convenções de módulo.
- Crescimento da UI pode justificar, no futuro, extrair um SPA próprio (a API REST já está pronta para isso).

## Alternativas consideradas

- **SPA separada (React/Angular) no Vercel:** mais estrutura, porém com build, segundo deploy e CORS — adiado até haver necessidade real.
