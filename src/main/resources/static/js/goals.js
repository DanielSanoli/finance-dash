const FinanceDashGoals = (() => {
    let goals = [];
    let onChanged = async () => {};
    let getSelectedPeriod = () => ({ month: new Date().getMonth() + 1, year: new Date().getFullYear() });

    async function load(month, year) {
        const container = document.getElementById("goals-list");
        if (container) {
            container.innerHTML = `<p class="empty-state">Carregando metas...</p>`;
        }

        goals = await FinanceDashApi.getGoals(month, year);
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
                <article class="goal-item" data-goal-id="${goal.id}">
                    <div class="goal-top">
                        <div>
                            <strong>${FinanceDashUi.escapeHtml(goal.title)}</strong>
                            <p class="subtitle">${labelType(goal.type)}${goal.categoryName ? ` • ${FinanceDashUi.escapeHtml(goal.categoryName)}` : ""}</p>
                        </div>
                        <strong>${FinanceDashUi.formatPercent(progress)}</strong>
                    </div>
                    <div class="progress-bar" aria-label="Progresso de ${FinanceDashUi.escapeHtml(goal.title)}">
                        <span style="width:${safeProgress}%"></span>
                    </div>
                    <div class="goal-footer">
                        <p class="subtitle">${FinanceDashUi.formatCurrency(goal.currentAmount)} de ${FinanceDashUi.formatCurrency(goal.targetAmount)}</p>
                        <div class="actions">
                            <button type="button" class="button-secondary" data-action="edit-goal">Editar</button>
                            <button type="button" class="button-danger" data-action="delete-goal">Excluir</button>
                        </div>
                    </div>
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

    function resetForm() {
        const form = document.getElementById("goal-form");
        const idInput = document.getElementById("goal-id");
        const submitButton = document.getElementById("goal-submit");
        const cancelButton = document.getElementById("goal-cancel");
        const { month, year } = getSelectedPeriod();

        form?.reset();
        if (idInput) idInput.value = "";
        if (form) {
            form.elements.month.value = String(month);
            form.elements.year.value = String(year);
        }
        if (submitButton) submitButton.textContent = "Salvar meta";
        cancelButton?.classList.add("hidden");
        FinanceDashCategories.renderGoalCategoryOptions();
    }

    function fillForm(goal) {
        const form = document.getElementById("goal-form");
        const submitButton = document.getElementById("goal-submit");
        const cancelButton = document.getElementById("goal-cancel");
        if (!form) return;

        form.elements.goalId.value = goal.id;
        form.elements.title.value = goal.title;
        form.elements.month.value = goal.month;
        form.elements.year.value = goal.year;
        form.elements.targetAmount.value = goal.targetAmount;
        form.elements.type.value = goal.type;
        FinanceDashCategories.renderGoalCategoryOptions();
        form.elements.categoryId.value = goal.categoryId || "";
        if (submitButton) submitButton.textContent = "Atualizar meta";
        cancelButton?.classList.remove("hidden");
        form.scrollIntoView({ behavior: "smooth", block: "center" });
    }

    async function handleSubmit(event) {
        event.preventDefault();
        const form = event.currentTarget;
        const feedback = document.getElementById("goal-feedback");
        const submitButton = document.getElementById("goal-submit");
        const formData = new FormData(form);
        const id = formData.get("goalId");
        const categoryId = formData.get("categoryId");
        const payload = {
            title: formData.get("title"),
            month: Number(formData.get("month")),
            year: Number(formData.get("year")),
            targetAmount: Number(formData.get("targetAmount")),
            type: formData.get("type"),
            categoryId: categoryId || null
        };

        try {
            FinanceDashUi.setButtonLoading(submitButton, true, id ? "Atualizando..." : "Salvando...");
            if (id) {
                await FinanceDashApi.updateGoal(id, payload);
            } else {
                await FinanceDashApi.createGoal(payload);
            }

            FinanceDashUi.setButtonLoading(submitButton, false);
            resetForm();
            FinanceDashUi.setFeedback(feedback, id ? "Meta atualizada com sucesso." : "Meta salva com sucesso.", "success");
            FinanceDashUi.showToast(id ? "Meta atualizada." : "Meta criada.");
            await onChanged();
        } catch (error) {
            FinanceDashUi.setFeedback(feedback, error.message, "error");
        } finally {
            FinanceDashUi.setButtonLoading(submitButton, false);
        }
    }

    async function handleListClick(event) {
        const button = event.target.closest("button[data-action]");
        const item = event.target.closest("[data-goal-id]");
        if (!button || !item) return;

        const goal = goals.find((candidate) => candidate.id === item.dataset.goalId);
        if (!goal) return;

        if (button.dataset.action === "edit-goal") {
            fillForm(goal);
            return;
        }

        const confirmed = await FinanceDashUi.confirmAction({
            title: "Excluir meta",
            message: `Tem certeza que deseja excluir "${goal.title}"?`,
            confirmText: "Excluir",
            danger: true
        });

        if (!confirmed) return;

        try {
            FinanceDashUi.setButtonLoading(button, true, "Excluindo...");
            await FinanceDashApi.deleteGoal(goal.id);
            FinanceDashUi.showToast("Meta excluída.");
            await onChanged();
        } catch (error) {
            FinanceDashUi.showToast(error.message);
        } finally {
            FinanceDashUi.setButtonLoading(button, false);
        }
    }

    function syncFormPeriod() {
        const form = document.getElementById("goal-form");
        if (!form || form.elements.goalId.value) return;
        const { month, year } = getSelectedPeriod();
        form.elements.month.value = String(month);
        form.elements.year.value = String(year);
    }

    function bindEvents(callback, periodGetter) {
        onChanged = callback || onChanged;
        getSelectedPeriod = periodGetter || getSelectedPeriod;
        document.getElementById("goal-form")?.addEventListener("submit", handleSubmit);
        document.getElementById("goal-cancel")?.addEventListener("click", resetForm);
        document.getElementById("goals-list")?.addEventListener("click", handleListClick);
        resetForm();
    }

    return {
        load,
        bindEvents,
        syncFormPeriod
    };
})();

