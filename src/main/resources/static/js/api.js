const FinanceDashApi = (() => {
    const tokenKey = "financeDashToken";
    const refreshTokenKey = "financeDashRefreshToken";
    const defaultHeaders = {
        "Content-Type": "application/json"
    };
    let refreshPromise = null;

    async function request(path, options = {}, allowRefresh = true) {
        const headers = {
            ...defaultHeaders,
            ...(options.headers || {})
        };
        const token = getToken();
        if (token) {
            headers.Authorization = `Bearer ${token}`;
        }

        const response = await fetch(path, {
            ...options,
            headers
        });

        if (response.status === 204) {
            return null;
        }

        const contentType = response.headers.get("content-type") || "";
        const body = contentType.includes("application/json") ? await response.json() : await response.text();

        if (!response.ok) {
            if (response.status === 401 && allowRefresh && getRefreshToken() && !path.endsWith("/refresh")) {
                const refreshed = await tryRefreshToken();
                if (refreshed) {
                    return request(path, options, false);
                }
            }

            if (response.status === 401) {
                clearSession();
                window.dispatchEvent(new CustomEvent("financedash:unauthorized"));
            }
            if (response.status === 402) {
                window.dispatchEvent(new CustomEvent("financedash:subscription-required"));
            }
            const message = typeof body === "object" && body.message ? body.message : "Erro ao comunicar com a API";
            throw new Error(message);
        }

        return body;
    }

    async function tryRefreshToken() {
        if (refreshPromise) {
            return refreshPromise;
        }

        refreshPromise = (async () => {
            try {
                const response = await fetch("/api/v1/auth/refresh", {
                    method: "POST",
                    headers: defaultHeaders,
                    body: JSON.stringify({ refreshToken: getRefreshToken() })
                });

                if (!response.ok) {
                    return false;
                }

                const body = await response.json();
                setToken(body.token);
                setRefreshToken(body.refreshToken);
                return true;
            } catch (error) {
                return false;
            } finally {
                refreshPromise = null;
            }
        })();

        return refreshPromise;
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

    function setToken(token) {
        window.localStorage.setItem(tokenKey, token);
    }

    function getToken() {
        return window.localStorage.getItem(tokenKey);
    }

    function setRefreshToken(token) {
        if (token) {
            window.localStorage.setItem(refreshTokenKey, token);
        }
    }

    function getRefreshToken() {
        return window.localStorage.getItem(refreshTokenKey);
    }

    function clearSession() {
        window.localStorage.removeItem(tokenKey);
        window.localStorage.removeItem(refreshTokenKey);
    }

    function storeAuthResponse(response) {
        setToken(response.token);
        setRefreshToken(response.refreshToken);
    }

    return {
        setToken,
        getToken,
        setRefreshToken,
        getRefreshToken,
        clearToken: clearSession,
        clearSession,
        storeAuthResponse,
        register: (payload) => request("/api/v1/auth/register", {
            method: "POST",
            body: JSON.stringify(payload)
        }),
        login: (payload) => request("/api/v1/auth/login", {
            method: "POST",
            body: JSON.stringify(payload)
        }),
        refresh: (payload) => request("/api/v1/auth/refresh", {
            method: "POST",
            body: JSON.stringify(payload)
        }, false),
        forgotPassword: (payload) => request("/api/v1/auth/forgot-password", {
            method: "POST",
            body: JSON.stringify(payload)
        }, false),
        resetPassword: (payload) => request("/api/v1/auth/reset-password", {
            method: "POST",
            body: JSON.stringify(payload)
        }, false),
        verifyEmail: (token) => request(`/api/v1/auth/verify-email${query({ token })}`, {}, false),
        me: () => request("/api/v1/auth/me"),
        checkoutPro: () => request("/api/v1/billing/checkout/pro", { method: "POST" }),
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
        }),
        getRadarMonthProjection: () => request("/api/v1/radar/month-projection"),
        getRadarSafeToSpend: () => request("/api/v1/radar/safe-to-spend"),
        getRadarOverdueReceivables: () => request("/api/v1/radar/overdue-receivables"),
        getRadarFreelanceGap: () => request("/api/v1/radar/freelance-gap"),
        getRadarMinimumProjectPrice: (estimatedHours) => request(
            `/api/v1/radar/minimum-project-price${query({ estimatedHours })}`
        ),
        getRadarCutAnalysis: () => request("/api/v1/radar/cut-analysis"),
        getRadarAlerts: (unreadOnly = false) => request(
            `/api/v1/radar/alerts${query({ unreadOnly })}`
        ),
        markRadarAlertRead: (id) => request(`/api/v1/radar/alerts/${id}/read`, { method: "POST" }),
        askRadar: (question) => request("/api/v1/radar/ask", {
            method: "POST",
            body: JSON.stringify({ question })
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
