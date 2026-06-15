const FinanceDashDashboard = (() => {
    let expensesChart;
    let incomesChart;

    async function load(month, year) {
        const dashboard = await FinanceDashApi.getDashboard(month, year);
        renderCards(dashboard);
        renderCharts(dashboard);
        renderPeriod(dashboard);
        return dashboard;
    }

    function renderCards(dashboard) {
        document.getElementById("total-income").textContent = FinanceDashUi.formatCurrency(dashboard.totalIncome);
        document.getElementById("total-expense").textContent = FinanceDashUi.formatCurrency(dashboard.totalExpense);
        document.getElementById("balance").textContent = FinanceDashUi.formatCurrency(dashboard.balance);
        document.getElementById("income-count").textContent = `${dashboard.incomeCount} receitas`;
        document.getElementById("expense-count").textContent = `${dashboard.expenseCount} despesas`;
        document.getElementById("transaction-count").textContent = `${dashboard.transactionCount} lançamentos no mês`;
    }

    function renderPeriod(dashboard) {
        const text = `${FinanceDashUi.formatDate(dashboard.startDate)} - ${FinanceDashUi.formatDate(dashboard.endDate)}`;
        document.getElementById("expense-period").textContent = text;
        document.getElementById("income-period").textContent = text;
    }

    function renderCharts(dashboard) {
        expensesChart = renderCategoryChart(
            "expenses-chart",
            expensesChart,
            dashboard.expensesByCategory,
            "Despesas"
        );
        incomesChart = renderCategoryChart(
            "incomes-chart",
            incomesChart,
            dashboard.incomesByCategory,
            "Receitas"
        );
    }

    function renderCategoryChart(canvasId, currentChart, items, label) {
        const canvas = document.getElementById(canvasId);
        const context = canvas.getContext("2d");

        if (currentChart) {
            currentChart.destroy();
        }

        const labels = items.map((item) => `${item.categoryName} (${FinanceDashUi.formatPercent(item.percentage)})`);
        const data = items.map((item) => Number(item.amount));
        const colors = items.map((item) => item.categoryColor || "#38BDF8");

        return new Chart(context, {
            type: "doughnut",
            data: {
                labels,
                datasets: [{
                    label,
                    data,
                    backgroundColor: colors,
                    borderColor: "#0f172a",
                    borderWidth: 3
                }]
            },
            options: {
                plugins: {
                    legend: {
                        labels: {
                            color: "#e5e7eb"
                        },
                        position: "bottom"
                    },
                    tooltip: {
                        callbacks: {
                            label: (context) => `${context.label}: ${FinanceDashUi.formatCurrency(context.parsed)}`
                        }
                    }
                }
            }
        });
    }

    return {
        load
    };
})();

