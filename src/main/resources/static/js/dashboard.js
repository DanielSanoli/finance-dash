const FinanceDashDashboard = (() => {
    let expensesChart;
    let incomesChart;
    let expandedChart;
    let latestDashboard;

    async function load(month, year) {
        const dashboard = await FinanceDashApi.getDashboard(month, year);
        latestDashboard = dashboard;
        renderCards(dashboard);
        renderInsights(dashboard);
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

    function renderInsights(dashboard) {
        const totalIncome = Number(dashboard.totalIncome || 0);
        const totalExpense = Number(dashboard.totalExpense || 0);
        const balance = Number(dashboard.balance || 0);
        const transactionCount = Number(dashboard.transactionCount || 0);
        const savingsRate = totalIncome > 0 ? (balance / totalIncome) * 100 : 0;
        const averageTransaction = transactionCount > 0 ? (totalIncome + totalExpense) / transactionCount : 0;

        setInsightValue("savings-rate", FinanceDashUi.formatPercent(savingsRate), savingsRate >= 0 ? "positive" : "negative");
        document.getElementById("savings-rate-caption").textContent = savingsRate >= 0
            ? "Saldo positivo sobre receitas"
            : "Despesas acima das receitas";
        document.getElementById("average-transaction").textContent = FinanceDashUi.formatCurrency(averageTransaction);

        renderTopCategory("expense", dashboard.expensesByCategory);
        renderTopCategory("income", dashboard.incomesByCategory);
    }

    function setInsightValue(elementId, value, stateClass) {
        const valueElement = document.getElementById(elementId);
        const card = valueElement.closest(".insight-card");
        valueElement.textContent = value;
        card.classList.remove("positive", "negative");
        card.classList.add(stateClass);
    }

    function renderTopCategory(kind, categories) {
        const topCategory = [...(categories || [])]
            .sort((first, second) => Number(second.amount || 0) - Number(first.amount || 0))[0];
        const nameElement = document.getElementById(`top-${kind}-category`);
        const amountElement = document.getElementById(`top-${kind}-amount`);

        if (!topCategory) {
            nameElement.textContent = "-";
            amountElement.textContent = kind === "expense" ? "Sem despesas no período" : "Sem receitas no período";
            return;
        }

        nameElement.textContent = topCategory.categoryName;
        amountElement.textContent = `${FinanceDashUi.formatCurrency(topCategory.amount)} (${FinanceDashUi.formatPercent(topCategory.percentage)})`;
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
            "Despesas",
            true
        );
        incomesChart = renderCategoryChart(
            "incomes-chart",
            incomesChart,
            dashboard.incomesByCategory,
            "Receitas",
            true
        );
    }

    function renderCategoryChart(canvasId, currentChart, items, label, compact = false) {
        items = items || [];
        const canvas = document.getElementById(canvasId);
        const context = canvas.getContext("2d");

        if (currentChart) {
            currentChart.destroy();
        }

        const hasData = items.length > 0;
        const labels = hasData
            ? items.map((item) => `${item.categoryName} (${FinanceDashUi.formatPercent(item.percentage)})`)
            : ["Sem dados"];
        const data = hasData ? items.map((item) => Number(item.amount)) : [1];
        const colors = hasData ? items.map((item) => item.categoryColor || "#38BDF8") : ["#334155"];

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
                cutout: compact ? "68%" : "58%",
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: !compact,
                        labels: {
                            color: "#e5e7eb"
                        },
                        position: "bottom"
                    },
                    tooltip: {
                        enabled: hasData,
                        callbacks: {
                            label: (context) => `${context.label}: ${FinanceDashUi.formatCurrency(context.parsed)}`
                        }
                    }
                }
            }
        });
    }

    function openExpandedChart(chartType) {
        if (!latestDashboard) return;

        const config = chartType === "expenses"
            ? {
                title: "Despesas por categoria",
                items: latestDashboard.expensesByCategory,
                label: "Despesas",
                period: document.getElementById("expense-period").textContent
            }
            : {
                title: "Receitas por categoria",
                items: latestDashboard.incomesByCategory,
                label: "Receitas",
                period: document.getElementById("income-period").textContent
            };

        document.getElementById("chart-modal-title").textContent = config.title;
        document.getElementById("chart-modal-subtitle").textContent = config.period;
        expandedChart = renderCategoryChart("expanded-chart", expandedChart, config.items, config.label, false);

        const modal = document.getElementById("chart-modal");
        modal.classList.add("show");
        modal.setAttribute("aria-hidden", "false");
    }

    function closeExpandedChart() {
        const modal = document.getElementById("chart-modal");
        modal?.classList.remove("show");
        modal?.setAttribute("aria-hidden", "true");
    }

    function bindEvents() {
        document.querySelectorAll(".chart-expand").forEach((button) => {
            button.addEventListener("click", () => openExpandedChart(button.dataset.chart));
        });
        document.getElementById("chart-modal-close")?.addEventListener("click", closeExpandedChart);
        document.getElementById("chart-modal")?.addEventListener("click", (event) => {
            if (event.target.id === "chart-modal") {
                closeExpandedChart();
            }
        });
    }

    return {
        load,
        bindEvents
    };
})();

