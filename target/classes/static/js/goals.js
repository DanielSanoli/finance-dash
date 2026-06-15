const FinanceDashGoals = (() => {
    async function load(month, year) {
        const goals = await FinanceDashApi.getGoals(month, year);
        renderGoals(goals || []);
        return goals;
    }

    function renderGoals(goals) {
        const container = document.getElementById("goals-list");

        if (!goals.length) {
            container.innerHTML = `<p class="empty-state">Nenhuma meta cadastrada para o período.</p>`;
            return;
        }

        container.innerHTML = goals.map((goal) => {
            const progress = Number(goal.progressPercentage || 0);
            const safeProgress = Math.max(0, Math.min(progress, 100));
            return `
                <article class="goal-item">
                    <div class="goal-top">
                        <div>
                            <strong>${goal.title}</strong>
                            <p class="subtitle">${labelType(goal.type)}${goal.categoryName ? ` • ${goal.categoryName}` : ""}</p>
                        </div>
                        <strong>${FinanceDashUi.formatPercent(progress)}</strong>
                    </div>
                    <div class="progress-bar" aria-label="Progresso de ${goal.title}">
                        <span style="width:${safeProgress}%"></span>
                    </div>
                    <p class="subtitle" style="margin:.75rem 0 0">
                        ${FinanceDashUi.formatCurrency(goal.currentAmount)} de ${FinanceDashUi.formatCurrency(goal.targetAmount)}
                    </p>
                </article>
            `;
        }).join("");
    }

    function labelType(type) {
        const labels = {
            INCOME_TARGET: "Meta de receita",
            EXPENSE_LIMIT: "Limite de despesa",
            SAVINGS_TARGET: "Meta de economia"
        };
        return labels[type] || type;
    }

    return {
        load
    };
})();

