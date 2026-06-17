const FinanceDashAuth = (() => {
    let onAuthenticated = async () => {};
    let currentUser = null;

    async function init(callback) {
        onAuthenticated = callback;
        bindEvents();
        window.addEventListener("financedash:unauthorized", () => {
            showLoggedOut();
            FinanceDashUi.showToast("Sessão expirada. Faça login novamente.");
        });
        window.addEventListener("financedash:subscription-required", refreshAccountStatus);

        await handleAuthQueryParams();

        if (!FinanceDashApi.getToken()) {
            showLoggedOut();
            return;
        }

        try {
            const user = await FinanceDashApi.me();
            await showLoggedIn(user);
        } catch (error) {
            FinanceDashApi.clearSession();
            showLoggedOut();
        }
    }

    function toggleAccountPanel(open) {
        const panel = document.getElementById("account-panel");
        if (!panel) {
            return;
        }

        if (open === undefined) {
            open = panel.classList.contains("hidden");
        }

        panel.classList.toggle("hidden", !open);
        if (open) {
            panel.scrollIntoView({ behavior: "smooth", block: "start" });
        }
    }

    function bindEvents() {
        document.getElementById("login-tab")?.addEventListener("click", () => showAuthForm("login"));
        document.getElementById("register-tab")?.addEventListener("click", () => showAuthForm("register"));
        document.getElementById("forgot-tab")?.addEventListener("click", () => showAuthForm("forgot"));
        document.getElementById("forgot-back")?.addEventListener("click", () => showAuthForm("login"));
        document.getElementById("login-form")?.addEventListener("submit", handleLogin);
        document.getElementById("register-form")?.addEventListener("submit", handleRegister);
        document.getElementById("forgot-form")?.addEventListener("submit", handleForgotPassword);
        document.getElementById("reset-form")?.addEventListener("submit", handleResetPassword);
        document.getElementById("account-button")?.addEventListener("click", () => {
            toggleAccountPanel(true);
        });
        document.getElementById("account-close-button")?.addEventListener("click", () => {
            toggleAccountPanel(false);
        });
        document.getElementById("logout-button")?.addEventListener("click", logout);
        document.getElementById("checkout-pro-button")?.addEventListener("click", handleCheckoutPro);
    }

    async function handleAuthQueryParams() {
        const params = new URLSearchParams(window.location.search);
        const verifyToken = params.get("verify");
        const resetToken = params.get("reset");

        if (verifyToken) {
            try {
                const response = await FinanceDashApi.verifyEmail(verifyToken);
                FinanceDashUi.showToast(response.message || "Email verificado.");
            } catch (error) {
                FinanceDashUi.showToast(error.message);
            } finally {
                clearAuthQueryParams();
            }
            return;
        }

        if (resetToken) {
            showLoggedOut();
            showAuthForm("reset");
            document.getElementById("reset-token").value = resetToken;
            clearAuthQueryParams();
        }
    }

    function clearAuthQueryParams() {
        const url = new URL(window.location.href);
        url.searchParams.delete("verify");
        url.searchParams.delete("reset");
        window.history.replaceState({}, "", url.pathname + url.search + url.hash);
    }

    function showAuthForm(formName) {
        const forms = {
            login: document.getElementById("login-form"),
            register: document.getElementById("register-form"),
            forgot: document.getElementById("forgot-form"),
            reset: document.getElementById("reset-form")
        };

        Object.entries(forms).forEach(([name, element]) => {
            element?.classList.toggle("hidden", name !== formName);
        });

        document.getElementById("login-tab")?.classList.toggle("button-secondary", formName !== "login");
        document.getElementById("register-tab")?.classList.toggle("button-secondary", formName !== "register");
    }

    async function handleLogin(event) {
        event.preventDefault();
        await submitAuthForm(event.currentTarget, "login-submit", "login-feedback", FinanceDashApi.login);
    }

    async function handleRegister(event) {
        event.preventDefault();
        await submitAuthForm(event.currentTarget, "register-submit", "register-feedback", FinanceDashApi.register);
    }

    async function handleForgotPassword(event) {
        event.preventDefault();
        const button = document.getElementById("forgot-submit");
        const feedback = document.getElementById("forgot-feedback");
        const formData = new FormData(event.currentTarget);
        const payload = Object.fromEntries(formData.entries());

        try {
            FinanceDashUi.setButtonLoading(button, true, "Enviando...");
            FinanceDashUi.setFeedback(feedback, "");
            const response = await FinanceDashApi.forgotPassword(payload);
            FinanceDashUi.setFeedback(feedback, response.message, "success");
        } catch (error) {
            FinanceDashUi.setFeedback(feedback, error.message, "error");
        } finally {
            FinanceDashUi.setButtonLoading(button, false);
        }
    }

    async function handleResetPassword(event) {
        event.preventDefault();
        const button = document.getElementById("reset-submit");
        const feedback = document.getElementById("reset-feedback");
        const formData = new FormData(event.currentTarget);
        const payload = Object.fromEntries(formData.entries());

        try {
            FinanceDashUi.setButtonLoading(button, true, "Salvando...");
            FinanceDashUi.setFeedback(feedback, "");
            const response = await FinanceDashApi.resetPassword(payload);
            FinanceDashUi.setFeedback(feedback, response.message, "success");
            FinanceDashUi.showToast("Senha atualizada. Faça login.");
            showAuthForm("login");
        } catch (error) {
            FinanceDashUi.setFeedback(feedback, error.message, "error");
        } finally {
            FinanceDashUi.setButtonLoading(button, false);
        }
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
            FinanceDashApi.storeAuthResponse(response);
            await showLoggedIn(response.user);
            FinanceDashUi.showToast("Acesso liberado.");
        } catch (error) {
            FinanceDashUi.setFeedback(feedback, error.message, "error");
        } finally {
            FinanceDashUi.setButtonLoading(button, false);
        }
    }

    async function handleCheckoutPro() {
        const button = document.getElementById("checkout-pro-button");
        const feedback = document.getElementById("checkout-feedback");

        try {
            FinanceDashUi.setButtonLoading(button, true, "Gerando checkout...");
            FinanceDashUi.setFeedback(feedback, "");
            const response = await FinanceDashApi.checkoutPro();

            if (response.checkoutUrl) {
                window.open(response.checkoutUrl, "_blank", "noopener,noreferrer");
            }

            FinanceDashUi.setFeedback(feedback, response.message, response.checkoutUrl ? "success" : "");
            FinanceDashUi.showToast(response.message);
            await refreshAccountStatus();
        } catch (error) {
            FinanceDashUi.setFeedback(feedback, error.message, "error");
        } finally {
            FinanceDashUi.setButtonLoading(button, false);
        }
    }

    async function refreshAccountStatus() {
        if (!FinanceDashApi.getToken()) {
            return;
        }

        try {
            const user = await FinanceDashApi.me();
            currentUser = user;
            renderAccount(user);
        } catch (error) {
            // Mantém o usuário logado para visualizar status da conta.
        }
    }

    async function showLoggedIn(user) {
        currentUser = user;
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
        currentUser = null;
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
        setText("account-subscription-ends", user.subscriptionEndsAt
            ? `Termina em ${formatDateTime(user.subscriptionEndsAt)}`
            : "Aguardando confirmação de pagamento");
        setText("account-email-verified", user.emailVerified ? "Verificado" : "Pendente de verificação");

        const badge = document.getElementById("account-access-badge");
        if (badge) {
            badge.textContent = user.accessMessage || "Acesso ativo";
            badge.classList.toggle("blocked", !user.accessAllowed);
        }

        const checkoutButton = document.getElementById("checkout-pro-button");
        if (checkoutButton) {
            const proActive = user.plan === "PRO" && user.subscriptionStatus === "ACTIVE";
            checkoutButton.disabled = proActive;
            checkoutButton.textContent = proActive ? "Plano Pro ativo" : "Assinar Pro";
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
        FinanceDashApi.clearSession();
        showLoggedOut();
        FinanceDashUi.showToast("Você saiu da sua conta.");
    }

    return {
        init,
        logout
    };
})();
