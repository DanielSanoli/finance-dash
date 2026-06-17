const FinanceDashApp = (() => {
    let initialized = false;

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

    async function refresh() {
        const { month, year } = getSelectedPeriod();
        FinanceDashGoals.syncFormPeriod();
        await Promise.all([
            FinanceDashDashboard.load(month, year),
            FinanceDashTransactions.load(month, year),
            FinanceDashGoals.load(month, year)
        ]);
    }

    async function start() {
        if (initialized) {
            await refresh();
            return;
        }
        initialized = true;
        initPeriodControls();
        FinanceDashTransactions.setDefaultDate();
        FinanceDashCategories.bindEvents(refresh);
        FinanceDashTransactions.bindEvents(refresh);
        FinanceDashGoals.bindEvents(refresh, getSelectedPeriod);
        FinanceDashDashboard.bindEvents();
        document.getElementById("reload-dashboard").addEventListener("click", async () => {
            const button = document.getElementById("reload-dashboard");
            try {
                FinanceDashUi.setButtonLoading(button, true, "Atualizando...");
                await refresh();
                FinanceDashUi.showToast("Dashboard atualizado.");
            } catch (error) {
                FinanceDashUi.showToast(error.message);
            } finally {
                FinanceDashUi.setButtonLoading(button, false);
            }
        });

        await refresh();
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

    return {
        start,
        boot
    };
})();

document.addEventListener("DOMContentLoaded", FinanceDashApp.boot);

