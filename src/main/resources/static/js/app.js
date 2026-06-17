const FinanceDashApp = (() => {
    let initialized = false;
    let activeView = "dashboard";

    const VIEW_COPY = {
        dashboard: {
            title: "Dashboard financeiro",
            subtitle: "Controle receitas, despesas, categorias e metas em uma visão mensal."
        },
        radar: {
            title: "Radar financeiro",
            subtitle: "Projeção do mês, alertas de risco e copiloto que sugere ações."
        }
    };

    function initPeriodControls() {
        const monthSelect = document.getElementById("month");
        const yearInput = document.getElementById("year");
        const currentDate = new Date();
        const months = [
            "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
        ];

        monthSelect.innerHTML = months.map((month, index) => {
            const value = index + 1;
            return `<option value="${value}">${month}</option>`;
        }).join("");

        monthSelect.value = String(currentDate.getMonth() + 1);
        yearInput.value = String(currentDate.getFullYear());
    }

    function getSelectedPeriod() {
        return {
            month: Number(document.getElementById("month").value),
            year: Number(document.getElementById("year").value)
        };
    }

    function updateHeaderCopy() {
        const copy = VIEW_COPY[activeView] || VIEW_COPY.dashboard;
        setText("app-page-title", copy.title);
        setText("app-page-subtitle", copy.subtitle);
    }

    function setActiveView(view) {
        activeView = view;
        document.getElementById("dashboard-view")?.classList.toggle("hidden", view !== "dashboard");
        document.getElementById("radar-view")?.classList.toggle("hidden", view !== "radar");
        document.getElementById("period-filter")?.classList.toggle("hidden", view !== "dashboard");
        document.getElementById("radar-period")?.classList.toggle("hidden", view !== "radar");

        if (view === "radar") {
            updateRadarPeriodLabel();
        }

        document.querySelectorAll("[data-app-view]").forEach((tab) => {
            tab.classList.toggle("active", tab.dataset.appView === view);
        });

        updateHeaderCopy();
    }

    async function refreshDashboard() {
        const { month, year } = getSelectedPeriod();
        FinanceDashGoals.syncFormPeriod();
        await Promise.all([
            FinanceDashDashboard.load(month, year),
            FinanceDashTransactions.load(month, year),
            FinanceDashGoals.load(month, year)
        ]);
    }

    async function refreshRadar() {
        await FinanceDashRadar.load();
    }

    async function refreshActiveView() {
        if (activeView === "radar") {
            await refreshRadar();
            return;
        }
        await refreshDashboard();
    }

    function bindNavigation() {
        document.querySelectorAll("[data-app-view]").forEach((tab) => {
            tab.addEventListener("click", async () => {
                const view = tab.dataset.appView;
                if (view === activeView) {
                    return;
                }
                setActiveView(view);
                try {
                    await refreshActiveView();
                } catch (error) {
                    FinanceDashUi.showToast(error.message);
                }
            });
        });
    }

    async function start() {
        if (initialized) {
            await refreshActiveView();
            return;
        }
        initialized = true;
        initPeriodControls();
        bindNavigation();
        setActiveView("dashboard");
        FinanceDashTransactions.setDefaultDate();
        FinanceDashCategories.bindEvents(refreshDashboard);
        FinanceDashTransactions.bindEvents(refreshDashboard);
        FinanceDashGoals.bindEvents(refreshDashboard, getSelectedPeriod);
        FinanceDashDashboard.bindEvents();
        FinanceDashRadar.bindEvents(refreshRadar);

        document.getElementById("reload-dashboard").addEventListener("click", async () => {
            const button = document.getElementById("reload-dashboard");
            try {
                FinanceDashUi.setButtonLoading(button, true, "Atualizando...");
                await refreshDashboard();
                FinanceDashUi.showToast("Dashboard atualizado.");
            } catch (error) {
                FinanceDashUi.showToast(error.message);
            } finally {
                FinanceDashUi.setButtonLoading(button, false);
            }
        });

        await refreshDashboard();
    }

    async function boot() {
        try {
            await FinanceDashAuth.init(async () => {
                await FinanceDashCategories.load();
                await start();
            });
        } catch (error) {
            FinanceDashUi.showToast(error.message);
            console.error(error);
        }
    }

    function updateRadarPeriodLabel() {
        const months = [
            "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
        ];
        const now = new Date();
        const label = `${months[now.getMonth()]}/${now.getFullYear()}`;
        setText("radar-period-label", label);
    }

    function setText(id, value) {
        const element = document.getElementById(id);
        if (element) {
            element.textContent = value;
        }
    }

    return {
        start,
        boot
    };
})();

document.addEventListener("DOMContentLoaded", FinanceDashApp.boot);
