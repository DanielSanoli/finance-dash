const FinanceDashCategories = (() => {
    let categories = [];
    let onChanged = async () => {};

    async function load() {
        categories = await FinanceDashApi.getCategories();
        renderTransactionCategoryOptions();
        renderTransactionFilterCategoryOptions();
        renderGoalCategoryOptions();
        renderList();
        return categories;
    }

    function getByType(type) {
        return categories.filter((category) => (!type || category.type === type) && category.active);
    }

    function renderTransactionCategoryOptions() {
        const typeSelect = document.getElementById("transaction-type");
        const categorySelect = document.getElementById("transaction-category");
        if (!typeSelect || !categorySelect) return;

        const selectedType = typeSelect.value;
        const options = getByType(selectedType);

        categorySelect.innerHTML = options.length
            ? options.map((category) => `<option value="${category.id}">${category.name}</option>`).join("")
            : "<option value=\"\">Nenhuma categoria disponível</option>";
    }

    function renderTransactionFilterCategoryOptions() {
        const typeSelect = document.getElementById("transaction-filter-type");
        const categorySelect = document.getElementById("transaction-filter-category");
        if (!categorySelect) return;

        const selectedType = typeSelect?.value || "";
        const options = getByType(selectedType);
        categorySelect.innerHTML = "<option value=\"\">Todas as categorias</option>"
            + options.map((category) => `<option value="${category.id}">${FinanceDashUi.escapeHtml(category.name)}</option>`).join("");
    }

    function renderGoalCategoryOptions() {
        const typeSelect = document.getElementById("goal-type");
        const categorySelect = document.getElementById("goal-category");
        if (!typeSelect || !categorySelect) return;

        const typeByGoal = {
            INCOME_TARGET: "INCOME",
            EXPENSE_LIMIT: "EXPENSE"
        };
        const categoryType = typeByGoal[typeSelect.value];
        const options = categoryType ? getByType(categoryType) : [];
        categorySelect.disabled = !categoryType;
        categorySelect.innerHTML = "<option value=\"\">Meta geral</option>"
            + options.map((category) => `<option value="${category.id}">${FinanceDashUi.escapeHtml(category.name)}</option>`).join("");
    }

    function renderList() {
        const container = document.getElementById("categories-list");
        if (!container) return;

        if (!categories.length) {
            container.innerHTML = `<p class="empty-state">Nenhuma categoria cadastrada.</p>`;
            return;
        }

        container.innerHTML = categories.map((category) => `
            <article class="list-item" data-category-id="${category.id}">
                <div class="list-item-main">
                    <span class="color-dot" style="background:${category.color}"></span>
                    <div>
                        <strong>${FinanceDashUi.escapeHtml(category.name)}</strong>
                        <p class="subtitle">${category.type === "INCOME" ? "Receita" : "Despesa"}</p>
                    </div>
                </div>
                <div class="actions">
                    <button type="button" class="button-secondary" data-action="edit-category">Editar</button>
                    <button type="button" class="button-danger" data-action="delete-category">Excluir</button>
                </div>
            </article>
        `).join("");
    }

    function resetForm() {
        const form = document.getElementById("category-form");
        const idInput = document.getElementById("category-id");
        const submitButton = document.getElementById("category-submit");
        const cancelButton = document.getElementById("category-cancel");

        form?.reset();
        if (idInput) idInput.value = "";
        if (submitButton) submitButton.textContent = "Salvar categoria";
        cancelButton?.classList.add("hidden");
    }

    function fillForm(category) {
        const form = document.getElementById("category-form");
        const submitButton = document.getElementById("category-submit");
        const cancelButton = document.getElementById("category-cancel");
        if (!form) return;

        form.elements.categoryId.value = category.id;
        form.elements.name.value = category.name;
        form.elements.type.value = category.type;
        form.elements.color.value = category.color;
        if (submitButton) submitButton.textContent = "Atualizar categoria";
        cancelButton?.classList.remove("hidden");
        form.scrollIntoView({ behavior: "smooth", block: "center" });
    }

    async function handleSubmit(event) {
        event.preventDefault();
        const form = event.currentTarget;
        const feedback = document.getElementById("category-feedback");
        const submitButton = document.getElementById("category-submit");
        const formData = new FormData(form);
        const id = formData.get("categoryId");
        const payload = {
            name: formData.get("name"),
            type: formData.get("type"),
            color: formData.get("color")
        };

        try {
            FinanceDashUi.setButtonLoading(submitButton, true, id ? "Atualizando..." : "Salvando...");
            if (id) {
                await FinanceDashApi.updateCategory(id, payload);
            } else {
                await FinanceDashApi.createCategory(payload);
            }

            FinanceDashUi.setButtonLoading(submitButton, false);
            resetForm();
            FinanceDashUi.setFeedback(feedback, id ? "Categoria atualizada com sucesso." : "Categoria salva com sucesso.", "success");
            FinanceDashUi.showToast(id ? "Categoria atualizada." : "Categoria criada.");
            await load();
            await onChanged();
        } catch (error) {
            FinanceDashUi.setFeedback(feedback, error.message, "error");
        } finally {
            FinanceDashUi.setButtonLoading(submitButton, false);
        }
    }

    async function handleListClick(event) {
        const button = event.target.closest("button[data-action]");
        const item = event.target.closest("[data-category-id]");
        if (!button || !item) return;

        const category = categories.find((candidate) => candidate.id === item.dataset.categoryId);
        if (!category) return;

        if (button.dataset.action === "edit-category") {
            fillForm(category);
            return;
        }

        const confirmed = await FinanceDashUi.confirmAction({
            title: "Excluir categoria",
            message: `Tem certeza que deseja excluir "${category.name}"? Categorias em uso deixam de aparecer para novos lançamentos.`,
            confirmText: "Excluir",
            danger: true
        });

        if (!confirmed) return;

        try {
            FinanceDashUi.setButtonLoading(button, true, "Excluindo...");
            await FinanceDashApi.deleteCategory(category.id);
            FinanceDashUi.showToast("Categoria excluída.");
            await load();
            await onChanged();
        } catch (error) {
            FinanceDashUi.showToast(error.message);
        } finally {
            FinanceDashUi.setButtonLoading(button, false);
        }
    }

    function bindEvents(callback) {
        onChanged = callback || onChanged;
        const typeSelect = document.getElementById("transaction-type");
        typeSelect?.addEventListener("change", renderTransactionCategoryOptions);
        document.getElementById("transaction-filter-type")?.addEventListener("change", renderTransactionFilterCategoryOptions);
        document.getElementById("goal-type")?.addEventListener("change", renderGoalCategoryOptions);
        document.getElementById("category-form")?.addEventListener("submit", handleSubmit);
        document.getElementById("category-cancel")?.addEventListener("click", resetForm);
        document.getElementById("categories-list")?.addEventListener("click", handleListClick);
    }

    return {
        load,
        getByType,
        bindEvents,
        renderTransactionCategoryOptions,
        renderTransactionFilterCategoryOptions,
        renderGoalCategoryOptions
    };
})();

