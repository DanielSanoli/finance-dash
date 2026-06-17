const FinanceDashRadar = (() => {
    const CHIP_QUESTIONS = [
        "Vou fechar o mês positivo?",
        "Quanto posso gastar até o fim do mês?",
        "Qual cliente está atrasando meu caixa?",
        "Preciso pegar mais um freela?",
        "Qual valor mínimo cobrar no próximo projeto?",
        "O que devo cortar para bater minha meta?"
    ];

    const DISCLAIMER = "Sugestão, não consultoria financeira.";

    let loading = false;

    function bindEvents(refreshHandler) {
        document.getElementById("radar-min-price-calc")?.addEventListener("click", calculateMinimumPrice);
        document.getElementById("radar-ask-send")?.addEventListener("click", sendQuestion);
        document.getElementById("radar-setup-button")?.addEventListener("click", () => {
            FinanceDashApp.openFinancialProfile();
        });
        document.getElementById("radar-ask-input")?.addEventListener("keydown", (event) => {
            if (event.key === "Enter") {
                event.preventDefault();
                sendQuestion();
            }
        });
        document.getElementById("reload-radar")?.addEventListener("click", async () => {
            const button = document.getElementById("reload-radar");
            try {
                FinanceDashUi.setButtonLoading(button, true, "Atualizando...");
                await refreshHandler();
                FinanceDashUi.showToast("Radar atualizado.");
            } catch (error) {
                FinanceDashUi.showToast(error.message);
            } finally {
                FinanceDashUi.setButtonLoading(button, false);
            }
        });

        renderChips();
    }

    function renderChips() {
        const container = document.getElementById("radar-chips");
        if (!container || container.childElementCount > 0) {
            return;
        }

        container.innerHTML = CHIP_QUESTIONS.map((question) => (
            `<button class="chip" type="button" data-question="${FinanceDashUi.escapeHtml(question)}">${FinanceDashUi.escapeHtml(question)}</button>`
        )).join("");

        container.querySelectorAll(".chip").forEach((chip) => {
            chip.addEventListener("click", () => askQuestion(chip.dataset.question));
        });
    }

    async function load() {
        if (loading) {
            return;
        }
        loading = true;
        setSectionLoading(true);

        try {
            const settings = await FinanceDashSettings.load();
            if (!FinanceDashSettings.isConfigured(settings)) {
                showSetupState();
                return;
            }

            hideSetupState();

            const [
                projection,
                safeToSpend,
                overdue,
                freelanceGap,
                cutAnalysis,
                alerts
            ] = await Promise.all([
                FinanceDashApi.getRadarMonthProjection(),
                FinanceDashApi.getRadarSafeToSpend(),
                FinanceDashApi.getRadarOverdueReceivables(),
                FinanceDashApi.getRadarFreelanceGap(),
                FinanceDashApi.getRadarCutAnalysis(),
                FinanceDashApi.getRadarAlerts(false)
            ]);

            renderProjection(projection, safeToSpend);
            renderCoreCards(safeToSpend, overdue, freelanceGap);
            renderAlerts(alerts);
            renderOverdueTable(overdue);
            renderCutAnalysis(cutAnalysis);
            await calculateMinimumPrice();
        } finally {
            loading = false;
            setSectionLoading(false);
        }
    }

    function showSetupState() {
        document.getElementById("radar-setup-banner")?.classList.remove("hidden");
        document.getElementById("radar-data-sections")?.classList.add("hidden");
    }

    function hideSetupState() {
        document.getElementById("radar-setup-banner")?.classList.add("hidden");
        document.getElementById("radar-data-sections")?.classList.remove("hidden");
    }

    function setSectionLoading(isLoading) {
        document.getElementById("radar-view")?.classList.toggle("is-loading", isLoading);
    }

    function renderProjection(projection, safeToSpend) {
        const heroValue = document.getElementById("radar-projected-balance");
        const badge = document.getElementById("radar-projection-badge");
        const assumptionsList = document.getElementById("radar-projection-assumptions");
        const heroPanel = document.getElementById("radar-hero-panel");

        if (heroPanel) {
            heroPanel.classList.toggle("pos", projection.positive);
            heroPanel.classList.toggle("neg", !projection.positive);
        }

        if (heroValue) {
            const projected = Number(projection.projectedBalance || 0);
            const sign = projected >= 0 ? "+" : "";
            heroValue.textContent = `${sign}${FinanceDashUi.formatCurrency(projected)}`;
            heroValue.classList.toggle("pos", projection.positive);
            heroValue.classList.toggle("neg", !projection.positive);
        }

        if (badge) {
            badge.textContent = projection.positive ? "▲ Deve fechar positivo" : "▼ Projeção negativa";
            badge.classList.toggle("pos", projection.positive);
            badge.classList.toggle("neg", !projection.positive);
        }

        setText("radar-current-balance", FinanceDashUi.formatCurrency(projection.currentBalance));
        setText("radar-month-goal", FinanceDashUi.formatCurrency(projection.goal));
        setText("radar-reserve-target", FinanceDashUi.formatCurrency(safeToSpend.reserveTarget));
        setText("radar-gauge-safe", FinanceDashUi.formatCurrency(safeToSpend.safeToSpendTotal));
        setText("radar-gauge-caption", buildGaugeCaption(projection, safeToSpend));
        updateGaugeBar(projection, safeToSpend);

        if (assumptionsList) {
            const assumptions = mergeAssumptions(projection.assumptions, safeToSpend.assumptions);
            assumptionsList.innerHTML = assumptions.length
                ? assumptions.map((item) => `<li>${FinanceDashUi.escapeHtml(item)}</li>`).join("")
                : "<li>Sem premissas adicionais para este mês.</li>";
        }
    }

    function updateGaugeBar(projection, safeToSpend) {
        const wrap = document.getElementById("radar-gauge-bar-wrap");
        const bar = document.getElementById("radar-gauge-bar");
        if (!wrap || !bar) {
            return;
        }

        const reserve = Number(safeToSpend.reserveTarget || 0);
        if (reserve <= 0) {
            wrap.hidden = true;
            return;
        }

        const projected = Number(projection.projectedBalance || 0);
        const ratio = Math.min(100, Math.max(0, (projected / reserve) * 100));
        wrap.hidden = false;
        bar.style.width = `${ratio}%`;
        bar.classList.toggle("pos", projection.positive);
        bar.classList.toggle("neg", !projection.positive);
    }

    function buildGaugeCaption(projection, safeToSpend) {
        const reserveAssumption = (safeToSpend.assumptions || []).find((item) => /reserva/i.test(item));
        if (reserveAssumption) {
            return reserveAssumption;
        }
        return `Projeção ${FinanceDashUi.formatCurrency(projection.projectedBalance)} · reserva alvo ${FinanceDashUi.formatCurrency(safeToSpend.reserveTarget)}.`;
    }

    function renderCoreCards(safeToSpend, overdue, freelanceGap) {
        setText("radar-safe-total", FinanceDashUi.formatCurrency(safeToSpend.safeToSpendTotal));
        setText(
            "radar-safe-caption",
            `≈ ${FinanceDashUi.formatCurrency(safeToSpend.safeToSpendPerDay)}/dia · ${safeToSpend.daysRemaining} dias restantes`
        );
        setText("radar-blocked-total", FinanceDashUi.formatCurrency(overdue.totalBlocked));
        setText(
            "radar-blocked-caption",
            `${(overdue.items || []).length} recebível(is) vencido(s)`
        );

        const needsFreela = document.getElementById("radar-freela-answer");
        const freelaCard = needsFreela?.closest(".card");
        if (needsFreela) {
            needsFreela.textContent = freelanceGap.needsMoreFreelance ? "Sim" : "Não";
            needsFreela.classList.toggle("warn", freelanceGap.needsMoreFreelance);
            needsFreela.classList.toggle("ok", !freelanceGap.needsMoreFreelance);
        }
        if (freelaCard) {
            freelaCard.classList.toggle("needs-attention", freelanceGap.needsMoreFreelance);
        }

        setText(
            "radar-freela-caption",
            freelanceGap.needsMoreFreelance
                ? `Gap de ${FinanceDashUi.formatCurrency(freelanceGap.incomeGap)} · meta ${FinanceDashUi.formatCurrency(freelanceGap.monthlyIncomeGoal)}`
                : `Meta ${FinanceDashUi.formatCurrency(freelanceGap.monthlyIncomeGoal)} dentro do esperado`
        );
    }

    function renderAlerts(alerts) {
        const container = document.getElementById("radar-alerts-list");
        const countLabel = document.getElementById("radar-alerts-count");
        const activeAlerts = (alerts || []).filter((alert) => !alert.read);

        if (countLabel) {
            countLabel.textContent = `${activeAlerts.length} ativo(s)`;
        }

        if (!container) {
            return;
        }

        if (!activeAlerts.length) {
            container.innerHTML = emptyState("Nenhum alerta ativo", "Quando o motor detectar riscos, eles aparecem aqui.");
            return;
        }

        container.innerHTML = activeAlerts.map((alert) => {
            const severityClass = severityToClass(alert.severity);
            const severityLabel = alert.severity || "INFO";
            return `
                <article class="alert ${severityClass}" data-alert-id="${alert.id}">
                    <span class="dot"></span>
                    <div class="a-body">
                        <p class="a-msg">
                            ${FinanceDashUi.escapeHtml(alert.message)}
                            <span class="sev ${severityClass}">${FinanceDashUi.escapeHtml(severityLabel)}</span>
                        </p>
                        <p class="a-act">Ação: ${FinanceDashUi.escapeHtml(alert.actionSuggestion || "Revise seu caixa e recebíveis.")}</p>
                    </div>
                    <button class="a-read button-secondary" type="button" data-read-alert="${alert.id}">Marcar lido</button>
                </article>
            `;
        }).join("");

        container.querySelectorAll("[data-read-alert]").forEach((button) => {
            button.addEventListener("click", () => markAlertRead(button.dataset.readAlert));
        });
    }

    async function markAlertRead(alertId) {
        try {
            await FinanceDashApi.markRadarAlertRead(alertId);
            const alerts = await FinanceDashApi.getRadarAlerts(false);
            renderAlerts(alerts);
            FinanceDashUi.showToast("Alerta marcado como lido.");
        } catch (error) {
            FinanceDashUi.showToast(error.message);
        }
    }

    function renderOverdueTable(overdue) {
        const tbody = document.getElementById("radar-overdue-body");
        if (!tbody) {
            return;
        }

        const items = overdue.items || [];
        if (!items.length) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="4">${emptyState("Caixa livre de atrasos", "Nenhum recebível vencido no momento.")}</td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = items.map((item) => `
            <tr>
                <td>${FinanceDashUi.escapeHtml(item.clientName || "Cliente")}</td>
                <td class="num">${FinanceDashUi.formatCurrency(item.amount)}</td>
                <td class="num late">${item.daysOverdue} dias</td>
                <td class="num">${FinanceDashUi.escapeHtml(String(item.impact ?? "-"))}</td>
            </tr>
        `).join("");
    }

    function renderCutAnalysis(cutAnalysis) {
        setText("radar-cut-gap", FinanceDashUi.formatCurrency(cutAnalysis.currentProjectedBalance));
        setText(
            "radar-cut-after",
            `Após cortes simulados: ${FinanceDashUi.formatCurrency(cutAnalysis.projectedBalanceAfterCuts)}`
        );

        const tbody = document.getElementById("radar-cut-body");
        const footnote = document.getElementById("radar-cut-footnote");
        const items = cutAnalysis.items || [];

        if (tbody) {
            if (!items.length) {
                tbody.innerHTML = `
                    <tr>
                        <td colspan="2">${emptyState("Sem cortes sugeridos", "Marque despesas como não essenciais para ver sugestões.")}</td>
                    </tr>
                `;
            } else {
                tbody.innerHTML = items.slice(0, 6).map((item) => `
                    <tr>
                        <td>${FinanceDashUi.escapeHtml(item.categoryName || item.description)}</td>
                        <td class="num">${FinanceDashUi.formatCurrency(item.amount)}</td>
                    </tr>
                `).join("");
            }
        }

        if (footnote) {
            const assumptions = cutAnalysis.assumptions || [];
            footnote.textContent = assumptions.length
                ? assumptions[assumptions.length - 1]
                : `Total cortável: ${FinanceDashUi.formatCurrency(cutAnalysis.totalCuttable)}.`;
        }
    }

    async function calculateMinimumPrice() {
        const hoursInput = document.getElementById("radar-min-price-hours");
        const output = document.getElementById("radar-min-price-output");
        const rateLabel = document.getElementById("radar-min-hourly-rate");
        const footnote = document.getElementById("radar-min-price-footnote");
        const button = document.getElementById("radar-min-price-calc");

        if (!hoursInput || !output) {
            return;
        }

        const hours = Number(hoursInput.value);
        if (!hours || hours <= 0) {
            output.textContent = "-";
            return;
        }

        try {
            FinanceDashUi.setButtonLoading(button, true, "Calculando...");
            const result = await FinanceDashApi.getRadarMinimumProjectPrice(hours);
            output.textContent = FinanceDashUi.formatCurrency(result.minimumProjectPrice);
            if (rateLabel) {
                rateLabel.textContent = FinanceDashUi.formatCurrency(result.minimumHourlyRate);
            }
            if (footnote) {
                footnote.textContent = (result.assumptions || []).join(" · ") || DISCLAIMER;
            }
        } catch (error) {
            output.textContent = "-";
            FinanceDashUi.showToast(error.message);
        } finally {
            FinanceDashUi.setButtonLoading(button, false);
        }
    }

    function askQuestion(question) {
        if (!question) {
            return;
        }
        appendMessage(question, "user");
        sendQuestionToApi(question);
    }

    async function sendQuestion() {
        const input = document.getElementById("radar-ask-input");
        const question = input?.value.trim();
        if (!question) {
            return;
        }
        input.value = "";
        askQuestion(question);
    }

    async function sendQuestionToApi(question) {
        const sendButton = document.getElementById("radar-ask-send");
        try {
            FinanceDashUi.setButtonLoading(sendButton, true, "Pensando...");
            const response = await FinanceDashApi.askRadar(question);
            appendMessage(response.answer || "Sem resposta disponível.", "bot");
        } catch (error) {
            appendMessage(error.message, "bot");
        } finally {
            FinanceDashUi.setButtonLoading(sendButton, false);
        }
    }

    function appendMessage(text, role) {
        const stream = document.getElementById("radar-chat-stream");
        if (!stream) {
            return;
        }

        const message = document.createElement("div");
        message.className = `msg ${role}`;
        if (role === "bot") {
            const includesDisclaimer = String(text).includes(DISCLAIMER);
            message.innerHTML = `
                <span class="msg-text">${formatBotMessage(text)}</span>
                ${includesDisclaimer ? "" : `<span class="disc">${DISCLAIMER}</span>`}
            `;
        } else {
            message.textContent = text;
        }
        stream.appendChild(message);
        stream.scrollTop = stream.scrollHeight;
    }

    function formatBotMessage(text) {
        return FinanceDashUi.escapeHtml(text).replaceAll("\n", "<br>");
    }

    function emptyState(title, message) {
        return `
            <div class="empty-state-box">
                <span class="empty-state-icon" aria-hidden="true">○</span>
                <strong>${FinanceDashUi.escapeHtml(title)}</strong>
                <p>${FinanceDashUi.escapeHtml(message)}</p>
            </div>
        `;
    }

    function severityToClass(severity) {
        if (severity === "CRITICAL") {
            return "crit";
        }
        if (severity === "WARNING") {
            return "warn";
        }
        return "info";
    }

    function mergeAssumptions(...groups) {
        const merged = [];
        const seen = new Set();
        groups.flat().filter(Boolean).forEach((item) => {
            if (!seen.has(item)) {
                seen.add(item);
                merged.push(item);
            }
        });
        return merged;
    }

    function setText(id, value) {
        const element = document.getElementById(id);
        if (element) {
            element.textContent = value;
        }
    }

    return {
        bindEvents,
        load
    };
})();
