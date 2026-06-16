const FinanceDashTransactions = (() => {
    let transactions = [];
    let currentFilters = {
        type: "",
        categoryId: "",
        size: 10,
        sort: "transactionDate,desc"
    };
    let onChanged = async () => {};

    async function load(month, year) {
        const table = document.getElementById("transactions-table");
        if (table) {
            table.innerHTML = `<tr><td colspan="6" class="empty-state">Carregando lançamentos...</td></tr>`;
        }

        const page = await FinanceDashApi.getTransactions({
            month,
            year,
            page: 0,
            ...currentFilters
        });
        transactions = page.content || [];
        renderTable(transactions);
        document.getElementById("transactions-total").textContent = `${page.totalElements || 0} registros`;
        return page;
    }

    function renderTable(transactions) {
        const table = document.getElementById("transactions-table");

        if (!transactions.length) {
            table.innerHTML = `<tr><td colspan="6" class="empty-state">Nenhum lançamento encontrado.</td></tr>`;
            return;
        }

        table.innerHTML = transactions.map((transaction) => `
            <tr data-transaction-id="${transaction.id}">
                <td>${FinanceDashUi.formatDate(transaction.transactionDate)}</td>
                <td>${FinanceDashUi.escapeHtml(transaction.description)}</td>
                <td>${renderCategory(transaction)}</td>
                <td><span class="badge ${transaction.type}">${transaction.type === "INCOME" ? "Receita" : "Despesa"}</span></td>
                <td>${FinanceDashUi.formatCurrency(transaction.amount)}</td>
                <td>
                    <div class="actions">
                        <button type="button" class="button-secondary" data-action="edit-transaction">Editar</button>
                        <button type="button" class="button-danger" data-action="delete-transaction">Excluir</button>
                    </div>
                </td>
            </tr>
        `).join("");
    }

    function renderCategory(transaction) {
        const color = transaction.categoryColor || "#38BDF8";
        return `<span style="display:inline-flex;align-items:center;gap:.45rem"><span class="color-dot" style="background:${color}"></span>${FinanceDashUi.escapeHtml(transaction.categoryName)}</span>`;
    }

    function bindEvents(onSaved) {
        onChanged = onSaved;
        const form = document.getElementById("transaction-form");
        const feedback = document.getElementById("transaction-feedback");
        form?.addEventListener("submit", async (event) => {
            event.preventDefault();
            const submitButton = document.getElementById("transaction-submit");
            FinanceDashUi.setFeedback(feedback, "Salvando...");

            const formData = new FormData(form);
            const id = formData.get("transactionId");
            const payload = {
                description: formData.get("description"),
                amount: Number(formData.get("amount")),
                type: formData.get("type"),
                categoryId: formData.get("categoryId"),
                transactionDate: formData.get("transactionDate"),
                paymentMethod: formData.get("paymentMethod") || null,
                notes: formData.get("notes") || null
            };

            try {
                FinanceDashUi.setButtonLoading(submitButton, true, id ? "Atualizando..." : "Salvando...");
                if (id) {
                    await FinanceDashApi.updateTransaction(id, payload);
                } else {
                    await FinanceDashApi.createTransaction(payload);
                }

                FinanceDashUi.setButtonLoading(submitButton, false);
                resetForm();
                FinanceDashUi.setFeedback(feedback, id ? "Lançamento atualizado com sucesso." : "Lançamento salvo com sucesso.", "success");
                FinanceDashUi.showToast(id ? "Lançamento atualizado." : "Lançamento salvo com sucesso.");
                await onSaved();
            } catch (error) {
                FinanceDashUi.setFeedback(feedback, error.message, "error");
            } finally {
                FinanceDashUi.setButtonLoading(submitButton, false);
            }
        });

        document.getElementById("transactions-table")?.addEventListener("click", handleTableClick);
        document.getElementById("transaction-filters")?.addEventListener("submit", handleFilterSubmit);
        document.getElementById("transaction-filters-reset")?.addEventListener("click", handleFilterReset);
        document.getElementById("transaction-edit-form")?.addEventListener("submit", handleEditSubmit);
        document.getElementById("transaction-edit-cancel")?.addEventListener("click", closeEditModal);
        document.getElementById("transaction-edit-close")?.addEventListener("click", closeEditModal);
        document.getElementById("transaction-edit-type")?.addEventListener("change", () => renderEditCategoryOptions());
        document.getElementById("transaction-edit-modal")?.addEventListener("click", (event) => {
            if (event.target.id === "transaction-edit-modal") {
                closeEditModal();
            }
        });
    }

    function setDefaultDate() {
        const input = document.querySelector("input[name='transactionDate']");
        if (input && !input.value) {
            input.value = new Date().toISOString().slice(0, 10);
        }
    }

    function resetForm() {
        const form = document.getElementById("transaction-form");
        const idInput = document.getElementById("transaction-id");
        const submitButton = document.getElementById("transaction-submit");

        form?.reset();
        if (idInput) idInput.value = "";
        if (submitButton) submitButton.textContent = "Salvar lançamento";
        setDefaultDate();
        FinanceDashCategories.renderTransactionCategoryOptions();
    }

    function openEditModal(transaction) {
        const modal = document.getElementById("transaction-edit-modal");
        const form = document.getElementById("transaction-edit-form");
        if (!form) return;

        form.elements.transactionId.value = transaction.id;
        form.elements.description.value = transaction.description;
        form.elements.amount.value = transaction.amount;
        form.elements.type.value = transaction.type;
        renderEditCategoryOptions(transaction.categoryId);
        form.elements.categoryId.value = transaction.categoryId;
        form.elements.transactionDate.value = transaction.transactionDate;
        form.elements.paymentMethod.value = transaction.paymentMethod || "";
        form.elements.notes.value = transaction.notes || "";
        FinanceDashUi.setFeedback(document.getElementById("transaction-edit-feedback"), "");
        modal.classList.add("show");
        modal.setAttribute("aria-hidden", "false");
    }

    function closeEditModal() {
        const modal = document.getElementById("transaction-edit-modal");
        const form = document.getElementById("transaction-edit-form");
        modal?.classList.remove("show");
        modal?.setAttribute("aria-hidden", "true");
        form?.reset();
    }

    function renderEditCategoryOptions(selectedId = "") {
        const typeSelect = document.getElementById("transaction-edit-type");
        const categorySelect = document.getElementById("transaction-edit-category");
        if (!typeSelect || !categorySelect) return;

        const options = FinanceDashCategories.getByType(typeSelect.value);
        categorySelect.innerHTML = options.length
            ? options.map((category) => `<option value="${category.id}">${FinanceDashUi.escapeHtml(category.name)}</option>`).join("")
            : "<option value=\"\">Nenhuma categoria disponível</option>";

        if (selectedId) {
            categorySelect.value = selectedId;
        }
    }

    async function handleEditSubmit(event) {
        event.preventDefault();
        const form = event.currentTarget;
        const feedback = document.getElementById("transaction-edit-feedback");
        const submitButton = document.getElementById("transaction-edit-submit");
        const formData = new FormData(form);
        const id = formData.get("transactionId");
        const payload = {
            description: formData.get("description"),
            amount: Number(formData.get("amount")),
            type: formData.get("type"),
            categoryId: formData.get("categoryId"),
            transactionDate: formData.get("transactionDate"),
            paymentMethod: formData.get("paymentMethod") || null,
            notes: formData.get("notes") || null
        };

        try {
            FinanceDashUi.setButtonLoading(submitButton, true, "Atualizando...");
            await FinanceDashApi.updateTransaction(id, payload);
            FinanceDashUi.setButtonLoading(submitButton, false);
            closeEditModal();
            FinanceDashUi.showToast("Lançamento atualizado.");
            await onChanged();
        } catch (error) {
            FinanceDashUi.setFeedback(feedback, error.message, "error");
        } finally {
            FinanceDashUi.setButtonLoading(submitButton, false);
        }
    }

    async function handleTableClick(event) {
        const button = event.target.closest("button[data-action]");
        const row = event.target.closest("[data-transaction-id]");
        if (!button || !row) return;

        const transaction = transactions.find((candidate) => candidate.id === row.dataset.transactionId);
        if (!transaction) return;

        if (button.dataset.action === "edit-transaction") {
            openEditModal(transaction);
            return;
        }

        const confirmed = await FinanceDashUi.confirmAction({
            title: "Excluir lançamento",
            message: `Tem certeza que deseja excluir "${transaction.description}"? O dashboard será recalculado depois da exclusão.`,
            confirmText: "Excluir",
            danger: true
        });

        if (!confirmed) return;

        try {
            FinanceDashUi.setButtonLoading(button, true, "Excluindo...");
            await FinanceDashApi.deleteTransaction(transaction.id);
            FinanceDashUi.showToast("Lançamento excluído.");
            await onChanged();
        } catch (error) {
            FinanceDashUi.showToast(error.message);
        } finally {
            FinanceDashUi.setButtonLoading(button, false);
        }
    }

    async function handleFilterSubmit(event) {
        event.preventDefault();
        const formData = new FormData(event.currentTarget);
        currentFilters = {
            type: formData.get("type"),
            categoryId: formData.get("categoryId"),
            size: Number(formData.get("size") || 10),
            sort: formData.get("sort") || "transactionDate,desc"
        };
        await onChanged();
    }

    async function handleFilterReset() {
        const form = document.getElementById("transaction-filters");
        form?.reset();
        currentFilters = {
            type: "",
            categoryId: "",
            size: 10,
            sort: "transactionDate,desc"
        };
        FinanceDashCategories.renderTransactionFilterCategoryOptions();
        await onChanged();
    }

    return {
        load,
        bindEvents,
        setDefaultDate,
        resetForm
    };
})();

