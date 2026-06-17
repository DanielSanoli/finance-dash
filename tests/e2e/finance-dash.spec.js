const { test, expect } = require("@playwright/test");

async function loginAsDemo(page) {
    await page.goto("/");
    await expect(page.getByRole("heading", { name: "Entre ou crie sua conta" })).toBeVisible();
    await page.locator("#login-form input[name='email']").fill("demo@financedash.com");
    await page.locator("#login-form input[name='password']").fill("demo12345");
    await page.getByRole("button", { name: "Entrar" }).click();
    await expect(page.getByRole("heading", { name: "Conta e billing" })).toBeVisible({ timeout: 15_000 });
}

test.describe("FinanceDash frontend", () => {
    test("exige login antes de exibir o dashboard", async ({ page }) => {
        await page.goto("/");

        await expect(page.getByRole("heading", { name: "Entre ou crie sua conta" })).toBeVisible();
        await expect(page.locator("#app-content")).toHaveClass(/hidden/);
    });

    test("carrega dashboard após login demo", async ({ page }) => {
        await loginAsDemo(page);

        await expect(page.getByRole("heading", { name: "Dashboard financeiro" })).toBeVisible();
        await expect(page.getByRole("heading", { name: "Novo lançamento" })).toBeVisible();
        await expect(page.getByRole("heading", { name: "Categorias" })).toBeVisible();
        await expect(page.getByRole("heading", { name: "Nova meta" })).toBeVisible();
        await expect(page.getByLabel("Filtros de lançamentos")).toBeVisible();
    });

    test("permite alternar filtros de transações sem recarregar a página", async ({ page }) => {
        await loginAsDemo(page);

        await page.locator("#transaction-filter-type").selectOption("EXPENSE");
        await page.getByRole("button", { name: "Aplicar filtros" }).click();

        await expect(page.getByText(/registros/)).toBeVisible();
    });
});
