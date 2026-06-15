const FinanceDashApp = (() => {
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
        initPeriodControls();
        FinanceDashTransactions.setDefaultDate();
        FinanceDashCategories.bindEvents(refresh);
        FinanceDashTransactions.bindEvents(refresh);
        FinanceDashGoals.bindEvents(refresh, getSelectedPeriod);
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

        try {
            await FinanceDashCategories.load();
            await refresh();
        } catch (error) {
            FinanceDashUi.showToast(error.message);
            console.error(error);
        }
    }

    return {
        start
    };
})();

document.addEventListener("DOMContentLoaded", FinanceDashApp.start);

