const FinanceDashTransactions = (() => {
    async function load(month, year) {
        const page = await FinanceDashApi.getTransactions({ month, year, page: 0, size: 10, sort: "transactionDate,desc" });
        renderTable(page.content || []);
        document.getElementById("transactions-total").textContent = `${page.totalElements || 0} registros`;
        return page;
    }

    function renderTable(transactions) {
        const table = document.getElementById("transactions-table");

        if (!transactions.length) {
            table.innerHTML = `<tr><td colspan="5" class="empty-state">Nenhum lançamento encontrado.</td></tr>`;
            return;
        }

        table.innerHTML = transactions.map((transaction) => `
            <tr>
                <td>${FinanceDashUi.formatDate(transaction.transactionDate)}</td>
                <td>${transaction.description}</td>
                <td>${renderCategory(transaction)}</td>
                <td><span class="badge ${transaction.type}">${transaction.type === "INCOME" ? "Receita" : "Despesa"}</span></td>
                <td>${FinanceDashUi.formatCurrency(transaction.amount)}</td>
            </tr>
        `).join("");
    }

    function renderCategory(transaction) {
        const color = transaction.categoryColor || "#38BDF8";
        return `<span style="display:inline-flex;align-items:center;gap:.45rem"><span style="width:.7rem;height:.7rem;border-radius:999px;background:${color}"></span>${transaction.categoryName}</span>`;
    }

    function bindEvents(onSaved) {
        const form = document.getElementById("transaction-form");
        const feedback = document.getElementById("transaction-feedback");
        form?.addEventListener("submit", async (event) => {
            event.preventDefault();
            feedback.textContent = "Salvando...";
            feedback.className = "feedback";

            const formData = new FormData(form);
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
                await FinanceDashApi.createTransaction(payload);
                form.reset();
                setDefaultDate();
                FinanceDashCategories.renderTransactionCategoryOptions();
                feedback.textContent = "Lançamento salvo com sucesso.";
                feedback.className = "feedback success";
                FinanceDashUi.showToast("Lançamento salvo com sucesso.");
                await onSaved();
            } catch (error) {
                feedback.textContent = error.message;
                feedback.className = "feedback error";
            }
        });
    }

    function setDefaultDate() {
        const input = document.querySelector("input[name='transactionDate']");
        if (input && !input.value) {
            input.value = new Date().toISOString().slice(0, 10);
        }
    }

    return {
        load,
        bindEvents,
        setDefaultDate
    };
})();

