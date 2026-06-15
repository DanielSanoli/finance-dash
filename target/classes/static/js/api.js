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
        getTransactions: (params) => request(`/api/v1/transactions${query(params)}`),
        createTransaction: (payload) => request("/api/v1/transactions", {
            method: "POST",
            body: JSON.stringify(payload)
        }),
        getGoals: (month, year) => request(`/api/v1/goals/monthly${query({ month, year })}`)
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

    return {
        formatCurrency,
        formatPercent,
        formatDate,
        showToast
    };
})();

