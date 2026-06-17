const FinanceDashTransactions = (() => {
    let transactions = [];
    let currentPage = 0;
    let currentFilters = {
        type: "",
        categoryId: "",
        size: 10,
        sort: "transactionDate,desc"
    };
    let onChanged = async () => {};

    function buildPayload(formData) {
        const type = formData.get("type");
        const status = formData.get("status") || "PAID";
        const isRecurring = formData.get("isRecurring") === "on";
        const nonEssential = formData.get("nonEssential") === "on";

        return {
            description: formData.get("description"),
            amount: Number(formData.get("amount")),
            type,
            categoryId: formData.get("categoryId"),
            transactionDate: formData.get("transactionDate"),
            paymentMethod: formData.get("paymentMethod") || null,
            notes: formData.get("notes") || null,
            status,
            dueDate: status === "PAID" ? null : (formData.get("dueDate") || null),
            isRecurring,
            recurrenceRule: isRecurring ? (formData.get("recurrenceRule") || "MONTHLY") : "NONE",
            clientName: type === "INCOME" && status !== "PAID" ? (formData.get("clientName") || null) : null,
            essential: type === "EXPENSE" ? !nonEssential : null
        };
    }

    function syncRadarFieldVisibility(form) {
        if (!form) {
            return;
        }

        const type = form.elements.type?.value || "EXPENSE";
        const status = form.elements.status?.value || "PAID";
        const isRecurring = form.elements.isRecurring?.checked;

        toggleRadarField(form, "dueDate", status !== "PAID");
        toggleRadarField(form, "clientName", type === "INCOME" && status !== "PAID");
        toggleRadarField(form, "recurrenceRule", isRecurring);
        toggleRadarField(form, "nonEssential", type === "EXPENSE");
    }

    function toggleRadarField(form, fieldName, visible) {
        const label = form.querySelector(`[data-radar-field="${fieldName}"]`);
        label?.classList.toggle("hidden", !visible);
    }

    function bindRadarFieldEvents(form) {
        if (!form) {
            return;
        }

        ["type", "status", "isRecurring"].forEach((fieldName) => {
            form.elements[fieldName]?.addEventListener("change", () => syncRadarFieldVisibility(form));
            if (fieldName === "isRecurring") {
                form.elements[fieldName]?.addEventListener("click", () => syncRadarFieldVisibility(form));
            }
        });
        syncRadarFieldVisibility(form);
    }

    function fillRadarFields(form, transaction) {
        if (!form || !transaction) {
            return;
        }

        if (form.elements.status) {
            form.elements.status.value = transaction.status || "PAID";
        }
        if (form.elements.dueDate) {
            form.elements.dueDate.value = transaction.dueDate || "";
        }
        if (form.elements.isRecurring) {
            form.elements.isRecurring.checked = Boolean(transaction.isRecurring);
        }
        if (form.elements.recurrenceRule) {
            form.elements.recurrenceRule.value = transaction.recurrenceRule || "MONTHLY";
        }
        if (form.elements.clientName) {
            form.elements.clientName.value = transaction.clientName || "";
        }
        if (form.elements.nonEssential) {
            form.elements.nonEssential.checked = transaction.essential === false;
        }
        syncRadarFieldVisibility(form);
    }

    async function load(month, year) {
        const table = document.getElementById("transactions-table");
        if (table) {
            table.innerHTML = `<tr><td colspan="6" class="empty-state">Carregando lançamentos...</td></tr>`;
        }

        const page = await FinanceDashApi.getTransactions({
            month,
            year,
            page: currentPage,
            ...currentFilters
        });
        transactions = page.content || [];
        renderTable(transactions);
        renderPagination(page);
        document.getElementById("transactions-total").textContent = `${page.totalElements || 0} registros`;
        return page;
    }

    function renderPagination(page) {
        const prevButton = document.getElementById("transactions-prev");
        const nextButton = document.getElementById("transactions-next");
        const label = document.getElementById("transactions-page-label");
        const totalPages = page.totalPages || 0;
        const pageNumber = page.number ?? currentPage;

        currentPage = pageNumber;

        if (label) {
            if (totalPages <= 1) {
                label.textContent = totalPages === 0 ? "Nenhuma página" : "Página 1 de 1";
            } else {
                label.textContent = `Página ${pageNumber + 1} de ${totalPages}`;
            }
        }

        if (prevButton) {
            prevButton.disabled = pageNumber <= 0;
        }
        if (nextButton) {
            nextButton.disabled = totalPages === 0 || pageNumber >= totalPages - 1;
        }
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
                <td>${renderDescription(transaction)}</td>
                <td>${renderCategory(transaction)}</td>
                <td>${renderTypeBadge(transaction)}</td>
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

    function renderDescription(transaction) {
        const statusLabel = renderStatusHint(transaction);
        return `
            <div>${FinanceDashUi.escapeHtml(transaction.description)}</div>
            ${statusLabel}
        `;
    }

    function renderStatusHint(transaction) {
        if (!transaction.status || transaction.status === "PAID") {
            return "";
        }

        const labels = {
            PENDING: "Pendente",
            OVERDUE: "Atrasado"
        };
        const label = labels[transaction.status] || transaction.status;
        const client = transaction.clientName ? ` · ${FinanceDashUi.escapeHtml(transaction.clientName)}` : "";
        const dueDate = transaction.dueDate ? ` · venc. ${FinanceDashUi.formatDate(transaction.dueDate)}` : "";
        return `<small class="muted-label">${label}${client}${dueDate}</small>`;
    }

    function renderTypeBadge(transaction) {
        return `<span class="badge ${transaction.type}">${transaction.type === "INCOME" ? "Receita" : "Despesa"}</span>`;
    }

    function renderCategory(transaction) {
        const color = transaction.categoryColor || "#38BDF8";
        return `<span style="display:inline-flex;align-items:center;gap:.45rem"><span class="color-dot" style="background:${color}"></span>${FinanceDashUi.escapeHtml(transaction.categoryName)}</span>`;
    }

    function bindEvents(onSaved) {
        onChanged = onSaved;
        const form = document.getElementById("transaction-form");
        const feedback = document.getElementById("transaction-feedback");
        bindRadarFieldEvents(form);

        form?.addEventListener("submit", async (event) => {
            event.preventDefault();
            const submitButton = document.getElementById("transaction-submit");
            FinanceDashUi.setFeedback(feedback, "Salvando...");

            const formData = new FormData(form);
            const id = formData.get("transactionId");
            const payload = buildPayload(formData);

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
                currentPage = 0;
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
        document.getElementById("transaction-edit-type")?.addEventListener("change", () => {
            renderEditCategoryOptions();
            syncRadarFieldVisibility(document.getElementById("transaction-edit-form"));
        });
        document.getElementById("transaction-edit-modal")?.addEventListener("click", (event) => {
            if (event.target.id === "transaction-edit-modal") {
                closeEditModal();
            }
        });
        document.getElementById("transactions-prev")?.addEventListener("click", async () => {
            if (currentPage <= 0) {
                return;
            }
            currentPage -= 1;
            await onChanged();
        });
        document.getElementById("transactions-next")?.addEventListener("click", async () => {
            currentPage += 1;
            await onChanged();
        });

        bindRadarFieldEvents(document.getElementById("transaction-edit-form"));
    }

    function setDefaultDate() {
        const input = document.querySelector("#transaction-form input[name='transactionDate']");
        if (input && !input.value) {
            input.value = new Date().toISOString().slice(0, 10);
        }
    }

    function resetForm() {
        const form = document.getElementById("transaction-form");
        const idInput = document.getElementById("transaction-id");
        const submitButton = document.getElementById("transaction-submit");

        form?.reset();
        if (idInput) {
            idInput.value = "";
        }
        if (submitButton) {
            submitButton.textContent = "Salvar lançamento";
        }
        setDefaultDate();
        FinanceDashCategories.renderTransactionCategoryOptions();
        syncRadarFieldVisibility(form);
    }

    function openEditModal(transaction) {
        const modal = document.getElementById("transaction-edit-modal");
        const form = document.getElementById("transaction-edit-form");
        if (!form) {
            return;
        }

        form.elements.transactionId.value = transaction.id;
        form.elements.description.value = transaction.description;
        form.elements.amount.value = transaction.amount;
        form.elements.type.value = transaction.type;
        renderEditCategoryOptions(transaction.categoryId);
        form.elements.categoryId.value = transaction.categoryId;
        form.elements.transactionDate.value = transaction.transactionDate;
        form.elements.paymentMethod.value = transaction.paymentMethod || "";
        form.elements.notes.value = transaction.notes || "";
        fillRadarFields(form, transaction);
        FinanceDashUi.setFeedback(document.getElementById("transaction-edit-feedback"), "");
        FinanceDashUi.showModal(modal, "input[name='description']");
    }

    function closeEditModal() {
        const modal = document.getElementById("transaction-edit-modal");
        const form = document.getElementById("transaction-edit-form");
        FinanceDashUi.hideModal(modal);
        form?.reset();
        syncRadarFieldVisibility(form);
    }

    function renderEditCategoryOptions(selectedId = "") {
        const typeSelect = document.getElementById("transaction-edit-type");
        const categorySelect = document.getElementById("transaction-edit-category");
        if (!typeSelect || !categorySelect) {
            return;
        }

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
        const payload = buildPayload(formData);

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
        if (!button || !row) {
            return;
        }

        const transaction = transactions.find((candidate) => candidate.id === row.dataset.transactionId);
        if (!transaction) {
            return;
        }

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

        if (!confirmed) {
            return;
        }

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
        currentPage = 0;
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
        currentPage = 0;
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
