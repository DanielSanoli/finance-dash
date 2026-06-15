const { test, expect } = require("@playwright/test");

test.describe("FinanceDash frontend", () => {
    test("carrega dashboard e exibe fluxos principais do MVP", async ({ page }) => {
        await page.goto("/");

        await expect(page.getByRole("heading", { name: "Dashboard financeiro" })).toBeVisible();
        await expect(page.getByRole("heading", { name: "Novo lançamento" })).toBeVisible();
        await expect(page.getByRole("heading", { name: "Categorias" })).toBeVisible();
        await expect(page.getByRole("heading", { name: "Nova meta" })).toBeVisible();
        await expect(page.getByLabel("Filtros de lançamentos")).toBeVisible();
    });

    test("permite alternar filtros de transações sem recarregar a página", async ({ page }) => {
        await page.goto("/");

        await page.locator("#transaction-filter-type").selectOption("EXPENSE");
        await page.getByRole("button", { name: "Aplicar filtros" }).click();

        await expect(page.getByText(/registros/)).toBeVisible();
    });
});
