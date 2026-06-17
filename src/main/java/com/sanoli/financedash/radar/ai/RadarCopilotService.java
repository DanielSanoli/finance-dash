package com.sanoli.financedash.radar.ai;

import com.sanoli.financedash.config.RadarAiProperties;
import com.sanoli.financedash.dto.RadarAskResponse;
import com.sanoli.financedash.exception.BusinessException;
import com.sanoli.financedash.radar.engine.MonthProjectionResult;
import com.sanoli.financedash.radar.engine.OverdueReceivablesResult;
import com.sanoli.financedash.radar.engine.SafeToSpendResult;
import com.sanoli.financedash.radar.engine.MinimumProjectPriceResult;
import com.sanoli.financedash.radar.engine.FreelanceGapResult;
import com.sanoli.financedash.radar.engine.CutAnalysisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.math.BigDecimal;

@Service
public class RadarCopilotService {

    private static final Logger log = LoggerFactory.getLogger(RadarCopilotService.class);

    private static final String SYSTEM_PROMPT = """
            Você é o copiloto financeiro do FinanceDash para freelancers e MEIs.
            Regras obrigatórias:
            1. Nunca invente valores monetários. Use apenas números retornados pelas ferramentas.
            2. Sempre termine com uma ação prática, as premissas (campo assumptions do DTO) e a frase exata: Sugestão, não consultoria financeira.
            3. Se a pergunta não corresponder a nenhuma ferramenta, liste as perguntas suportadas sem números.
            4. Responda em português do Brasil, de forma direta e amigável.
            Ferramentas disponíveis: projetarSaldoMes, safeToSpend, recebiveisAtrasados,
            precisaMaisFreela, precoMinimoProjeto, analisarCortes.
            """;

    private static final Pattern HOURS_PATTERN = Pattern.compile("(\\d+[\\.,]?\\d*)\\s*(h|hrs?|horas?)?", Pattern.CASE_INSENSITIVE);

    private static final Set<String> PROJECTION_KEYWORDS = Set.of(
            "fechar", "positivo", "negativo", "saldo", "proje", "projec", "mês", "mes", "fecha"
    );
    private static final Set<String> SAFE_TO_SPEND_KEYWORDS = Set.of(
            "gastar", "safe", "dispon", "posso gastar", "quanto posso", "sobra"
    );
    private static final Set<String> OVERDUE_KEYWORDS = Set.of(
            "atras", "receb", "cliente", "cobrar", "caixa", "inadimpl"
    );
    private static final Set<String> CUT_KEYWORDS = Set.of(
            "cort", "econom", "dispens", "não essencial", "nao essencial", "reduzir gasto", "onde cortar"
    );
    private static final Set<String> PRICE_KEYWORDS = Set.of(
            "preço", "preco", "cobrar", "precificar", "valor mínimo", "valor minimo", "quanto cobrar", "projeto de"
    );
    private static final Set<String> FREELANCE_KEYWORDS = Set.of(
            "freela", "freelance", "horas extras", "pegar mais", "meta de receita", "falta receita", "preciso vender"
    );

    private final RadarEngineTools radarEngineTools;
    private final RadarToolInvocationContext invocationContext;
    private final RadarAiProperties radarAiProperties;
    private final Optional<ChatClient> chatClient;

    public RadarCopilotService(
            RadarEngineTools radarEngineTools,
            RadarToolInvocationContext invocationContext,
            RadarAiProperties radarAiProperties,
            ObjectProvider<ChatModel> chatModelProvider,
            ObjectProvider<ChatClient.Builder> chatClientBuilderProvider
    ) {
        this.radarEngineTools = radarEngineTools;
        this.invocationContext = invocationContext;
        this.radarAiProperties = radarAiProperties;
        this.chatClient = buildChatClient(radarAiProperties, chatModelProvider, chatClientBuilderProvider);
    }

    public RadarAskResponse ask(String question) {
        invocationContext.clear();

        if (shouldUseSpringAi()) {
            try {
                return askWithSpringAi(question);
            } catch (RuntimeException exception) {
                log.warn("Falha no copiloto Spring AI; usando roteador determinístico", exception);
            }
        }

        return askDeterministic(question);
    }

