const FinanceDashApi = (() => {
    const defaultHeaders = {
        "Content-Type": "application/json"
    };

    async function request(path, options = {}) {
        const response = await fetch(path, {
            headers: defaultHeaders,
            ...options
        });

        if (response.status === 204) {
            return null;
        }

        const contentType = response.headers.get("content-type") || "";
        const body = contentType.includes("application/json") ? await response.json() : await response.text();

        if (!response.ok) {
            const message = typeof body === "object" && body.message ? body.message : "Erro ao comunicar com a API";
            throw new Error(message);
        }

        return body;
    }

    function query(params) {
        const search = new URLSearchParams();
        Object.entries(params).forEach(([key, value]) => {
            if (value !== undefined && value !== null && value !== "") {
                search.append(key, value);
            }
        });
        const queryString = search.toString();
        return queryString ? `?${queryString}` : "";
    }

    return {
        getDashboard: (month, year) => request(`/api/v1/dashboard/monthly${query({ month, year })}`),
        getCategories: () => request("/api/v1/categories"),
        createCategory: (payload) => request("/api/v1/categories", {
            method: "POST",
            body: JSON.stringify(payload)
        }),
        updateCategory: (id, payload) => request(`/api/v1/categories/${id}`, {
            method: "PUT",
            body: JSON.stringify(payload)
        }),
        deleteCategory: (id) => request(`/api/v1/categories/${id}`, {
            method: "DELETE"
        }),
        getTransactions: (params) => request(`/api/v1/transactions${query(params)}`),
        createTransaction: (payload) => request("/api/v1/transactions", {
            method: "POST",
            body: JSON.stringify(payload)
        }),
        updateTransaction: (id, payload) => request(`/api/v1/transactions/${id}`, {
            method: "PUT",
            body: JSON.stringify(payload)
        }),
        deleteTransaction: (id) => request(`/api/v1/transactions/${id}`, {
            method: "DELETE"
        }),
        getGoals: (month, year) => request(`/api/v1/goals/monthly${query({ month, year })}`),
        createGoal: (payload) => request("/api/v1/goals", {
            method: "POST",
            body: JSON.stringify(payload)
        }),
        updateGoal: (id, payload) => request(`/api/v1/goals/${id}`, {
            method: "PUT",
            body: JSON.stringify(payload)
        }),
        deleteGoal: (id) => request(`/api/v1/goals/${id}`, {
            method: "DELETE"
        })
    };
})();

const FinanceDashUi = (() => {
    const currencyFormatter = new Intl.NumberFormat("pt-BR", {
        style: "currency",
        currency: "BRL"
    });

    const percentFormatter = new Intl.NumberFormat("pt-BR", {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });

    function formatCurrency(value) {
        return currencyFormatter.format(Number(value || 0));
    }

    function formatPercent(value) {
        return `${percentFormatter.format(Number(value || 0))}%`;
    }

    function formatDate(value) {
        if (!value) return "-";
        const [year, month, day] = value.split("-");
        return `${day}/${month}/${year}`;
    }

    function showToast(message) {
        const toast = document.getElementById("toast");
        toast.textContent = message;
        toast.classList.add("show");
        window.setTimeout(() => toast.classList.remove("show"), 3200);
    }

    function escapeHtml(value) {
        return String(value ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll("\"", "&quot;")
            .replaceAll("'", "&#039;");
    }

    function setButtonLoading(button, loading, loadingText = "Salvando...") {
        if (!button) return;

        if (loading) {
            button.dataset.originalText = button.textContent;
            button.textContent = loadingText;
            button.disabled = true;
            button.classList.add("is-loading");
            return;
        }

        button.textContent = button.dataset.originalText || button.textContent;
        button.disabled = false;
        button.classList.remove("is-loading");
        delete button.dataset.originalText;
    }

    function setFeedback(element, message, type = "") {
        if (!element) return;
        element.textContent = message;
        element.className = `feedback ${type}`.trim();
    }

    function confirmAction({ title, message, confirmText = "Confirmar", danger = false }) {
        const modal = document.getElementById("confirm-modal");
        const titleElement = document.getElementById("confirm-modal-title");
        const messageElement = document.getElementById("confirm-modal-message");
        const confirmButton = document.getElementById("confirm-modal-confirm");
        const cancelButton = document.getElementById("confirm-modal-cancel");

        titleElement.textContent = title;
        messageElement.textContent = message;
        confirmButton.textContent = confirmText;
        confirmButton.classList.toggle("danger", danger);

        return new Promise((resolve) => {
            function close(result) {
                modal.classList.remove("show");
                modal.setAttribute("aria-hidden", "true");
                confirmButton.removeEventListener("click", onConfirm);
                cancelButton.removeEventListener("click", onCancel);
                modal.removeEventListener("click", onBackdrop);
                resolve(result);
            }

            function onConfirm() {
                close(true);
            }

            function onCancel() {
                close(false);
            }

            function onBackdrop(event) {
                if (event.target === modal) {
                    close(false);
                }
            }

            confirmButton.addEventListener("click", onConfirm);
            cancelButton.addEventListener("click", onCancel);
            modal.addEventListener("click", onBackdrop);
            modal.classList.add("show");
            modal.setAttribute("aria-hidden", "false");
        });
    }

    return {
        formatCurrency,
        formatPercent,
        formatDate,
        showToast,
        escapeHtml,
        setButtonLoading,
        setFeedback,
        confirmAction
    };
})();

