const FinanceDashAuth = (() => {
    let onAuthenticated = async () => {};

    async function init(callback) {
        onAuthenticated = callback;
        bindEvents();
        window.addEventListener("financedash:unauthorized", () => {
            showLoggedOut();
            FinanceDashUi.showToast("Sessão expirada. Faça login novamente.");
        });

        if (!FinanceDashApi.getToken()) {
            showLoggedOut();
            return;
        }

        try {
            const user = await FinanceDashApi.me();
            await showLoggedIn(user);
        } catch (error) {
            FinanceDashApi.clearToken();
            showLoggedOut();
        }
    }

    function bindEvents() {
        document.getElementById("login-tab")?.addEventListener("click", () => showAuthForm("login"));
        document.getElementById("register-tab")?.addEventListener("click", () => showAuthForm("register"));
        document.getElementById("login-form")?.addEventListener("submit", handleLogin);
        document.getElementById("register-form")?.addEventListener("submit", handleRegister);
        document.getElementById("account-button")?.addEventListener("click", () => {
            document.getElementById("account-panel")?.scrollIntoView({ behavior: "smooth", block: "start" });
        });
        document.getElementById("logout-button")?.addEventListener("click", logout);
    }

    function showAuthForm(formName) {
        const isLogin = formName === "login";
        document.getElementById("login-form")?.classList.toggle("hidden", !isLogin);
        document.getElementById("register-form")?.classList.toggle("hidden", isLogin);
        document.getElementById("login-tab")?.classList.toggle("button-secondary", !isLogin);
        document.getElementById("register-tab")?.classList.toggle("button-secondary", isLogin);
    }

    async function handleLogin(event) {
        event.preventDefault();
        await submitAuthForm(event.currentTarget, "login-submit", "login-feedback", FinanceDashApi.login);
    }

    async function handleRegister(event) {
        event.preventDefault();
        await submitAuthForm(event.currentTarget, "register-submit", "register-feedback", FinanceDashApi.register);
    }

    async function submitAuthForm(form, buttonId, feedbackId, action) {
        const button = document.getElementById(buttonId);
        const feedback = document.getElementById(feedbackId);
        const formData = new FormData(form);
        const payload = Object.fromEntries(formData.entries());

        try {
            FinanceDashUi.setButtonLoading(button, true, "Aguarde...");
            FinanceDashUi.setFeedback(feedback, "");
            const response = await action(payload);
            FinanceDashApi.setToken(response.token);
            await showLoggedIn(response.user);
            FinanceDashUi.showToast("Acesso liberado.");
        } catch (error) {
            FinanceDashUi.setFeedback(feedback, error.message, "error");
        } finally {
            FinanceDashUi.setButtonLoading(button, false);
        }
    }

    async function showLoggedIn(user) {
        document.getElementById("auth-section")?.classList.add("hidden");
        document.getElementById("app-content")?.classList.remove("hidden");
        document.getElementById("period-filter")?.classList.remove("hidden");
        document.getElementById("user-menu")?.classList.remove("hidden");
        document.getElementById("user-menu-name").textContent = user.name;
        renderAccount(user);
        if (user.accessAllowed) {
            await onAuthenticated();
        }
    }

    function showLoggedOut() {
        document.getElementById("auth-section")?.classList.remove("hidden");
        document.getElementById("app-content")?.classList.add("hidden");
        document.getElementById("period-filter")?.classList.add("hidden");
        document.getElementById("user-menu")?.classList.add("hidden");
        document.getElementById("subscription-warning")?.classList.add("hidden");
        showAuthForm("login");
    }

    function renderAccount(user) {
        setText("account-name", user.name);
        setText("account-email", user.email);
        setText("account-plan", planLabel(user.plan));
        setText("account-status", statusLabel(user.subscriptionStatus));
        setText("account-trial-days", `${user.trialDaysRemaining || 0} dias`);
        setText("account-trial-ends", user.trialEndsAt ? `Termina em ${formatDateTime(user.trialEndsAt)}` : "Sem trial ativo");
        setText("account-subscription-status", statusLabel(user.subscriptionStatus));
        setText("account-subscription-ends", user.subscriptionEndsAt ? `Termina em ${formatDateTime(user.subscriptionEndsAt)}` : "Gateway ainda não integrado");

        const badge = document.getElementById("account-access-badge");
        if (badge) {
            badge.textContent = user.accessMessage || "Acesso ativo";
            badge.classList.toggle("blocked", !user.accessAllowed);
        }

        const warning = document.getElementById("subscription-warning");
        warning?.classList.toggle("hidden", user.accessAllowed);
        setText("subscription-warning-title", user.accessMessage || "Acesso bloqueado");
        setText("subscription-warning-message", "As APIs financeiras ficam bloqueadas até regularizar o plano ou renovar o trial.");
    }

    function setText(id, value) {
        const element = document.getElementById(id);
        if (element) {
            element.textContent = value;
        }
    }

    function planLabel(plan) {
        const labels = {
            FREE: "Free",
            PRO: "Pro"
        };
        return labels[plan] || plan || "-";
    }

    function statusLabel(status) {
        const labels = {
            TRIALING: "Trial",
            ACTIVE: "Ativa",
            PAST_DUE: "Pagamento pendente",
            CANCELED: "Cancelada"
        };
        return labels[status] || status || "-";
    }

    function formatDateTime(value) {
        return new Intl.DateTimeFormat("pt-BR", {
            dateStyle: "short",
            timeStyle: "short"
        }).format(new Date(value));
    }

    function logout() {
        FinanceDashApi.clearToken();
        showLoggedOut();
        FinanceDashUi.showToast("Você saiu da sua conta.");
    }

    return {
        init,
        logout
    };
})();
