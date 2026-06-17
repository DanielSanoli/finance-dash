const FinanceDashSettings = (() => {
    let cached = null;

    function isConfigured(settings) {
        if (!settings) {
            return false;
        }
        return [
            settings.monthlyReserveTarget,
            settings.monthlyFixedCost,
            settings.billableHoursPerMonth,
            settings.monthlyIncomeGoal
        ].some((value) => Number(value || 0) > 0);
    }

    function percentFromRate(rate) {
        const numeric = Number(rate || 0);
        if (!numeric) {
            return "";
        }
        return String(Number((numeric * 100).toFixed(2)));
    }

    function rateFromPercent(percent) {
        const numeric = Number(percent);
        if (!Number.isFinite(numeric) || numeric <= 0) {
            return null;
        }
        return Number((numeric / 100).toFixed(4));
    }

    async function load() {
        cached = await FinanceDashApi.getUserSettings();
        return cached;
    }

    function getCached() {
        return cached;
    }

    function fillForm(settings) {
        const form = document.getElementById("settings-form");
        if (!form || !settings) {
            return;
        }

        form.elements.monthlyIncomeGoal.value = settings.monthlyIncomeGoal ?? "";
        form.elements.monthlyReserveTarget.value = settings.monthlyReserveTarget ?? "";
        form.elements.monthlyFixedCost.value = settings.monthlyFixedCost ?? "";
        form.elements.billableHoursPerMonth.value = settings.billableHoursPerMonth ?? "";
        form.elements.taxRatePercent.value = percentFromRate(settings.taxRate);
        form.elements.desiredMarginPercent.value = percentFromRate(settings.desiredMargin);
        form.elements.digestFrequency.value = settings.digestFrequency || "WEEKLY";
    }

    function buildPayload(formData) {
        return {
            monthlyIncomeGoal: numberOrNull(formData.get("monthlyIncomeGoal")),
            monthlyReserveTarget: numberOrNull(formData.get("monthlyReserveTarget")),
            monthlyFixedCost: numberOrNull(formData.get("monthlyFixedCost")),
            billableHoursPerMonth: numberOrNull(formData.get("billableHoursPerMonth")),
            taxRate: rateFromPercent(formData.get("taxRatePercent")),
            desiredMargin: rateFromPercent(formData.get("desiredMarginPercent")),
            digestFrequency: formData.get("digestFrequency") || "WEEKLY"
        };
    }

    function numberOrNull(value) {
        if (value === null || value === undefined || value === "") {
            return null;
        }
        const numeric = Number(value);
        return Number.isFinite(numeric) ? numeric : null;
    }

    function bindEvents(onSaved) {
        const form = document.getElementById("settings-form");
        form?.addEventListener("submit", async (event) => {
            event.preventDefault();
            const button = document.getElementById("settings-submit");
            const feedback = document.getElementById("settings-feedback");
            const formData = new FormData(form);

            try {
                FinanceDashUi.setButtonLoading(button, true, "Salvando...");
                FinanceDashUi.setFeedback(feedback, "");
                cached = await FinanceDashApi.updateUserSettings(buildPayload(formData));
                fillForm(cached);
                FinanceDashUi.setFeedback(feedback, "Perfil financeiro salvo.", "success");
                FinanceDashUi.showToast("Perfil financeiro atualizado.");
                await onSaved(cached);
            } catch (error) {
                FinanceDashUi.setFeedback(feedback, error.message, "error");
            } finally {
                FinanceDashUi.setButtonLoading(button, false);
            }
        });
    }

    return {
        load,
        getCached,
        isConfigured,
        fillForm,
        bindEvents
    };
})();