    private RadarAskResponse askWithSpringAi(String question) {
        ChatClient client = chatClient.orElseThrow(() -> new BusinessException("Copiloto IA indisponível"));
        String answer = client.prompt()
                .system(SYSTEM_PROMPT)
                .tools(radarEngineTools)
                .user(question)
                .call()
                .content();

        String usedFunction = invocationContext.getUsedFunction();
        Object data = invocationContext.getData();
        if (usedFunction == null) {
            return new RadarAskResponse(
                    answer != null ? answer : RadarAnswerComposer.UNSUPPORTED_MESSAGE,
                    "unsupported",
                    null
            );
        }

        return new RadarAskResponse(answer, usedFunction, data);
    }

    private RadarAskResponse askDeterministic(String question) {
        String normalized = question.toLowerCase(Locale.ROOT);

        if (matchesAny(normalized, OVERDUE_KEYWORDS)) {
            OverdueReceivablesResult result = radarEngineTools.recebiveisAtrasados();
            return new RadarAskResponse(
                    RadarAnswerComposer.composeOverdueReceivables(result),
                    "recebiveisAtrasados",
                    result
            );
        }

        if (matchesAny(normalized, CUT_KEYWORDS)) {
            CutAnalysisResult result = radarEngineTools.analisarCortes();
            return new RadarAskResponse(
                    RadarAnswerComposer.composeCutAnalysis(result),
                    "analisarCortes",
                    result
            );
        }

        if (matchesAny(normalized, PRICE_KEYWORDS)) {
            Optional<BigDecimal> hours = extractHours(normalized);
            if (hours.isEmpty()) {
                return new RadarAskResponse(
                        "Informe as horas estimadas do projeto (ex.: preço mínimo para 40 horas).\n\n"
                                + RadarAnswerComposer.DISCLAIMER,
                        "unsupported",
                        null
                );
            }
            MinimumProjectPriceResult result = radarEngineTools.precoMinimoProjeto(hours.get().doubleValue());
            return new RadarAskResponse(
                    RadarAnswerComposer.composeMinimumProjectPrice(result),
                    "precoMinimoProjeto",
                    result
            );
        }

        if (matchesAny(normalized, FREELANCE_KEYWORDS)) {
            FreelanceGapResult result = radarEngineTools.precisaMaisFreela();
            return new RadarAskResponse(
                    RadarAnswerComposer.composeFreelanceGap(result),
                    "precisaMaisFreela",
                    result
            );
        }

        if (matchesAny(normalized, SAFE_TO_SPEND_KEYWORDS)) {
            SafeToSpendResult result = radarEngineTools.safeToSpend();
            return new RadarAskResponse(
                    RadarAnswerComposer.composeSafeToSpend(result),
                    "safeToSpend",
                    result
            );
        }

        if (matchesAny(normalized, PROJECTION_KEYWORDS)) {
            MonthProjectionResult result = radarEngineTools.projetarSaldoMes();
            return new RadarAskResponse(
                    RadarAnswerComposer.composeMonthProjection(result),
                    "projetarSaldoMes",
                    result
            );
        }

        return new RadarAskResponse(RadarAnswerComposer.UNSUPPORTED_MESSAGE, "unsupported", null);
    }

    private boolean shouldUseSpringAi() {
        return radarAiProperties.isEnabled() && chatClient.isPresent();
    }

    private static Optional<ChatClient> buildChatClient(
            RadarAiProperties radarAiProperties,
            ObjectProvider<ChatModel> chatModelProvider,
            ObjectProvider<ChatClient.Builder> chatClientBuilderProvider
    ) {
        if (!radarAiProperties.isEnabled()) {
            return Optional.empty();
        }
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) {
            return Optional.empty();
        }
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder != null) {
            return Optional.of(builder.build());
        }
        return Optional.of(ChatClient.builder(chatModel).build());
    }

    private static boolean matchesAny(String text, Set<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }

    private static Optional<BigDecimal> extractHours(String text) {
        Matcher matcher = HOURS_PATTERN.matcher(text);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(new BigDecimal(matcher.group(1).replace(',', '.')));
    }
}
