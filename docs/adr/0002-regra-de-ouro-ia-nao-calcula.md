# ADR 0002 — Regra de Ouro: a IA não calcula valores monetários

**Status:** Aceito · **Data:** 2026-06

## Contexto

O FinanceDash Radar responde perguntas financeiras em linguagem natural ("vou fechar o mês positivo?", "quanto posso gastar?"). A forma ingênua seria deixar o modelo de linguagem (LLM) interpretar os dados e produzir os números diretamente.

Em um produto financeiro, porém, um número errado destrói a confiança. LLMs podem "alucinar" valores e não são reproduzíveis nem auditáveis.

## Decisão

**A IA nunca calcula valores monetários.** Todo número (saldo, projeção, preço, gap) é produzido por código Java determinístico e testável (`radar.engine`). A camada de IA (`radar.ai`) apenas:

1. interpreta a pergunta;
2. escolhe a função correta via **function calling** (`@Tool` em `RadarEngineTools`);
3. recebe o DTO com os números e as premissas;
4. compõe a resposta em linguagem natural, sempre com ação sugerida, premissas e o aviso "sugestão, não consultoria financeira".

## Consequências

**Positivas**
- Números auditáveis, reproduzíveis e cobertos por testes unitários.
- O produto funciona mesmo sem IA (modo determinístico via endpoints REST).
- Custo de IA controlado: o modelo só formata, não "pensa" os cálculos.

**Negativas / trade-offs**
- Exige manter as funções do motor e suas ferramentas (`@Tool`) sincronizadas.
- A IA depende de prompts/guardrails bem definidos para não inserir números próprios.

## Alternativas consideradas

- **LLM calculando direto:** descartado por risco de alucinação e falta de auditabilidade.
- **Somente regras, sem IA:** funcional, mas perde a interface conversacional que diferencia o produto. A solução adotada mantém os dois mundos.
