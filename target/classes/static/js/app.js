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
        await Promise.all([
            FinanceDashDashboard.load(month, year),
            FinanceDashTransactions.load(month, year),
            FinanceDashGoals.load(month, year)
        ]);
    }

    async function start() {
        initPeriodControls();
        FinanceDashTransactions.setDefaultDate();
        FinanceDashCategories.bindEvents();
        FinanceDashTransactions.bindEvents(refresh);
        document.getElementById("reload-dashboard").addEventListener("click", async () => {
            try {
                await refresh();
                FinanceDashUi.showToast("Dashboard atualizado.");
            } catch (error) {
                FinanceDashUi.showToast(error.message);
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

