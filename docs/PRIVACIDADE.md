# Política de Privacidade — FinanceDash

**Última atualização:** junho de 2026

Esta Política de Privacidade descreve como o **FinanceDash** ("aplicação", "nós") coleta, usa, armazena e protege os dados pessoais dos usuários ("você"), em conformidade com a **Lei Geral de Proteção de Dados Pessoais (Lei nº 13.709/2018 — LGPD)**.

Ao criar uma conta e utilizar o FinanceDash, você declara estar ciente desta Política.

> ⚖️ **Aviso:** este documento é um modelo de boa-fé, elaborado a partir do funcionamento técnico da aplicação. Antes de operar comercialmente, recomenda-se a revisão por um(a) advogado(a) para adequação ao seu caso concreto.

---

## 1. Quem é o controlador dos dados

O controlador dos dados pessoais tratados nesta aplicação é o responsável pelo FinanceDash.

- **Contato / Encarregado (DPO):** `daniel.sanoli28@gmail.com`

Para qualquer solicitação relacionada a esta Política ou aos seus direitos, utilize o contato acima.

---

## 2. Quais dados coletamos

Coletamos apenas os dados necessários para o funcionamento do serviço:

### 2.1 Dados de cadastro e conta
- Nome
- Endereço de e-mail
- Senha (armazenada de forma **criptografada** com hash BCrypt — nunca em texto puro)
- Status de verificação de e-mail

### 2.2 Dados financeiros inseridos por você
- Lançamentos (receitas e despesas): descrição, valor, data, vencimento, status, método de pagamento, observações
- Categorias e metas financeiras
- Nome de clientes associados a recebíveis (quando você os informa)
- Perfil financeiro: meta de receita, reserva, custo fixo, horas faturáveis, alíquota de imposto e margem

### 2.3 Dados de assinatura
- Plano, status da assinatura e datas de trial/assinatura
- Identificadores de cliente/assinatura junto ao provedor de pagamento (Asaas)

> 💳 **Nós não armazenamos dados de cartão de crédito.** O processamento de pagamentos é feito pelo provedor **Asaas**, que possui sua própria política de privacidade.

### 2.4 Dados técnicos
- Tokens de autenticação (JWT e refresh tokens) para manter sua sessão
- Registros (logs) de operação para segurança e diagnóstico, sem conteúdo sensível

---

## 3. Para que usamos os dados (finalidade)

| Finalidade | Exemplos |
|---|---|
| Prestação do serviço | Registrar lançamentos, gerar dashboard, calcular metas e projeções do Radar |
| Autenticação e segurança | Login, refresh de sessão, recuperação de senha, limite de tentativas |
| Funcionalidades do Radar | Projeções, alertas e respostas do copiloto a partir dos seus próprios dados |
| Cobrança | Gerenciar trial e assinatura Pro via Asaas |
| Comunicação transacional | E-mails de verificação, redefinição de senha e resumos (digest) do Radar |

**Não vendemos seus dados** e não os utilizamos para publicidade de terceiros.

---

## 4. Base legal (LGPD)

O tratamento se fundamenta em:

- **Execução de contrato** (art. 7º, V): para prestar o serviço que você contratou.
- **Consentimento** (art. 7º, I): ao se cadastrar e fornecer seus dados.
- **Cumprimento de obrigação legal/regulatória** (art. 7º, II): quando aplicável (ex.: obrigações fiscais do controlador).
- **Legítimo interesse** (art. 7º, IX): para segurança, prevenção a fraudes e melhoria do serviço, sempre respeitando seus direitos.

---

## 5. Inteligência Artificial (Radar)

O copiloto do Radar pode utilizar um provedor de modelo de linguagem (OpenAI) para interpretar suas perguntas e redigir respostas. Nesses casos:

- São enviados ao provedor o texto da sua pergunta e os **resultados numéricos** necessários para compor a resposta;
- Os **cálculos financeiros são feitos no nosso backend** — o modelo de IA não realiza os cálculos;
- O uso da IA é **opcional** na operação do serviço e pode ser desabilitado pelo controlador.

O provedor de IA trata os dados conforme seus próprios termos. Recomenda-se não inserir dados sensíveis desnecessários nas perguntas.

---

## 6. Compartilhamento de dados

Compartilhamos dados apenas com **operadores** essenciais ao serviço:

- **Asaas** — processamento de pagamentos e assinaturas;
- **Provedor de IA (OpenAI)** — apenas quando o copiloto do Radar é utilizado;
- **Provedor de e-mail/SMTP** — envio de e-mails transacionais;
- **Provedor de hospedagem/banco de dados** — armazenamento da aplicação.

Não há compartilhamento com terceiros para fins de marketing.

---

## 7. Armazenamento e segurança

Adotamos medidas técnicas e organizacionais para proteger seus dados, incluindo:

- Senhas com hash **BCrypt**;
- Autenticação **stateless** via JWT e refresh tokens revogáveis;
- Tráfego por **HTTPS** em produção;
- **Isolamento por usuário** (multi-tenant): cada conta acessa somente os próprios dados;
- Limite de tentativas de login e logs sem dados sensíveis.

Nenhum sistema é 100% imune; em caso de incidente de segurança relevante, adotaremos as providências e comunicações previstas na LGPD.

---

## 8. Retenção e exclusão

Mantemos seus dados enquanto sua conta estiver ativa. Você pode solicitar a **exclusão** da sua conta e dos dados associados pelo contato da seção 1. Alguns dados podem ser retidos por prazo legal quando houver obrigação aplicável (ex.: registros fiscais ou de transações).

---

## 9. Seus direitos como titular (art. 18 da LGPD)

Você pode, a qualquer momento, solicitar:

- **Confirmação** da existência de tratamento;
- **Acesso** aos seus dados;
- **Correção** de dados incompletos, inexatos ou desatualizados;
- **Anonimização, bloqueio ou eliminação** de dados desnecessários ou tratados em desconformidade;
- **Portabilidade** dos dados;
- **Eliminação** dos dados tratados com base no consentimento;
- **Informação** sobre compartilhamentos;
- **Revogação do consentimento**.

Para exercer qualquer direito, escreva para `daniel.sanoli28@gmail.com`. Responderemos no menor prazo possível.

---

## 10. Cookies e armazenamento local

A aplicação utiliza armazenamento local do navegador (localStorage) para guardar os tokens de sessão e manter você autenticado. Esses dados ficam no seu dispositivo e podem ser limpos ao sair da conta ou limpar o navegador.

---

## 11. Alterações desta Política

Podemos atualizar esta Política periodicamente. A data de "última atualização" no topo indica a versão vigente. Mudanças relevantes poderão ser comunicadas pelos canais da aplicação.

---

_FinanceDash — comprometido com a privacidade e a proteção dos seus dados._
