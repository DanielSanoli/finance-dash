const FinanceDashCategories = (() => {
    let categories = [];

    async function load() {
        categories = await FinanceDashApi.getCategories();
        renderTransactionCategoryOptions();
        return categories;
    }

    function getByType(type) {
        return categories.filter((category) => category.type === type && category.active);
    }

    function renderTransactionCategoryOptions() {
        const typeSelect = document.getElementById("transaction-type");
        const categorySelect = document.getElementById("transaction-category");
        if (!typeSelect || !categorySelect) return;

        const selectedType = typeSelect.value;
        const options = getByType(selectedType);

        categorySelect.innerHTML = options.length
            ? options.map((category) => `<option value="${category.id}">${category.name}</option>`).join("")
            : "<option value=\"\">Nenhuma categoria disponível</option>";
    }

    function bindEvents() {
        const typeSelect = document.getElementById("transaction-type");
        typeSelect?.addEventListener("change", renderTransactionCategoryOptions);
    }

    return {
        load,
        getByType,
        bindEvents,
        renderTransactionCategoryOptions
    };
})();

